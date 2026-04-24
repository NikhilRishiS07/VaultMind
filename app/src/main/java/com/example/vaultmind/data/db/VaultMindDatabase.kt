package com.example.vaultmind.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, PasswordEntryEntity::class, ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VaultMindDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun passwordEntryDao(): PasswordEntryDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: VaultMindDatabase? = null

        fun getInstance(context: Context): VaultMindDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultMindDatabase::class.java,
                    "vaultmind.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
