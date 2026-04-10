package com.mateyou.duedate.notifications

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.mateyou.duedate.MainActivity
import com.mateyou.duedate.R
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.SmsDiagnosticLogger
import com.mateyou.duedate.BillActionReceiver
import com.mateyou.duedate.data.isOverdue
import java.util.*
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val sharedPrefs = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE)

    fun scheduleRemindersForBill(bill: DueDate) {
        val remindersEnabled = sharedPrefs.getBoolean("payment_reminders_enabled", true)
        
        // If reminders are disabled, bill is paid, or deleted, ensure all existing alarms are cancelled
        if (!remindersEnabled || bill.isPaid || bill.isDeleted) {
            cancelAllAlarmsForBill(bill.id)
            return
        }

        val reminderTime = sharedPrefs.getString("reminder_time", "08:00") ?: "08:00"
        val timeParts = reminderTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val dayOptions = mapOf(
            "on_due_day" to 0,
            "1_day_before" to 1,
            "2_days_before" to 2,
            "3_days_before" to 3,
            "4_days_before" to 4,
            "5_days_before" to 5,
            "6_days_before" to 6,
            "1_week_before" to 7
        )

        val scheduledLogs = mutableListOf<String>()
        val dateFormatter = SimpleDateFormat("dd MMM hh:mm a", Locale.US)

        dayOptions.forEach { (key, daysBack) ->
            val isDefault = (key == "on_due_day" || key == "5_days_before")
            if (sharedPrefs.getBoolean("reminder_day_$key", isDefault)) {
                val alarmTime = scheduleAlarm(bill, daysBack, hour, minute)
                if (alarmTime != null) {
                    scheduledLogs.add("- $key: ${dateFormatter.format(Date(alarmTime)).uppercase()}")
                } else {
                    scheduledLogs.add("- $key: SKIPPED (Past time)")
                }
            } else {
                // If a specific reminder day is disabled, cancel it just in case
                cancelAlarm(bill.id, daysBack)
            }
        }
        
        // Handle Overdue Daily Alarms
        if (isOverdue(bill.dueDate)) {
            val alarmTime = scheduleAlarm(bill, -1, hour, minute) // -1 flag for overdue
            if (alarmTime != null) {
                scheduledLogs.add("- daily_overdue: ${dateFormatter.format(Date(alarmTime)).uppercase()}")
            }
        } else {
            cancelAlarm(bill.id, -1)
        }
        
        val daysList = scheduledLogs.filter { !it.contains("SKIPPED") }.map { 
            val prefix = it.split(": ")[0].substring(2)
            when (prefix) {
                "on_due_day" -> "due day"
                "1_day_before" -> "1 day before"
                "1_week_before" -> "1 week before"
                "daily_overdue" -> "daily overdue"
                else -> "${prefix.split("_")[0]} days before"
            }
        }
        val daysStr = if (daysList.isEmpty()) "None" else daysList.joinToString(", ")
        
        SmsDiagnosticLogger.logActivity(context, bill.bankName, bill.cardName, bill.cardNumber, "Payment Reminders set for $daysStr at ${sharedPrefs.getString("reminder_time", "08:00")}", bill.currencySymbol)
        SmsDiagnosticLogger.resetSchedulerLog(context, bill.customName ?: bill.bankName, scheduledLogs)
    }
    
    private fun scheduleAlarm(bill: DueDate, daysBack: Int, hour: Int, minute: Int): Long? {
        val targetReminder = Calendar.getInstance().apply {
            if (daysBack == -1) {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                val billCal = Calendar.getInstance().apply { timeInMillis = bill.dueDate }
                set(Calendar.YEAR, billCal.get(Calendar.YEAR))
                set(Calendar.MONTH, billCal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, billCal.get(Calendar.DAY_OF_MONTH))
                add(Calendar.DAY_OF_YEAR, -daysBack)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val now = Calendar.getInstance()

        if (targetReminder.after(now)) {
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("bill_id", bill.id)
                putExtra("bill_name", bill.customName ?: bill.bankName)
                putExtra("card_name", bill.cardName)
                putExtra("amount", bill.amount)
                putExtra("currency_symbol", bill.currencySymbol)
                putExtra("due_date", bill.dueDate)
                putExtra("is_overdue_reminder", daysBack == -1)
                action = "com.mateyou.duedate.REMINDER_${bill.id}_$daysBack"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                bill.id * 100 + (if (daysBack == -1) 99 else daysBack),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetReminder.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetReminder.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetReminder.timeInMillis, pendingIntent)
            }
            return targetReminder.timeInMillis
        }
        return null
    }

    private fun cancelAlarm(billId: Int, daysBack: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.mateyou.duedate.REMINDER_${billId}_$daysBack"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            billId * 100 + (if (daysBack == -1) 99 else daysBack),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAllAlarmsForBill(billId: Int) {
        val daysBackOptions = listOf(-1, 0, 1, 2, 3, 4, 5, 6, 7)
        daysBackOptions.forEach { daysBack ->
            cancelAlarm(billId, daysBack)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getIntExtra("bill_id", -1)
        if (billId == -1) return

        val data = Data.Builder()
            .putInt("bill_id", billId)
            .putString("bill_name", intent.getStringExtra("bill_name") ?: "Bill")
            .putString("card_name", intent.getStringExtra("card_name"))
            .putDouble("amount", intent.getDoubleExtra("amount", 0.0))
            .putString("currency_symbol", intent.getStringExtra("currency_symbol") ?: "₹")
            .putLong("due_date", intent.getLongExtra("due_date", 0L))
            .putBoolean("is_overdue_reminder", intent.getBooleanExtra("is_overdue_reminder", false))
            .build()

        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    companion object {
        private val notificationMutex = Mutex()
        private var lastNotificationTime = 0L
    }

    override suspend fun doWork(): Result {
        val billId = inputData.getInt("bill_id", -1)
        val billName = inputData.getString("bill_name") ?: "Bill"
        val cardName = inputData.getString("card_name")
        val amount = inputData.getDouble("amount", 0.0)
        val currencySymbol = inputData.getString("currency_symbol") ?: "₹"
        val dueDate = inputData.getLong("due_date", 0L)
        val isOverdueReminder = inputData.getBoolean("is_overdue_reminder", false)

        val db = AppDatabase.getDatabase(applicationContext)
        val bill = db.dueDateDao().getDueDateSync(billId)

        if (bill != null && !bill.isPaid && !bill.isDeleted && !bill.isArchived) {
            notificationMutex.withLock {
                val now = System.currentTimeMillis()
                val elapsed = now - lastNotificationTime
                
                // If the last notification was shown less than 2 seconds ago, wait to avoid spamming
                if (elapsed < 2000) {
                    delay(2000 - elapsed)
                }

                // Check overdue status using standard logic
                val overdue = isOverdue(bill.dueDate)
                
                if (isOverdueReminder || overdue) {
                    showNotification(billId, billName, cardName, amount, currencySymbol, dueDate, isActuallyOverdue = true)
                    // Reschedule for tomorrow if still overdue
                    NotificationScheduler(applicationContext).scheduleRemindersForBill(bill)
                } else {
                    showNotification(billId, billName, cardName, amount, currencySymbol, dueDate, isActuallyOverdue = false)
                }
                
                lastNotificationTime = System.currentTimeMillis()
                SmsDiagnosticLogger.appendSchedulerLog(applicationContext, billName, "ALARM TRIGGERED: Showing notification for bill $billId")
            }
        } else {
            val reason = when {
                bill == null -> "Bill not found"
                bill.isPaid -> "Bill already paid"
                bill.isDeleted -> "Bill is deleted"
                bill.isArchived -> "Bill is archived"
                else -> "Condition not met"
            }
            SmsDiagnosticLogger.appendSchedulerLog(applicationContext, billName, "ALARM TRIGGERED: Notification skipped - $reason")
            // Cleanup just in case
            NotificationScheduler(applicationContext).cancelAllAlarmsForBill(billId)
        }

        return Result.success()
    }

    private fun showNotification(billId: Int, name: String, cardName: String?, amount: Double, currencySymbol: String, dueDate: Long, isActuallyOverdue: Boolean) {
        val channelId = if (isActuallyOverdue) "overdue_bills" else "payment_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nameStr = if (isActuallyOverdue) "Overdue Bill Alerts" else "Payment Reminders"
            val channel = NotificationChannel(
                channelId, 
                nameStr, 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isActuallyOverdue) "Daily alerts for overdue bills" else "Reminders for upcoming bill payments"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isActuallyOverdue) "Overdue Bill Alert" else "Bill Reminder"
        
        val dateLabel = if (isActuallyOverdue) {
            val today = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val billDueDay = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val diffDays = ((today - billDueDay) / (1000 * 60 * 60 * 24)).toInt()
            if (diffDays <= 0) "just recently" 
            else if (diffDays == 1) "yesterday" 
            else "$diffDays days ago"
        } else {
            if (android.text.format.DateUtils.isToday(dueDate)) {
                "today"
            } else {
                "on " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(dueDate))
            }
        }

        val cardStr = if (cardName != null) " $cardName" else ""
        val message = if (isActuallyOverdue) {
            "Your $name$cardStr bill of $currencySymbol$amount is OVERDUE (Due was $dateLabel)"
        } else {
            "Your $name$cardStr bill of $currencySymbol$amount is due $dateLabel"
        }

        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAV_DESTINATION", "bills")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext, 
            channelId.hashCode(), 
            contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val paidIntent = Intent(applicationContext, BillActionReceiver::class.java).apply {
            action = "com.mateyou.duedate.ACTION_MARK_PAID"
            putExtra("bill_id", billId)
            putExtra("notification_id", notificationId)
        }
        val paidPendingIntent = PendingIntent.getBroadcast(
            applicationContext, 
            billId + 1000, // Unique request code
            paidIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_credit_score)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "MARK AS PAID", paidPendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
