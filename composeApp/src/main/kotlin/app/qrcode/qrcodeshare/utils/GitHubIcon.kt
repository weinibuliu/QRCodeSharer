package app.qrcode.qrcodeshare.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GitHubIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GitHub",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 2f)
            curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
            curveTo(2f, 16.418f, 4.865f, 20.166f, 8.839f, 21.489f)
            curveTo(9.339f, 21.581f, 9.521f, 21.278f, 9.521f, 21.017f)
            curveTo(9.521f, 20.782f, 9.513f, 20.14f, 9.508f, 19.283f)
            curveTo(6.726f, 19.884f, 6.139f, 17.883f, 6.139f, 17.883f)
            curveTo(5.685f, 16.743f, 5.029f, 16.442f, 5.029f, 16.442f)
            curveTo(4.121f, 15.818f, 5.098f, 15.831f, 5.098f, 15.831f)
            curveTo(6.101f, 15.901f, 6.629f, 16.858f, 6.629f, 16.858f)
            curveTo(7.521f, 18.388f, 8.97f, 17.953f, 9.539f, 17.701f)
            curveTo(9.631f, 17.046f, 9.889f, 16.611f, 10.175f, 16.419f)
            curveTo(7.954f, 16.225f, 5.62f, 15.347f, 5.62f, 11.489f)
            curveTo(5.62f, 10.408f, 6.01f, 9.523f, 6.649f, 8.828f)
            curveTo(6.546f, 8.633f, 6.203f, 7.626f, 6.747f, 6.264f)
            curveTo(6.747f, 6.264f, 7.587f, 6.056f, 9.497f, 7.334f)
            curveTo(10.295f, 7.114f, 11.15f, 7.004f, 12f, 7f)
            curveTo(12.85f, 7.004f, 13.705f, 7.114f, 14.503f, 7.334f)
            curveTo(16.413f, 6.056f, 17.253f, 6.264f, 17.253f, 6.264f)
            curveTo(17.797f, 7.626f, 17.454f, 8.633f, 17.351f, 8.828f)
            curveTo(17.99f, 9.523f, 18.38f, 10.408f, 18.38f, 11.489f)
            curveTo(18.38f, 15.357f, 16.046f, 16.222f, 13.82f, 16.413f)
            curveTo(14.175f, 16.711f, 14.496f, 17.299f, 14.496f, 18.197f)
            curveTo(14.496f, 19.478f, 14.484f, 20.511f, 14.484f, 21.017f)
            curveTo(14.484f, 21.281f, 14.664f, 21.586f, 15.174f, 21.488f)
            curveTo(19.138f, 20.163f, 22f, 16.417f, 22f, 12f)
            curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
            close()
        }
    }.build()
