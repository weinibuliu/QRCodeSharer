package app.qrcode.qrcodesharer

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import app.qrcode.qrcodesharer.network.NetworkClient
import app.qrcode.qrcodesharer.utils.AppTheme
import app.qrcode.qrcodesharer.utils.ConnectionStatusManager
import app.qrcode.qrcodesharer.utils.StoresManager
import kotlinx.coroutines.delay

/**
 * 全局同步状态，用于在同步时禁用 tab 切换
 */
object SyncState {
    var isDownloadSyncing by mutableStateOf(false)
    var isUploadScanning by mutableStateOf(false)
}

@Composable
fun App() {
    val context = LocalContext.current
    val storesManager = remember { StoresManager(context) }
    val darkMode by storesManager.darkMode.collectAsState(initial = "System")
    val themeColor by storesManager.themeColor.collectAsState(initial = "Blue")

    val hostAddress by storesManager.hostAddress.collectAsState(initial = "")
    val timeout by storesManager.connectTimeout.collectAsState(initial = 2500)
    val userId by storesManager.userId.collectAsState(initial = "")
    val userAuth by storesManager.userAuth.collectAsState(initial = "")

    // 初始化网络客户端
    LaunchedEffect(hostAddress) {
        if (hostAddress.isNotBlank()) {
            try {
                NetworkClient.initService(hostAddress, timeout)
            } catch (e: Exception) {
                e.printStackTrace()
                NetworkClient.clearService()
                ConnectionStatusManager.setDisconnected()
            }
        } else {
            NetworkClient.clearService()
            ConnectionStatusManager.setDisconnected()
        }
    }

    // 只有当 userId、userAuth 和 hostAddress 都已配置时才触发连接检查
    // 使用防抖延迟避免输入时频繁触发导致 429 错误
    LaunchedEffect(userId, userAuth, hostAddress) {
        if (hostAddress.isNotBlank() && userId.isNotBlank() && userAuth.isNotBlank()) {
            delay(1000L) // 防抖：等待 1 秒后再检查
            ConnectionStatusManager.checkNow(userId, userAuth)
            ConnectionStatusManager.startPeriodicCheck(userId, userAuth)
        }
    }

    val isDarkTheme = when (darkMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    AppTheme(darkTheme = isDarkTheme, themeColor = themeColor) {
        var selectedTab by remember { mutableIntStateOf(0) }
        var previousTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("上传", "下载", "设置")

        // 检查是否可以切换 tab（同步时禁用切换）
        val canSwitchTab = !SyncState.isDownloadSyncing && !SyncState.isUploadScanning

        fun onTabSelected(index: Int) {
            if (!canSwitchTab) return  // 同步时禁止切换
            previousTab = selectedTab
            selectedTab = index
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        tabs.forEachIndexed { index, title ->
                            NavigationRailItem(
                                icon = {
                                    when (index) {
                                        0 -> Icon(Icons.Default.ArrowUpward, contentDescription = title)
                                        1 -> Icon(Icons.Default.ArrowDownward, contentDescription = title)
                                        2 -> Icon(Icons.Default.Settings, contentDescription = title)
                                    }
                                },
                                label = { Text(title) },
                                selected = selectedTab == index,
                                onClick = { onTabSelected(index) },
                                enabled = canSwitchTab || selectedTab == index
                            )
                        }
                    }
                }
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TabContent(selectedTab = selectedTab, previousTab = previousTab)
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, title ->
                            NavigationBarItem(
                                icon = {
                                    when (index) {
                                        0 -> Icon(Icons.Default.ArrowUpward, contentDescription = title)
                                        1 -> Icon(Icons.Default.ArrowDownward, contentDescription = title)
                                        2 -> Icon(Icons.Default.Settings, contentDescription = title)
                                    }
                                },
                                label = { Text(title) },
                                selected = selectedTab == index,
                                onClick = { onTabSelected(index) },
                                enabled = canSwitchTab || selectedTab == index
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    TabContent(selectedTab = selectedTab, previousTab = previousTab)
                }
            }
        }
    }
}

@Composable
private fun TabContent(selectedTab: Int, previousTab: Int) {
    // 所有 tab 始终存在于组合中，使用动画控制显示
    Box(modifier = Modifier.fillMaxSize()) {
        listOf(0, 1, 2).forEach { tabIndex ->
            val isSelected = selectedTab == tabIndex

            // 动画化的透明度
            val alpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "alpha_$tabIndex"
            )

            // 动画化的水平偏移 - 根据切换方向决定滑入/滑出方向
            val offsetX by animateFloatAsState(
                targetValue = when {
                    isSelected -> 0f
                    tabIndex > selectedTab -> 100f  // 在右边
                    else -> -100f  // 在左边
                },
                animationSpec = tween(200),
                label = "offsetX_$tabIndex"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isSelected) 1f else 0f)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.translationX = offsetX
                    }
            ) {
                // 内容层
                when (tabIndex) {
                    0 -> UploadScreen()
                    1 -> DownloadScreen()
                    2 -> SettingsScreen()
                }
                
                // 非选中时覆盖一个透明遮罩拦截所有点击
                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { /* 拦截点击，不做任何事 */ }
                            )
                    )
                }
            }
        }
    }
}
