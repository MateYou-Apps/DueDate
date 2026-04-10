package com.mateyou.duedate.data

import com.mateyou.duedate.ThemeMode
import com.mateyou.duedate.DateFormatPreference

data class BackupData(
    val bills: List<DueDate>? = null,
    val banks: List<BankEntity>? = null,
    val customTemplates: List<CustomTemplate>? = null,
    val appearance: AppearanceSettings? = null,
    val notifications: NotificationSettings? = null,
    val databaseSettings: DatabaseSettings? = null
)

data class AppearanceSettings(
    val themeMode: ThemeMode,
    val isMaterialYouEnabled: Boolean
)

data class NotificationSettings(
    val billDetectionEnabled: Boolean,
    val paymentRemindersEnabled: Boolean,
    val reminderDays: Map<String, Boolean>,
    val reminderTime: String
)

data class DatabaseSettings(
    val autoRecoveryEnabled: Boolean,
    val autoArchiveEnabled: Boolean,
    val autoPayZeroBills: Boolean,
    val dateFormat: DateFormatPreference
)
