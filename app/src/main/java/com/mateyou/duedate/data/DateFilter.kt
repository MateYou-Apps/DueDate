package com.mateyou.duedate.data

import java.text.SimpleDateFormat
import java.util.*

enum class DateFilter(val label: String) {
    ALL_TIME("All Time"),
    LAST_YEAR("Last Year"),
    THIS_YEAR("This Year"),
    NEXT_YEAR("Next Year"),
    LAST_QUARTER("Last Quarter"),
    THIS_QUARTER("This Quarter"),
    NEXT_QUARTER("Next Quarter"),
    LAST_MONTH("Last Month"),
    THIS_MONTH("This Month"),
    NEXT_MONTH("Next Month")
}

data class DateRange(val start: Long, val end: Long)

fun DateFilter.getDateRange(): DateRange? {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    return when (this) {
        DateFilter.ALL_TIME -> null
        DateFilter.LAST_YEAR -> {
            cal.add(Calendar.YEAR, -1)
            val start = cal.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.THIS_YEAR -> {
            val start = cal.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.NEXT_YEAR -> {
            cal.add(Calendar.YEAR, 1)
            val start = cal.apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.LAST_MONTH -> {
            cal.add(Calendar.MONTH, -1)
            val start = cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.THIS_MONTH -> {
            val start = cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.NEXT_MONTH -> {
            cal.add(Calendar.MONTH, 1)
            val start = cal.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val end = cal.apply { set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)) }.timeInMillis
            DateRange(start, end)
        }
        DateFilter.LAST_QUARTER -> getQuarterRange(cal, -1)
        DateFilter.THIS_QUARTER -> getQuarterRange(cal, 0)
        DateFilter.NEXT_QUARTER -> getQuarterRange(cal, 1)
    }
}

fun DateFilter.getDynamicTitle(): String {
    val cal = Calendar.getInstance()
    return when (this) {
        DateFilter.ALL_TIME -> "Your Bills"
        DateFilter.LAST_YEAR -> (cal.get(Calendar.YEAR) - 1).toString()
        DateFilter.THIS_YEAR -> cal.get(Calendar.YEAR).toString()
        DateFilter.NEXT_YEAR -> (cal.get(Calendar.YEAR) + 1).toString()
        DateFilter.LAST_MONTH -> {
            cal.add(Calendar.MONTH, -1)
            SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
        }
        DateFilter.THIS_MONTH -> SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
        DateFilter.NEXT_MONTH -> {
            cal.add(Calendar.MONTH, 1)
            SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
        }
        DateFilter.LAST_QUARTER -> getQuarterTitle(cal, -1)
        DateFilter.THIS_QUARTER -> getQuarterTitle(cal, 0)
        DateFilter.NEXT_QUARTER -> getQuarterTitle(cal, 1)
    }
}

private fun getQuarterTitle(cal: Calendar, offset: Int): String {
    val currentQuarter = cal.get(Calendar.MONTH) / 3
    val targetQuarter = (currentQuarter + offset + 4) % 4
    val startMonth = targetQuarter * 3
    val endMonth = startMonth + 2
    
    val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
    val startCal = (cal.clone() as Calendar).apply { set(Calendar.MONTH, startMonth) }
    val endCal = (cal.clone() as Calendar).apply { set(Calendar.MONTH, endMonth) }
    
    return "${sdf.format(startCal.time)} to ${sdf.format(endCal.time)}"
}

private fun getQuarterRange(cal: Calendar, offset: Int): DateRange {
    val currentMonth = cal.get(Calendar.MONTH)
    val currentQuarter = currentMonth / 3
    val targetQuarterTotal = currentQuarter + offset
    
    val yearOffset = if (targetQuarterTotal < 0) -1 else if (targetQuarterTotal > 3) 1 else 0
    val finalQuarter = (targetQuarterTotal + 4) % 4
    
    val startCal = (cal.clone() as Calendar).apply {
        add(Calendar.YEAR, yearOffset)
        set(Calendar.MONTH, finalQuarter * 3)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val endCal = (startCal.clone() as Calendar).apply {
        set(Calendar.MONTH, (finalQuarter * 3) + 2)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    
    return DateRange(startCal.timeInMillis, endCal.timeInMillis)
}
