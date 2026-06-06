package com.klaustracker.app.data.local

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

object DatabaseEncryption {
    private const val PREF_FILE = "tracker_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTES = 32

    fun supportFactory(context: Context): SupportFactory {
        val passphrase = getOrCreatePassphrase(context.applicationContext)
        return SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
    }

    private fun getOrCreatePassphrase(context: Context): String {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val raw = ByteArray(PASSPHRASE_BYTES)
        SecureRandom().nextBytes(raw)
        val generated = Base64.encodeToString(raw, Base64.NO_WRAP)
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated
    }
}
