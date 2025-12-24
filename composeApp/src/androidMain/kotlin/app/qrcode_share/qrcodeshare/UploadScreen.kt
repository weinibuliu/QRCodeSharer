package app.qrcode_share.qrcodeshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UploadScreen() {
    var isScanning by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isScanning) Arrangement.Top else Arrangement.Center
    ) {
        Text(text = scannedResult?.let { "Scanned Result: $it" } ?: "No result yet")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { isScanning = !isScanning }) {
            Text(if (isScanning) "Stop Scanning" else "Scan QR Code")
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ScanScreen { result ->
                    scannedResult = result
                }
            }
        }
    }
}
