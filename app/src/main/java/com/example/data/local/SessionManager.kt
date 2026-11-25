package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "kundarki_auth_session")

data class PersistedSession(
    val userId: String? = null,
    val accessToken: String? = null
)

class SessionManager(private val context: Context) {

    private val keyUserId = stringPreferencesKey("logged_in_user_id")
    private val keyAccessToken = stringPreferencesKey("supabase_access_token")

    val sessionFlow: Flow<PersistedSession> = context.authDataStore.data.map { prefs ->
        PersistedSession(
            userId = prefs[keyUserId],
            accessToken = prefs[keyAccessToken]
        )
    }

    suspend fun saveSession(userId: String, accessToken: String? = null) {
        context.authDataStore.edit { prefs ->
            prefs[keyUserId] = userId
            if (!accessToken.isNullOrBlank()) {
                prefs[keyAccessToken] = accessToken
            } else {
                prefs.remove(keyAccessToken)
            }
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(keyUserId)
            prefs.remove(keyAccessToken)
        }
    }
}
