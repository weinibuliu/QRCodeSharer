package app.qrcode.qrcodeshare.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 获取应用版本名称
 */
fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
}

/**
 * 检查版本号是否为 commit hash（开发构建）
 * commit hash 通常是 7-40 位的十六进制字符串
 */
fun isCommitHash(version: String): Boolean {
    if (version.isBlank() || version == "unknown") return false
    // commit hash 通常是 7-40 位的十六进制字符
    val hexPattern = Regex("^[0-9a-fA-F]{7,40}$")
    return hexPattern.matches(version)
}
