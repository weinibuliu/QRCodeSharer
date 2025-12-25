package app.qrcode.qrcodeshare

import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qrcode.qrcodeshare.network.NetworkClient
import app.qrcode.qrcodeshare.utils.SettingsManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val followUser by settingsManager.followUser.collectAsState(initial = 0)
    val followUsers by settingsManager.followUsers.collectAsState(initial = emptyMap())
    val userId by settingsManager.userId.collectAsState(initial = "")
    val userAuth by settingsManager.userAuth.collectAsState(initial = "")
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }

    var isPolling by remember { mutableStateOf(false) }
    var lastContent by remember { mutableStateOf<String?>(null) }
    var lastUpdateAt by remember { mutableStateOf<Long?>(null) }

    // Local state for followUser input to prevent cursor jumping
    var followUserInput by remember { mutableStateOf("") }
    var isFollowUserSynced by remember { mutableStateOf(false) }

    LaunchedEffect(followUser) {
        if (!isFollowUserSynced && followUser != 0) {
            followUserInput = followUser.toString()
            isFollowUserSynced = true
        }
    }

    LaunchedEffect(isPolling) {
        if (isPolling) {
            while (isActive) {
                val startTime = System.currentTimeMillis()
                try {
                    val service = NetworkClient.getService()
                    if (service != null) {
                        val uId = userId.toIntOrNull()
                        if (uId != null) {
                            val result = service.getCode(followUser, uId, userAuth)
                            if (result.content != null) {
                                if (result.content != lastContent) {
                                    qrCodeBitmap = generateQRCode(result.content)
                                    lastContent = result.content
                                }
                                lastUpdateAt = result.updateAt
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val duration = System.currentTimeMillis() - startTime
                if (duration < 1000) {
                    delay(1000 - duration)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Text("订阅设置", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (followUsers.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val currentFollowUserName = followUsers[followUser.toLong()] ?: "自定义"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = currentFollowUserName,
                    onValueChange = { },
                    label = { Text("选择关注用户") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    followUsers.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                val idInt = id.toInt()
                                scope.launch { settingsManager.saveFollowUser(idInt) }
                                followUserInput = idInt.toString()
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = followUserInput,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    followUserInput = newValue
                    scope.launch { settingsManager.saveFollowUser(newValue.toIntOrNull() ?: 0) }
                }
            },
            label = { Text("用户 ID (Int)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isPolling) {
                    isPolling = false
                } else {
                    val service = NetworkClient.getService()
                    if (service == null) {
                        Toast.makeText(context, "请先配置主机地址", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val uId = userId.toIntOrNull()
                    if (uId == null) {
                        Toast.makeText(context, "请先配置 User ID", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isPolling = true
                    Toast.makeText(context, "开始同步", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = followUser != 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPolling) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            if (isPolling) {
                Text("停止同步")
            } else {
                Text("开始同步")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR Code",
                modifier = Modifier
                    .size(250.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isFullScreen = true })
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            lastUpdateAt?.let { ts ->
                val date = Date(ts * 1000)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                Text("更新于: ${sdf.format(date)}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    lastContent?.let { content ->
                        val clipboard =
                            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("QR Content", content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !lastContent.isNullOrEmpty()
            ) {
                Text("复制内容")
            }
        }

        if (isFullScreen && qrCodeBitmap != null) {
            Dialog(
                onDismissRequest = { isFullScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { isFullScreen = false }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "Full Screen QR Code",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

fun generateQRCode(content: String): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val writer = MultiFormatWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
