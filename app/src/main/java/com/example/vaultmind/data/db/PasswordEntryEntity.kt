package com.example.vaultmind.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_entries")
data class PasswordEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceEnc: String,
    val usernameEnc: String,
    val passwordEnc: String,
    val strengthEnc: String,
    val createdAt: Long
)
