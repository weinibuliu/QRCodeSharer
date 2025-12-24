package app.qrcode_share.qrcodeshare

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.qrcode_share.qrcodeshare.utils.SettingsManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val followUser by settingsManager.followUser.collectAsState(initial = 0)
    val followUsers by settingsManager.followUsers.collectAsState(initial = emptyMap())
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var isFullScreen by remember { mutableStateOf(false) }

    // Local state for followUser input to prevent cursor jumping
    var followUserInput by remember { mutableStateOf("") }
    var isFollowUserSynced by remember { mutableStateOf(false) }

    LaunchedEffect(followUser) {
        if (!isFollowUserSynced && followUser != 0) {
            followUserInput = followUser.toString()
            isFollowUserSynced = true
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

        var expanded by remember { mutableStateOf(false) }
        val currentFollowUserName = followUsers[followUser.toLong()] ?: "未选择 / 自定义"

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

        OutlinedTextField(
            value = followUserInput,
            onValueChange = { newValue ->
                followUserInput = newValue
                if (newValue.isEmpty()) {
                    scope.launch { settingsManager.saveFollowUser(0) }
                } else if (newValue.all { it.isDigit() }) {
                    scope.launch { settingsManager.saveFollowUser(newValue.toInt()) }
                }
            },
            label = { Text("用户 ID (Int)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (inputUrl.isNotEmpty()) {
                qrCodeBitmap = generateQRCode(inputUrl)
            }
        }) {
            Text("生成二维码")
        }

        Spacer(modifier = Modifier.height(24.dp))

        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR Code",
                modifier = Modifier
                    .size(250.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { isFullScreen = true })
                    }
            )
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
                        .clickable { isFullScreen = false },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "Full Screen QR Code",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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
