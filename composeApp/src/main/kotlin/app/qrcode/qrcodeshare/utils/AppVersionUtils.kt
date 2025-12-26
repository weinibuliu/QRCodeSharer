package app.qrcode.qrcodeshare.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * 构建类型枚举
 */

enum class BuildType {
    RELEASE,      // 正式发布版本
    DEBUG,        // Debug 构建
    DEV           // 开发构建（commit hash 版本号）
}

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
 * 检查是否为 Debug 构建
 */
fun isDebugBuild(context: Context): Boolean {
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

/**
 * 检查版本号是否为 commit hash（开发构建）
 * commit hash 通常是 7-40 位的十六进制字符串
 */
fun isCommitHash(version: String): Boolean {
    if (version.isBlank() || version == "unknown") return false
    val hexPattern = Regex("^[0-9a-fA-F]{7,40}$")
    return hexPattern.matches(version)
}


/**
 * 获取当前构建类型
 */
fun getBuildType(context: Context): BuildType {
    val versionName = getAppVersionName(context)
    return when {
        isCommitHash(versionName) -> BuildType.DEV
        isDebugBuild(context) -> BuildType.DEBUG
        else -> BuildType.RELEASE
    }
}
