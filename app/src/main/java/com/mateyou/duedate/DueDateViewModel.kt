@file:OptIn(ExperimentalMaterial3Api::class)

package com.mateyou.duedate

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.AppearanceSettings
import com.mateyou.duedate.data.BackupData
import com.mateyou.duedate.data.BankEntity
import com.mateyou.duedate.data.CustomTemplate
import com.mateyou.duedate.data.DatabaseSettings
import com.mateyou.duedate.data.DateFilter
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.DueDateRepository
import com.mateyou.duedate.data.NotificationSettings
import com.mateyou.duedate.data.getDateRange
import com.mateyou.duedate.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class DateFormatPreference { DAY_MONTH, MONTH_DAY }

class DueDateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DueDateRepository
    private val sharedPrefs = application.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE)
    
    private val _selectedFilter = MutableStateFlow(loadFilter())
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isScoreMode = MutableStateFlow(sharedPrefs.getBoolean("score_mode_active", false))
    val isScoreMode = _isScoreMode.asStateFlow()

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode = _themeMode.asStateFlow()

    private val _isMaterialYouEnabled = MutableStateFlow(sharedPrefs.getBoolean("material_you_active", true))
    val isMaterialYouEnabled = _isMaterialYouEnabled.asStateFlow()

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(sharedPrefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    private val _isXiaomiPromptCompleted = MutableStateFlow(sharedPrefs.getBoolean("xiaomi_prompt_completed", false))
    val isXiaomiPromptCompleted = _isXiaomiPromptCompleted.asStateFlow()

    // Security
    private val _biometricLockEnabled = MutableStateFlow(sharedPrefs.getBoolean("biometric_lock_enabled", false))
    val biometricLockEnabled = _biometricLockEnabled.asStateFlow()

    // Notification Settings
    private val _billDetectionEnabled = MutableStateFlow(sharedPrefs.getBoolean("bill_detection_enabled", true))
    val billDetectionEnabled = _billDetectionEnabled.asStateFlow()

    private val _paymentRemindersEnabled = MutableStateFlow(sharedPrefs.getBoolean("payment_reminders_enabled", true))
    val paymentRemindersEnabled = _paymentRemindersEnabled.asStateFlow()

    private val _reminderDays = MutableStateFlow(loadReminderDays())
    val reminderDays = _reminderDays.asStateFlow()

    private val _reminderTime = MutableStateFlow(sharedPrefs.getString("reminder_time", "08:00") ?: "08:00")
    val reminderTime = _reminderTime.asStateFlow()

    private val _archiveSortOption = MutableStateFlow(loadSortOption("archive_sort_option"))
    val archiveSortOption = _archiveSortOption.asStateFlow()

    private val _trashSortOption = MutableStateFlow(loadSortOption("trash_sort_option"))
    val trashSortOption = _trashSortOption.asStateFlow()

    private val _autoArchiveEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_archive_enabled", true))
    val autoArchiveEnabled = _autoArchiveEnabled.asStateFlow()

    private val _autoRecoveryEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_recovery_enabled", true))
    val autoRecoveryEnabled = _autoRecoveryEnabled.asStateFlow()

    private val _autoPayZeroBills = MutableStateFlow(sharedPrefs.getBoolean("auto_pay_zero_bills", true))
    val autoPayZeroBills = _autoPayZeroBills.asStateFlow()

    private val _dateFormat = MutableStateFlow(loadDateFormat())
    val dateFormat = _dateFormat.asStateFlow()

    val activeDueDates: Flow<List<DueDate>>
    val filteredDueDates: Flow<List<DueDate>>
    val archivedDueDates: Flow<List<DueDate>>
    val deletedDueDates: Flow<List<DueDate>>
    
    // Bank operations
    val activeBanks: Flow<List<BankEntity>>
    val deletedBanks: Flow<List<BankEntity>>

    private val _duplicateBills = MutableStateFlow<List<DueDate>>(emptyList())
    val duplicateBills = _duplicateBills.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = DueDateRepository(database.dueDateDao(), database.bankDao())
        activeDueDates = repository.activeDueDates
        archivedDueDates = repository.archivedDueDates
        deletedDueDates = repository.deletedDueDates
        activeBanks = repository.activeBanks
        deletedBanks = repository.deletedBanks
        
        filteredDueDates = combine(activeDueDates, _selectedFilter) { dates, filter ->
            val range = filter.getDateRange()
            if (range == null) {
                dates
            } else {
                dates.filter { it.dueDate in range.start..range.end }
            }
        }
        
        SmsDiagnosticLogger.init(application)
        checkNewDuplicates()
        initializeDefaultBanks()
        
        if (_autoRecoveryEnabled.value) {
            runAutoRecovery()
        }
    }

    private fun runAutoRecovery() = viewModelScope.launch(Dispatchers.IO) {
        val latestDate = repository.getLatestReceivedDate() ?: return@launch
        parseExistingSmsSince(latestDate)
    }

    private fun initializeDefaultBanks() = viewModelScope.launch(Dispatchers.IO) {
        val existing = repository.getAllBanksSync()
        val defaults = BankConfig.getDefaultBankEntities()
        
        // Sync logic: Add missing banks or update builtInName for existing ones
        defaults.forEach { default ->
            val matching = existing.find { it.builtInName == default.builtInName || it.name == default.name }
            if (matching == null) {
                repository.insertBank(default)
            } else if (matching.builtInName == null) {
                // Migration for existing users: ensure builtInName is set and matches
                repository.updateBank(matching.copy(builtInName = default.name, isBuiltIn = true))
            }
        }
    }

    private fun loadFilter(): DateFilter {
        val filterName = sharedPrefs.getString("selected_filter", DateFilter.ALL_TIME.name)
        return try { DateFilter.valueOf(filterName!!) } catch (e: Exception) { DateFilter.ALL_TIME }
    }

    private fun loadThemeMode(): ThemeMode {
        val themeName = sharedPrefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try { ThemeMode.valueOf(themeName!!) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    private fun loadReminderDays(): Map<String, Boolean> {
        val defaultDays = mapOf(
            "on_due_day" to true,
            "1_day_before" to false,
            "2_days_before" to false,
            "3_days_before" to false,
            "4_days_before" to false,
            "5_days_before" to true,
            "6_days_before" to false,
            "1_week_before" to false
        )
        return defaultDays.mapValues { (key, default) -> sharedPrefs.getBoolean("reminder_day_$key", default) }
    }

    private fun loadSortOption(prefsKey: String): SortOption {
        val sortName = sharedPrefs.getString(prefsKey, SortOption.DATE_ADDED.name)
        return try { SortOption.valueOf(sortName!!) } catch (e: Exception) { SortOption.DATE_ADDED }
    }

    private fun loadDateFormat(): DateFormatPreference {
        val formatName = sharedPrefs.getString("date_format_preference", DateFormatPreference.DAY_MONTH.name)
        return try { DateFormatPreference.valueOf(formatName!!) } catch (e: Exception) { DateFormatPreference.DAY_MONTH }
    }

    fun updateFilter(filter: DateFilter) {
        _selectedFilter.value = filter
        sharedPrefs.edit().putString("selected_filter", filter.name).apply()
    }

    fun setArchiveSortOption(option: SortOption) {
        _archiveSortOption.value = option
        sharedPrefs.edit().putString("archive_sort_option", option.name).apply()
    }

    fun setTrashSortOption(option: SortOption) {
        _trashSortOption.value = option
        sharedPrefs.edit().putString("trash_sort_option", option.name).apply()
    }

    fun toggleScoreMode() {
        val newValue = !_isScoreMode.value
        _isScoreMode.value = newValue
        sharedPrefs.edit().putBoolean("score_mode_active", newValue).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setMaterialYouEnabled(enabled: Boolean) {
        _isMaterialYouEnabled.value = enabled
        sharedPrefs.edit().putBoolean("material_you_active", enabled).apply()
    }

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    fun resetOnboarding() {
        _isOnboardingCompleted.value = false
        sharedPrefs.edit().putBoolean("onboarding_completed", false).apply()
    }

    fun completeXiaomiPrompt() {
        _isXiaomiPromptCompleted.value = true
        sharedPrefs.edit().putBoolean("xiaomi_prompt_completed", true).apply()
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
        sharedPrefs.edit().putBoolean("biometric_lock_enabled", enabled).apply()
    }

    fun setBillDetectionEnabled(enabled: Boolean) {
        _billDetectionEnabled.value = enabled
        sharedPrefs.edit().putBoolean("bill_detection_enabled", enabled).apply()
    }

    fun setPaymentRemindersEnabled(enabled: Boolean) {
        _paymentRemindersEnabled.value = enabled
        sharedPrefs.edit().putBoolean("payment_reminders_enabled", enabled).apply()
        syncAllReminders()
    }

    fun updateReminderDay(key: String, enabled: Boolean) {
        val currentDays = _reminderDays.value.toMutableMap()
        currentDays[key] = enabled
        _reminderDays.value = currentDays
        sharedPrefs.edit().putBoolean("reminder_day_$key", enabled).apply()
        syncAllReminders()
    }

    fun setReminderTime(time: String) {
        _reminderTime.value = time
        sharedPrefs.edit().putString("reminder_time", time).apply()
        syncAllReminders()
    }

    private fun syncAllReminders() = viewModelScope.launch(Dispatchers.IO) {
        val scheduler = NotificationScheduler(getApplication())
        val bills = AppDatabase.getDatabase(getApplication()).dueDateDao().getNonDeletedDueDatesSync()
        bills.forEach { scheduler.scheduleRemindersForBill(it) }
    }

    fun setAutoArchiveEnabled(enabled: Boolean) {
        _autoArchiveEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_archive_enabled", enabled).apply()
    }

    fun setAutoRecoveryEnabled(enabled: Boolean) {
        _autoRecoveryEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_recovery_enabled", enabled).apply()
    }

    fun setAutoPayZeroBills(enabled: Boolean) {
        _autoPayZeroBills.value = enabled
        sharedPrefs.edit().putBoolean("auto_pay_zero_bills", enabled).apply()
    }

    fun setDateFormat(format: DateFormatPreference) {
        _dateFormat.value = format
        sharedPrefs.edit().putString("date_format_preference", format.name).apply()
    }

    fun insert(dueDate: DueDate) = viewModelScope.launch {
        repository.insert(dueDate)
    }

    fun setPaidStatus(id: Int, isPaid: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.setPaidStatus(id, isPaid)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, if (isPaid) "Bill Marked as Paid" else "Bill Marked as Unpaid", bill.currencySymbol)
            
            // Auto-archive logic: if a bill is marked as paid and feature is enabled, archive older paid bills of same card
            if (isPaid && _autoArchiveEnabled.value) {
                repository.archiveOlderPaidBills(bill.bankName, bill.cardName, bill.cardNumber, bill.dueDate)
            }

            if (!isPaid) {
                NotificationScheduler(getApplication()).scheduleRemindersForBill(bill)
            }
        }
    }

    fun setPartialPayment(id: Int, amount: Double) = viewModelScope.launch(Dispatchers.IO) {
        repository.setPartialPayment(id, amount)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            val amountStr = "${bill.currencySymbol}${String.format(Locale.getDefault(), "%.2f", amount)}"
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Marked as Partially Paid ($amountStr)", bill.currencySymbol)
        }
    }

    fun archive(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.archive(id)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Archived", bill.currencySymbol)
        }
    }

    fun unarchive(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.unarchive(id)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Unarchived", bill.currencySymbol)
            NotificationScheduler(getApplication()).scheduleRemindersForBill(bill)
        }
    }

    fun delete(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(id)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Moved to Trash", bill.currencySymbol)
        }
    }

    fun restore(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.restore(id)
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Restored from Trash", bill.currencySymbol)
            NotificationScheduler(getApplication()).scheduleRemindersForBill(bill)
        }
    }

    fun deletePermanently(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(getApplication())
        val bill = db.dueDateDao().getDueDateSync(id)
        if (bill != null) {
            SmsDiagnosticLogger.logActivity(getApplication(), bill.bankName, bill.cardName, bill.cardNumber, "Bill Permanently Deleted", bill.currencySymbol)
        }
        repository.deletePermanently(id)
    }

    fun trashAllArchived() = viewModelScope.launch {
        repository.trashAllArchived()
    }

    fun deleteAllTrashed() = viewModelScope.launch {
        repository.deleteAllTrashed()
    }

    fun renameCard(bank: String, card: String?, cardNumber: String?, name: String) = viewModelScope.launch {
        repository.renameCard(bank, card, cardNumber, name)
    }

    fun getHistory(bank: String, card: String?, cardNumber: String?): Flow<List<DueDate>> {
        return repository.getHistory(bank, card, cardNumber)
    }

    private fun checkNewDuplicates() = viewModelScope.launch {
        val duplicates = repository.getNewDuplicates()
        if (duplicates.isNotEmpty()) {
            _duplicateBills.value = duplicates
            repository.clearNewDuplicatesFlag()
        }
    }
    
    fun clearDuplicatePopup() {
        _duplicateBills.value = emptyList()
    }

    // Bank operations
    fun addBank(name: String, isBrandedCard: Boolean, senderIds: String, aliases: String, svgLogo: String?) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertBank(BankEntity(name = name, isBrandedCard = isBrandedCard, senderIds = senderIds, aliases = aliases, svgLogo = svgLogo))
    }

    fun updateBank(bank: BankEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateBank(bank)
    }

    fun setBankDeletedStatus(id: Int, deleted: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.setBankDeletedStatus(id, deleted)
    }

    fun deleteBankPermanently(bank: BankEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteBankPermanently(bank)
    }

    fun restoreBankToDefault(bank: BankEntity) = viewModelScope.launch(Dispatchers.IO) {
        val defaults = BankConfig.getDefaultBankEntities()
        val default = defaults.find { it.builtInName == bank.builtInName }
        if (default != null) {
            repository.updateBank(bank.copy(
                name = default.name,
                senderIds = default.senderIds,
                aliases = default.aliases,
                svgLogo = null // Back to built-in logo
            ))
        }
    }

    fun parseExistingSms(count: Int?, days: Int?) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        
        val cursor = context.contentResolver.query(uri, projection, null, null, "date DESC")
        cursor?.use {
            var processedWhitelistedCount = 0
            val limit = count ?: Int.MAX_VALUE
            val cutoffDate = if (days != null) {
                System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
            } else 0L
            
            while (it.moveToNext()) {
                val address = it.getString(0) ?: continue
                val body = it.getString(1) ?: continue
                val date = it.getLong(2)
                
                if (days != null && date < cutoffDate) break
                
                if (BankConfig.isBankSender(address)) {
                    BillDetectionProcessor.processMessage(context, address, body, date)
                    processedWhitelistedCount++
                    if (count != null && processedWhitelistedCount >= limit) break
                }
            }
        }
    }

    fun parseExistingSmsSince(timestamp: Long) = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")
        val selection = "date > ?"
        val selectionArgs = arrayOf(timestamp.toString())
        
        val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")
        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(0) ?: continue
                val body = it.getString(1) ?: continue
                val date = it.getLong(2)
                
                if (BankConfig.isBankSender(address)) {
                    BillDetectionProcessor.processMessage(context, address, body, date)
                }
            }
        }
    }

    fun resetApp() {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.clearApplicationUserData()
    }

    // Backup & Restore
    suspend fun prepareBackupData(
        includeBills: Boolean,
        includeBanks: Boolean,
        includeTemplates: Boolean,
        includeAppearance: Boolean,
        includeNotifications: Boolean,
        includeDatabaseSettings: Boolean
    ): String {
        return withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            val templatePrefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
            val templateJson = templatePrefs.getString("templates_list", "[]") ?: "[]"
            
            // Array-based parsing for R8 compatibility
            val templates: List<CustomTemplate> = try {
                Gson().fromJson(templateJson, Array<CustomTemplate>::class.java)?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val backupData = BackupData(
                bills = if (includeBills) repository.getAllBillsSync() else null,
                banks = if (includeBanks) repository.getAllBanksSync() else null,
                customTemplates = if (includeTemplates) templates else null,
                appearance = if (includeAppearance) AppearanceSettings(themeMode.value, isMaterialYouEnabled.value) else null,
                notifications = if (includeNotifications) NotificationSettings(billDetectionEnabled.value, paymentRemindersEnabled.value, reminderDays.value, reminderTime.value) else null,
                databaseSettings = if (includeDatabaseSettings) DatabaseSettings(autoRecoveryEnabled.value, autoArchiveEnabled.value, autoPayZeroBills.value, dateFormat.value) else null
            )
            Gson().toJson(backupData)
        }
    }

    suspend fun restoreData(json: String) {
        withContext(Dispatchers.IO) {
            val backupData = Gson().fromJson(json, BackupData::class.java) ?: return@withContext
            val context = getApplication<Application>()
            
            // 1. Restore Settings
            backupData.appearance?.let {
                setThemeMode(it.themeMode)
                setMaterialYouEnabled(it.isMaterialYouEnabled)
            }
            
            backupData.notifications?.let {
                setBillDetectionEnabled(it.billDetectionEnabled)
                setPaymentRemindersEnabled(it.paymentRemindersEnabled)
                it.reminderDays.forEach { (key, enabled) -> updateReminderDay(key, enabled) }
                setReminderTime(it.reminderTime)
            }
            
            backupData.databaseSettings?.let {
                setAutoRecoveryEnabled(it.autoRecoveryEnabled)
                setAutoArchiveEnabled(it.autoArchiveEnabled)
                setAutoPayZeroBills(it.autoPayZeroBills)
                setDateFormat(it.dateFormat)
            }
            
            // 2. Restore Banks (Safe Append - Avoid duplicates, merge sender IDs)
            backupData.banks?.let { banks ->
                val currentBanks = repository.getAllBanksSync().toMutableList()
                banks.forEach { backupBank ->
                    val existing = currentBanks.find { 
                        (backupBank.builtInName != null && it.builtInName == backupBank.builtInName) ||
                        (backupBank.name == it.name)
                    }
                    
                    if (existing == null) {
                        repository.insertBank(backupBank.copy(id = 0))
                        currentBanks.add(backupBank)
                    } else {
                        // Merge sender IDs
                        val existingIds = existing.senderIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        val backupIds = backupBank.senderIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        val mergedIds = (existingIds + backupIds).sorted().joinToString(", ")
                        
                        if (mergedIds != existing.senderIds) {
                            val updatedBank = existing.copy(senderIds = mergedIds)
                            repository.updateBank(updatedBank)
                            // Update our local tracking list
                            val index = currentBanks.indexOf(existing)
                            if (index != -1) {
                                currentBanks[index] = updatedBank
                            }
                        }
                    }
                }
            }
            
            // 3. Restore Templates (Safe Append - Match by name and regex)
            backupData.customTemplates?.let { importedTemplates ->
                val templatePrefs = context.getSharedPreferences("custom_templates", Context.MODE_PRIVATE)
                val existingJson = templatePrefs.getString("templates_list", "[]") ?: "[]"
                
                // Array-based parsing for R8 compatibility
                val existingTemplates: MutableList<CustomTemplate> = try {
                    Gson().fromJson(existingJson, Array<CustomTemplate>::class.java)?.toMutableList() ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
                
                var modified = false
                importedTemplates.forEach { imported ->
                    val isDuplicate = existingTemplates.any { 
                        it.name == imported.name || (it.patternRegex == imported.patternRegex && it.rawSms == imported.rawSms)
                    }
                    
                    if (!isDuplicate) {
                        // Neutralize ID to avoid conflicts (use current timestamp as ID string)
                        existingTemplates.add(imported.copy(id = (System.currentTimeMillis() + existingTemplates.size).toString()))
                        modified = true
                    }
                }
                
                if (modified) {
                    templatePrefs.edit().putString("templates_list", Gson().toJson(existingTemplates)).apply()
                }
            }

            // 4. Restore Bills (Safe Append - Neutralize IDs)
            backupData.bills?.let { bills ->
                bills.forEach { bill ->
                    val duplicate = repository.findDuplicate(bill.bankName, bill.cardName, bill.cardNumber, bill.amount, bill.dueDate)
                    if (duplicate == null) {
                        repository.insert(bill.copy(id = 0))
                    }
                }
            }
            
            syncAllReminders()
        }
    }
}
