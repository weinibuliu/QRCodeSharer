package app.qrcode.sharer.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 二维码显示组件
 * 支持点击全屏查看
 */
@Composable
fun QrCodeDisplay(
    bitmap: Bitmap?,
    placeholderBitmap: Bitmap?,
    size: Dp = 250.dp,
    placeholderText: String = "等待同步...",
    modifier: Modifier = Modifier
) {
    var isFullScreen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (bitmap != null) {
                            isFullScreen = true
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            if (placeholderBitmap != null) {
                Image(
                    bitmap = placeholderBitmap.asImageBitmap(),
                    contentDescription = "Placeholder",
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(10.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // 全屏弹窗
    if (isFullScreen && bitmap != null) {
        FullScreenQrCodeDialog(
            bitmap = bitmap,
            onDismiss = { isFullScreen = false }
        )
    }
}

/**
 * 全屏二维码弹窗
 */
@Composable
fun FullScreenQrCodeDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Full Screen QR Code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
