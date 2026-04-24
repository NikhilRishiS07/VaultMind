package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insert(entity: ExpenseEntity)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("SELECT IFNULL(SUM(amountValue), 0.0) FROM expenses")
    suspend fun sumAmount(): Double
}
