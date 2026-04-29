package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PasswordEntryDao {
    @Query("SELECT * FROM password_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<PasswordEntryEntity>

    @Insert
    suspend fun insert(entity: PasswordEntryEntity)

    @Update
    suspend fun update(entity: PasswordEntryEntity)

    @Query("DELETE FROM password_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun count(): Int
}
