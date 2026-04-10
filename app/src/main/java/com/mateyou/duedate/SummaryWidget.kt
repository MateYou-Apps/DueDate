@file:Suppress("RestrictedApi")

package com.mateyou.duedate

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mateyou.duedate.data.AppDatabase
import com.mateyou.duedate.data.isOverdue
import com.mateyou.duedate.ui.theme.SuccessGreenDark
import com.mateyou.duedate.ui.theme.SuccessGreenLight

class SummaryWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val allBills = try {
            db.dueDateDao().getNonDeletedDueDatesSync().filter { !it.isArchived }
        } catch (e: Exception) {
            emptyList()
        }

        val total = allBills.size
        val due = allBills.count { !it.isPaid && !isOverdue(it.dueDate) }
        val late = allBills.count { !it.isPaid && isOverdue(it.dueDate) }
        val paid = allBills.count { it.isPaid }

        provideContent {
            GlanceTheme {
                SummaryWidgetContent(total, due, late, paid)
            }
        }
    }

    @Composable
    private fun SummaryWidgetContent(total: Int, due: Int, late: Int, paid: Int) {
        val size = LocalSize.current
        val padding = 12.dp
        
        // Dynamic font sizes based on widget dimensions
        val isEnlarged = size.width > 240.dp && size.height > 110.dp
        val countFontSize = if (isEnlarged) 28.sp else 18.sp
        val labelFontSize = if (isEnlarged) 14.sp else 11.sp

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp) // Adjusted outer radius to match common widget style
                .background(GlanceTheme.colors.surface)
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SummaryItem("Total", total.toString(), "TOTAL", countFontSize, labelFontSize, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(padding))
            SummaryItem("Due", due.toString(), "DUE", countFontSize, labelFontSize, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(padding))
            SummaryItem("Late", late.toString(), "LATE", countFontSize, labelFontSize, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(padding))
            SummaryItem("Paid", paid.toString(), "PAID", countFontSize, labelFontSize, GlanceModifier.defaultWeight())
        }
    }

    @Composable
    private fun SummaryItem(
        label: String,
        count: String,
        filter: String,
        countFontSize: androidx.compose.ui.unit.TextUnit,
        labelFontSize: androidx.compose.ui.unit.TextUnit,
        modifier: GlanceModifier
    ) {
        val successGreenProvider = androidx.glance.color.ColorProvider(
            day = SuccessGreenLight,
            night = SuccessGreenDark
        )

        val countColor = when (filter) {
            "DUE" -> GlanceTheme.colors.primary
            "LATE" -> GlanceTheme.colors.error
            "PAID" -> successGreenProvider
            else -> GlanceTheme.colors.onSurface
        }

        Box(
            modifier = modifier
                .fillMaxHeight()
                .cornerRadius(12.dp) // Adjusted inner radius to match the outer container curvature better
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(actionRunCallback<OpenBillsAction>(actionParametersOf(OpenBillsAction.KEY_FILTER to filter))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = count,
                    style = TextStyle(
                        color = countColor,
                        fontSize = countFontSize,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = label,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSecondaryContainer,
                        fontSize = labelFontSize
                    )
                )
            }
        }
    }
}

class OpenBillsAction : ActionCallback {
    companion object {
        val KEY_FILTER = ActionParameters.Key<String>("bill_filter")
    }
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val filter = parameters[KEY_FILTER] ?: "TOTAL"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "bills")
            putExtra("BILL_FILTER", filter)
            putExtra("NAV_DESTINATION", "bills")
        }
        context.startActivity(intent)
    }
}

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}
