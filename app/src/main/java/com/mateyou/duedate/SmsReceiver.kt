package com.mateyou.duedate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.DueDateDao
import com.mateyou.duedate.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        // Prevent race conditions when multiple SMS are processed simultaneously
        private val processingMutex = Mutex()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val database = AppDatabase.getDatabase(context)
        val dao = database.dueDateDao()

        scope.launch {
            try {
                // 1. Handle REAL SMS
                if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    val smsMap = mutableMapOf<String, StringBuilder>()
                    var latestTimestamp = 0L

                    for (message in messages) {
                        val sender = message.displayOriginatingAddress ?: continue
                        val body = message.messageBody ?: continue
                        smsMap.getOrPut(sender) { StringBuilder() }.append(body)
                        latestTimestamp = message.timestampMillis
                    }

                    for ((sender, body) in smsMap) {
                        SmsDiagnosticLogger.logSmsReceived(context, sender, body.toString())
                        processMessage(context, dao, sender, body.toString(), latestTimestamp)
                    }
                }
                // 2. Handle TEST SMS (Simulated via ADB)
                else if (intent.action == "com.mateyou.duedate.SIMULATE_SMS") {
                    val sender = intent.getStringExtra("sender") ?: "HDFCBK"
                    val body = intent.getStringExtra("body") ?: ""
                    SmsDiagnosticLogger.logSmsReceived(context, sender, body)
                    processMessage(context, dao, sender, body, System.currentTimeMillis())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processMessage(context: Context, dao: DueDateDao, sender: String, body: String, timestamp: Long) {
        if (BankConfig.isBankSender(sender)) {
            val parsed = SmsParser.parse(context, body, timestamp, sender)
            if (parsed != null) {
                processingMutex.withLock {
                    // Match logic: Match bank, card name, and card number against ALL non-deleted bills
                    val duplicate = dao.findDuplicate(
                        parsed.bankName,
                        parsed.cardName,
                        parsed.cardNumber,
                        parsed.amount,
                        parsed.dueDate
                    )

                    if (duplicate == null) {
                        // Check if we should carry over a custom name from a previous bill of the same card
                        val existingCustomName = dao.findExistingCustomName(
                            parsed.bankName,
                            parsed.cardName,
                            parsed.cardNumber
                        )

                        val prefs = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE)
                        val autoPayZeroEnabled = prefs.getBoolean("auto_pay_zero_bills", true)
                        val isPaidAutomatically = autoPayZeroEnabled && parsed.amount <= 0.0
                        
                        val billToInsert = parsed.copy(
                            customName = existingCustomName,
                            isPaid = isPaidAutomatically,
                            paidAt = if (isPaidAutomatically) System.currentTimeMillis() else null
                        )

                        val insertedId = dao.insert(billToInsert)
                        val amountStr = "${billToInsert.currencySymbol}${billToInsert.amount}"
                        val activityMsg = if (isPaidAutomatically) "New Bill Detected & Marked as Paid ($amountStr)" else "New Bill Detected ($amountStr)"
                        SmsDiagnosticLogger.logActivity(context, billToInsert.bankName, billToInsert.cardName, billToInsert.cardNumber, activityMsg, billToInsert.currencySymbol)

                        // Schedule reminders using the official database ID if NOT paid
                        if (!isPaidAutomatically) {
                            val billWithId = billToInsert.copy(id = insertedId.toInt())
                            NotificationScheduler(context).scheduleRemindersForBill(billWithId)
                        }

                        val billDetectionEnabled = prefs.getBoolean("bill_detection_enabled", true)
                        if (billDetectionEnabled) {
                            val cardStr = if (billToInsert.cardName != null) " ${billToInsert.cardName}" else ""
                            if (isPaidAutomatically) {
                                delay(2000)
                                showNotification(
                                    context,
                                    "Bill Marked as Paid Automatically",
                                    "${billToInsert.bankName}$cardStr bill of ${billToInsert.currencySymbol}${billToInsert.amount} was marked as paid.",
                                    "bill_auto_paid",
                                    billId = insertedId.toInt()
                                )
                            } else {
                                val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(billToInsert.dueDate))
                                val message = "${billToInsert.bankName}$cardStr: ${billToInsert.currencySymbol}${billToInsert.amount} (Due on $dateStr)"
                                showNotification(context, "New Bill Detected", message, "bill_alerts")
                            }
                        }
                    } else {
                        // It's a duplicate of a non-deleted bill - Move to Trash
                        val trashDuplicate = parsed.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), isNewDuplicate = true)
                        dao.insert(trashDuplicate)

                        val billDetectionEnabled = context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE).getBoolean("bill_detection_enabled", true)
                        if (billDetectionEnabled) {
                            delay(1000)
                            val bank = parsed.bankName
                            val cardName = if (parsed.cardName != null) " ${parsed.cardName}" else ""
                            val cardNum = if (parsed.cardNumber != null) " ${parsed.cardNumber}" else ""
                            val message = "$bank$cardName$cardNum bill was detected as duplicate and moved to trash automatically."
                            showNotification(context, "Duplicate Bill Trashed", message, "bill_duplicates")
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String, channelId: String, billId: Int? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

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
