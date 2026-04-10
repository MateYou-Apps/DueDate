package com.mateyou.duedate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.DueDateDao
import com.mateyou.duedate.notifications.NotificationScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BillDetectionProcessor {
    private val processingMutex = Mutex()

    suspend fun processMessage(context: Context, sender: String, body: String, timestamp: Long, isManual: Boolean = false) {
        if (!isManual && !BankConfig.isBankSender(sender)) return

        val database = AppDatabase.getDatabase(context)
        val dao = database.dueDateDao()

        val parsed = SmsParser.parse(context, body, timestamp, if (isManual) null else sender)
        if (parsed != null) {
            processParsedBill(context, dao, parsed, isManual, body)
        }
    }

    /**
     * Processes a parsed bill. 
     * Returns true if a new bill was added, false if it was a duplicate.
     */
    suspend fun processParsedBill(context: Context, dao: DueDateDao, parsed: DueDate, isManual: Boolean, rawBody: String? = null): Boolean {
        processingMutex.withLock {
            // If manual, clean up card name to remove redundant bank name prefix/suffix
            val processedBill = if (isManual && parsed.cardName != null) {
                var cleanedName = parsed.cardName
                val bankName = parsed.bankName.lowercase()
                
                // Remove bank name if it's at the start or end of the card name
                if (cleanedName.lowercase().startsWith(bankName)) {
                    cleanedName = cleanedName.substring(bankName.length).trim()
                } else if (cleanedName.lowercase().endsWith(bankName)) {
                    cleanedName = cleanedName.substring(0, cleanedName.length - bankName.length).trim()
                }
                
                // If the entire card name was just the bank name OR is just digits, set to null to avoid redundancy
                if (cleanedName.isEmpty() || cleanedName.equals(parsed.bankName, ignoreCase = true) || cleanedName.all { it.isDigit() }) {
                    parsed.copy(cardName = null)
                } else {
                    parsed.copy(cardName = cleanedName)
                }
            } else {
                parsed
            }

            // Match logic: Match bank, card name, and card number against ALL non-deleted bills
            val duplicate = dao.findDuplicate(
                processedBill.bankName, 
                processedBill.cardName, 
                processedBill.cardNumber, 
                processedBill.amount, 
                processedBill.dueDate
            )
            
            if (duplicate == null) {
                // Check if we should carry over a custom name from a previous bill of the same card
                val existingCustomName = dao.findExistingCustomName(
                    processedBill.bankName, 
                    processedBill.cardName, 
                    processedBill.cardNumber
                )
                
                val autoPayZeroEnabled = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE).getBoolean("auto_pay_zero_bills", true)
                val isPaidAutomatically = autoPayZeroEnabled && processedBill.amount <= 0.0

                val billToInsert = processedBill.copy(
                    customName = existingCustomName,
                    isPaid = isPaidAutomatically,
                    paidAt = if (isPaidAutomatically) System.currentTimeMillis() else null
                )
                
                val insertedId = dao.insert(billToInsert)
                val amountStr = "${billToInsert.currencySymbol}${String.format(Locale.getDefault(), "%.2f", billToInsert.amount)}"
                val activityMsg = if (isManual) {
                    "Manual Bill Added ($amountStr)"
                } else {
                    if (isPaidAutomatically) "New Bill Detected & Marked as Paid ($amountStr)" else "New Bill Detected ($amountStr)"
                }
                
                SmsDiagnosticLogger.logActivity(context, billToInsert.bankName, billToInsert.cardName, billToInsert.cardNumber, activityMsg, billToInsert.currencySymbol)
                
                // Schedule reminders using the official database ID if NOT paid
                if (!isPaidAutomatically) {
                    val billWithId = billToInsert.copy(id = insertedId.toInt())
                    NotificationScheduler(context).scheduleRemindersForBill(billWithId)
                }

                // Show notification for auto-detected bills
                val billDetectionEnabled = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE).getBoolean("bill_detection_enabled", true)
                if (billDetectionEnabled) {
                    val cardStr = if (billToInsert.cardName != null) " ${billToInsert.cardName}" else ""
                    if (isPaidAutomatically) {
                        delay(1000)
                        showNotification(
                            context, 
                            "Bill Marked as Paid Automatically", 
                            "${billToInsert.bankName}$cardStr bill of ${billToInsert.currencySymbol}${billToInsert.amount} was marked as paid.", 
                            "bill_auto_paid",
                            billId = insertedId.toInt()
                        )
                    } else if (!isManual) {
                        val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(billToInsert.dueDate))
                        val message = "${billToInsert.bankName}$cardStr: ${billToInsert.currencySymbol}${billToInsert.amount} (Due on $dateStr)"
                        showNotification(context, "New Bill Detected", message, "bill_alerts")
                    }
                }
                return true
            } else {
                // It's a duplicate of a non-deleted bill - Move to Trash
                val trashDuplicate = processedBill.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), isNewDuplicate = true)
                dao.insert(trashDuplicate)
                
                // Always show duplicate notification if enabled (Manual OR Auto)
                val billDetectionEnabled = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE).getBoolean("bill_detection_enabled", true)
                if (billDetectionEnabled) {
                    delay(500)
                    val bank = processedBill.bankName
                    val cardName = if (processedBill.cardName != null) " ${processedBill.cardName}" else ""
                    val cardNum = if (processedBill.cardNumber != null) " ${processedBill.cardNumber}" else ""
                    val message = "$bank$cardName$cardNum bill was detected as duplicate and moved to trash automatically."
                    showNotification(context, "Duplicate Bill Trashed", message, "bill_duplicates")
                }
                return false
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, channelId: String, billId: Int? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when(channelId) {
                "bill_alerts" -> "Bill Alerts"
                "bill_auto_paid" -> "Auto-Paid Bills"
                "bill_duplicates" -> "Duplicate Bill Alerts"
                "payment_reminders" -> "Payment Reminders"
                "overdue_bills" -> "Overdue Bill Alerts"
                else -> "General"
            }
            
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for $channelName"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            when (channelId) {
                "bill_alerts" -> putExtra("NAV_DESTINATION", "bills")
                "bill_auto_paid" -> putExtra("NAV_DESTINATION", "paid_bills")
                "bill_duplicates" -> putExtra("NAV_DESTINATION", "trash")
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 
            channelId.hashCode(), 
            contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_credit_score)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (billId != null && channelId == "bill_auto_paid") {
            val undoIntent = Intent(context, BillActionReceiver::class.java).apply {
                action = "com.mateyou.duedate.ACTION_MARK_UNPAID"
                putExtra("bill_id", billId)
                putExtra("notification_id", notificationId)
            }
            val undoPendingIntent = PendingIntent.getBroadcast(
                context, 
                billId, 
                undoIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "UNDO", undoPendingIntent)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
