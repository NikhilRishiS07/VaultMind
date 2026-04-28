package com.example.vaultmind.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<NoteEntity>

    @Insert
    suspend fun insert(entity: NoteEntity): Long

    @Query("UPDATE notes SET title = :title, bodyEnc = :bodyEnc, bodyPlain = :bodyPlain, categoryEnc = :categoryEnc, locked = :locked, pinned = :pinned, isPublic = :isPublic, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(
        id: Long,
        title: String,
        bodyEnc: String,
        bodyPlain: String,
        categoryEnc: String,
        locked: Boolean,
        pinned: Boolean,
        isPublic: Boolean,
        updatedAt: Long
    )

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int
}
