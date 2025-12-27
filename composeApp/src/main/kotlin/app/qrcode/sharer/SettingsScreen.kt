package app.qrcode.sharer

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.qrcode.sharer.compose.GitHubIcon
import app.qrcode.sharer.compose.settings.*
import app.qrcode.sharer.network.NetworkClient
import app.qrcode.sharer.utils.*
import kotlinx.coroutines.delay
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
    val autoCheckUpdate by storesManager.autoCheckUpdate.collectAsState(initial = true)

    // 用户配置的内联状态
    var userConfigTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isUserConfigLoading by remember { mutableStateOf(false) }

    // 高级设置的内联状态
    var advancedTestResult by remember { mutableStateOf<ConnectionTestResult?>(null) }
    var isAdvancedLoading by remember { mutableStateOf(false) }

    // Local states for text fields
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

    // 开发者模式
    val developerMode by storesManager.developerMode.collectAsState(initial = false)
    val forceShowUpdateDialog by storesManager.forceShowUpdateDialog.collectAsState(initial = false)
    var versionClickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // 更新检查状态
    var updateCheckResult by remember { mutableStateOf<UpdateCheckResult?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var includePreRelease by remember { mutableStateOf(false) }

    // 在 RELEASE 构建下自动检查更新
    LaunchedEffect(buildType, autoCheckUpdate) {
        if (buildType == BuildType.RELEASE && autoCheckUpdate) {
            isCheckingUpdate = true
            updateCheckResult = checkForUpdate(versionName, includePreRelease = false)
            isCheckingUpdate = false
            if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                showUpdateDialog = true
            }
        }
    }

    fun manualCheckUpdate() {
        isCheckingUpdate = true
        updateCheckResult = null
        scope.launch {
            updateCheckResult =
                checkForUpdate(versionName, includePreRelease = includePreRelease, forceShow = forceShowUpdateDialog)
            isCheckingUpdate = false
            if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                showUpdateDialog = true
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    suspend fun performConnectionTest(): ConnectionTestResult {
        val service = NetworkClient.getService() ?: return ConnectionTestResult(false, "请先配置主机地址")
        val uId = userId.toIntOrNull() ?: return ConnectionTestResult(false, "请先配置有效的 User ID")

        return try {
            val startTime = System.currentTimeMillis()
            service.testConnection(uId, userAuth)
            val duration = System.currentTimeMillis() - startTime
            ConnectionStatusManager.setConnected()
            ConnectionTestResult(true, "连接成功", duration)
        } catch (e: Exception) {
            ConnectionStatusManager.handleException(e)
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

    fun testConnectionForUserConfig() {
        isUserConfigLoading = true
        userConfigTestResult = null
        scope.launch {
            val result = performConnectionTest()
            userConfigTestResult = result
            isUserConfigLoading = false
            delay(5000)
            userConfigTestResult = null
        }
    }

    fun testConnectionForAdvanced() {
        isAdvancedLoading = true
        advancedTestResult = null
        scope.launch {
            val result = performConnectionTest()
            advancedTestResult = result
            isAdvancedLoading = false
            delay(5000)
            advancedTestResult = null
        }
    }

    // 权限刷新触发器
    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionRefreshKey++
    }

    // ===== 配置卡片组件 =====

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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hostAddressInput,
                    onValueChange = { hostAddressInput = it; scope.launch { storesManager.saveHostAddress(it) } },
                    label = { Text("主机地址") },
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
                    ExposedDropdownMenu(expanded = darkModeExpanded, onDismissRequest = { darkModeExpanded = false }) {
                        listOf(
                            "System" to "跟随系统",
                            "Light" to "浅色模式",
                            "Dark" to "深色模式"
                        ).forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                scope.launch { storesManager.saveDarkMode(key) }
                                darkModeExpanded = false
                            })
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
                        onDismissRequest = { themeColorExpanded = false }) {
                        listOf("Blue", "Red", "Green", "Purple").forEach { color ->
                            DropdownMenuItem(text = { Text(color) }, onClick = {
                                scope.launch { storesManager.saveThemeColor(color) }
                                themeColorExpanded = false
                            })
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
                        onDismiss = { showDialog = false; editingUser = null },
                        onConfirm = { id, name ->
                            scope.launch { storesManager.saveFollowUsers(id.toInt(), name) }
                            showDialog = false
                            editingUser = null
                        }
                    )
                }

                ElevatedButton(
                    onClick = { editingUser = null; showDialog = true },
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
                            headlineContent = { Text("$name ($id)") },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editingUser = id to name; showDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    }
                                    IconButton(onClick = { scope.launch { storesManager.removeFollowUser(id) } }) {
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

    val permissionStatusCard = @Composable {
        key(permissionRefreshKey) {
            PermissionStatusCard(onPermissionChanged = {})
        }
    }


    val advancedConfig = @Composable {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    value = connectTimeoutInput,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            connectTimeoutInput = it
                            if (it.isNotEmpty()) scope.launch { storesManager.saveConnectTimeout(it.toLong()) }
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
                            if (it.isNotEmpty()) scope.launch { storesManager.saveRequestInterval(it.toLong()) }
                        }
                    },
                    label = { Text("请求间隔 (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("显示扫描详情", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showScanDetails,
                        onCheckedChange = { scope.launch { storesManager.saveShowScanDetails(it) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("启用震动", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enableVibration,
                        onCheckedChange = { scope.launch { storesManager.saveEnableVibration(it) } })
                }
                Spacer(modifier = Modifier.height(16.dp))
                TestConnectionButton(
                    isLoading = isAdvancedLoading,
                    testResult = advancedTestResult,
                    onClick = { testConnectionForAdvanced() })
            }
        }
    }

    val aboutCard = @Composable {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                // 版本号（Debug 构建下连续点击 7 次可开启开发者模式）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = buildType == BuildType.DEBUG && !developerMode) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastClickTime > 1000) versionClickCount = 1 else versionClickCount++
                            lastClickTime = currentTime
                            when {
                                versionClickCount >= 7 -> {
                                    scope.launch { storesManager.saveDeveloperMode(true) }
                                    Toast.makeText(context, "已开启开发者模式", Toast.LENGTH_SHORT).show()
                                    versionClickCount = 0
                                }

                                versionClickCount >= 4 -> Toast.makeText(
                                    context,
                                    "再点击 ${7 - versionClickCount} 次开启开发者模式",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("当前版本: $versionName", style = MaterialTheme.typography.bodyMedium)
                    if (buildType != BuildType.RELEASE) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "启动时自动检测更新",
                        modifier = Modifier.weight(1f),
                        color = if (buildType != BuildType.RELEASE) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = if (buildType != BuildType.RELEASE) false else autoCheckUpdate,
                        onCheckedChange = { scope.launch { storesManager.saveAutoCheckUpdate(it) } },
                        enabled = buildType == BuildType.RELEASE
                    )
                }

                // 接收预发布版本开关
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("接收预发布版本更新", modifier = Modifier.weight(1f))
                    Switch(checked = includePreRelease, onCheckedChange = { includePreRelease = it })
                }

                // 开发者设置
                if (buildType == BuildType.DEBUG && developerMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "开发者选项",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("强制显示更新弹窗", modifier = Modifier.weight(1f))
                        Switch(
                            checked = forceShowUpdateDialog,
                            onCheckedChange = { scope.launch { storesManager.saveForceShowUpdateDialog(it) } })
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    var showStoresEditor by remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { showStoresEditor = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Storage, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("查看/编辑 Stores")
                    }
                    if (showStoresEditor) {
                        StoresEditorDialog(storesManager = storesManager, onDismiss = { showStoresEditor = false })
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                storesManager.saveDeveloperMode(false); storesManager.saveForceShowUpdateDialog(
                                false
                            )
                            }
                            Toast.makeText(context, "已关闭开发者模式", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeveloperMode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("关闭开发者模式")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 检查更新按钮
                ElevatedButton(
                    onClick = { manualCheckUpdate() },
                    enabled = !isCheckingUpdate && buildType != BuildType.DEV,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("检查中...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (buildType == BuildType.DEV) "开发构建不支持检查更新" else "检查更新")
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
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "检查失败: ${result.message}",
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
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "已是最新版本",
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
                        Icon(GitHubIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("项目地址", style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/weinibuliu/QRCodeSharer".toUri())
                        context.startActivity(intent)
                    }) { Text("前往") }
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

    // ===== 主布局 =====
    Column(modifier = Modifier.fillMaxSize()) {
        if (buildType != BuildType.RELEASE) {
            DevBuildWarningBanner(buildType = buildType)
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
                    AnimatedVisibility(visible = isVisible, enter = fadeIn() + slideInVertically { it / 4 }) {
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
                    AnimatedVisibility(visible = isVisible, enter = fadeIn() + slideInVertically { it / 4 }) {
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
                AnimatedVisibility(visible = isVisible, enter = fadeIn() + slideInVertically { it / 4 }) {
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
