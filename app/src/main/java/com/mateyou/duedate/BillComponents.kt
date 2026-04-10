@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.mateyou.duedate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditScore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caverock.androidsvg.SVG
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.daysRemaining
import com.mateyou.duedate.data.isOverdue
import com.mateyou.duedate.ui.theme.SuccessGreenDark
import com.mateyou.duedate.ui.theme.SuccessGreenLight
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun SvgLogo(
    svgString: String?,
    bankName: String,
    modifier: Modifier = Modifier,
    cardName: String? = null
) {
    val bitmap = remember(svgString) {
        if (svgString.isNullOrBlank()) null else {
            try {
                val svg = SVG.getFromString(svgString)
                val canvasDim = 512f
                
                var viewBox = svg.documentViewBox
                if (viewBox == null) {
                    val dw = svg.documentWidth
                    val dh = svg.documentHeight
                    val w = if (dw > 0) dw else canvasDim
                    val h = if (dh > 0) dh else canvasDim
                    viewBox = RectF(0f, 0f, w, h)
                }

                val vWidth = viewBox.width()
                val vHeight = viewBox.height()
                val maxDim = max(vWidth, vHeight)
                val margin = maxDim * 0.10f
                val squareDim = maxDim + (margin * 2f)
                
                svg.setDocumentViewBox(
                    viewBox.centerX() - (squareDim / 2f),
                    viewBox.centerY() - (squareDim / 2f),
                    squareDim,
                    squareDim
                )

                svg.setDocumentWidth(canvasDim)
                svg.setDocumentHeight(canvasDim)

                val bmp = Bitmap.createBitmap(canvasDim.toInt(), canvasDim.toInt(), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                
                svg.renderToCanvas(canvas)
                bmp.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }

    Box(modifier = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
        } else {
            val logoRes = BankConfig.getBankLogo(bankName, cardName)
            if (logoRes != null) {
                Icon(painterResource(logoRes), null, modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Outlined.AccountBalance, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SvgIntro(
    svgString: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(svgString) {
        try {
            val svg = SVG.getFromString(svgString)
            val canvasDim = 512f
            
            var viewBox = svg.documentViewBox
            if (viewBox == null) {
                val dw = svg.documentWidth
                val dh = svg.documentHeight
                val w = if (dw > 0) dw else canvasDim
                val h = if (dh > 0) dh else canvasDim
                viewBox = RectF(0f, 0f, w, h)
            }

            val vWidth = viewBox.width()
            val vHeight = viewBox.height()
            val maxDim = max(vWidth, vHeight)
            val margin = maxDim * 0.10f
            val squareDim = maxDim + (margin * 2f)
            
            svg.setDocumentViewBox(
                viewBox.centerX() - (squareDim / 2f),
                viewBox.centerY() - (squareDim / 2f),
                squareDim,
                squareDim
            )

            svg.setDocumentWidth(canvasDim)
            svg.setDocumentHeight(canvasDim)

            val bmp = Bitmap.createBitmap(canvasDim.toInt(), canvasDim.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            
            svg.renderToCanvas(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) { null }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BillCard(
    dueDate: DueDate, 
    onTogglePaid: () -> Unit, 
    onLongClick: () -> Unit = {}, 
    onClick: () -> Unit = {}, 
    isAnimating: Boolean = false, 
    svgLogo: String? = null,
    trailingActions: @Composable (RowScope.() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val overdue = isOverdue(dueDate.dueDate); val daysLeft = daysRemaining(dueDate.dueDate)

    val successGreen = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight

    val badgeColorTarget = when { dueDate.isPaid -> successGreen; overdue -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.primary } // Use Primary accent instead of Orange (0xFFFFA000)
    val badgeColor by animateColorAsState(targetValue = badgeColorTarget, label = "badgeColor")

    val isDark = isSystemInDarkTheme()
    val innerBgTarget = if (overdue && !dueDate.isPaid) {
        if (isDark) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    }
    val innerBg by animateColorAsState(targetValue = innerBgTarget, label = "innerBg")

    val buttonBgTarget = if (dueDate.isPaid) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primaryContainer
    val buttonBg by animateColorAsState(targetValue = buttonBgTarget, label = "buttonBg")

    val buttonContentColorTarget = if (dueDate.isPaid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
    val buttonContentColor by animateColorAsState(targetValue = buttonContentColorTarget, label = "buttonContentColor")

    Card(modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongClick), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    SvgLogo(
                        svgString = svgLogo,
                        bankName = dueDate.bankName,
                        cardName = dueDate.cardName,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val displayName = dueDate.customName ?: (dueDate.bankName + (if (dueDate.cardName != null) " ${dueDate.cardName}" else ""))
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(if (!dueDate.cardNumber.isNullOrBlank()) "●●${dueDate.cardNumber}" else "Unknown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val badgeText = when { dueDate.isPaid -> "Paid"; overdue -> "Overdue"; else -> { val unit = if (daysLeft == 1) "day" else "days"; "$daysLeft $unit left" } }; Surface(color = badgeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f))) { Text(text = badgeText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(innerBg)
                .padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Total Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline); Text("${dueDate.currencySymbol}${String.format(Locale.getDefault(), "%.2f", dueDate.amount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }; Column(horizontalAlignment = Alignment.End) { Text("Minimum Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline); Text("${dueDate.currencySymbol}${String.format(Locale.getDefault(), "%.2f", dueDate.minAmount ?: 0.0)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }
                Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline);
                    Spacer(modifier = Modifier.width(6.dp));
                    Text("Due on ${dateFormat.format(Date(dueDate.dueDate))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                };
                val statusText = when {
                    dueDate.isDeleted -> getStatusLabel("Trashed", dueDate.deletedAt ?: dueDate.receivedDate)
                    dueDate.isArchived -> getStatusLabel("Archived", dueDate.archivedAt ?: dueDate.receivedDate)
                    dueDate.isPaid -> getStatusLabel("Marked as Paid", dueDate.paidAt ?: dueDate.receivedDate)
                    dueDate.partialAmount != null -> {
                        val amtStr = "${dueDate.currencySymbol}${String.format(Locale.getDefault(), "%.2f", dueDate.partialAmount)}"
                        getStatusLabel("Paid $amtStr", dueDate.partialPaidAt ?: dueDate.receivedDate)
                    }
                    else -> getStatusLabel("Updated", dueDate.receivedDate)
                }
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
                Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (trailingActions != null) { Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { trailingActions() } } else { Spacer(Modifier.weight(1f)) }
                Button(
                    onClick = onTogglePaid,
                    modifier = Modifier.fillMaxHeight(),    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonContentColor
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        if (dueDate.isPaid) Icons.Default.Refresh else Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (dueDate.isPaid) "Mark Unpaid" else "Mark as Paid",
                        style = MaterialTheme.typography.labelLarge
                    )
                }                }
            }
        }
    }
}

fun getStatusLabel(prefix: String, timestamp: Long): String {
    return if (android.text.format.DateUtils.isToday(timestamp)) {
        "$prefix today"
    } else {
        "$prefix on " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun BillActionMenu(bill: DueDate, onAction: (String) -> Unit) {
    Column(modifier = Modifier
        .navigationBarsPadding()
        .padding(bottom = 16.dp)) {
        ListItem(headlineContent = { Text("View bill history") }, leadingContent = { Icon(Icons.Default.History, null) }, modifier = Modifier.clickable { onAction("history") })
        ListItem(headlineContent = { Text("Add partial payment") }, leadingContent = { Icon(Icons.Default.Payments, null) }, modifier = Modifier.clickable { onAction("partial") })
        ListItem(headlineContent = { Text("Rename this bill") }, leadingContent = { Icon(Icons.Default.Edit, null) }, modifier = Modifier.clickable { onAction("rename") })
        ListItem(headlineContent = { Text("Archive this bill") }, leadingContent = { Icon(Icons.Default.Archive, null) }, modifier = Modifier.clickable { onAction("archive") })
        ListItem(headlineContent = { Text("Delete this bill", color = MaterialTheme.colorScheme.error) }, leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { onAction("delete") })
    }
}

@Composable
fun RenameDialog(bill: DueDate, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var textValue by remember { mutableStateOf(TextFieldValue(bill.customName ?: "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Card") },
        text = {
            SelectionContainer {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Card Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(textValue.text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PartialPaymentDialog(bill: DueDate, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var textValue by remember { mutableStateOf(TextFieldValue("")) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Partial Payment") },
        text = {
            Column {
                Text("Enter the amount you paid for this bill. This won't mark the bill as fully paid.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { 
                        textValue = it
                        val amt = it.text.toDoubleOrNull()
                        errorText = when {
                            amt == null && it.text.isNotEmpty() -> "Invalid amount"
                            amt != null && amt > bill.amount -> "Cannot exceed total due (${bill.currencySymbol}${bill.amount})"
                            else -> null
                        }
                    },
                    label = { Text("Amount Paid (${bill.currencySymbol})") },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = { errorText?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { 
            TextButton(
                enabled = textValue.text.isNotEmpty() && errorText == null,
                onClick = { textValue.text.toDoubleOrNull()?.let { onConfirm(it) } }
            ) { Text("Add") } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
fun CalendarScreen(viewModel: DueDateViewModel, onBillClick: (DueDate) -> Unit, initialMonthOffset: Int = 0) {
    val activeDueDates by viewModel.activeDueDates.collectAsState(initial = emptyList())
    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())

    val successGreen = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight

    val bankLogoMap = remember(activeBanks) {
        activeBanks.associateBy({ it.name.lowercase() }, { it.svgLogo })
    }

    var selectedMonth by remember { 
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.MONTH, initialMonthOffset) }) 
    }
    
    LaunchedEffect(initialMonthOffset) {
        if (initialMonthOffset != 0) {
            selectedMonth = Calendar.getInstance().apply { add(Calendar.MONTH, initialMonthOffset) }
        }
    }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val billsThisMonth = activeDueDates.filter { val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate }; cal.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) }
    
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("Calendar"); Spacer(modifier = Modifier.height(24.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { selectedMonth = (selectedMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Prev") }; Text(monthFormat.format(selectedMonth.time), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); IconButton(onClick = { selectedMonth = (selectedMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next") } }
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedContent(
                targetState = selectedMonth,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "CalendarFade"
            ) { targetMonth ->
                CalendarGrid(targetMonth, activeDueDates)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            if (billsThisMonth.isEmpty()) { Box(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp), contentAlignment = Alignment.Center) { Text("No bills due this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) } }
        }
        items(billsThisMonth) { bill ->
            val svgLogo = bankLogoMap[bill.cardName?.lowercase() ?: ""] ?: bankLogoMap[bill.bankName.lowercase()]
            Card(modifier = Modifier
                .fillMaxWidth()
                .clickable { onBillClick(bill) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        SvgLogo(
                            svgString = svgLogo,
                            bankName = bill.bankName,
                            cardName = bill.cardName,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val displayName = bill.customName ?: (bill.bankName + (if (bill.cardName != null) " ${bill.cardName}" else ""))
                            Text(displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val isToday = android.text.format.DateUtils.isToday(bill.dueDate)
                            Text(if (isToday) "Due today" else "Due on ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(bill.dueDate))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${bill.currencySymbol}${bill.amount}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
                        val overdue = isOverdue(bill.dueDate)
                        Text(if (overdue && !bill.isPaid) "OVERDUE" else if (bill.isPaid) "PAID" else "DUE", style = MaterialTheme.typography.labelSmall, color = if (overdue && !bill.isPaid) MaterialTheme.colorScheme.error else if (bill.isPaid) successGreen else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(calendar: Calendar, bills: List<DueDate>) {
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); val firstDayOfWeek = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val totalDaysToShow = if ((firstDayOfWeek + daysInMonth) > 35) 42 else 35; val days = (0 until totalDaysToShow).map { i -> val dayNumber = i - firstDayOfWeek + 1; if (dayNumber in 1..daysInMonth) dayNumber else null }; val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    Column { Row(modifier = Modifier.fillMaxWidth()) { weekdays.forEach { day -> Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) } }; Spacer(modifier = Modifier.height(8.dp)); val rows = days.chunked(7); Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { rows.forEach { week -> if (week.any { it != null }) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { week.forEach { day -> if (day != null) { val hasBill = bills.any { val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate }; cal.get(Calendar.DAY_OF_MONTH) == day && cal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) }; val isToday = Calendar.getInstance().let { it.get(Calendar.DAY_OF_MONTH) == day && it.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && it.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) }; Box(modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(
            if (isToday) MaterialTheme.colorScheme.primary else if (hasBill) MaterialTheme.colorScheme.primaryContainer.copy(
                alpha = 0.5f
            ) else Color.Transparent
        ), contentAlignment = Alignment.Center) { Text(day.toString(), style = MaterialTheme.typography.bodyMedium, color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface); if (hasBill && !isToday) Box(modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 6.dp)
        .size(4.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary)) else if (hasBill && isToday) Box(modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 6.dp)
        .size(4.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.onPrimary)) } } else Spacer(modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)) } } } } } }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("due_date_prefs", Context.MODE_PRIVATE) }
    var isScoreMode by remember { mutableStateOf(prefs.getBoolean("score_mode_active", false)) }

    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (title == "DueDate") {
                        isScoreMode = !isScoreMode
                        prefs.edit().putBoolean("score_mode_active", isScoreMode).apply()
                    }
                })
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        if (isScoreMode && title == "DueDate") {
            Icon(Icons.Default.CreditScore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        } else {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
fun MonthlyGraph(history: List<DueDate>, modifier: Modifier = Modifier) {
    if (history.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val sortedHistory = history.sortedBy { it.dueDate }
    val maxAmount = sortedHistory.maxOf { it.amount }.toFloat().coerceAtLeast(100f)
    val minAmount = sortedHistory.minOf { it.amount }.toFloat().coerceAtMost(0f)
    val amountRange = maxAmount - minAmount
    
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val visibleMonths = 12
    val monthWidth = screenWidth / visibleMonths
    val totalWidth = monthWidth * sortedHistory.size.coerceAtLeast(visibleMonths)

    var selectedPoint by remember { mutableStateOf<Pair<DueDate, Offset>?>(null) }

    // Automatically scroll to the end (latest data) when the graph is loaded
    LaunchedEffect(sortedHistory) {
        scrollState.scrollTo(scrollState.maxValue)
    }
    
    LaunchedEffect(selectedPoint) {
        if (selectedPoint != null) {
            delay(3000)
            selectedPoint = null
        }
    }

    Box(modifier = modifier
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
        .padding(8.dp)) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(32.dp)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val divisions = 4
            for (i in divisions downTo 0) {
                val value = minAmount + (amountRange / divisions) * i
                Text(
                    text = if (value >= 1000) "${(value / 1000).toInt()}K" else value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }

        // Scrollable area for the graph
        Box(
            modifier = Modifier
                .padding(start = 32.dp)
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            val colorPrimary = MaterialTheme.colorScheme.primary
            val density = LocalDensity.current

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .fillMaxHeight()
                    .pointerInput(sortedHistory) {
                        detectTapGestures { offset ->
                            val index = (offset.x / monthWidth.toPx()).toInt()
                            if (index in sortedHistory.indices) {
                                val item = sortedHistory[index]
                                val y =
                                    (1f - (item.amount.toFloat() - minAmount) / amountRange) * (size.height - 48.dp.toPx()) + 24.dp.toPx()
                                val x = index * monthWidth.toPx() + monthWidth.toPx() / 2
                                // Simple distance check
                                if (Offset(x, y).minus(offset).getDistance() < 40.dp.toPx()) {
                                    selectedPoint = item to Offset(x, y)
                                }
                            }
                        }
                    }
            ) {
                val height = size.height - 48.dp.toPx()
                val topPadding = 24.dp.toPx()
                val gridAlpha = 0.1f

                // Horizontal Grid Lines
                val divisions = 4
                for (i in 0..divisions) {
                    val y = topPadding + (height / divisions) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = gridAlpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = sortedHistory.mapIndexed { index, item ->
                    val x = index * monthWidth.toPx() + monthWidth.toPx() / 2
                    val y = (1f - (item.amount.toFloat() - minAmount) / amountRange) * height + topPadding
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val path = Path()
                    val fillPath = Path()
                    
                    path.moveTo(points[0].x, points[0].y)
                    fillPath.moveTo(points[0].x, size.height - 24.dp.toPx())
                    fillPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2, p1.y)
                        val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2, p2.y)
                        path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                        fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
                    }

                    fillPath.lineTo(points.last().x, size.height - 24.dp.toPx())
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(colorPrimary.copy(alpha = 0.4f), Color.Transparent),
                            startY = points.minOf { it.y },
                            endY = size.height - 24.dp.toPx()
                        )
                    )

                    drawPath(
                        path = path,
                        color = colorPrimary,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw dots and X-axis labels
                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = colorPrimary,
                        radius = 4.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )

                    val dateText = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(sortedHistory[index].dueDate))
                    drawContext.canvas.nativeCanvas.drawText(
                        dateText,
                        point.x,
                        size.height - 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = Color.Gray.toArgb()
                            textSize = 8.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }

            // Tooltip fix: use offset instead of padding to avoid crash and coerce in bounds
            selectedPoint?.let { (item, offset) ->
                val tooltipWidth = 80.dp
                val tooltipHeight = 50.dp
                
                val xOffset = with(density) {
                    (offset.x - (tooltipWidth.toPx() / 2))
                        .coerceIn(0f, totalWidth.toPx() - tooltipWidth.toPx())
                        .toDp()
                }
                
                val yOffset = with(density) {
                    // Try to show above, if too high, show below
                    val pxAbove = offset.y - tooltipHeight.toPx() - 10.dp.toPx()
                    if (pxAbove >= 0) pxAbove.toDp() else (offset.y + 10.dp.toPx()).toDp()
                }

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(item.dueDate)), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        Text("${item.currencySymbol}${String.format(Locale.getDefault(), "%.2f", item.amount)}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(bill: DueDate, viewModel: DueDateViewModel, onBack: () -> Unit) {
    val history by viewModel.getHistory(bill.bankName, bill.cardName, bill.cardNumber).collectAsState(initial = emptyList())
    val activeBanks by viewModel.activeBanks.collectAsState(initial = emptyList())

    val successGreen = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight

    val bankLogoMap = remember(activeBanks) {
        activeBanks.associateBy({ it.name.lowercase() }, { it.svgLogo })
    }
    val svgLogo = bankLogoMap[bill.cardName?.lowercase() ?: ""] ?: bankLogoMap[bill.bankName.lowercase()]

    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val shortDueDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onBack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    Column {
                        val displayName = bill.customName ?: (bill.bankName + (if (bill.cardName != null) " ${bill.cardName}" else ""))
                        Text(displayName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val displayCard = if (!bill.cardNumber.isNullOrBlank()) "●●${bill.cardNumber}" else "Unknown"
                        Text(displayCard, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                SvgLogo(
                    svgString = svgLogo,
                    bankName = bill.bankName,
                    cardName = bill.cardName,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp)); Text("Overview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp));
            
            MonthlyGraph(
                history = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(modifier = Modifier.height(24.dp)); Text("Statements", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp));
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(history) { item -> 
                    val overdue = isOverdue(item.dueDate)
                    val statusText = when {
                        item.isPaid -> "PAID"
                        overdue -> "OVERDUE"
                        else -> "DUE"
                    }
                    val statusColor = when {
                        item.isPaid -> successGreen
                        overdue -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) { 
                        Row(modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateFormat.format(Date(item.dueDate)), style = MaterialTheme.typography.titleMedium); 
                                Text("${item.currencySymbol}${String.format(Locale.getDefault(), "%.2f", item.amount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); 
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Due by ${shortDueDateFormat.format(Date(item.dueDate))}", style = MaterialTheme.typography.bodySmall) 
                                Text(statusText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = statusColor)
                            }
                        } 
                    } 
                } 
            }
        }
    }
    BackHandler { onBack() }
}


@Composable
fun SummaryBox(label: String, count: String, modifier: Modifier = Modifier, isSelected: Boolean = false, color: Color? = null, onClick: () -> Unit = {}) { val bgColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f); val contentColor = color ?: MaterialTheme.colorScheme.onSurface; Column(modifier = modifier
    .clip(RoundedCornerShape(16.dp))
    .background(bgColor)
    .clickable(onClick = onClick)
    .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor); Text(label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) contentColor else MaterialTheme.colorScheme.outline) } }

@Composable
fun ActionSeparator() {
    VerticalDivider(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .width(1.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}
