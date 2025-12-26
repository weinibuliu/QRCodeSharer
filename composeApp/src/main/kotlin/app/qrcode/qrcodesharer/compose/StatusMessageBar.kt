package app.qrcode.qrcodesharer.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * 状态消息栏组件
 * 点击可复制消息内容
 */
@Composable
fun StatusMessageBar(
    message: String,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = MaterialTheme.shapes.small,
        color = when {
            isError -> MaterialTheme.colorScheme.errorContainer
            isSuccess -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = when {
                isError -> MaterialTheme.colorScheme.onErrorContainer
                isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Status Message", message)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
        )
    }
}
