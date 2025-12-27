package app.qrcode.sharer.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define basic colors for themes
val BluePrimary = Color(0xFF5894C4)
val RedPrimary = Color(0xFFE75A55)
val GreenPrimary = Color(0xFF6AC26E)
val PurplePrimary = Color(0xFFAF50C1)

private val BlueLightScheme = lightColorScheme(primary = BluePrimary)
private val BlueDarkScheme = darkColorScheme(primary = BluePrimary)

private val RedLightScheme = lightColorScheme(primary = RedPrimary)
private val RedDarkScheme = darkColorScheme(primary = RedPrimary)

private val GreenLightScheme = lightColorScheme(primary = GreenPrimary)
private val GreenDarkScheme = darkColorScheme(primary = GreenPrimary)

private val PurpleLightScheme = lightColorScheme(primary = PurplePrimary)
private val PurpleDarkScheme = darkColorScheme(primary = PurplePrimary)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: String = "Blue",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeColor) {
        "Red" -> if (darkTheme) RedDarkScheme else RedLightScheme
        "Green" -> if (darkTheme) GreenDarkScheme else GreenLightScheme
        "Purple" -> if (darkTheme) PurpleDarkScheme else PurpleLightScheme
        else -> if (darkTheme) BlueDarkScheme else BlueLightScheme // Default to Blue
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

