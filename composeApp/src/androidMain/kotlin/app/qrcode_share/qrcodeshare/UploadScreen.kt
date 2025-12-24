package app.qrcode_share.qrcodeshare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UploadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val userId by settingsManager.userId.collectAsState(initial = "")

    var isScanning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isScanning) Arrangement.Top else Arrangement.Center
    ) {
        Text(text = if (userId.isNotEmpty()) "User ID: $userId" else "User ID is NULL!")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { isScanning = !isScanning }) {
            Text(if (isScanning) "停止" else "扫描二维码")
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ScanScreen { result ->
                    scope.launch {
                        settingsManager.saveUserId(result)
                        isScanning = false
                    }
                }
            }
        }
    }
}
