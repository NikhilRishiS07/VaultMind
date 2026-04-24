package com.example.vaultmind.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEnc: String,
    val bodyEnc: String,
    val categoryEnc: String,
    val locked: Boolean,
    val pinned: Boolean,
    val updatedAt: Long,
    val createdAt: Long
)
