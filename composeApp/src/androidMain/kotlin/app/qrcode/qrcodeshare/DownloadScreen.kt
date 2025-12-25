package app.qrcode.qrcodeshare

import android.content.res.Configuration
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qrcode.qrcodeshare.network.NetworkClient
import app.qrcode.qrcodeshare.utils.StoresManager
import app.qrcode.qrcodeshare.utils.generateQRCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val storesManager = remember { StoresManager(context) }
    val followUser by storesManager.followUser.collectAsState(initial = 0)
    val followUsers by storesManager.followUsers.collectAsState(initial = emptyMap())
    val userId by storesManager.userId.collectAsState(initial = "")
    val userAuth by storesManager.userAuth.collectAsState(initial = "")
    val requestInterval by storesManager.requestInterval.collectAsState(initial = 500)

    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isPolling by remember { mutableStateOf(false) }
    var isCheckingUser by remember { mutableStateOf(false) }
    var lastContent by remember { mutableStateOf<String?>(null) }
    var lastUpdateAt by remember { mutableStateOf<Long?>(null) }
    var statusMessage by remember { mutableStateOf("准备就绪") }
    val mutex = remember { Mutex() }

    val placeholderBitmap = remember { generateQRCode("https://github.com/weinibuliu/QRCodeShare?xxxxxx") }

    // Local state for followUser input to prevent cursor jumping
    var followUserInput by remember { mutableStateOf("") }
    var isFollowUserSynced by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(followUser) {
        if (!isFollowUserSynced && followUser != 0) {
            followUserInput = followUser.toString()
            isFollowUserSynced = true
        }
    }

    LaunchedEffect(isPolling) {
        if (isPolling) {
            statusMessage = "正在连接..."
            while (isActive) {
                val startTime = System.currentTimeMillis()
                mutex.withLock {
                    try {
                        val service = NetworkClient.getService()
                        if (service != null) {
                            val uId = userId.toIntOrNull()
                            if (uId != null) {
                                val requestStartTime = System.currentTimeMillis()
                                val result = service.getCode(followUser, uId, userAuth)
                                val requestDuration = System.currentTimeMillis() - requestStartTime

                                if (result.content != null) {
                                    if (result.content != lastContent) {
                                        qrCodeBitmap = generateQRCode(result.content)
                                        lastContent = result.content
                                        statusMessage =
                                            "同步中：内容已更新 (请求耗时: ${requestDuration}ms)\n点击二维码可全屏"
                                    } else {
                                        statusMessage =
                                            "同步中: 内容无变化 (请求耗时: ${requestDuration}ms)\n点击二维码可全屏"
                                    }
                                    lastUpdateAt = result.updateAt
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        e.printStackTrace()
                        statusMessage = "错误: ${e.message}"
                    }
                }

                val duration = System.currentTimeMillis() - startTime
                if (duration < requestInterval) {
                    delay(requestInterval - duration)
                }
            }
        } else {
            statusMessage = "等待同步"
        }
    }

    val settingsContent = @Composable {
        Text("订阅设置", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (followUsers.isNotEmpty()) {
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
                                scope.launch { storesManager.saveFollowUser(idInt) }
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
                    scope.launch { storesManager.saveFollowUser(newValue.toIntOrNull() ?: 0) }
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

                    scope.launch {
                        isCheckingUser = true
                        statusMessage = "正在检查用户..."
                        try {
                            service.getUser(uId, userAuth, followUser)
                            isPolling = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (e is HttpException && e.code() == 404) {
                                statusMessage = "错误: 订阅用户不存在"
                                Toast.makeText(context, "无法开始同步: 用户不存在", Toast.LENGTH_SHORT).show()
                            } else {
                                statusMessage = "错误: 网络异常或服务器错误\nDetails: ${e.message}"
                                Toast.makeText(context, "无法开始同步: 检查失败", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            isCheckingUser = false
                        }
                    }
                }
            },
            enabled = followUser != 0 && !isCheckingUser,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPolling) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            if (isCheckingUser) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("检查中...")
            } else if (isPolling) {
                Text("停止同步")
            } else {
                Text("开始同步")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val isError = statusMessage.startsWith("错误")
        val isSuccess = statusMessage.startsWith("已更新")

        Surface(
            shape = MaterialTheme.shapes.small,
            color = when {
                isError -> MaterialTheme.colorScheme.errorContainer
                isSuccess -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = when {
                    isError -> MaterialTheme.colorScheme.onErrorContainer
                    isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        val clipboard =
                            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Status Message", statusMessage)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
            )
        }
    }

    val displayContent = @Composable {
        if (qrCodeBitmap != null) {
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
                    Text("复制 URL")
                }
            }
        } else {
            placeholderBitmap?.let { bitmap ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(250.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Placeholder",
                        modifier = Modifier
                            .matchParentSize()
                            .blur(15.dp),
                        contentScale = ContentScale.Fit
                    )

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = "等待同步...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

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
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                settingsContent()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                displayContent()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            settingsContent()
            Spacer(modifier = Modifier.height(16.dp))
            displayContent()
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
                            onTap = { _ -> isFullScreen = false }
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
