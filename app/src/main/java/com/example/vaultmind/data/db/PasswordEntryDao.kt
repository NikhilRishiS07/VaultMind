package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PasswordEntryDao {
    @Query("SELECT * FROM password_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<PasswordEntryEntity>

    @Insert
    suspend fun insert(entity: PasswordEntryEntity)

    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun count(): Int
}
