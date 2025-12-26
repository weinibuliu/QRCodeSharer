package app.qrcode.qrcodesharer.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 同步控制按钮组件
 */
@Composable
fun SyncControlButton(
    isActive: Boolean,
    isLoading: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    startText: String = "开始同步",
    stopText: String = "停止同步",
    loadingText: String = "检查中...",
    startIcon: ImageVector = Icons.Default.PlayArrow,
    stopIcon: ImageVector = Icons.Default.Stop,
    enabled: Boolean = true
) {
    if (!isActive) {
        ElevatedButton(
            onClick = onStart,
            enabled = enabled && !isLoading,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(loadingText)
            } else {
                Icon(startIcon, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(startText)
            }
        }
    } else {
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(stopIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stopText)
        }
    }
}

/**
 * 扫描控制按钮（上传页面专用）
 */
@Composable
fun ScanControlButton(
    isScanning: Boolean,
    isLoading: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    SyncControlButton(
        isActive = isScanning,
        isLoading = isLoading,
        onStart = onStart,
        onStop = onStop,
        modifier = modifier,
        startText = "扫描二维码",
        stopText = "停止",
        loadingText = "检查连接...",
        startIcon = Icons.Default.QrCodeScanner
    )
}
