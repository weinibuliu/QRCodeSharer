package app.qrcode.qrcodesharer.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import app.qrcode.qrcodesharer.network.GitHubClient
import app.qrcode.qrcodesharer.network.GitHubRelease

/**
 * 构建类型枚举
 */
enum class BuildType {
    RELEASE,      // 正式发布版本
    DEBUG,        // Debug 构建
    DEV           // 开发构建（commit hash 版本号）
}

/**
 * 更新类型枚举
 */
enum class UpdateChannel {
    STABLE,       // 稳定版
    PRERELEASE    // 测试版/预发布版
}

/**
 * 更新检查结果
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val release: GitHubRelease,
        val channel: UpdateChannel,
        val currentVersion: String,
        val newVersion: String
    ) : UpdateCheckResult()

    data object NoUpdate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * SemVer 版本号解析
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null
) : Comparable<SemVer> {

    companion object {
        private val SEMVER_REGEX = Regex(
            """^v?(\d+)\.(\d+)\.(\d+)(?:-([a-zA-Z0-9.-]+))?(?:\+[a-zA-Z0-9.-]+)?$"""
        )

        fun parse(version: String): SemVer? {
            val match = SEMVER_REGEX.matchEntire(version.trim()) ?: return null
            return SemVer(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                preRelease = match.groupValues[4].takeIf { it.isNotEmpty() }
            )
        }
    }

    val isPreRelease: Boolean get() = preRelease != null

    override fun compareTo(other: SemVer): Int {
        // 比较主版本号
        if (major != other.major) return major.compareTo(other.major)
        // 比较次版本号
        if (minor != other.minor) return minor.compareTo(other.minor)
        // 比较补丁版本号
        if (patch != other.patch) return patch.compareTo(other.patch)

        // 预发布版本比较
        // 正式版本 > 预发布版本
        return when {
            preRelease == null && other.preRelease == null -> 0
            preRelease == null -> 1  // this 是正式版，other 是预发布版
            other.preRelease == null -> -1  // this 是预发布版，other 是正式版
            else -> comparePreRelease(preRelease, other.preRelease)
        }
    }

    private fun comparePreRelease(a: String, b: String): Int {
        val partsA = a.split(".")
        val partsB = b.split(".")

        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val partA = partsA.getOrNull(i)
            val partB = partsB.getOrNull(i)

            when {
                partA == null -> return -1
                partB == null -> return 1
                else -> {
                    val numA = partA.toIntOrNull()
                    val numB = partB.toIntOrNull()

                    val cmp = when {
                        numA != null && numB != null -> numA.compareTo(numB)
                        numA != null -> -1  // 数字 < 字符串
                        numB != null -> 1   // 字符串 > 数字
                        else -> partA.compareTo(partB)
                    }
                    if (cmp != 0) return cmp
                }
            }
        }
        return 0
    }

    override fun toString(): String {
        return if (preRelease != null) "$major.$minor.$patch-$preRelease" else "$major.$minor.$patch"
    }
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
    } catch (_: Exception) {
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

/**
 * 检查更新（仅在 RELEASE 构建下调用）
 * @param currentVersion 当前版本号
 * @param includePreRelease 是否包含预发布版本
 * @param forceShow 强制显示更新弹窗（即使已是最新版本）
 */
suspend fun checkForUpdate(
    currentVersion: String,
    includePreRelease: Boolean = false,
    forceShow: Boolean = false
): UpdateCheckResult {
    return try {
        val currentSemVer = SemVer.parse(currentVersion)
            ?: return UpdateCheckResult.Error("无法解析当前版本号: $currentVersion")

        val releases = GitHubClient.service.getReleases()
            .filter { !it.draft }
            .mapNotNull { release ->
                SemVer.parse(release.tagName)?.let { semVer -> semVer to release }
            }

        if (releases.isEmpty()) {
            return UpdateCheckResult.NoUpdate
        }

        // 根据是否包含预发布版本筛选
        val candidates = if (includePreRelease) {
            releases
        } else {
            releases.filter { !it.first.isPreRelease && !it.second.prerelease }
        }

        if (candidates.isEmpty()) {
            return UpdateCheckResult.NoUpdate
        }

        // 找到最新版本
        val latest = candidates.maxByOrNull { it.first }
            ?: return UpdateCheckResult.NoUpdate

        val (latestSemVer, latestRelease) = latest

        // 比较版本（forceShow 时跳过版本比较）
        if (forceShow || latestSemVer > currentSemVer) {
            val channel = if (latestSemVer.isPreRelease || latestRelease.prerelease) {
                UpdateChannel.PRERELEASE
            } else {
                UpdateChannel.STABLE
            }
            UpdateCheckResult.UpdateAvailable(
                release = latestRelease,
                channel = channel,
                currentVersion = currentVersion,
                newVersion = latestRelease.tagName
            )
        } else {
            UpdateCheckResult.NoUpdate
        }
    } catch (e: Exception) {
        UpdateCheckResult.Error(e.message ?: "检查更新失败")
    }
}
