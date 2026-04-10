package com.mateyou.duedate.data

import kotlinx.coroutines.flow.Flow

class DueDateRepository(
    private val dueDateDao: DueDateDao,
    private val bankDao: BankDao
) {
    val activeDueDates: Flow<List<DueDate>> = dueDateDao.getActiveDueDates()
    val archivedDueDates: Flow<List<DueDate>> = dueDateDao.getArchivedDueDates()
    val deletedDueDates: Flow<List<DueDate>> = dueDateDao.getDeletedDueDates()

    // Bank Operations
    val activeBanks: Flow<List<BankEntity>> = bankDao.getActiveBanksFlow()
    val deletedBanks: Flow<List<BankEntity>> = bankDao.getDeletedBanksFlow()

    suspend fun insertBank(bank: BankEntity) {
        bankDao.insert(bank)
    }

    suspend fun updateBank(bank: BankEntity) {
        bankDao.update(bank)
    }

    suspend fun setBankDeletedStatus(id: Int, deleted: Boolean) {
        bankDao.setDeletedStatus(id, deleted)
    }

    suspend fun getBankByName(name: String): BankEntity? {
        return bankDao.getBankByName(name)
    }

    suspend fun getAllBanksSync(): List<BankEntity> {
        return bankDao.getAllBanksSync()
    }

    suspend fun deleteBankPermanently(bank: BankEntity) {
        bankDao.delete(bank)
    }

    // DueDate Operations
    suspend fun insert(dueDate: DueDate) {
        dueDateDao.insert(dueDate)
    }

    suspend fun getAllBillsSync(): List<DueDate> {
        return dueDateDao.getAllBillsSync()
    }

    suspend fun setPaidStatus(id: Int, isPaid: Boolean) {
        dueDateDao.setPaidStatus(id, isPaid, if (isPaid) System.currentTimeMillis() else null)
    }

    suspend fun setPartialPayment(id: Int, amount: Double) {
        dueDateDao.setPartialPayment(id, amount, System.currentTimeMillis())
    }

    suspend fun archive(id: Int) {
        dueDateDao.archiveById(id, System.currentTimeMillis())
    }

    suspend fun unarchive(id: Int) {
        dueDateDao.unarchiveById(id)
    }

    suspend fun delete(id: Int) {
        dueDateDao.deleteById(id, System.currentTimeMillis())
    }

    suspend fun restore(id: Int) {
        dueDateDao.restoreById(id)
    }

    suspend fun deletePermanently(id: Int) {
        dueDateDao.deletePermanently(id)
    }

    suspend fun trashAllArchived() {
        dueDateDao.trashAllArchived(System.currentTimeMillis())
    }

    suspend fun deleteAllTrashed() {
        dueDateDao.deleteAllTrashed()
    }

    suspend fun archiveOlderPaidBills(bank: String, card: String?, cardNumber: String?, currentBillDueDate: Long) {
        dueDateDao.archiveOlderPaidBills(bank, card, cardNumber, currentBillDueDate, System.currentTimeMillis())
    }

    suspend fun getLatestReceivedDate(): Long? {
        return dueDateDao.getLatestReceivedDate()
    }

    suspend fun renameCard(bank: String, card: String?, cardNumber: String?, name: String) {
        dueDateDao.renameCard(bank, card, cardNumber, name)
    }

    fun getHistory(bank: String, card: String?, cardNumber: String?): Flow<List<DueDate>> {
        return dueDateDao.getHistory(bank, card, cardNumber)
    }

    suspend fun findDuplicate(bank: String, card: String?, cardNumber: String?, amt: Double, date: Long): DueDate? {
        return dueDateDao.findDuplicate(bank, card, cardNumber, amt, date)
    }

    suspend fun findExistingCustomName(bank: String, card: String?, cardNumber: String?): String? {
        return dueDateDao.findExistingCustomName(bank, card, cardNumber)
    }

    suspend fun getNewDuplicates(): List<DueDate> {
        return dueDateDao.getNewDuplicates()
    }

    suspend fun clearNewDuplicatesFlag() {
        dueDateDao.clearNewDuplicatesFlag()
    }
}
