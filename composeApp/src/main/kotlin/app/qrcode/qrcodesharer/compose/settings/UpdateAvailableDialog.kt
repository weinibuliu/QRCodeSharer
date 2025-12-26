package app.qrcode.qrcodesharer.compose.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.qrcode.qrcodesharer.utils.UpdateChannel

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "$currentVersion → $newVersion",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MarkdownText(
                        text = releaseNotes.take(1000),
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
