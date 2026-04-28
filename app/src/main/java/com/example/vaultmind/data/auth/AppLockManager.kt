package com.example.vaultmind.data.auth

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppLockManager(private val context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun ensureTempUser() {
        if (prefs.contains(KEY_USERNAME)) {
            // Keep demo credentials in sync across app updates.
            val existingUsername = prefs.getString(KEY_USERNAME, null)
            if (existingUsername == TEMP_USERNAME) {
                resetTempPasswordHash()
            }
            return
        }

        val username = TEMP_USERNAME
        val password = TEMP_PASSWORD
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(password, salt)

        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putBoolean(KEY_LOCKED, true)
            .apply()
    }

    fun verifyCredentials(username: String, password: String): Boolean {
        val savedUsername = prefs.getString(KEY_USERNAME, null) ?: return false
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false

        if (username != savedUsername) return false

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val providedHash = hashPassword(password, salt)
        val matches = providedHash.contentEquals(expectedHash)

        if (matches) {
            prefs.edit().putBoolean(KEY_LOCKED, false).apply()
        }

        return matches
    }

    fun markLocked() {
        prefs.edit().putBoolean(KEY_LOCKED, true).apply()
    }

    fun isLocked(): Boolean = prefs.getBoolean(KEY_LOCKED, true)

    fun tempUsername(): String = prefs.getString(KEY_USERNAME, TEMP_USERNAME) ?: TEMP_USERNAME

    private fun resetTempPasswordHash() {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val hash = hashPassword(TEMP_PASSWORD, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(keySpec).encoded
    }

    companion object {
        const val TEMP_USERNAME = "vaultpilot"
        const val TEMP_PASSWORD = "123"

        private const val PREF_FILE = "vaultmind_secure_auth"
        private const val KEY_USERNAME = "username"
        private const val KEY_SALT = "salt"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_LOCKED = "locked"

        private const val HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val HASH_ITERATIONS = 120_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_SIZE = 16
    }
}
