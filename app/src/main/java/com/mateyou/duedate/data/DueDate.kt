package com.mateyou.duedate.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "due_dates")
data class DueDate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val cardName: String?,
    val cardNumber: String? = null,
    var customName: String? = null,
    val amount: Double,
    val minAmount: Double? = null,
    val currencySymbol: String = "₹",
    val dueDate: Long,
    val receivedDate: Long,
    val isPaid: Boolean = false,
    val isArchived: Boolean = false,
    val autoArchived: Boolean = false,
    val isNewDuplicate: Boolean = false,
    val isDeleted: Boolean = false,
    val paidAt: Long? = null,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val partialAmount: Double? = null,
    val partialPaidAt: Long? = null
)

fun isOverdue(dueDate: Long): Boolean {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return dueDate < cal.timeInMillis
}

fun daysRemaining(dueDate: Long): Int {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val today = cal.timeInMillis
    val diff = dueDate - today
    return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
}
