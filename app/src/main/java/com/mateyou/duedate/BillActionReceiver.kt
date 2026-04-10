package com.mateyou.duedate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mateyou.duedate.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.NotificationManager

class BillActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getIntExtra("bill_id", -1)
        val notificationId = intent.getIntExtra("notification_id", -1)
        
        if (billId == -1) return

        when (intent.action) {
            "com.mateyou.duedate.ACTION_MARK_UNPAID" -> {
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    db.dueDateDao().setPaidStatus(billId, false, null)
                    
                    val bill = db.dueDateDao().getDueDateSync(billId)
                    if (bill != null) {
                        SmsDiagnosticLogger.logActivity(context, bill.bankName, bill.cardName, bill.cardNumber, "Bill Auto-Paid UNDONE (Marked Unpaid)")
                    }
                    
                    cancelNotification(context, notificationId)
                }
            }
            "com.mateyou.duedate.ACTION_MARK_PAID" -> {
                scope.launch {
                    val db = AppDatabase.getDatabase(context)
                    db.dueDateDao().setPaidStatus(billId, true, System.currentTimeMillis())
                    
                    val bill = db.dueDateDao().getDueDateSync(billId)
                    if (bill != null) {
                        SmsDiagnosticLogger.logActivity(context, bill.bankName, bill.cardName, bill.cardNumber, "Bill Marked Paid from Notification")
                    }
                    
                    cancelNotification(context, notificationId)
                }
            }
        }
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }
}
