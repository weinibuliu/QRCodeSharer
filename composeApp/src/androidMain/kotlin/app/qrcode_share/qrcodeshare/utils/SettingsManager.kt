package app.qrcode_share.qrcodeshare.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_AUTH = stringPreferencesKey("user_auth")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val FOLLOW_USER = intPreferencesKey("follow_user_id")
        val FOLLOW_USERS = stringPreferencesKey("follow_users") // JSON String like {40001: "name1", 40002: "name2"}
        val SHOW_SCAN_DETAILS = booleanPreferencesKey("show_scan_details")
        val HOST_ADDRESS = stringPreferencesKey("host_address")
        val HOST_PORT = intPreferencesKey("host_port")
    }

    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID] ?: ""
    }

    val userAuth: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_AUTH] ?: ""
    }

    val darkMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: "System"
    }

    val themeColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_COLOR] ?: "Blue"
    }

    val followUser: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[FOLLOW_USER] ?: 0
    }

    val followUsers: Flow<Map<Long, String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[FOLLOW_USERS] ?: "{}"
        try {
            Json.decodeFromString<Map<Long, String>>(jsonString)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val showScanDetails: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_SCAN_DETAILS] ?: false
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

    suspend fun saveFollowUser(followUser: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOW_USER] = followUser
        }
    }

    suspend fun saveFollowUsers(followUser: String) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOW_USERS] = followUser
        }
    }

    suspend fun saveFollowUsers(followUserMap: Map<Long, String>) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOW_USERS] = Json.encodeToString(followUserMap)
        }
    }

    suspend fun saveShowScanDetails(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_SCAN_DETAILS] = enabled
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
