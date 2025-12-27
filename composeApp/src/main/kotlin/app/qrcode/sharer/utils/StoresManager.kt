package app.qrcode.sharer.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stores")


class StoresManager(private val context: Context) {

    companion object {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_AUTH = stringPreferencesKey("user_auth")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val FOLLOW_USER = intPreferencesKey("follow_user_id")
        val FOLLOW_USERS = stringPreferencesKey("follow_users") // JSON String like {40001: "name1", 40002: "name2"}
        val ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
        val SHOW_SCAN_DETAILS = booleanPreferencesKey("show_scan_details")
        val HOST_ADDRESS = stringPreferencesKey("host_address")
        val CONNECT_TIMEOUT = longPreferencesKey("connect_timeout")
        val REQUEST_INTERVAL = longPreferencesKey("request_interval")
        val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val FORCE_SHOW_UPDATE_DIALOG = booleanPreferencesKey("force_show_update_dialog")
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

    val followUsers: Flow<Map<Int, String>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[FOLLOW_USERS] ?: "{}"
        try {
            Json.decodeFromString<Map<Int, String>>(jsonString)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    val enableVibration: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_VIBRATION] ?: true
    }

    val showScanDetails: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_SCAN_DETAILS] ?: false
    }

    val hostAddress: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOST_ADDRESS] ?: ""
    }

    val connectTimeout: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[CONNECT_TIMEOUT] ?: 2500
    }

    val requestInterval: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[REQUEST_INTERVAL] ?: 500
    }

    val autoCheckUpdate: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_CHECK_UPDATE] ?: true
    }

    val developerMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVELOPER_MODE] ?: false
    }

    val forceShowUpdateDialog: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FORCE_SHOW_UPDATE_DIALOG] ?: false
    }

    /**
     * 获取所有存储的键值对（用于开发者调试）
     */
    val allPreferences: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        preferences.asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }
    }

    /**
     * 通用保存方法（用于开发者调试）
     */
    suspend fun savePreference(key: String, value: String) {
        context.dataStore.edit { preferences ->
            when (key) {
                USER_ID.name -> preferences[USER_ID] = value
                USER_AUTH.name -> preferences[USER_AUTH] = value
                DARK_MODE.name -> preferences[DARK_MODE] = value
                THEME_COLOR.name -> preferences[THEME_COLOR] = value
                FOLLOW_USER.name -> value.toIntOrNull()?.let { preferences[FOLLOW_USER] = it }
                FOLLOW_USERS.name -> preferences[FOLLOW_USERS] = value
                ENABLE_VIBRATION.name -> preferences[ENABLE_VIBRATION] = value.toBooleanStrictOrNull() ?: false
                SHOW_SCAN_DETAILS.name -> preferences[SHOW_SCAN_DETAILS] = value.toBooleanStrictOrNull() ?: false
                HOST_ADDRESS.name -> preferences[HOST_ADDRESS] = value
                CONNECT_TIMEOUT.name -> value.toLongOrNull()?.let { preferences[CONNECT_TIMEOUT] = it }
                REQUEST_INTERVAL.name -> value.toLongOrNull()?.let { preferences[REQUEST_INTERVAL] = it }
                AUTO_CHECK_UPDATE.name -> preferences[AUTO_CHECK_UPDATE] = value.toBooleanStrictOrNull() ?: true
                DEVELOPER_MODE.name -> preferences[DEVELOPER_MODE] = value.toBooleanStrictOrNull() ?: false
                FORCE_SHOW_UPDATE_DIALOG.name -> preferences[FORCE_SHOW_UPDATE_DIALOG] =
                    value.toBooleanStrictOrNull() ?: false

                else -> preferences[stringPreferencesKey(key)] = value
            }
        }
    }

    /**
     * 保存任意键值对（用于开发者调试添加新键）
     */
    suspend fun saveRawPreference(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
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

    suspend fun saveFollowUsers(id: Int, name: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[FOLLOW_USERS] ?: "{}"
            val currentMap = try {
                Json.decodeFromString<MutableMap<Int, String>>(currentJson)
            } catch (_: Exception) {
                mutableMapOf()
            }
            currentMap[id] = name
            preferences[FOLLOW_USERS] = Json.encodeToString(currentMap)
        }
    }

    suspend fun removeFollowUser(id: Int) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[FOLLOW_USERS] ?: "{}"
            val currentMap = try {
                Json.decodeFromString<MutableMap<Int, String>>(currentJson)
            } catch (_: Exception) {
                mutableMapOf()
            }
            currentMap.remove(id)
            preferences[FOLLOW_USERS] = Json.encodeToString(currentMap)
        }
    }

    suspend fun saveFollowUsers(followUserMap: Map<Int, String>) {
        context.dataStore.edit { preferences ->
            preferences[FOLLOW_USERS] = Json.encodeToString(followUserMap)
        }
    }

    suspend fun saveEnableVibration(enableVibration: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_VIBRATION] = enableVibration
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

    suspend fun saveConnectTimeout(timeout: Long) {
        context.dataStore.edit { preferences ->
            preferences[CONNECT_TIMEOUT] = timeout
        }
    }

    suspend fun saveRequestInterval(interval: Long) {
        context.dataStore.edit { preferences ->
            preferences[REQUEST_INTERVAL] = interval
        }
    }

    suspend fun saveAutoCheckUpdate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_CHECK_UPDATE] = enabled
        }
    }

    suspend fun saveDeveloperMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVELOPER_MODE] = enabled
        }
    }

    suspend fun saveForceShowUpdateDialog(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FORCE_SHOW_UPDATE_DIALOG] = enabled
        }
    }
}
