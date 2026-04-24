package com.example.vaultmind

import android.content.Context
import com.example.vaultmind.BuildConfig
import com.example.vaultmind.data.VaultRepository
import com.example.vaultmind.data.auth.AppLockManager
import com.example.vaultmind.data.db.VaultMindDatabase
import com.example.vaultmind.data.security.CryptoManager
import com.example.vaultmind.notes.GroqSuggestionClient
import com.example.vaultmind.notes.GroqSuggestionsRepository

object AppGraph {
    @Volatile
    private var repositoryInstance: VaultRepository? = null

    @Volatile
    private var lockManagerInstance: AppLockManager? = null

    @Volatile
    private var groqSuggestionsRepositoryInstance: GroqSuggestionsRepository? = null

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

    fun groqSuggestionsRepository(context: Context): GroqSuggestionsRepository {
        return groqSuggestionsRepositoryInstance ?: synchronized(this) {
            groqSuggestionsRepositoryInstance ?: buildGroqSuggestionsRepository(context).also { groqSuggestionsRepositoryInstance = it }
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

    private fun buildGroqSuggestionsRepository(context: Context): GroqSuggestionsRepository {
        return GroqSuggestionsRepository(
            client = GroqSuggestionClient(
                apiKey = BuildConfig.GROQ_API_KEY,
                model = "llama-3.1-8b-instant"
            )
        )
    }
}
