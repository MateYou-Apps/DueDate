package com.mateyou.duedate.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BankDao {
    @Query("SELECT * FROM custom_banks ORDER BY name ASC")
    fun getAllBanksFlow(): Flow<List<BankEntity>>

    @Query("SELECT * FROM custom_banks WHERE isDeleted = 0 ORDER BY name ASC")
    fun getActiveBanksFlow(): Flow<List<BankEntity>>

    @Query("SELECT * FROM custom_banks WHERE isDeleted = 1 ORDER BY name ASC")
    fun getDeletedBanksFlow(): Flow<List<BankEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bank: BankEntity)

    @Update
    suspend fun update(bank: BankEntity)

    @Delete
    suspend fun delete(bank: BankEntity)

    @Query("UPDATE custom_banks SET isDeleted = :deleted WHERE id = :id")
    suspend fun setDeletedStatus(id: Int, deleted: Boolean)

    @Query("SELECT * FROM custom_banks WHERE name = :name LIMIT 1")
    suspend fun getBankByName(name: String): BankEntity?
    
    @Query("SELECT * FROM custom_banks")
    suspend fun getAllBanksSync(): List<BankEntity>
}
