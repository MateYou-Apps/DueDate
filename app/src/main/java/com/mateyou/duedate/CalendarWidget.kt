@file:Suppress("RestrictedApi")

package com.mateyou.duedate

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.DueDate
import com.mateyou.duedate.data.isOverdue
import com.mateyou.duedate.ui.theme.SuccessGreenDark
import com.mateyou.duedate.ui.theme.SuccessGreenLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarWidget : GlanceAppWidget() {
    
    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val allBills = try { db.dueDateDao().getNonDeletedDueDatesSync() } catch (e: Exception) { emptyList() }

        provideContent {
            GlanceTheme {
                WidgetContent(allBills)
            }
        }
    }

    @Composable
    private fun WidgetContent(allBills: List<DueDate>) {
        val prefs = currentState<Preferences>()
        val monthOffsetKey = intPreferencesKey("month_offset")
        val monthOffset = prefs[monthOffsetKey] ?: 0
        
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
        
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        
        // Point 2: Sort: Unpaid (Overdue, then Due) first, Paid at bottom
        val bills = allBills.filter {
            val billCal = Calendar.getInstance().apply { timeInMillis = it.dueDate }
            billCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
            billCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
        }.sortedWith(
            compareBy<DueDate> { it.isPaid } // false (0) < true (1) -> Unpaid first
                .thenBy { it.dueDate }
        )

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_credit_score),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
                
                Spacer(GlanceModifier.width(8.dp))

                Text(
                    text = monthName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<OpenAppAction>(
                        actionParametersOf(OpenAppAction.KEY_OFFSET to monthOffset)
                    ))
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(16.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .padding(4.dp)
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(ChangeMonthAction.KEY_OFFSET to -1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_left_arrow),
                            contentDescription = "Prev",
                            modifier = GlanceModifier.size(24.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .cornerRadius(16.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .padding(4.dp)
                            .clickable(actionRunCallback<ChangeMonthAction>(actionParametersOf(ChangeMonthAction.KEY_OFFSET to 1))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_right_arrow),
                            contentDescription = "Next",
                            modifier = GlanceModifier.size(24.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
                        )
                    }
                }
            }

            // View Box (LazyColumn)
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.surface)
            ) {
                if (bills.isEmpty()) {
                    item {
                        Box(
                            modifier = GlanceModifier.fillMaxWidth().height(100.dp).clickable(actionRunCallback<OpenAppAction>(
                                actionParametersOf(OpenAppAction.KEY_OFFSET to monthOffset)
                            )),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No bills this month",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                            )
                        }
                    }
                } else {
                    bills.forEachIndexed { index, bill ->
                        item {
                            BillItem(bill, monthOffset)
                        }
                        // Point 5: Spacing only BETWEEN cards
                        if (index < bills.size - 1) {
                            item {
                                Spacer(GlanceModifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BillItem(bill: DueDate, monthOffset: Int) {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val isToday = android.text.format.DateUtils.isToday(bill.dueDate)
        val overdue = isOverdue(bill.dueDate)

        val successGreenProvider = androidx.glance.color.ColorProvider(
            day = SuccessGreenLight,
            night = SuccessGreenDark
        )

        val logoRes = BankConfig.getBankLogo(bill.bankName, bill.cardName) ?: R.drawable.ic_bank_generic

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                // Use secondaryContainer for a more muted accent that mimics alpha
                .background(GlanceTheme.colors.secondaryContainer) 
                .padding(12.dp)
                .clickable(actionRunCallback<OpenAppAction>(
                    actionParametersOf(OpenAppAction.KEY_OFFSET to monthOffset)
                )),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(8.dp)
                    .background(GlanceTheme.colors.surface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(logoRes),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                )
            }
            
            Spacer(GlanceModifier.width(12.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                val displayName = bill.customName ?: (bill.bankName + (if (bill.cardName != null) " ${bill.cardName}" else ""))
                Text(
                    text = displayName,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    maxLines = 1
                )
                Text(
                    text = if (isToday) "Due today" else "Due on ${dateFormat.format(Date(bill.dueDate))}",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                )
            }
            
            Spacer(GlanceModifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${bill.currencySymbol}${String.format(Locale.getDefault(), "%.2f", bill.amount)}",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
                
                val statusText = when {
                    bill.isPaid -> "PAID"
                    overdue -> "OVERDUE"
                    else -> "DUE"
                }
                
                val statusColor = when {
                    bill.isPaid -> successGreenProvider
                    overdue -> GlanceTheme.colors.error
                    else -> GlanceTheme.colors.primary
                }

                Text(
                    text = statusText,
                    style = TextStyle(
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class ChangeMonthAction : ActionCallback {
    companion object {
        val KEY_OFFSET = ActionParameters.Key<Int>("offset")
        private val monthOffsetKey = intPreferencesKey("month_offset")
    }
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val offsetDelta = parameters[KEY_OFFSET] ?: 0
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val currentOffset = prefs[monthOffsetKey] ?: 0
            prefs.toMutablePreferences().apply {
                this[monthOffsetKey] = currentOffset + offsetDelta
            }
        }
        CalendarWidget().update(context, glanceId)
    }
}

class OpenAppAction : ActionCallback {
    companion object {
        val KEY_OFFSET = ActionParameters.Key<Int>("month_offset")
    }
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val offset = parameters[KEY_OFFSET] ?: 0
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "calendar")
            putExtra("month_offset", offset)
        }
        context.startActivity(intent)
    }
}

class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}
