package com.example.vaultmind.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, PasswordEntryEntity::class, ExpenseEntity::class],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns: title (plaintext), bodyPlain (plaintext for public notes), isPublic flag
                database.execSQL("ALTER TABLE notes ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN bodyPlain TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE notes ADD COLUMN isPublic INTEGER NOT NULL DEFAULT 0")

                // Attempt to decrypt existing titleEnc into title using CryptoManager
                try {
                    val cursor = database.query("SELECT id, titleEnc FROM notes")
                    val crypto = com.example.vaultmind.data.security.CryptoManager()
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val titleEnc = cursor.getString(1)
                        var decrypted = ""
                        try {
                            if (!titleEnc.isNullOrEmpty()) decrypted = crypto.decrypt(titleEnc)
                        } catch (e: Exception) {
                            // leave decrypted empty if decrypt fails
                        }
                        val stmt = database.compileStatement("UPDATE notes SET title = ? WHERE id = ?")
                        stmt.bindString(1, decrypted)
                        stmt.bindLong(2, id)
                        stmt.execute()
                    }
                    cursor.close()
                } catch (e: Exception) {
                    // best effort migration; if anything fails, leave title blank
                }
            }
        }
    }
}
