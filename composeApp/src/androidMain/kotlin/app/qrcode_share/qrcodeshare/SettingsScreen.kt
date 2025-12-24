package app.qrcode_share.qrcodeshare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val userId by settingsManager.userId.collectAsState(initial = "")
    val userAuth by settingsManager.userAuth.collectAsState(initial = "")
    val darkMode by settingsManager.darkMode.collectAsState(initial = "System")
    val themeColor by settingsManager.themeColor.collectAsState(initial = "Blue")
    val hostAddress by settingsManager.hostAddress.collectAsState(initial = "")
    val hostPort by settingsManager.hostPort.collectAsState(initial = 8080)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("用户配置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userId,
            onValueChange = { scope.launch { settingsManager.saveUserId(it) } },
            label = { Text("User ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userAuth,
            onValueChange = { scope.launch { settingsManager.saveUserAuth(it) } },
            label = { Text("User Auth") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {}) {
            Text("测试服务器连接")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("外观设置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        val darkModeMap = mapOf(
            "System" to "跟随系统",
            "Light" to "浅色模式",
            "Dark" to "深色模式"
        )
        val darkModeOptions = darkModeMap.values.toList()
        val currentDarkModeDisplay = darkModeMap[darkMode] ?: "跟随系统"

        SettingsDropdown(
            label = "深色模式",
            options = darkModeOptions,
            selectedOption = currentDarkModeDisplay,
            onOptionSelected = { selected ->
                val internalValue = darkModeMap.entries.firstOrNull { it.value == selected }?.key ?: "System"
                if (internalValue != darkMode) {
                    scope.launch { settingsManager.saveDarkMode(internalValue) }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val themeColorMap = mapOf(
            "Blue" to "蓝色",
            "Red" to "红色",
            "Green" to "绿色",
            "Purple" to "紫色"
        )
        val themeColorOptions = themeColorMap.values.toList()
        val currentThemeColorDisplay = themeColorMap[themeColor] ?: "蓝色"

        SettingsDropdown(
            label = "主题色",
            options = themeColorOptions,
            selectedOption = currentThemeColorDisplay,
            onOptionSelected = { selected ->
                val internalValue = themeColorMap.entries.firstOrNull { it.value == selected }?.key ?: "Blue"
                if (internalValue != themeColor) {
                    scope.launch { settingsManager.saveThemeColor(internalValue) }
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("高级设置", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = hostAddress,
            onValueChange = { scope.launch { settingsManager.saveHostAddress(it) } },
            label = { Text("主机地址") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = hostPort.toString(),
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    scope.launch { settingsManager.saveHostPort(it.toIntOrNull() ?: 0) }
                }
            },
            label = { Text("主机端口") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = { },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

