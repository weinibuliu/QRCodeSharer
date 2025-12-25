package app.qrcode.qrcodeshare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.qrcode.qrcodeshare.network.NetworkClient
import app.qrcode.qrcodeshare.utils.SettingsManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val userId by settingsManager.userId.collectAsState(initial = "")
    val userAuth by settingsManager.userAuth.collectAsState(initial = "")
    val darkMode by settingsManager.darkMode.collectAsState(initial = "System")
    val themeColor by settingsManager.themeColor.collectAsState(initial = "Blue")
    val followUsers by settingsManager.followUsers.collectAsState(initial = emptyMap())
    val enableVibration by settingsManager.enableVibration.collectAsState(initial = true)
    val showScanDetails by settingsManager.showScanDetails.collectAsState(initial = false)
    val hostAddress by settingsManager.hostAddress.collectAsState(initial = "")
    val connectTimeout by settingsManager.connectTimeout.collectAsState(initial = "")

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isNotificationError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Local states for text fields to prevent cursor jumping
    var userIdInput by remember { mutableStateOf(userId) }
    var isUserIdSynced by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        if (!isUserIdSynced && userId.isNotEmpty()) {
            userIdInput = userId
            isUserIdSynced = true
        }
    }

    var userAuthInput by remember { mutableStateOf(userAuth) }
    var isUserAuthSynced by remember { mutableStateOf(false) }
    LaunchedEffect(userAuth) {
        if (!isUserAuthSynced && userAuth.isNotEmpty()) {
            userAuthInput = userAuth
            isUserAuthSynced = true
        }
    }

    var hostAddressInput by remember { mutableStateOf(hostAddress) }
    var isHostAddressSynced by remember { mutableStateOf(false) }
    LaunchedEffect(hostAddress) {
        if (!isHostAddressSynced && hostAddress.isNotEmpty()) {
            hostAddressInput = hostAddress
            isHostAddressSynced = true
        }
    }

    var connectTimeoutInput by remember { mutableStateOf(connectTimeout.toString()) }
    var isConnectTimeoutSynced by remember { mutableStateOf(false) }
    LaunchedEffect(connectTimeout) {
        if (!isConnectTimeoutSynced && connectTimeout != "") {
            connectTimeoutInput = connectTimeout.toString()
            isConnectTimeoutSynced = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (notificationMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNotificationError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isNotificationError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notificationMessage!!,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { notificationMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }

        Text("用户配置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userIdInput,
            onValueChange = {
                userIdInput = it
                scope.launch { settingsManager.saveUserId(it) }
            },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userAuthInput,
            onValueChange = {
                userAuthInput = it
                scope.launch { settingsManager.saveUserAuth(it) }
            },
            label = { Text("User Auth") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    notificationMessage = null
                    val service = NetworkClient.getService()
                    if (service == null) {
                        notificationMessage = "请先配置主机地址和端口"
                        isNotificationError = true
                        isLoading = false
                        return@launch
                    }

                    val id = userIdInput.toIntOrNull()
                    if (id == null) {
                        notificationMessage = "User ID 必须是数字"
                        isNotificationError = true
                        isLoading = false
                        return@launch
                    }

                    try {
                        service.testConnection(id, userAuthInput)
                        notificationMessage = "连接成功"
                        isNotificationError = false
                    } catch (e: Exception) {
                        notificationMessage = "连接失败: ${e.message}"
                        isNotificationError = true
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("连接中...")
            } else {
                Text("测试服务器连接")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("偏好设置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Text("关注用户", style = MaterialTheme.typography.titleMedium)
        if (followUsers.isNotEmpty()) {
            followUsers.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$name ($id)", style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = {
                        val newMap = followUsers.toMutableMap()
                        newMap.remove(id)
                        scope.launch { settingsManager.saveFollowUsers(newMap) }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                HorizontalDivider()
            }
        } else {
            Text(
                text = "暂无关注用户",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showAddUserDialog by remember { mutableStateOf(false) }
        var showImportJsonDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { showAddUserDialog = true }) {
                Text("添加关注用户")
            }

            Button(onClick = { showImportJsonDialog = true }) {
                Text("从 JSON 导入")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("允许震动")
            Switch(
                checked = enableVibration,
                onCheckedChange = { scope.launch { settingsManager.saveEnableVibration(it) } }
            )
        }



        if (showAddUserDialog) {
            var newId by remember { mutableStateOf("") }
            var newName by remember { mutableStateOf("") }
            var isNameError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddUserDialog = false },
                title = { Text("添加关注用户") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newId,
                            onValueChange = { if (it.all { char -> char.isDigit() }) newId = it },
                            label = { Text("User ID (数字)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                isNameError = false
                            },
                            label = { Text("Name") },
                            isError = isNameError,
                            supportingText = { if (isNameError) Text("Name 不能为空") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val id = newId.toLongOrNull()
                        if (id != null && newName.isNotBlank()) {
                            val newMap = followUsers.toMutableMap()
                            newMap[id] = newName
                            scope.launch { settingsManager.saveFollowUsers(newMap) }
                            showAddUserDialog = false
                        } else if (newName.isBlank()) {
                            isNameError = true
                        }
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUserDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showImportJsonDialog) {
            var jsonInput by remember { mutableStateOf("") }
            var errorText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showImportJsonDialog = false },
                title = { Text("从 JSON 导入") },
                text = {
                    Column {
                        Text("""请输入 JSON 字符串，例如：{12345:"Alice", 67890:"Bob"}""")
                        OutlinedTextField(
                            value = jsonInput,
                            onValueChange = { jsonInput = it },
                            label = { Text("JSON String") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )
                        if (errorText.isNotEmpty()) {
                            Text(errorText, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            val importedMap = Json.decodeFromString<Map<Long, String>>(jsonInput)
                            if (importedMap.values.any { it.isBlank() }) {
                                errorText = "导入失败: 存在用户的 Name 为空"
                            } else {
                                val newMap = followUsers.toMutableMap()
                                newMap.putAll(importedMap)
                                scope.launch { settingsManager.saveFollowUsers(newMap) }
                                showImportJsonDialog = false
                            }
                        } catch (e: Exception) {
                            errorText = "解析失败: ${e.message}"
                        }
                    }) {
                        Text("导入")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportJsonDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("外观设置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        val darkModeMap = mapOf(
            "System" to "跟随系统",
            "Light" to "浅色模式",
            "Dark" to "深色模式"
        )
        val darkModeOptions = darkModeMap.values.toList()
        val currentDarkModeDisplay = darkModeMap[darkMode] ?: "跟随系统"

        SettingsDropdown(
            label = "深色模式",
            options = darkModeOptions,
            selectedOption = currentDarkModeDisplay,
            onOptionSelected = { selected ->
                val internalValue = darkModeMap.entries.firstOrNull { it.value == selected }?.key ?: "System"
                if (internalValue != darkMode) {
                    scope.launch { settingsManager.saveDarkMode(internalValue) }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val themeColorMap = mapOf(
            "Blue" to "蓝色",
            "Red" to "红色",
            "Green" to "绿色",
            "Purple" to "紫色"
        )
        val themeColorOptions = themeColorMap.values.toList()
        val currentThemeColorDisplay = themeColorMap[themeColor] ?: "蓝色"

        SettingsDropdown(
            label = "主题色",
            options = themeColorOptions,
            selectedOption = currentThemeColorDisplay,
            onOptionSelected = { selected ->
                val internalValue = themeColorMap.entries.firstOrNull { it.value == selected }?.key ?: "Blue"
                if (internalValue != themeColor) {
                    scope.launch { settingsManager.saveThemeColor(internalValue) }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("高级设置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Warning")
                Spacer(modifier = Modifier.width(8.dp))
                Text("除非您了解您在干什么，否则不要更改任何内容！")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("显示扫描详情")
            Switch(
                checked = showScanDetails,
                onCheckedChange = { scope.launch { settingsManager.saveShowScanDetails(it) } }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = hostAddressInput,
            onValueChange = {
                hostAddressInput = it
                scope.launch { settingsManager.saveHostAddress(it) }
            },
            label = { Text("主机地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = connectTimeoutInput,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    connectTimeoutInput = newValue
                    val timeout = newValue.toLongOrNull() ?: 2500L
                    scope.launch { settingsManager.saveConnectTimeout(timeout) }
                }
            },
            label = { Text("请求超时时间 (毫秒)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}
