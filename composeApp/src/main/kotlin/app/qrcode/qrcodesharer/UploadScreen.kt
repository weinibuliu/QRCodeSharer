package app.qrcode.qrcodesharer

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.qrcode.qrcodesharer.compose.ScanControlButton
import app.qrcode.qrcodesharer.compose.upload.Scanner
import app.qrcode.qrcodesharer.network.CodeUpdate
import app.qrcode.qrcodesharer.network.NetworkClient
import app.qrcode.qrcodesharer.utils.ConnectionStatusBar
import app.qrcode.qrcodesharer.utils.ConnectionStatusManager
import app.qrcode.qrcodesharer.utils.StoresManager
import app.qrcode.qrcodesharer.utils.VibrationHelper
import kotlinx.coroutines.launch


@Composable
fun UploadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storesManager = remember { StoresManager(context) }
    val userId by storesManager.userId.collectAsState(initial = "")
    val userAuth by storesManager.userAuth.collectAsState(initial = "")
    val showScanDetails = storesManager.showScanDetails.collectAsState(initial = false)

    var count by remember { mutableIntStateOf(0) }
    var urlResult by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var isCheckingConnection by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 同步 isScanning 状态到全局 SyncState，用于禁用 tab 切换
    LaunchedEffect(isScanning) {
        SyncState.isUploadScanning = isScanning
    }

    fun onStopScan() {
        isScanning = false
        count = 0
        urlResult = ""
    }

    // 开始扫描前检查连接
    fun startScan() {
        val service = NetworkClient.getService()
        if (service == null) {
            Toast.makeText(context, "请先配置主机地址", Toast.LENGTH_SHORT).show()
            return
        }

        val uId = userId.toIntOrNull()
        if (uId == null) {
            Toast.makeText(context, "请先配置 User ID", Toast.LENGTH_SHORT).show()
            return
        }

        isCheckingConnection = true
        scope.launch {
            try {
                service.testConnection(uId, userAuth)
                ConnectionStatusManager.setConnected()
                isCheckingConnection = false
                isScanning = true
            } catch (e: Exception) {
                ConnectionStatusManager.handleException(e)
                isCheckingConnection = false
                Toast.makeText(context, "无法连接到服务器: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onScanResult: (String) -> Unit = { result ->
        scope.launch {
            if (result.isNotEmpty() && urlResult != result) {
                VibrationHelper(context).vibrate(30)
                urlResult = result
                count += 1

                val service = NetworkClient.getService()
                if (service != null) {
                    val uId = userId.toIntOrNull()
                    if (uId != null) {
                        try {
                            service.patchCode(uId, userAuth, CodeUpdate(content = result))
                            ConnectionStatusManager.setConnected()
                            Toast.makeText(context, "已上传", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            ConnectionStatusManager.handleException(e)
                            Toast.makeText(context, "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val controls = @Composable {
        OutlinedCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (userId.isNotEmpty()) "User ID: $userId" else "User ID is NULL!",
                    style = MaterialTheme.typography.titleMedium
                )

                AnimatedVisibility(
                    visible = isScanning && showScanDetails.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Count: $count", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "URL: $urlResult", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ScanControlButton(
                    isScanning = isScanning,
                    isLoading = isCheckingConnection,
                    onStart = { startScan() },
                    onStop = { onStopScan() }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 左上角连接状态
        ConnectionStatusBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    controls()
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        Scanner(onScanResult)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isScanning) Arrangement.Top else Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(48.dp)) // 为状态栏留空间
                controls()

                if (isScanning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Scanner(onScanResult)
                    }
                }
            }
        }
    }
}
