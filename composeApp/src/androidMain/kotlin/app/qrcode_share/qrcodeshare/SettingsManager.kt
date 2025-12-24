package app.qrcode_share.qrcodeshare

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_AUTH = stringPreferencesKey("user_auth")
        val USERNAME = stringPreferencesKey("username")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val HOST_ADDRESS = stringPreferencesKey("host_address")
        val HOST_PORT = intPreferencesKey("host_port")
    }

    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID] ?: ""
    }

    val userAuth: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_AUTH] ?: ""
    }

    val username: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME] ?: ""
    }

    val darkMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: "System"
    }

    val themeColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_COLOR] ?: "Blue"
    }

    val hostAddress: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOST_ADDRESS] ?: ""
    }

    val hostPort: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HOST_PORT] ?: 8080
    }

    suspend fun saveUserId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = id
        }
    }

    suspend fun saveUserAuth(auth: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AUTH] = auth
        }
    }

    suspend fun saveUsername(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME] = name
        }
    }

    suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = mode
        }
    }

    suspend fun saveThemeColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_COLOR] = color
        }
    }

    suspend fun saveHostAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[HOST_ADDRESS] = address
        }
    }

    suspend fun saveHostPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[HOST_PORT] = port
        }
    }
}

