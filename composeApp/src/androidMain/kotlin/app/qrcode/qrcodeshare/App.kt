package app.qrcode.qrcodeshare

import android.content.res.Configuration
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.qrcode.qrcodeshare.network.NetworkClient
import app.qrcode.qrcodeshare.utils.AppTheme
import app.qrcode.qrcodeshare.utils.StoresManager

@Composable
fun App() {
    val context = LocalContext.current
    val storesManager = remember { StoresManager(context) }
    val darkMode by storesManager.darkMode.collectAsState(initial = "System")
    val themeColor by storesManager.themeColor.collectAsState(initial = "Blue")

    val hostAddress by storesManager.hostAddress.collectAsState(initial = "")
    val timeout by storesManager.connectTimeout.collectAsState(initial = 2500)

    LaunchedEffect(hostAddress) {
        if (hostAddress.isNotBlank()) {
            try {
                NetworkClient.initService(hostAddress, timeout)
            } catch (e: Exception) {
                e.printStackTrace()
                NetworkClient.clearService()
            }
        } else {
            NetworkClient.clearService()
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
        val tabs = listOf("上传", "下载", "设置")

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
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
                Scaffold { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally { height -> height } + fadeIn() togetherWith
                                            slideOutHorizontally { height -> -height } + fadeOut()
                                } else {
                                    slideInHorizontally { height -> -height } + fadeIn() togetherWith
                                            slideOutHorizontally { height -> height } + fadeOut()
                                }
                            },
                            label = "TabTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> UploadScreen()
                                1 -> DownloadScreen()
                                2 -> SettingsScreen()
                            }
                        }
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
                                onClick = { selectedTab = index }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "TabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            0 -> UploadScreen()
                            1 -> DownloadScreen()
                            2 -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}
