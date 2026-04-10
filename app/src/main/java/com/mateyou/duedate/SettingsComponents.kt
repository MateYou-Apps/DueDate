@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mateyou.duedate

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mateyou.duedate.data.BankEntity
import com.mateyou.duedate.data.DueDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class SortOption(val label: String) {
    DATE_ADDED("Date added"),
    LATEST_DUE("Latest due"),
    OLDEST_DUE("Oldest due")
}

@Composable
fun ArchiveTrashOverlay(
    title: String,
    bills: List<DueDate>,
    viewModel: DueDateViewModel,
    onBack: () -> Unit,
    onBillClick: (DueDate) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var billToConfirmActionLocal by remember { mutableStateOf<Pair<DueDate, String>?>(null) }
    var billToAnimateIdLocal by remember { mutableStateOf<Int?>(null) }

    var showBulkConfirm by remember { mutableStateOf(false) }
    var isCollapsingAll by remember { mutableStateOf(false) }

    val sortOption by (if (title == "Archive") viewModel.archiveSortOption else viewModel.trashSortOption).collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())
    val bankLogoMap = remember(activeBanks) {
        activeBanks.associateBy({ it.name.lowercase() }, { it.svgLogo })
    }

    val sortedBills = remember(bills, sortOption) {
        when (sortOption) {
            SortOption.DATE_ADDED -> {
                if (title == "Archive") {
                    bills.sortedByDescending { it.archivedAt ?: 0L }
                } else {
                    bills.sortedByDescending { it.deletedAt ?: 0L }
                }
            }
            SortOption.LATEST_DUE -> bills.sortedByDescending { it.dueDate }
            SortOption.OLDEST_DUE -> bills.sortedBy { it.dueDate }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bills.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            MaterialTheme(
                                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
                            ) {
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.width(180.dp)
                                ) {
                                    SortOption.entries.forEach { option ->
                                        val isSelected = option == sortOption
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option.label,
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                if (title == "Archive") viewModel.setArchiveSortOption(option) else viewModel.setTrashSortOption(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { showBulkConfirm = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (title == "Archive") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(
                                        alpha = 0.2f
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = if (title == "Archive") Icons.Default.DeleteSweep else Icons.Default.DeleteForever,
                                contentDescription = "Clear All",
                                tint = if (title == "Archive") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isCollapsingAll,
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(sortedBills, key = { it.id }) { bill ->
                        var isLocalExiting by remember { mutableStateOf(false) }
                        val isExiting = isLocalExiting || billToAnimateIdLocal == bill.id

                        val svgLogo = bankLogoMap[bill.cardName?.lowercase() ?: ""] ?: bankLogoMap[bill.bankName.lowercase()]

                        AnimatedVisibility(
                            visible = !isExiting,
                            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut(),
                            modifier = Modifier.animateItemPlacement(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        ) {
                            BillCard(
                                dueDate = bill,
                                onTogglePaid = { viewModel.setPaidStatus(bill.id, !bill.isPaid) },
                                onClick = { if (title == "Archive") onBillClick(bill) },
                                svgLogo = svgLogo,
                                trailingActions = {
                                    if (title == "Archive") {
                                        IconButton(onClick = {
                                            scope.launch {
                                                isLocalExiting = true
                                                delay(300)
                                                viewModel.unarchive(bill.id)
                                            }
                                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Unarchive, "Unarchive") }
                                        ActionSeparator()
                                        IconButton(onClick = { billToConfirmActionLocal = bill to "delete" }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Delete, "Trash", tint = MaterialTheme.colorScheme.error) }
                                    } else {
                                        IconButton(onClick = {
                                            scope.launch {
                                                isLocalExiting = true
                                                delay(300)
                                                viewModel.restore(bill.id)
                                            }
                                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Restore, "Restore") }
                                        ActionSeparator()
                                        IconButton(onClick = { billToConfirmActionLocal = bill to "archive" }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Archive, "Archive") }
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                    if (bills.isEmpty()) item { Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(if (title == "Archive") Icons.Default.Archive else Icons.Default.Delete, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)); Spacer(Modifier.height(16.dp)); Text("No bills in $title", color = MaterialTheme.colorScheme.outline) } } }
                }
            }
        }

        billToConfirmActionLocal?.let { (bill, action) ->
            AlertDialog(
                onDismissRequest = { billToConfirmActionLocal = null },
                title = { Text(if (action == "archive") "Archive Bill?" else "Move to Trash?") },
                text = { Text(if (action == "archive") "This bill will be moved to the archive." else "This bill will be moved to the trash.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val id = bill.id
                            billToAnimateIdLocal = id
                            billToConfirmActionLocal = null
                            delay(350)
                            if (action == "archive") viewModel.archive(id) else viewModel.delete(id)
                            billToAnimateIdLocal = null
                        }
                    }) { Text(if (action == "archive") "Archive" else "Trash") }
                },
                dismissButton = { TextButton(onClick = { billToConfirmActionLocal = null }) { Text("Cancel") } }
            )
        }

        if (showBulkConfirm) {
            val bulkTitle = if (title == "Archive") "Move All to Trash?" else "Delete All Permanently?"
            val bulkBody = if (title == "Archive") "All archived bills will be moved to the trash." else "All trashed bills will be permanently deleted. This action cannot be undone."
            val bulkAction = if (title == "Archive") "Trash All" else "Delete All"

            AlertDialog(
                onDismissRequest = { showBulkConfirm = false },
                title = { Text(bulkTitle) },
                text = { Text(bulkBody) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            isCollapsingAll = true
                            showBulkConfirm = false
                            delay(400)
                            if (title == "Archive") viewModel.trashAllArchived() else viewModel.deleteAllTrashed()
                            isCollapsingAll = false
                        }
                    }, colors = if (title == "Trash") ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error) else ButtonDefaults.textButtonColors()) { Text(bulkAction) }
                },
                dismissButton = { TextButton(onClick = { showBulkConfirm = false }) { Text("Cancel") } }
            )
        }
    }
    BackHandler { onBack() }
}

