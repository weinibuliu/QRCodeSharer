package app.qrcode.qrcodesharer.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

@Serializable
data class CodeResult(
    @SerialName("content") val content: String?,
    @SerialName("update_at") val updateAt: Long?
)

@Serializable
data class CodeUpdate(
    @SerialName("content") val content: String?
)

interface ApiService {

    @GET("/")
    suspend fun testConnection(
        @Query("id") id: Int?,
        @Query("auth") auth: String?
    ): Map<String, String> // Assuming empty object or simple map for now

    @GET("/code/get")
    suspend fun getCode(
        @Query("follow_user_id") followUserId: Int,
        @Query("id") id: Int?,
        @Query("auth") auth: String?
    ): CodeResult


    @PATCH("/code/patch")
    suspend fun patchCode(
        @Query("id") id: Int?,
        @Query("auth") auth: String?,
        @Body codeUpdate: CodeUpdate
    ): Map<String, String>

    @GET("/user/get")
    suspend fun getUser(
        @Query("id") id: Int?,
        @Query("auth") auth: String?,
        @Query("check_id") checkId: Int?
    )
}
