package com.mateyou.duedate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DueDateDao {
    @Query("SELECT * FROM due_dates WHERE isArchived = 0 AND isDeleted = 0 ORDER BY isPaid ASC, dueDate ASC")
    fun getActiveDueDates(): Flow<List<DueDate>>

    @Query("SELECT * FROM due_dates WHERE isDeleted = 0 ORDER BY dueDate ASC")
    fun getAllCalendarBills(): Flow<List<DueDate>>

    @Query("SELECT * FROM due_dates WHERE isDeleted = 0")
    suspend fun getNonDeletedDueDatesSync(): List<DueDate>

    @Query("SELECT * FROM due_dates WHERE id = :id")
    fun getDueDateSync(id: Int): DueDate?

    @Query("SELECT * FROM due_dates WHERE isArchived = 1 AND isDeleted = 0 ORDER BY archivedAt DESC, dueDate DESC")
    fun getArchivedDueDates(): Flow<List<DueDate>>

    @Query("SELECT * FROM due_dates WHERE isDeleted = 1 ORDER BY deletedAt DESC, dueDate DESC")
    fun getDeletedDueDates(): Flow<List<DueDate>>

    @Query("SELECT * FROM due_dates")
    suspend fun getAllBillsSync(): List<DueDate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dueDate: DueDate): Long

    @Query("UPDATE due_dates SET isPaid = :paid, paidAt = :timestamp WHERE id = :id")
    suspend fun setPaidStatus(id: Int, paid: Boolean, timestamp: Long?)

    @Query("UPDATE due_dates SET partialAmount = :amt, partialPaidAt = :timestamp WHERE id = :id")
    suspend fun setPartialPayment(id: Int, amt: Double, timestamp: Long)

    @Query("UPDATE due_dates SET isArchived = 1, isDeleted = 0, archivedAt = :timestamp WHERE id = :id")
    suspend fun archiveById(id: Int, timestamp: Long)

    @Query("UPDATE due_dates SET isArchived = 0, isDeleted = 0 WHERE id = :id")
    suspend fun unarchiveById(id: Int)

    @Query("UPDATE due_dates SET isDeleted = 1, isArchived = 0, deletedAt = :timestamp WHERE id = :id")
    suspend fun deleteById(id: Int, timestamp: Long)

    @Query("UPDATE due_dates SET isDeleted = 0, isArchived = 0 WHERE id = :id")
    suspend fun restoreById(id: Int)

    @Query("DELETE FROM due_dates WHERE id = :id")
    suspend fun deletePermanently(id: Int)

    @Query("UPDATE due_dates SET isDeleted = 1, isArchived = 0, deletedAt = :timestamp WHERE isArchived = 1 AND isDeleted = 0")
    suspend fun trashAllArchived(timestamp: Long)

    @Query("DELETE FROM due_dates WHERE isDeleted = 1")
    suspend fun deleteAllTrashed()

    @Query("""
        UPDATE due_dates 
        SET customName = :name 
        WHERE bankName = :bank 
        AND (cardName = :card OR cardName IS NULL AND :card IS NULL)
        AND (cardNumber = :cardNumber OR cardNumber IS NULL AND :cardNumber IS NULL)
    """)
    suspend fun renameCard(bank: String, card: String?, cardNumber: String?, name: String)

    @Query("""
        SELECT * FROM due_dates 
        WHERE bankName = :bank 
        AND (cardName = :card OR cardName IS NULL AND :card IS NULL)
        AND (cardNumber = :cardNumber OR cardNumber IS NULL AND :cardNumber IS NULL)
        AND isDeleted = 0 
        ORDER BY dueDate DESC
    """)
    fun getHistory(bank: String, card: String?, cardNumber: String?): Flow<List<DueDate>>

    @Query("""
        SELECT * FROM due_dates 
        WHERE bankName = :bank 
        AND (cardName = :card OR cardName IS NULL AND :card IS NULL)
        AND (cardNumber = :cardNumber OR cardNumber IS NULL AND :cardNumber IS NULL)
        AND amount = :amt 
        AND ABS(dueDate - :date) < 2160000000
        AND isDeleted = 0 
        AND isArchived = 0
        LIMIT 1
    """)
    suspend fun findDuplicate(bank: String, card: String?, cardNumber: String?, amt: Double, date: Long): DueDate?

    @Query("""
        SELECT customName FROM due_dates 
        WHERE bankName = :bank 
        AND (cardName = :card OR cardName IS NULL AND :card IS NULL)
        AND (cardNumber = :cardNumber OR cardNumber IS NULL AND :cardNumber IS NULL)
        AND customName IS NOT NULL 
        LIMIT 1
    """)
    suspend fun findExistingCustomName(bank: String, card: String?, cardNumber: String?): String?

    @Query("SELECT * FROM due_dates WHERE isNewDuplicate = 1")
    suspend fun getNewDuplicates(): List<DueDate>

    @Query("UPDATE due_dates SET isNewDuplicate = 0")
    suspend fun clearNewDuplicatesFlag()

    @Query("""
        UPDATE due_dates 
        SET isArchived = 1, archivedAt = :timestamp 
        WHERE bankName = :bank 
        AND (cardName = :card OR cardName IS NULL AND :card IS NULL)
        AND (cardNumber = :cardNumber OR cardNumber IS NULL AND :cardNumber IS NULL)
        AND isPaid = 1 
        AND isArchived = 0 
        AND isDeleted = 0
        AND dueDate < :currentBillDueDate
    """)
    suspend fun archiveOlderPaidBills(bank: String, card: String?, cardNumber: String?, currentBillDueDate: Long, timestamp: Long)

    @Query("SELECT MAX(receivedDate) FROM due_dates")
    suspend fun getLatestReceivedDate(): Long?
}
