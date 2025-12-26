package app.qrcode.qrcodeshare.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun buildClient(timeout: Long = 2500): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .build()

    }

    private var apiService: ApiService? = null

    fun initService(baseUrl: String, timeout: Long = 2500): ApiService {
        val validBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        // Retrofit requires a valid URL. If baseUrl is empty or invalid, this will crash.
        // The caller should ensure baseUrl is valid.

        val retrofit = Retrofit.Builder()
            .baseUrl(validBaseUrl)
            .client(buildClient(timeout))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val service = retrofit.create(ApiService::class.java)
        apiService = service
        return service
    }

    fun getService(): ApiService? {
        return apiService
    }

    fun clearService() {
        apiService = null
    }
}
