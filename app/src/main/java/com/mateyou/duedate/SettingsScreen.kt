package com.mateyou.duedate

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DoorBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun SettingsScreen(
    viewModel: DueDateViewModel,
    onOpenArchive: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenFrequency: () -> Unit,
    onOpenActivityLog: () -> Unit,
    onOpenBanks: () -> Unit,
    onOpenParsing: () -> Unit,
    onOpenParserDiagnostics: () -> Unit,
    onOpenSchedulerDiagnostics: () -> Unit,
    onOpenParseExistingSms: () -> Unit,
    onOpenConfigureTemplates: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val archivedBills by viewModel.archivedDueDates.collectAsState(initial = emptyList())
    val deletedDueDates by viewModel.deletedDueDates.collectAsState(initial = emptyList())
    val themeMode by viewModel.themeMode.collectAsState()
    val isMaterialYouEnabled by viewModel.isMaterialYouEnabled.collectAsState()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsState()
    val billDetectionEnabled by viewModel.billDetectionEnabled.collectAsState()
    val paymentRemindersEnabled by viewModel.paymentRemindersEnabled.collectAsState()
    val autoArchiveEnabled by viewModel.autoArchiveEnabled.collectAsState()
    val autoRecoveryEnabled by viewModel.autoRecoveryEnabled.collectAsState()
    val autoPayZeroBills by viewModel.autoPayZeroBills.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()

    var showDiagnostics by rememberSaveable { mutableStateOf(false) }
    var appVersionTapCount by remember { mutableStateOf(0) }

    var showParserDialog by remember { mutableStateOf(false) }
    var showSchedulerDialog by remember { mutableStateOf(false) }
    var showReceiverDialog by remember { mutableStateOf(false) }
    var showDateFormatDropdown by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    
    // Backup State
    var showExportPopup by remember { mutableStateOf(false) }
    var includeBills by remember { mutableStateOf(true) }
    var includeBanks by remember { mutableStateOf(true) }
    var includeTemplates by remember { mutableStateOf(true) }
    var includeAppearance by remember { mutableStateOf(true) }
    var includeNotifications by remember { mutableStateOf(true) }
    var includeDatabaseSettings by remember { mutableStateOf(true) }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.prepareBackupData(
                    includeBills, 
                    includeBanks, 
                    includeTemplates, 
                    includeAppearance, 
                    includeNotifications, 
                    includeDatabaseSettings
                )
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = inputStream.bufferedReader().use { it.readText() }
                        viewModel.restoreData(json)
                        Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to restore backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Settings")
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Archive") },
                    supportingContent = { Text("${archivedBills.size} bill${if (archivedBills.size == 1) "" else "s"}") },
                    leadingContent = { Icon(Icons.Default.Archive, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenArchive() }
                )
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.background))
                ListItem(
                    headlineContent = { Text("Trash") },
                    supportingContent = { Text("${deletedDueDates.size} bill${if (deletedDueDates.size == 1) "" else "s"}") },
                    leadingContent = { Icon(Icons.Default.Delete, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenTrash() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Theme", style = MaterialTheme.typography.bodyLarge)
                            val themeDesc = when(themeMode) { ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark"; ThemeMode.SYSTEM -> "Follow System" }
                            Text(themeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThemeOptionButton(
                            icon = if (themeMode == ThemeMode.LIGHT) Icons.Default.LightMode else Icons.Outlined.LightMode,
                            isSelected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        ThemeOptionButton(
                            icon = if (themeMode == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Outlined.DarkMode,
                            isSelected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        ThemeOptionButton(
                            icon = if (themeMode == ThemeMode.SYSTEM) Icons.Default.Settings else Icons.Outlined.Settings,
                            isSelected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Column {
                            Text("Material You", style = MaterialTheme.typography.bodyLarge)
                            Text("Dynamic colors from wallpaper", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Switch(checked = isMaterialYouEnabled, onCheckedChange = { viewModel.setMaterialYouEnabled(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Bill Detection") },
                    supportingContent = { Text("Receive bill detection notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.NotificationsActive, null) },
                    trailingContent = { Switch(checked = billDetectionEnabled, onCheckedChange = { viewModel.setBillDetectionEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Payment Reminders") },
                    supportingContent = { Text("Receive payment reminder notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Alarm, null) },
                    trailingContent = { Switch(checked = paymentRemindersEnabled, onCheckedChange = { viewModel.setPaymentRemindersEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().clickable(enabled = paymentRemindersEnabled) { onOpenFrequency() }.padding(horizontal = 4.dp)) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "Frequency",
                                color = if (paymentRemindersEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        },
                        supportingContent = {
                            Text(
                                "Select notification intervals",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (paymentRemindersEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                            )
                        },
                        leadingContent = { Icon(Icons.Outlined.Tune, null, tint = if (paymentRemindersEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = if (paymentRemindersEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("Database", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Banks") },
                    supportingContent = { Text("Manage supported banks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.AccountBalance, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenBanks() }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Bill Parser") },
                    supportingContent = { Text("Test parsing or Add a bill", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Receipt, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenParsing() }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Parse Existing SMS") },
                    supportingContent = { Text("Scan history for recent bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Search, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenParseExistingSms() }.padding(horizontal = 4.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Date Format") },
                    supportingContent = { Text(if (dateFormat == DateFormatPreference.DAY_MONTH) "Prefer Day/Month (e.g. 25/05)" else "Prefer Month/Day (e.g. 05/25)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.CalendarToday, null) },
                    trailingContent = {
                        Box {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                            ) {
                                DropdownMenu(
                                    expanded = showDateFormatDropdown,
                                    onDismissRequest = { showDateFormatDropdown = false },
                                    modifier = Modifier.width(200.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Day/Month (Standard)",
                                                modifier = Modifier.padding(start = 8.dp),
                                                fontWeight = if (dateFormat == DateFormatPreference.DAY_MONTH) FontWeight.Bold else FontWeight.Normal,
                                                color = if (dateFormat == DateFormatPreference.DAY_MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setDateFormat(DateFormatPreference.DAY_MONTH)
                                            showDateFormatDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Month/Day (US Style)",
                                                modifier = Modifier.padding(start = 8.dp),
                                                fontWeight = if (dateFormat == DateFormatPreference.MONTH_DAY) FontWeight.Bold else FontWeight.Normal,
                                                color = if (dateFormat == DateFormatPreference.MONTH_DAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.setDateFormat(DateFormatPreference.MONTH_DAY)
                                            showDateFormatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showDateFormatDropdown = true }.padding(horizontal = 4.dp)
                )

                ListItem(
                    headlineContent = { Text("Sync Missed Bills") },
                    supportingContent = { Text("Scan for bills while app was closed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Sync, null) },
                    trailingContent = { Switch(checked = autoRecoveryEnabled, onCheckedChange = { viewModel.setAutoRecoveryEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Auto-Archive Paid Bills") },
                    supportingContent = { Text("Move older paid bills to archive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Archive, null) },
                    trailingContent = { Switch(checked = autoArchiveEnabled, onCheckedChange = { viewModel.setAutoArchiveEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Auto-Pay Zero Bills") },
                    supportingContent = { Text("Mark zero or negative bills as paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.CheckBox, null) },
                    trailingContent = { Switch(checked = autoPayZeroBills, onCheckedChange = { viewModel.setAutoPayZeroBills(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Export Data") },
                    supportingContent = { Text("Backup your data to a file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.FileUpload, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showExportPopup = true }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Import Data") },
                    supportingContent = { Text("Restore data from a .ddb file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.FileDownload, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { openDocumentLauncher.launch("*/*") }.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("App Lock") },
                    supportingContent = {
                        Text(
                            "Require authentication on app launch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Lock, null) },
                    trailingContent = { Switch(checked = biometricLockEnabled, onCheckedChange = { viewModel.setBiometricLockEnabled(it) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("Experimental", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Configure SMS Templates") },
                    supportingContent = {
                        Text(
                            "Add custom SMS parsing logic to the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Code, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onOpenConfigureTemplates() }.padding(horizontal = 4.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showDiagnostics,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    color = Color.Transparent
                ) {
                    Column(modifier = Modifier.padding(vertical = 20.dp)) {
                        Text(
                            "Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        ListItem(
                            headlineContent = { Text("Activity Log") },
                            supportingContent = {
                                Text(
                                    "View last 50 events",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.History, null) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onOpenActivityLog() }
                                .padding(horizontal = 4.dp)
                        )
                        ListItem(
                            headlineContent = { Text("Receiver Log") },
                            supportingContent = {
                                Text(
                                    "Last 10 incoming bank SMS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.Sms, null) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showReceiverDialog = true }
                                .padding(horizontal = 4.dp)
                        )
                        ListItem(
                            headlineContent = { Text("Parser Log") },
                            supportingContent = {
                                Text(
                                    "Last SMS Parsing Log",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.BugReport, null) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showParserDialog = true }
                                .padding(horizontal = 4.dp)
                        )
                        ListItem(
                            headlineContent = { Text("Scheduler Log") },
                            supportingContent = {
                                Text(
                                    "Last Bill Payment Reminder Scheduling Log",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.HourglassEmpty, null) },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showSchedulerDialog = true }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("DueDate") },
                    supportingContent = { Text("Version 1.0.2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Info, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { 
                        appVersionTapCount++
                        if (appVersionTapCount >= 5) {
                            showDiagnostics = !showDiagnostics
                            val msg = if (showDiagnostics) "Diagnostics enabled" else "Diagnostics disabled"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            appVersionTapCount = 0
                        }
                    }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Onboarding") },
                    supportingContent = { Text("Review the introductory walkthrough", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.DoorBack, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { viewModel.resetOnboarding() }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("User Guide") },
                    supportingContent = { Text("How to use DueDate effectively", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, "https://nicegist.github.io/a746a7dddc704cb496fd5c938b463926".toUri())
                        context.startActivity(intent)
                    }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Privacy and Terms") },
                    supportingContent = { Text("View our Privacy Policy and Terms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Shield, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_VIEW, "https://nicegist.github.io/a639614b1f3b83c8258dc680335e4dc9".toUri())
                        context.startActivity(intent)
                    }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Credits and Licenses") },
                    supportingContent = { Text("View Third-Party Credits and Licenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Assignment, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, "https://nicegist.github.io/36bdd11f7050b94b38b1342fff686aa8".toUri())
                        context.startActivity(intent)
                    }.padding(horizontal = 4.dp)
                )
                ListItem(
                    headlineContent = { Text("Feedback") },
                    supportingContent = { Text("Contact us via email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) },
                    leadingContent = { Icon(Icons.Outlined.Mail, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:".toUri()
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("mateyouapps@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback - DueDate App")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                        }
                    }.padding(horizontal = 4.dp)
                )

                ListItem(
                    headlineContent = { Text("Reset App", color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Erase bills and reset preferences", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) },
                    leadingContent = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showResetConfirmation = true }.padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset App?") },
            text = { Text("All your bill data will be permanently erased and preferences will be reset. SMS messages on your device will remain unaffected.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    viewModel.resetApp()
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportPopup) {
        AlertDialog(
            onDismissRequest = { showExportPopup = false },
            title = { Text("Export Data") },
            text = {
                Column {
                    Text("Select items to include in the backup:")
                    Spacer(modifier = Modifier.height(16.dp))
                    ExportCheckboxItem("Bills", includeBills) { includeBills = it }
                    ExportCheckboxItem("Banks", includeBanks) { includeBanks = it }
                    ExportCheckboxItem("Custom Templates", includeTemplates) { includeTemplates = it }
                    ExportCheckboxItem("Appearance Settings", includeAppearance) { includeAppearance = it }
                    ExportCheckboxItem("Notification Settings", includeNotifications) { includeNotifications = it }
                    ExportCheckboxItem("Database Settings", includeDatabaseSettings) { includeDatabaseSettings = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExportPopup = false
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    val fileName = "DueDate_Export_${dateFormat.format(Date())}.ddb"
                    createDocumentLauncher.launch(fileName)
                }) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { showExportPopup = false }) { Text("Cancel") }
            }
        )
    }

    if (showReceiverDialog) {
        val logs by SmsDiagnosticLogger.receiverLog.collectAsState()
        DiagnosticListDialog(title = "Receiver Log", logs = logs.map { it.substringAfter("|") }) { showReceiverDialog = false }
    }

    if (showParserDialog) {
        val lastLog by SmsDiagnosticLogger.lastLog.collectAsState()
        DiagnosticLogDialog(title = "Parser Log", log = lastLog) { showParserDialog = false }
    }

    if (showSchedulerDialog) {
        val schedulerLog by SmsDiagnosticLogger.schedulerLog.collectAsState()
        DiagnosticLogDialog(title = "Scheduler Log", log = schedulerLog) { showSchedulerDialog = false }
    }
}

@Composable
fun ExportCheckboxItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}


@Composable
fun SwipeOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount > 20) {
                        change.consume()
                        onDismiss()
                    }
                }
            }
    ) {
        content()
    }
}

@Composable
fun ThemeOptionButton(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
