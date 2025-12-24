package app.qrcode_share.qrcodeshare

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.qrcode_share.qrcodeshare.utils.AppTheme
import app.qrcode_share.qrcodeshare.utils.SettingsManager

@Composable
fun App() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val darkMode by settingsManager.darkMode.collectAsState(initial = "System")
    val themeColor by settingsManager.themeColor.collectAsState(initial = "Blue")

    val isDarkTheme = when (darkMode) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    AppTheme(darkTheme = isDarkTheme, themeColor = themeColor) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("上传", "下载", "设置")

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
                when (selectedTab) {
                    0 -> UploadScreen()
                    1 -> DownloadScreen()
                    2 -> SettingsScreen()
                }
            }
        }
    }
}

