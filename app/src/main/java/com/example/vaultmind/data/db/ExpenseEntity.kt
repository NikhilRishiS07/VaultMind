package com.example.vaultmind.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleEnc: String,
    val subtitleEnc: String,
    val amountTextEnc: String,
    val amountValue: Double,
    val tagEnc: String,
    val createdAt: Long
)
