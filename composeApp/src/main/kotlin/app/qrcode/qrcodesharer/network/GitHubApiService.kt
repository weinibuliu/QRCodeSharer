package app.qrcode.qrcodesharer.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/**
 * GitHub Release 数据类
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String,
    val body: String,
    val prerelease: Boolean,
    val draft: Boolean,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String
)

/**
 * GitHub API 服务接口
 */
interface GitHubApiService {
    @GET("repos/weinibuliu/QRCodeSharer/releases")
    suspend fun getReleases(): List<GitHubRelease>

    @GET("repos/weinibuliu/QRCodeSharer/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
