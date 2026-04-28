package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<ExpenseEntity>>

    @Insert
    suspend fun insert(entity: ExpenseEntity)

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("SELECT IFNULL(SUM(amountValue), 0.0) FROM expenses")
    suspend fun sumAmount(): Double
}

