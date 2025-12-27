package app.qrcode.sharer.compose.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.qrcode.sharer.utils.PermissionUtils
import app.qrcode.sharer.utils.findActivity

/**
 * 权限状态数据类
 */
data class PermissionState(
    val permission: String,
    val isGranted: Boolean,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isRuntimePermission: Boolean
)

/**
 * 权限状态卡片组件
 */
@Composable
fun PermissionStatusCard(
    onPermissionChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    var cameraGranted by remember {
        mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.CAMERA))
    }
    var internetGranted by remember {
        mutableStateOf(PermissionUtils.isPermissionGranted(context, Manifest.permission.INTERNET))
    }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraGranted = isGranted
        if (!isGranted && activity != null) {
            cameraPermanentlyDenied = !PermissionUtils.shouldShowRationale(
                activity,
                Manifest.permission.CAMERA
            )
        }
        onPermissionChanged()
    }

    val permissions = listOf(
        PermissionState(
            permission = Manifest.permission.CAMERA,
            isGranted = cameraGranted,
            icon = Icons.Default.CameraAlt,
            title = "摄像头权限",
            description = "用于扫描二维码",
            isRuntimePermission = true
        ),
        PermissionState(
            permission = Manifest.permission.INTERNET,
            isGranted = internetGranted,
            icon = Icons.Default.Wifi,
            title = "网络权限",
            description = "用于网络通信",
            isRuntimePermission = false
        )
    )

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "权限状态",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            permissions.forEach { permissionState ->
                PermissionItem(
                    permissionState = permissionState,
                    isPermanentlyDenied = if (permissionState.permission == Manifest.permission.CAMERA)
                        cameraPermanentlyDenied else false,
                    onRequestPermission = {
                        if (permissionState.permission == Manifest.permission.CAMERA) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onOpenSettings = {
                        PermissionUtils.openAppSettings(context)
                    }
                )

                if (permissionState != permissions.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permissionState: PermissionState,
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = permissionState.icon,
                contentDescription = permissionState.title,
                tint = if (permissionState.isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permissionState.title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = permissionState.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (permissionState.isGranted)
                    Icons.Default.CheckCircle
                else
                    Icons.Default.Cancel,
                contentDescription = if (permissionState.isGranted) "已授权" else "未授权",
                tint = if (permissionState.isGranted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }

        if (!permissionState.isGranted && permissionState.isRuntimePermission) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isPermanentlyDenied)
                    "权限已被永久拒绝，请在系统设置中手动开启"
                else
                    "点击下方按钮授予权限",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isPermanentlyDenied) {
                    FilledTonalButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("打开设置")
                    }
                } else {
                    OutlinedButton(onClick = onRequestPermission) {
                        Text("请求权限")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("打开设置")
                    }
                }
            }
        }

        if (!permissionState.isGranted && !permissionState.isRuntimePermission) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "此权限应在安装时自动授予，如未授予请检查应用安装",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
