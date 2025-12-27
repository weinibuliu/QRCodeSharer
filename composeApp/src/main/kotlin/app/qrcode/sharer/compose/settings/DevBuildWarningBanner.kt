package app.qrcode.sharer.compose.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.qrcode.sharer.utils.BuildType

/**
 * 开发/调试构建警告横幅组件
 * 不可关闭，显示在设置页面顶部
 */
@Composable
fun DevBuildWarningBanner(buildType: BuildType) {
    val buildTypeName = when (buildType) {
        BuildType.DEV -> "开发"
        BuildType.DEBUG -> "调试"
        BuildType.RELEASE -> return
    }
    val title = "您当前正在使用${buildTypeName}构建。\n这是一个仅供开发/调试使用的非正式版本，因此可能会存在问题。"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
