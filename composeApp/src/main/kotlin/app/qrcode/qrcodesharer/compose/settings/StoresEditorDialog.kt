package app.qrcode.qrcodesharer.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.qrcode.qrcodesharer.utils.StoresManager
import kotlinx.coroutines.launch

/**
 * Stores 编辑器弹窗（开发者选项）
 */
@Composable
fun StoresEditorDialog(
    storesManager: StoresManager,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allPrefs by storesManager.allPreferences.collectAsState(initial = emptyMap())
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stores 编辑器", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (allPrefs.isEmpty()) {
                    Text(
                        "暂无存储数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allPrefs.toSortedMap().forEach { (key, value) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingKey = key
                                        editingValue = value
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = value.ifEmpty { "(空)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    // 添加新键值对的弹窗
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newKey = ""
                newValue = ""
            },
            title = {
                Text("添加键值对", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("键名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("值") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            scope.launch {
                                storesManager.saveRawPreference(newKey, newValue)
                            }
                            showAddDialog = false
                            newKey = ""
                            newValue = ""
                        }
                    },
                    enabled = newKey.isNotBlank()
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newKey = ""
                    newValue = ""
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 编辑单个值的弹窗
    if (editingKey != null) {
        AlertDialog(
            onDismissRequest = { editingKey = null },
            title = {
                Text("编辑: $editingKey", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                OutlinedTextField(
                    value = editingValue,
                    onValueChange = { editingValue = it },
                    label = { Text("值") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            editingKey?.let { key ->
                                storesManager.savePreference(key, editingValue)
                            }
                        }
                        editingKey = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKey = null }) {
                    Text("取消")
                }
            }
        )
    }
}
