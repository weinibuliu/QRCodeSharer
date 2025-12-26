package app.qrcode.qrcodeshare

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.qrcode.qrcodeshare.network.NetworkClient
import app.qrcode.qrcodeshare.utils.StoresManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storesManager = remember { StoresManager(context) }

    val userId by storesManager.userId.collectAsState(initial = "")
    val userAuth by storesManager.userAuth.collectAsState(initial = "")
    val darkMode by storesManager.darkMode.collectAsState(initial = "System")
    val themeColor by storesManager.themeColor.collectAsState(initial = "Blue")
    val followUsers by storesManager.followUsers.collectAsState(initial = emptyMap())
    val enableVibration by storesManager.enableVibration.collectAsState(initial = true)
    val showScanDetails by storesManager.showScanDetails.collectAsState(initial = false)
    val hostAddress by storesManager.hostAddress.collectAsState(initial = "")
    val connectTimeout by storesManager.connectTimeout.collectAsState(initial = "")
    val requestInterval by storesManager.requestInterval.collectAsState(initial = "")

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

    var requestIntervalInput by remember { mutableStateOf(connectTimeout.toString()) }
    var requestIntervalSynced by remember { mutableStateOf(false) }
    LaunchedEffect(requestInterval) {
        if (!requestIntervalSynced && requestInterval != "") {
            requestIntervalInput = requestInterval.toString()
            requestIntervalSynced = true
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val notificationCard = @Composable {
        if (notificationMessage != null) {
            OutlinedCard(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNotificationError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isNotificationError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notificationMessage!!,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { notificationMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }

    val userConfig = @Composable {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "用户配置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = userIdInput,
                    onValueChange = {
                        userIdInput = it
                        if (it.all { char -> char.isDigit() }) {
                            scope.launch { storesManager.saveUserId(it) }
                        }
                    },
                    label = { Text("User ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userAuthInput,
                    onValueChange = {
                        userAuthInput = it
                        scope.launch { storesManager.saveUserAuth(it) }
                    },
                    label = { Text("User Auth") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                var showDialog by remember { mutableStateOf(false) }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("修改用户名") },
                        text = { Text("此功能尚未实现") },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("确定")
                            }
                        }
                    )
                }
                ElevatedButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("修改用户名")
                }
            }
        }
    }

    val appearanceConfig = @Composable {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "外观设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                var darkModeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = darkModeExpanded,
                    onExpandedChange = { darkModeExpanded = !darkModeExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = when (darkMode) {
                            "Light" -> "浅色模式"
                            "Dark" -> "深色模式"
                            else -> "跟随系统"
                        },
                        onValueChange = { },
                        label = { Text("深色模式") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = darkModeExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = darkModeExpanded,
                        onDismissRequest = { darkModeExpanded = false }
                    ) {
                        listOf(
                            "System" to "跟随系统",
                            "Light" to "浅色模式",
                            "Dark" to "深色模式"
                        ).forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    scope.launch { storesManager.saveDarkMode(key) }
                                    darkModeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var themeColorExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = themeColorExpanded,
                    onExpandedChange = { themeColorExpanded = !themeColorExpanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = themeColor,
                        onValueChange = { },
                        label = { Text("主题色") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeColorExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = themeColorExpanded,
                        onDismissRequest = { themeColorExpanded = false }
                    ) {
                        listOf("Blue", "Red", "Green", "Purple").forEach { color ->
                            DropdownMenuItem(
                                text = { Text(color) },
                                onClick = {
                                    scope.launch { storesManager.saveThemeColor(color) }
                                    themeColorExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val followUsersConfig = @Composable {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "关注用户",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                var showDialog by remember { mutableStateOf(false) }
                var editingUser by remember { mutableStateOf<Pair<Int, String>?>(null) }

                if (showDialog) {
                    FollowUserDialog(
                        id = editingUser?.first?.toString() ?: "",
                        name = editingUser?.second ?: "",
                        onDismiss = {
                            showDialog = false
                            editingUser = null
                        },
                        onConfirm = { id, name ->
                            scope.launch {
                                storesManager.saveFollowUsers(id.toInt(), name)
                                notificationMessage = if (editingUser == null) "关注用户已添加" else "关注用户已更新"
                                isNotificationError = false
                            }
                            showDialog = false
                            editingUser = null
                        }
                    )
                }

                ElevatedButton(
                    onClick = {
                        editingUser = null
                        showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加关注用户")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (followUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "暂无关注用户",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    followUsers.forEach { (id, name) ->
                        ListItem(
                            headlineContent = {
                                Text("$name ($id)")
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        editingUser = id to name
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                    IconButton(onClick = {
                                        scope.launch { storesManager.removeFollowUser(id) }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    val advancedConfig = @Composable {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "高级设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "除非您了解您在干什么 否则不要更改任何内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hostAddressInput,
                    onValueChange = {
                        hostAddressInput = it
                        scope.launch { storesManager.saveHostAddress(it) }
                    },
                    label = { Text("主机地址") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = connectTimeoutInput,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            connectTimeoutInput = it
                            if (it.isNotEmpty()) {
                                scope.launch { storesManager.saveConnectTimeout(it.toLong()) }
                            }
                        }
                    },
                    label = { Text("连接超时 (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = requestIntervalInput,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            requestIntervalInput = it
                            if (it.isNotEmpty()) {
                                scope.launch { storesManager.saveRequestInterval(it.toLong()) }
                            }
                        }
                    },
                    label = { Text("请求间隔 (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("显示扫描详情", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showScanDetails,
                        onCheckedChange = { scope.launch { storesManager.saveShowScanDetails(it) } }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("启用震动", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enableVibration,
                        onCheckedChange = { scope.launch { storesManager.saveEnableVibration(it) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val service = NetworkClient.getService()
                                if (service != null) {
                                    val uId = userId.toIntOrNull()
                                    if (uId != null) {
                                        service.testConnection(uId, userAuth)
                                        notificationMessage = "连接成功"
                                        isNotificationError = false
                                    } else {
                                        notificationMessage = "User ID 无效"
                                        isNotificationError = true
                                    }
                                } else {
                                    notificationMessage = "服务未初始化"
                                    isNotificationError = true
                                }
                            } catch (e: Exception) {
                                notificationMessage = "连接失败: ${e.message}"
                                isNotificationError = true
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Text("测试服务器连接")
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically { it / 4 }
                ) {
                    Column {
                        notificationCard()
                        userConfig()
                        Spacer(modifier = Modifier.height(16.dp))
                        appearanceConfig()
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically { it / 4 }
                ) {
                    Column {
                        followUsersConfig()
                        Spacer(modifier = Modifier.height(16.dp))
                        advancedConfig()
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically { it / 4 }
            ) {
                Column {
                    notificationCard()
                    userConfig()
                    Spacer(modifier = Modifier.height(16.dp))
                    appearanceConfig()
                    Spacer(modifier = Modifier.height(16.dp))
                    followUsersConfig()
                    Spacer(modifier = Modifier.height(16.dp))
                    advancedConfig()
                }
            }
        }
    }
}

@Composable
fun FollowUserDialog(
    id: String,
    name: String,
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String) -> Unit
) {
    var userId by remember { mutableStateOf(id) }
    var userName by remember { mutableStateOf(name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Follow User", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            userId = it
                        }
                    },
                    label = { Text("用户 ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (userId.isNotBlank() && userName.isNotBlank()) {
                    onConfirm(userId, userName)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
