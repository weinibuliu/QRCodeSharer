package app.qrcode.sharer.compose.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 关注用户编辑弹窗
 */
@Composable
fun FollowUserDialog(
    id: String,
    name: String,
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String) -> Unit
) {
    var userId by remember { mutableStateOf(id) }
    var userName by remember { mutableStateOf(name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("编辑关注用户", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = userId,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() }) {
                            userId = it
                        }
                    },
                    label = { Text("用户 ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (userId.isNotBlank() && userName.isNotBlank()) {
                    onConfirm(userId, userName)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
