package com.mateyou.duedate

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsDiagnosticLogger {
    private val _lastLog = MutableStateFlow<String?>(null)
    val lastLog: StateFlow<String?> = _lastLog

    private val _activityLog = MutableStateFlow<List<String>>(emptyList())
    val activityLog: StateFlow<List<String>> = _activityLog

    private val _schedulerLog = MutableStateFlow<String?>(null)
    val schedulerLog: StateFlow<String?> = _schedulerLog

    private val _receiverLog = MutableStateFlow<List<String>>(emptyList())
    val receiverLog: StateFlow<List<String>> = _receiverLog

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences("diagnostic_logs", Context.MODE_PRIVATE)
        
        // Load Activity Logs
        val activityLogs = prefs.getStringSet("activity_logs", emptySet())?.toList()?.sortedByDescending { it } ?: emptyList()
        _activityLog.value = activityLogs

        // Load Receiver Logs
        val receiverLogs = prefs.getStringSet("receiver_logs", emptySet())?.toList()?.sortedByDescending { it } ?: emptyList()
        _receiverLog.value = receiverLogs

        // Load Last Parser Log
        _lastLog.value = prefs.getString("last_parser_log", null)

        // Load Last Scheduler Log
        _schedulerLog.value = prefs.getString("last_scheduler_log", null)

        initialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("diagnostic_logs", Context.MODE_PRIVATE)
    }

    fun logActivity(context: Context, bank: String, cardName: String?, cardNumber: String?, event: String, currencySymbol: String = "₹") {
        init(context)
        val timeNow = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.US).format(Date())
        val cardStr = (if (cardName != null) " $cardName" else "") + (if (cardNumber != null) " ●●$cardNumber" else "")
        // Handle currency symbol in event message if it contains hardcoded ₹
        val formattedEvent = event.replace("₹", currencySymbol)
        val entry = "${System.currentTimeMillis()}|$timeNow - $bank$cardStr - $formattedEvent"
        
        val current = _activityLog.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) current.removeAt(50)
        _activityLog.value = current
        
        getPrefs(context).edit().putStringSet("activity_logs", _activityLog.value.toSet()).apply()
    }

    fun logSmsReceived(context: Context, sender: String, body: String) {
        init(context)
        val timeNow = SimpleDateFormat("dd MMM hh:mm:ss a", Locale.US).format(Date())
        val entry = "${System.currentTimeMillis()}|$timeNow | From: $sender\n$body"
        
        val current = _receiverLog.value.toMutableList()
        current.add(0, entry)
        if (current.size > 10) current.removeAt(10) // Keep last 10 SMS
        _receiverLog.value = current
        
        getPrefs(context).edit().putStringSet("receiver_logs", _receiverLog.value.toSet()).apply()
    }

    fun getDisplayActivityLogs(): List<String> {
        return _activityLog.value.map { it.substringAfter("|") }
    }

    fun getDisplayReceiverLogs(): List<String> {
        return _receiverLog.value.map { it.substringAfter("|") }
    }

    fun logParse(context: Context, body: String, sender: String?, bankName: String, cardName: String?, cardNumber: String?, totalDue: Double?, minDue: Double?, dueDate: Long, currencySymbol: String = "₹") {
        init(context)
        val log = """
            --- SMS PARSE LOG ---
            SENDER: ${sender ?: "Unknown"}
            
            RAW TEXT RECEIVED (Chars: ${body.length}):
            $body
            
            TEXT CHAR CODES (First 60 chars):
            ${body.take(60).map { c -> "${c}(${c.code})" }.joinToString(" ")}
            
            RESULTS:
            Bank Name: $bankName
            Card Name: $cardName
            Card Number: $cardNumber
            Currency: $currencySymbol
            Total Due: $totalDue
            Min Due: $minDue
            Due Date: ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(dueDate))} (epoch: $dueDate)
            ---------------------
        """.trimIndent()
        _lastLog.value = log
        getPrefs(context).edit().putString("last_parser_log", log).apply()
    }

    fun resetSchedulerLog(context: Context, billName: String, scheduledTimes: List<String>) {
        init(context)
        val timeNow = SimpleDateFormat("hh:mm a", Locale.US).format(Date()).uppercase()
        val log = """
--- SCHEDULER LOG ---
BILL: $billName
TIME: $timeNow

SCHEDULED REMINDERS:
${scheduledTimes.joinToString("\n")}
---------------------""".trimIndent()
        _schedulerLog.value = log
        getPrefs(context).edit().putString("last_scheduler_log", log).apply()
    }

    fun appendSchedulerLog(context: Context, billName: String, entry: String) {
        init(context)
        val timeNow = SimpleDateFormat("hh:mm a", Locale.US).format(Date()).uppercase()

        // Get the current log or start a fresh one if it's empty
        val currentLog = _schedulerLog.value?.removeSuffix("\n---------------------")
            ?: "--- SCHEDULER LOG ---\nBILL: $billName"

        // Append the new event and re-add the closing line
        val updatedLog = """
$currentLog
$timeNow - $entry
---------------------""".trimIndent()
        _schedulerLog.value = updatedLog
        getPrefs(context).edit().putString("last_scheduler_log", updatedLog).apply()
    }

    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
        _activityLog.value = emptyList()
        _receiverLog.value = emptyList()
        _lastLog.value = null
        _schedulerLog.value = null
    }
}
