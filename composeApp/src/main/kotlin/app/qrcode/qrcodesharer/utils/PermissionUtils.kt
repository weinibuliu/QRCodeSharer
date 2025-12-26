package app.qrcode.qrcodesharer.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限工具类，封装权限检查和请求相关的功能
 */
object PermissionUtils {

    /**
     * 检查指定权限是否已授予
     * @param context Android Context
     * @param permission 权限字符串 (如 Manifest.permission.CAMERA)
     * @return true 如果权限已授予
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查是否应该显示权限请求理由
     * 用于判断用户是否选择了"不再询问"
     * @param activity Activity 实例
     * @param permission 权限字符串
     * @return true 如果应该显示理由（用户之前拒绝过但未选择"不再询问"）
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 打开应用的系统设置页面
     * @param context Android Context
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

/**
 * Context 扩展函数，用于从 Context 中获取 Activity
 * 在 Compose 中，Context 可能被包装在 ContextWrapper 中，需要递归查找
 * @return Activity 实例，如果找不到则返回 null
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
