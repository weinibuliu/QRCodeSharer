package app.qrcode.qrcodesharer

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.qrcode.qrcodesharer.network.NetworkClient
import app.qrcode.qrcodesharer.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 开发/调试构建警告横幅组件
 * 不可关闭，显示在设置页面顶部
 */
@Composable
fun DevBuildWarningBanner(buildType: BuildType, versionName: String) {
    val (title, description) = when (buildType) {
        BuildType.DEV -> "您当前正在使用开发构建。\n这并不是一个正式版本，因此可能会存在问题。" to "版本: $versionName"
        BuildType.DEBUG -> "您当前正在使用调试构建。\n这并不是一个正式版本，因此可能会存在问题。" to "版本: $versionName (Debug)"
        BuildType.RELEASE -> return // 正式版本不显示横幅
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * 更新可用弹窗组件
 */
@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    newVersion: String,
    channel: UpdateChannel,
    releaseUrl: String,
    releaseNotes: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val channelText = when (channel) {
        UpdateChannel.STABLE -> "稳定版"
        UpdateChannel.PRERELEASE -> "测试版"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("发现新版本 ($channelText)")
        },
        text = {
            Column {
                Text(
                    text = "$currentVersion → $newVersion",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = releaseNotes.take(500) + if (releaseNotes.length > 500) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, releaseUrl.toUri())
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("前往下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        }
    )
}

/**
 * 连接测试结果数据类
 */
data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
    val duration: Long? = null
)

/**
 * 内联状态指示器组件
 * 用于显示操作结果的反馈信息
 */
@Composable
fun InlineStatusIndicator(
    result: ConnectionTestResult?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = result != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        result?.let {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = if (it.success)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (it.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (it.success)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (it.success)
                            "${it.message} (${it.duration}ms)"
                        else
                            it.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.success)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * 测试连接按钮组件
 * 包含按钮和内联状态指示器
 */
@Composable
fun TestConnectionButton(
    isLoading: Boolean,
    testResult: ConnectionTestResult?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ElevatedButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试中...")
            } else {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("测试服务器连接")
            }
        }
        InlineStatusIndicator(result = testResult)
    }
}

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
    val autoCheckUpdate by storesManager.autoCheckUpdate.collectAsState(initial = true)

    // 用户配置的内联状态
    var userConfigTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isUserConfigLoading by remember { mutableStateOf(false) }

    // 高级设置的内联状态
    var advancedTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isAdvancedLoading by remember { mutableStateOf(false) }

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

    var isVisible by remember { mutableStateOf(true) }

    // 版本号和构建类型检查
    val versionName = remember { getAppVersionName(context) }
    val buildType = remember { getBuildType(context) }

    // 更新检查状态
    var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var includePreRelease by remember { mutableStateOf(false) }

    // 在 RELEASE 构建下自动检查更新（如果启用）
    LaunchedEffect(buildType, autoCheckUpdate) {
        if (buildType == BuildType.RELEASE && autoCheckUpdate) {
            isCheckingUpdate = true
            updateCheckResult = checkForUpdate(versionName, includePreRelease = false)
            isCheckingUpdate = false
            // 自动弹出更新对话框
            if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                showUpdateDialog = true
            }
        }
    }

    // 手动检查更新函数
    fun manualCheckUpdate() {
        isCheckingUpdate = true
        updateCheckResult = null
        scope.launch {
            updateCheckResult = checkForUpdate(versionName, includePreRelease = includePreRelease)
            isCheckingUpdate = false
            if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                showUpdateDialog = true
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE    // 测试服务器连接的通用函数，返回结果
    suspend fun performConnectionTest(): ConnectionTestResult {
        val service = NetworkClient.getService() ?: return ConnectionTestResult(false, "请先配置主机地址")

        val uId = userId.toIntOrNull() ?: return ConnectionTestResult(false, "请先配置有效的 User ID")

        return try {
            val startTime = System.currentTimeMillis()
            service.testConnection(uId, userAuth)
            val duration = System.currentTimeMillis() - startTime
            ConnectionTestResult(true, "连接成功", duration)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("timeout", ignoreCase = true) == true -> "连接超时"
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "无法解析主机"
                e.message?.contains("401") == true || e.message?.contains(
                    "Unauthorized",
                    ignoreCase = true
                ) == true -> "认证失败"

                e.message?.contains("404") == true -> "用户不存在"
                else -> e.message ?: "未知错误"
            }
            ConnectionTestResult(false, errorMsg)
        }
    }

    // 用户配置测试连接（内联状态）
    fun testConnectionForUserConfig() {
        isUserConfigLoading = true
        userConfigTestResult = null
        scope.launch {
            val result = performConnectionTest()
            userConfigTestResult = result
            isUserConfigLoading = false
            // 5秒后自动清除状态
            delay(5000)
            userConfigTestResult = null
        }
    }

    // 高级设置测试连接（内联状态）
    fun testConnectionForAdvanced() {
        isAdvancedLoading = true
        advancedTestResult = null
        scope.launch {
            val result = performConnectionTest()
            advancedTestResult = result
            isAdvancedLoading = false
            // 5秒后自动清除状态
            delay(5000)
            advancedTestResult = null
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

                TestConnectionButton(
                    isLoading = isUserConfigLoading,
                    testResult = userConfigTestResult,
                    onClick = { testConnectionForUserConfig() }
                )
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

    // 权限刷新触发器 (Requirement 1.3, 3.2)
    var permissionRefreshKey by remember { mutableIntStateOf(0) }

    // 使用 LifecycleEventEffect 在页面恢复时刷新权限状态 (Requirement 3.2)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionRefreshKey++
    }

    // 权限状态卡片 (Requirements 1.1, 1.2, 1.3, 3.2)
    val permissionStatusCard = @Composable {
        // 使用 key 强制在 permissionRefreshKey 变化时重新组合
        key(permissionRefreshKey) {
            PermissionStatusCard(
                onPermissionChanged = {
                    // 权限变化时可以触发其他操作
                }
            )
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

                TestConnectionButton(
                    isLoading = isAdvancedLoading,
                    testResult = advancedTestResult,
                    onClick = { testConnectionForAdvanced() }
                )
            }
        }
    }

    // 关于页卡片
    val aboutCard = @Composable {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "关于",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))


                // 当前版本
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前版本: $versionName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (buildType != BuildType.RELEASE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (buildType == BuildType.DEBUG) "Debug" else "Dev",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 自动检测更新开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("启动时自动检测更新", modifier = Modifier.weight(1f))
                    Switch(
                        checked = autoCheckUpdate,
                        onCheckedChange = { scope.launch { storesManager.saveAutoCheckUpdate(it) } }
                    )
                }

                // 接收预发布版本开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("接收预发布版本更新", modifier = Modifier.weight(1f))
                    Switch(
                        checked = includePreRelease,
                        onCheckedChange = { includePreRelease = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 检查更新按钮
                ElevatedButton(
                    onClick = { manualCheckUpdate() },
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("检查中...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("检查更新")
                    }
                }

                // 检查结果提示
                when (val result = updateCheckResult) {
                    is UpdateCheckResult.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "检查失败: ${result.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    is UpdateCheckResult.NoUpdate -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "已是最新版本",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    else -> {}
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 项目地址
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = GitHubIcon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("项目地址", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/weinibuliu/QRCodeShare".toUri())
                            context.startActivity(intent)
                        }
                    ) {
                        Text("前往")
                    }
                }
            }
        }
    }

    // 更新弹窗
    val updateResult = updateCheckResult
    if (showUpdateDialog && updateResult is UpdateCheckResult.UpdateAvailable) {
        UpdateAvailableDialog(
            currentVersion = updateResult.currentVersion,
            newVersion = updateResult.newVersion,
            channel = updateResult.channel,
            releaseUrl = updateResult.release.htmlUrl,
            releaseNotes = updateResult.release.body,
            onDismiss = { showUpdateDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 开发/调试构建警告横幅（不可关闭）
        if (buildType != BuildType.RELEASE) {
            DevBuildWarningBanner(buildType = buildType, versionName = versionName)
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
                            permissionStatusCard()
                            Spacer(modifier = Modifier.height(16.dp))
                            advancedConfig()
                            Spacer(modifier = Modifier.height(16.dp))
                            aboutCard()
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
                        userConfig()
                        Spacer(modifier = Modifier.height(16.dp))
                        appearanceConfig()
                        Spacer(modifier = Modifier.height(16.dp))
                        followUsersConfig()
                        Spacer(modifier = Modifier.height(16.dp))
                        permissionStatusCard()
                        Spacer(modifier = Modifier.height(16.dp))
                        aboutCard()
                        Spacer(modifier = Modifier.height(16.dp))
                        advancedConfig()
                    }
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

/**
 * 权限状态数据类
 */
data class PermissionState(
    val permission: String,
    val isGranted: Boolean,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isRuntimePermission: Boolean  // CAMERA=true, INTERNET=false
)

/**
 * 权限状态卡片组件
 * 显示 CAMERA 和 INTERNET 权限状态，提供请求权限和跳转设置功能
 * 
 * Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3, 2.4, 3.1, 3.3, 4.1, 4.2, 4.3
 */
@Composable
fun PermissionStatusCard(
    onPermissionChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    // 权限状态
    var cameraGranted by remember {
        mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.CAMERA))
    }
    var internetGranted by remember {
        mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.INTERNET))
    }

    // 用于判断是否永久拒绝（用户选择了"不再询问"）
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    // 权限请求启动器 (Requirements 2.1, 2.2, 2.3)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraGranted = isGranted
        if (!isGranted && activity != null) {
            // 如果拒绝且不应该显示理由，说明用户选择了"不再询问"
            cameraPermanentlyDenied = !PermissionUtils.shouldShowRationale(
                activity,
                Manifest.permission.CAMERA
            )
        }
        onPermissionChanged()
    }

    // 构建权限状态列表
    val permissions = listOf(
        PermissionState(
            permission = Manifest.permission.CAMERA,
            isGranted = cameraGranted,
            icon = Icons.Default.CameraAlt,
            title = "摄像头权限",
            description = "用于扫描二维码",
            isRuntimePermission = true
        ),
        PermissionState(
            permission = Manifest.permission.INTERNET,
            isGranted = internetGranted,
            icon = Icons.Default.Wifi,
            title = "网络权限",
            description = "用于网络通信",
            isRuntimePermission = false
        )
    )

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "权限状态",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            permissions.forEach { permissionState ->
                PermissionItem(
                    permissionState = permissionState,
                    isPermanentlyDenied = if (permissionState.permission == Manifest.permission.CAMERA)
                        cameraPermanentlyDenied else false,
                    onRequestPermission = {
                        // 只有 CAMERA 权限需要运行时请求 (Requirement 2.1)
                        if (permissionState.permission == Manifest.permission.CAMERA) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenSettings = {
                        // 跳转系统设置 (Requirement 3.1)
                        PermissionUtils.openAppSettings(context)
                    }
                )

                if (permissionState != permissions.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

/**
 * 单个权限项组件
 */
@Composable
private fun PermissionItem(
    permissionState: PermissionState,
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 权限图标 (Requirement 4.3)
            Icon(
                imageVector = permissionState.icon,
                contentDescription = permissionState.title,
                tint = if (permissionState.isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 权限标题
                Text(
                    text = permissionState.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                // 权限用途说明 (Requirement 4.1)
                Text(
                    text = permissionState.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态图标和颜色 (Requirement 1.4)
            Icon(
                imageVector = if (permissionState.isGranted)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.Cancel,
                contentDescription = if (permissionState.isGranted) "已授权" else "未授权",
                tint = if (permissionState.isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }

        // 未授权时显示操作按钮和引导文字 (Requirements 2.4, 3.3, 4.2)
        if (!permissionState.isGranted && permissionState.isRuntimePermission) {
            Spacer(modifier = Modifier.height(8.dp))

            // 引导文字 (Requirement 4.2)
            Text(
                text = if (isPermanentlyDenied)
                    "权限已被永久拒绝，请在系统设置中手动开启"
                else
                    "点击下方按钮授予权限",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // 永久拒绝时只显示"打开设置"按钮 (Requirement 2.4)
                if (isPermanentlyDenied) {
                    FilledTonalButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("打开设置")
                    }
                } else {
                    // 显示请求权限按钮 (Requirement 2.1)
                    OutlinedButton(onClick = onRequestPermission) {
                        Text("请求权限")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // 同时提供打开设置选项 (Requirement 3.3)
                    FilledTonalButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("打开设置")
                    }
                }
            }
        }

        // INTERNET 权限未授权时的提示（通常不会发生，因为是普通权限）
        if (!permissionState.isGranted && !permissionState.isRuntimePermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "此权限应在安装时自动授予，如未授予请检查应用安装",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
