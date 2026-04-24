package com.example.vaultmind

import android.content.Context
import com.example.vaultmind.data.VaultRepository
import com.example.vaultmind.data.auth.AppLockManager
import com.example.vaultmind.data.db.VaultMindDatabase
import com.example.vaultmind.data.security.CryptoManager

object AppGraph {
    @Volatile
    private var repositoryInstance: VaultRepository? = null

    @Volatile
    private var lockManagerInstance: AppLockManager? = null

    fun repository(context: Context): VaultRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: buildRepository(context).also { repositoryInstance = it }
        }
    }

    fun lockManager(context: Context): AppLockManager {
        return lockManagerInstance ?: synchronized(this) {
            lockManagerInstance ?: AppLockManager(context.applicationContext).also { lockManagerInstance = it }
        }
    }

    private fun buildRepository(context: Context): VaultRepository {
        val db = VaultMindDatabase.getInstance(context)
        return VaultRepository(
            noteDao = db.noteDao(),
            passwordDao = db.passwordEntryDao(),
            expenseDao = db.expenseDao(),
            cryptoManager = CryptoManager()
        )
    }
}
