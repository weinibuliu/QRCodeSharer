package app.qrcode.qrcodesharer.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.qrcode.qrcodesharer.network.NetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class ConnectionState {
    CHECKING, ONLINE, OFFLINE
}

// 全局连接状态管理
object ConnectionStatusManager {
    private val _connectionState = MutableStateFlow(ConnectionState.CHECKING)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicCheckJob: Job? = null

    fun setConnected() {
        _connectionState.value = ConnectionState.ONLINE
    }

    fun setDisconnected() {
        _connectionState.value = ConnectionState.OFFLINE
    }

    fun updateState(state: ConnectionState) {
        _connectionState.value = state
    }

    // 根据异常类型判断是否为网络连接问题
    fun handleException(e: Exception) {
        when (e) {
            is ConnectException,
            is SocketTimeoutException,
            is UnknownHostException -> {
                _connectionState.value = ConnectionState.OFFLINE
            }

            is HttpException -> {
                when (e.code()) {
                    403, 429 -> _connectionState.value = ConnectionState.OFFLINE
                }
            }
            // 其他异常（如 400、500 等）不改变连接状态
        }
    }

    // 执行连接检查
    suspend fun performCheck(userId: String, userAuth: String): ConnectionState {
        return try {
            val service = NetworkClient.getService() ?: return ConnectionState.OFFLINE
            val uId = userId.toIntOrNull() ?: return ConnectionState.OFFLINE
            service.testConnection(uId, userAuth)
            ConnectionState.ONLINE
        } catch (_: Exception) {
            ConnectionState.OFFLINE
        }
    }

    // 立即执行一次检查
    fun checkNow(userId: String, userAuth: String) {
        scope.launch {
            _connectionState.value = ConnectionState.CHECKING
            val result = performCheck(userId, userAuth)
            _connectionState.value = result
        }
    }

    // 启动周期性检查（每5分钟）
    fun startPeriodicCheck(userId: String, userAuth: String) {
        periodicCheckJob?.cancel()
        periodicCheckJob = scope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L) // 5分钟
                val result = performCheck(userId, userAuth)
                _connectionState.value = result
            }
        }
    }
}

@Composable
fun ConnectionStatusBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storesManager = remember { StoresManager(context) }
    val userId by storesManager.userId.collectAsState(initial = "")

    val connectionState by ConnectionStatusManager.connectionState.collectAsState()

    val containerColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CHECKING -> MaterialTheme.colorScheme.surfaceVariant
            ConnectionState.ONLINE -> MaterialTheme.colorScheme.primaryContainer
            ConnectionState.OFFLINE -> MaterialTheme.colorScheme.errorContainer
        },
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when (connectionState) {
            ConnectionState.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
            ConnectionState.ONLINE -> MaterialTheme.colorScheme.onPrimaryContainer
            ConnectionState.OFFLINE -> MaterialTheme.colorScheme.onErrorContainer
        },
        label = "contentColor"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (connectionState) {
                ConnectionState.CHECKING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                }

                ConnectionState.ONLINE -> {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor
                    )
                }

                ConnectionState.OFFLINE -> {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor
                    )
                }
            }

            Column {
                Text(
                    text = if (userId.isNotEmpty()) "ID: $userId" else "未配置",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
                Text(
                    text = when (connectionState) {
                        ConnectionState.CHECKING -> "检查中..."
                        ConnectionState.ONLINE -> "在线"
                        ConnectionState.OFFLINE -> "离线"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
