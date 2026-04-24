package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Insert
    suspend fun insert(entity: NoteEntity)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