@Composable
fun ReminderFrequencyScreen(viewModel: DueDateViewModel, onBack: () -> Unit) {
    val reminderDays by viewModel.reminderDays.collectAsState()
    val reminderTime by viewModel.reminderTime.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    val formattedTime = remember(reminderTime) {
        val parts = reminderTime.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time).uppercase()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                Text("Payment Reminders", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                val dayOptions = listOf(
                    "on_due_day" to "On due day",
                    "1_day_before" to "1 day before",
                    "2_days_before" to "2 days before",
                    "3_days_before" to "3 days before",
                    "4_days_before" to "4 days before",
                    "5_days_before" to "5 days before",
                    "6_days_before" to "6 days before",
                    "1_week_before" to "One week before"
                )

                Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    dayOptions.forEach { (key, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                Checkbox(
                                    checked = reminderDays[key] ?: false,
                                    onCheckedChange = { viewModel.updateReminderDay(key, it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Reminder Time") },
                        supportingContent = { Text(formattedTime, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                        trailingContent = {
                            Icon(
                                Icons.Outlined.Schedule,
                                null,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showTimePicker = true }
                    )
                }
            }
        }
    }


    if (showTimePicker) {
        val initialHour = reminderTime.split(":")[0].toInt()
        val initialMinute = reminderTime.split(":")[1].toInt()

        var selectedHour by remember { mutableStateOf(if (initialHour == 0 || initialHour == 12) 12 else initialHour % 12) }
        var selectedMinute by remember { mutableStateOf(initialMinute) }
        var isAm by remember { mutableStateOf(initialHour < 12) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val finalHour = if (isAm) {
                        if (selectedHour == 12) 0 else selectedHour
                    } else {
                        if (selectedHour == 12) 12 else selectedHour + 12
                    }
                    val formattedTime = String.format("%02d:%02d", finalHour, selectedMinute)
                    viewModel.setReminderTime(formattedTime)
                    showTimePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp, 64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = String.format("%02d", selectedHour),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(":", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(horizontal = 8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp, 64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = String.format("%02d", selectedMinute),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .height(64.dp)
                                .width(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(if (isAm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isAm = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "AM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(if (!isAm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isAm = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Hour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = selectedHour.toFloat(),
                        onValueChange = { selectedHour = it.toInt().coerceIn(1, 12) },
                        valueRange = 1f..12f,
                        steps = 10,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Minutes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = selectedMinute.toFloat(),
                        onValueChange = { selectedMinute = it.toInt().coerceIn(0, 59) },
                        valueRange = 0f..59f,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        )
    }

    BackHandler { onBack() }
}

@Composable
fun ManageBanksScreen(viewModel: DueDateViewModel, onBack: () -> Unit) {
    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())
    val deletedBanks by viewModel.deletedBanks.collectAsState(initial = emptyList())
    var showAddMenu by remember { mutableStateOf(false) }
    var bankToEdit by remember { mutableStateOf<BankEntity?>(null) }
    var bankToAddType by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) } // isBrandedCard to showPopup
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    Text("Banks", style = MaterialTheme.typography.titleLarge)
                }
                Box {
                    IconButton(onClick = { showAddMenu = true }) { Icon(Icons.Default.Add, "Add New") }
                    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }, modifier = Modifier.width(180.dp)) {
                            DropdownMenuItem(text = { Text("Add Bank", modifier = Modifier.padding(start = 8.dp)) }, onClick = { showAddMenu = false; bankToAddType = false to true })
                            DropdownMenuItem(text = { Text("Add Branded Card", modifier = Modifier.padding(start = 8.dp)) }, onClick = { showAddMenu = false; bankToAddType = true to true })
                        }
                    }
                }
            }

            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Manage your banks and branded cards here. Tap on '+' button above to add a Bank or a Branded Credit Card. Tap on any existing item to edit or delete it. Branded cards are indicated with an * (asterisk) and override icons of issuing banks when matched. Use SVG files for icons. Existing icons can be customized too.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                val sortedBanks = activeBanks.filter { !it.isBrandedCard }.sortedBy { it.name.lowercase() }
                val sortedCards = activeBanks.filter { it.isBrandedCard }.sortedBy { it.name.lowercase() }

                items(sortedBanks) { bank -> BankGridItem(bank, onClick = { bankToEdit = bank }) }
                items(sortedCards) { card -> BankGridItem(card, onClick = { bankToEdit = card }) }

                if (deletedBanks.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Text("Deleted", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline) }
                    items(deletedBanks) { bank -> BankGridItem(bank, isDeletedSection = true, onClick = { bankToEdit = bank }) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    bankToEdit?.let { bank ->
        BankEditDialog(
            bank = bank,
            onDismiss = { bankToEdit = null },
            onConfirm = { updated ->
                viewModel.updateBank(updated)
                bankToEdit = null
                Toast.makeText(context, if (bank.isDeleted) "Restored" else "Saved", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.setBankDeletedStatus(bank.id, !bank.isDeleted)
                bankToEdit = null
                Toast.makeText(context, if (bank.isDeleted) "Restored" else "Deleted", Toast.LENGTH_SHORT).show()
            },
            onRestoreDefault = {
                viewModel.restoreBankToDefault(bank)
                bankToEdit = null
                Toast.makeText(context, "Restored to Default", Toast.LENGTH_SHORT).show()
            },
            onPermanentDelete = {
                viewModel.deleteBankPermanently(bank)
                bankToEdit = null
                Toast.makeText(context, "Permanently Deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }

    bankToAddType?.let { (isBranded, show) ->
        if (show) {
            BankEditDialog(
                bank = BankEntity(name = "", isBrandedCard = isBranded, senderIds = "", aliases = ""),
                isNew = true,
                onDismiss = { bankToAddType = null },
                onConfirm = { newBank ->
                    viewModel.addBank(newBank.name, newBank.isBrandedCard, newBank.senderIds, newBank.aliases, newBank.svgLogo)
                    bankToAddType = null
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
    BackHandler { onBack() }
}

@Composable
fun BankGridItem(bank: BankEntity, isDeletedSection: Boolean = false, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (isDeletedSection) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SvgLogo(
                svgString = bank.svgLogo,
                bankName = if (bank.isBrandedCard) "Bank" else bank.name,
                cardName = if (bank.isBrandedCard) bank.name else null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(text = if (bank.isBrandedCard) "${bank.name}*" else bank.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = if (isDeletedSection) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun BankEditDialog(
    bank: BankEntity,
    isNew: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (BankEntity) -> Unit,
    onDelete: () -> Unit = {},
    onRestoreDefault: () -> Unit = {},
    onPermanentDelete: () -> Unit = {}
) {
    var nameValue by remember { mutableStateOf(TextFieldValue(bank.name, TextRange(bank.name.length))) }
    var sendersValue by remember { mutableStateOf(TextFieldValue(bank.senderIds, TextRange(bank.senderIds.length))) }
    var aliasesValue by remember { mutableStateOf(TextFieldValue(bank.aliases, TextRange(bank.aliases.length))) }
    var svgCode by remember { mutableStateOf(bank.svgLogo) }
    var showDeleteForeverConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> svgCode = stream.bufferedReader().readText() } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                        Spacer(Modifier.width(8.dp))
                        val titlePrefix = if (isNew) "Add" else "Edit"
                        val titleSuffix = if (bank.isBrandedCard) "Card" else "Bank"
                        Text("$titlePrefix $titleSuffix", style = MaterialTheme.typography.titleLarge)
                    }
                    Row {
                        if (!isNew && bank.isBuiltIn) { IconButton(onClick = onRestoreDefault) { Icon(Icons.Default.RestorePage, "Reset to default") } }
                        if (!isNew) {
                            if (bank.isBuiltIn) {
                                IconButton(onClick = onDelete) { Icon(if (bank.isDeleted) Icons.Default.Restore else Icons.Default.Delete, if (bank.isDeleted) "Restore" else "Delete") }
                            } else {
                                IconButton(onClick = { showDeleteForeverConfirm = true }) { Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
                Column(modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                        .clickable { launcher.launch("image/svg+xml") }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp)
                        ), contentAlignment = Alignment.Center) {
                        if (isNew && svgCode == null) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            SvgLogo(
                                svgString = svgCode,
                                bankName = if (bank.isBrandedCard) "Bank" else nameValue.text,
                                cardName = if (bank.isBrandedCard) nameValue.text else null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(if (bank.isBrandedCard) "Card Name" else "Bank Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = nameValue, onValueChange = { nameValue = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(if (bank.isBrandedCard) "Aliases" else "Sender IDs", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = if (bank.isBrandedCard) aliasesValue else sendersValue, onValueChange = { if (bank.isBrandedCard) aliasesValue = it else sendersValue = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RoundedCornerShape(12.dp))
                    Text(if (bank.isBrandedCard) "Separate individual aliases with commas (,)" else "Add Sender IDs of your bank here to whitelist them. Separate individual IDs with commas (,)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(32.dp))
                }

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(bank.copy(name = nameValue.text, senderIds = sendersValue.text, aliases = aliasesValue.text, svgLogo = svgCode)) }, modifier = Modifier.padding(start = 16.dp)) { Text(if (isNew) "Save" else "Confirm") }
                }
            }
        }
    }

    if (showDeleteForeverConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteForeverConfirm = false },
            title = { Text("Delete Permanently?") },
            text = { Text("This will permanently remove this ${if (bank.isBrandedCard) "card" else "bank"} and all its configurations. Future bills from this bank will not be detected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteForeverConfirm = false
                        onPermanentDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteForeverConfirm = false }) { Text("Cancel") } }
        )
    }
}
