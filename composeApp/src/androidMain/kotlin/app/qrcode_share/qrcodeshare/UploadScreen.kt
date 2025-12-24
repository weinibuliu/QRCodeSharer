package app.qrcode_share.qrcodeshare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.qrcode_share.qrcodeshare.utils.ScanScreen
import app.qrcode_share.qrcodeshare.utils.SettingsManager
import app.qrcode_share.qrcodeshare.utils.VibrationHelper
import kotlinx.coroutines.launch


@Composable
fun UploadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val userId by settingsManager.userId.collectAsState(initial = "")
    val showScanDetails = settingsManager.showScanDetails.collectAsState(initial = false)

    var count by remember { mutableStateOf(0) }
    var urlResult by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    fun onStopScan() {
        isScanning = !isScanning
        count = 0
        urlResult = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isScanning) Arrangement.Top else Arrangement.Center
    ) {
        Text(text = if (userId.isNotEmpty()) "User ID: $userId" else "User ID is NULL!")
        if (isScanning && showScanDetails.value) {
            Text(text = "Count: $count")
            Text(text = "URL: $urlResult")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isScanning) {
            Button(onClick = { isScanning = !isScanning }) {
                Text("扫描二维码")
            }
        } else {
            Button(onClick = { onStopScan() }) {
                Text("停止")
            }
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
                        if (result.isNotEmpty() && urlResult != result) {
                            VibrationHelper(context).vibrate(30)
                            urlResult = result
                            count += 1
                        }
                    }
                }
            }
        }
    }
}

