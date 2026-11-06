package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_security")

class PasswordStore(private val context: Context) {
    companion object {
        private val PASSWORD_HASH_KEY = stringPreferencesKey("password_hash")
    }

    val hasPasswordFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[PASSWORD_HASH_KEY].isNullOrEmpty()
    }

    suspend fun setPassword(password: String) {
        val hashed = sha256("shop:$password")
        context.dataStore.edit { prefs ->
            prefs[PASSWORD_HASH_KEY] = hashed
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        val stored = context.dataStore.data.first()[PASSWORD_HASH_KEY] ?: return false
        return sha256("shop:$password") == stored
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
