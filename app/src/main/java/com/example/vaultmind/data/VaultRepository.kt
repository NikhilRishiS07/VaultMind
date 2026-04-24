package com.example.vaultmind.data

import com.example.vaultmind.data.db.ExpenseDao
import com.example.vaultmind.data.db.ExpenseEntity
import com.example.vaultmind.data.db.NoteDao
import com.example.vaultmind.data.db.NoteEntity
import com.example.vaultmind.data.db.PasswordEntryDao
import com.example.vaultmind.data.db.PasswordEntryEntity
import com.example.vaultmind.data.security.CryptoManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class VaultRepository(
    private val noteDao: NoteDao,
    private val passwordDao: PasswordEntryDao,
    private val expenseDao: ExpenseDao,
    private val cryptoManager: CryptoManager
) {
    private val prettyDateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)
        .withZone(ZoneId.systemDefault())

    suspend fun ensureSeedData() {
        if (noteDao.count() == 0) {
            val now = System.currentTimeMillis()
            saveNote(
                title = "Vault Credentials",
                body = "Production keys and backup recovery steps. Keep locked.",
                category = "Locked",
                locked = true,
                pinned = true,
                createdAt = now - 2 * DAY_MS
            )
            saveNote(
                title = "Release Checklist",
                body = "QA, signing, export validation, and device smoke tests.",
                category = "Work",
                locked = false,
                pinned = false,
                createdAt = now - DAY_MS
            )
        }

        if (passwordDao.count() == 0) {
            savePassword("Google Workspace", "alex.design@gmail.com", "Aex$2481#Vault", "Strong")
            savePassword("Netflix Premium", "home_vault_2024", "Ntfx@5520!", "Good")
        }

        if (expenseDao.count() == 0) {
            saveExpense("Apple Store Online", "Yesterday, 4:20 PM • Electronics", "-$1,299.00", -1299.00, "Secured")
            saveExpense("The Daily Grind", "Today, 9:15 AM • Food & Drink", "-$18.40", -18.40, "Secured")
            saveExpense("Nimbus Subscription", "Today, 8:10 AM • Software", "-$39.99", -39.99, "Secured")
        }
    }

    suspend fun getNotes(): List<NoteRecord> {
        return noteDao.getAll().map { entity ->
            NoteRecord(
                id = entity.id,
                title = cryptoManager.decrypt(entity.titleEnc),
                preview = cryptoManager.decrypt(entity.bodyEnc),
                category = cryptoManager.decrypt(entity.categoryEnc),
                locked = entity.locked,
                pinned = entity.pinned,
                lastEdited = relativeTimeLabel(entity.updatedAt),
                createdAt = "Created ${prettyDateFormatter.format(Instant.ofEpochMilli(entity.createdAt))}"
            )
        }
    }

    suspend fun saveNote(
        title: String,
        body: String,
        category: String,
        locked: Boolean,
        pinned: Boolean,
        createdAt: Long = System.currentTimeMillis()
    ) {
        val now = System.currentTimeMillis()
        noteDao.insert(
            NoteEntity(
                titleEnc = cryptoManager.encrypt(title),
                bodyEnc = cryptoManager.encrypt(body),
                categoryEnc = cryptoManager.encrypt(category),
                locked = locked,
                pinned = pinned,
                updatedAt = now,
                createdAt = createdAt
            )
        )
    }

    suspend fun getPasswords(): List<PasswordRecord> {
        return passwordDao.getAll().map { entity ->
            PasswordRecord(
                id = entity.id,
                service = cryptoManager.decrypt(entity.serviceEnc),
                username = cryptoManager.decrypt(entity.usernameEnc),
                password = cryptoManager.decrypt(entity.passwordEnc),
                strength = cryptoManager.decrypt(entity.strengthEnc)
            )
        }
    }

    suspend fun savePassword(service: String, username: String, password: String, strength: String) {
        passwordDao.insert(
            PasswordEntryEntity(
                serviceEnc = cryptoManager.encrypt(service),
                usernameEnc = cryptoManager.encrypt(username),
                passwordEnc = cryptoManager.encrypt(password),
                strengthEnc = cryptoManager.encrypt(strength),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getExpenses(): List<ExpenseRecord> {
        return expenseDao.getAll().map { entity ->
            ExpenseRecord(
                id = entity.id,
                title = cryptoManager.decrypt(entity.titleEnc),
                subtitle = cryptoManager.decrypt(entity.subtitleEnc),
                amountText = cryptoManager.decrypt(entity.amountTextEnc),
                tag = cryptoManager.decrypt(entity.tagEnc),
                amountValue = entity.amountValue
            )
        }
    }

    suspend fun saveExpense(
        title: String,
        subtitle: String,
        amountText: String,
        amountValue: Double,
        tag: String
    ) {
        expenseDao.insert(
            ExpenseEntity(
                titleEnc = cryptoManager.encrypt(title),
                subtitleEnc = cryptoManager.encrypt(subtitle),
                amountTextEnc = cryptoManager.encrypt(amountText),
                amountValue = amountValue,
                tagEnc = cryptoManager.encrypt(tag),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun dashboardSummary(): DashboardSummary {
        val notesCount = noteDao.count()
        val passwordsCount = passwordDao.count()
        val spend = expenseDao.sumAmount()
        return DashboardSummary(
            notesCount = notesCount,
            passwordsCount = passwordsCount,
            spendText = String.format(Locale.US, "$%,.0f", kotlin.math.abs(spend))
        )
    }

    private fun relativeTimeLabel(timeMs: Long): String {
        val delta = (System.currentTimeMillis() - timeMs).coerceAtLeast(0)
        val minutes = delta / 60_000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Last edited just now"
            minutes < 60 -> "Last edited ${minutes}m ago"
            hours < 24 -> "Last edited ${hours}h ago"
            else -> "Last edited ${days}d ago"
        }
    }

    data class NoteRecord(
        val id: Long,
        val title: String,
        val preview: String,
        val category: String,
        val locked: Boolean,
        val pinned: Boolean,
        val lastEdited: String,
        val createdAt: String
    )

    data class PasswordRecord(
        val id: Long,
        val service: String,
        val username: String,
        val password: String,
        val strength: String
    )

    data class ExpenseRecord(
        val id: Long,
        val title: String,
        val subtitle: String,
        val amountText: String,
        val tag: String,
        val amountValue: Double
    )

    data class DashboardSummary(
        val notesCount: Int,
        val passwordsCount: Int,
        val spendText: String
    )

    companion object {
        private const val DAY_MS = 86_400_000L
    }
}
