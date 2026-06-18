package com.example.tlctvscreenshot

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URL
import java.text.DateFormat
import java.util.Collections
import java.util.Date
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TCL_COMMAND_PORT = 6553
private const val TCL_DISCOVERY_PORT = 0x1989
private const val TCL_DISCOVERY_DURATION_MS = 8_000L
private const val TCL_DISCOVERY_SEND_INTERVAL_MS = 1_000L
private const val TCL_DISCOVERY_RECEIVE_TIMEOUT_MS = 250
private const val TCL_VERIFY_TIMEOUT_MS = 1_200
private const val SOCKET_CONNECT_TIMEOUT_MS = 5_000
private const val SOCKET_READ_TIMEOUT_MS = 10_000
private const val HTTP_CONNECT_TIMEOUT_MS = 1_000
private const val HTTP_READ_TIMEOUT_MS = 10_000
private const val HTTP_DOWNLOAD_ATTEMPTS = 30
private const val HTTP_DOWNLOAD_RETRY_DELAY_MS = 250L
private const val TCL_SCREENSHOT_SHOT_COUNT = 2
private const val TCL_SCREENSHOT_SHOT_GAP_MS = 500L
private const val MAX_SCREENSHOT_BYTES = 25 * 1024 * 1024
private const val MAX_TCL_PACKET_BYTES = 1024 * 1024

private const val TCL_REMOTE_KEY_COMMAND = 149
private const val TCL_KEY_UP = 11
private const val TCL_KEY_DOWN = 12
private const val TCL_KEY_LEFT = 13
private const val TCL_KEY_RIGHT = 14
private const val TCL_KEY_OK = 15
private const val TCL_KEY_BACK = 16
private const val TCL_KEY_MENU = 18
private const val TCL_KEY_HOME = 19
private const val TCL_KEY_POWER = 20
private const val TCL_KEY_VOLUME_UP = 21
private const val TCL_KEY_VOLUME_DOWN = 22
private const val TCL_KEY_MUTE = 23
private const val TCL_KEY_CHANNEL_UP = 27
private const val TCL_KEY_CHANNEL_DOWN = 28
private val TCL_AES_KEY = "tnscreentnscreen".toByteArray(Charsets.UTF_8)
private val TCL_AES_IV = byteArrayOf(
    0x12, 0x34, 0x56, 0x78,
    0x90.toByte(), 0xab.toByte(), 0xcd.toByte(), 0xef.toByte(),
    0x12, 0x34, 0x56, 0x78,
    0x90.toByte(), 0xab.toByte(), 0xcd.toByte(), 0xef.toByte()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TlcTvScreenshotApp()
        }
    }
}

@Composable
private fun TlcTvScreenshotApp() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScreenshotWorkbench()
        }
    }
}

@Composable
private fun ScreenshotWorkbench() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val screenshotDirectory = remember { File(context.filesDir, "TCast/Images") }
    var selectedDevice by remember { mutableStateOf(loadSelectedTclDevice(context)) }
    var tvIp by remember { mutableStateOf(selectedDevice?.ip.orEmpty()) }
    val androidId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }
    val tclStablePhoneId = remember {
        val preferences = context.getSharedPreferences("tcl_6553_identity", android.content.Context.MODE_PRIVATE)
        preferences.getString("phone_id", null)
            ?: UUID.randomUUID().toString().also { generatedId ->
                preferences.edit().putString("phone_id", generatedId).apply()
            }
    }
    var tclPhoneName by remember { mutableStateOf(Build.MODEL ?: "Android") }
    var tclUuid by remember { mutableStateOf(androidId.ifBlank { "android-id-unavailable" }) }
    var tclPhoneImei by remember { mutableStateOf(tclStablePhoneId) }
    var tclStatus by remember { mutableStateOf("Idle. Discover a TCL TV, then capture a screenshot directly from this app.") }
    var tclBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCapturingTcl by remember { mutableStateOf(false) }
    var remoteStatus by remember { mutableStateOf("Idle. Select a TV, then send remote-control buttons directly from this app.") }
    var isSendingRemote by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf<List<TclDiscoveryDevice>>(emptyList()) }
    var discoveryStatus by remember { mutableStateOf("Idle. Uses TCL UDP discovery on port $TCL_DISCOVERY_PORT, then verifies TCP $TCL_COMMAND_PORT.") }
    var isDiscovering by remember { mutableStateOf(false) }
    var screenshots by remember { mutableStateOf(loadScreenshotFiles(context)) }
    var selectedScreenshot by remember { mutableStateOf(screenshots.firstOrNull()) }
    var galleryBitmap by remember { mutableStateOf(selectedScreenshot?.let { BitmapFactory.decodeFile(it.absolutePath) }) }
    var galleryStatus by remember {
        mutableStateOf(
            if (screenshots.isEmpty()) "No saved screenshots yet." else "Loaded ${screenshots.size} saved screenshot(s)."
        )
    }
    var isExporting by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<File?>(null) }

    fun rememberSelectedDevice(device: TclDiscoveryDevice) {
        val remembered = device.toSelectedDevice()
        selectedDevice = remembered
        tvIp = remembered.ip
        saveSelectedTclDevice(context, remembered)
    }

    fun refreshGallery(preferredFile: File? = selectedScreenshot) {
        val loaded = loadScreenshotFiles(context)
        screenshots = loaded
        val nextSelected = preferredFile?.takeIf { it.exists() } ?: loaded.firstOrNull()
        selectedScreenshot = nextSelected
        galleryBitmap = nextSelected?.let { BitmapFactory.decodeFile(it.absolutePath) }
        galleryStatus = if (loaded.isEmpty()) "No saved screenshots yet." else "Loaded ${loaded.size} saved screenshot(s)."
    }

    fun startDiscovery() {
        isDiscovering = true
        discoveredDevices = emptyList()
        discoveryStatus = "Discovering TVs on the local network..."
        coroutineScope.launch {
            runCatching {
                discoverTclTvs(
                    context = context.applicationContext,
                    phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                    phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                    uuid = tclUuid.ifBlank { androidId }
                )
            }.onSuccess { devices ->
                discoveredDevices = devices
                devices.firstOrNull()?.let(::rememberSelectedDevice)
                discoveryStatus = if (devices.isEmpty()) {
                    "No TVs found. Confirm the phone and TV are on the same Wi-Fi subnet, or enter the TV IP manually below."
                } else {
                    "Found ${devices.size} TV candidate(s). The first one was selected and remembered."
                }
            }.onFailure { error ->
                discoveryStatus = "Discovery failed: ${error.message ?: error::class.java.simpleName}"
            }
            isDiscovering = false
        }
    }

    deleteCandidate?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete screenshot?") },
            text = { Text("Delete ${file.name} from this app's saved screenshots?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val deleted = runCatching { file.delete() }.getOrDefault(false)
                        deleteCandidate = null
                        refreshGallery()
                        galleryStatus = if (deleted) "Deleted ${file.name}." else "Could not delete ${file.name}."
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "TCL TV Screenshot",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Standalone screenshot capture and remote control for compatible TCL/TLC TVs. No companion app, no ADB, and no fixed TV IP address are required.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Find the TV", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Searches the local network using TCL UDP discovery and verifies candidates with the TV's TCP screenshot control port. If UDP announcements are missed, it falls back to a local subnet scan.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !isDiscovering, onClick = { startDiscovery() }) {
                        Text(if (isDiscovering) "Discovering..." else "Discover TV")
                    }
                    Button(enabled = !isDiscovering, onClick = { startDiscovery() }) {
                        Text("Rediscover")
                    }
                }
                Text(discoveryStatus, style = MaterialTheme.typography.bodySmall)
                discoveredDevices.forEach { device ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${device.name.ifBlank { "TCL TV" }} — ${device.ip}", fontWeight = FontWeight.Bold)
                            Text(
                                text = listOfNotNull(
                                    "source=${device.source}",
                                    device.mac?.let { "mac=$it" },
                                    device.algorithmType?.let { "algorithm=$it" }
                                ).joinToString("  "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            device.handshake?.let { handshake ->
                                Text(handshake, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                            Button(onClick = { rememberSelectedDevice(device) }) {
                                Text("Use ${device.ip}")
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Selected TV", style = MaterialTheme.typography.titleLarge)
                val current = selectedDevice
                if (current == null) {
                    Text("No remembered TV selected.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("${current.name.ifBlank { "TCL TV" }} — ${current.ip}", fontWeight = FontWeight.Bold)
                    Text("MAC: ${current.mac ?: "unknown"}")
                    Text("Source: ${current.source ?: "unknown"}")
                    Text("Algorithm: ${current.algorithmType ?: "unknown"}")
                    Text("Last verified: ${formatTimestamp(current.lastVerifiedAtMillis)}")
                    current.handshake?.let { handshake ->
                        Text(handshake, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !isDiscovering, onClick = { startDiscovery() }) {
                        Text("Rediscover")
                    }
                    Button(
                        enabled = selectedDevice != null,
                        onClick = {
                            forgetSelectedTclDevice(context)
                            selectedDevice = null
                            tvIp = ""
                            discoveredDevices = emptyList()
                            discoveryStatus = "Forgot remembered TV."
                        }
                    ) {
                        Text("Forget TV")
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("2. Capture screenshot", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Captures directly from the selected TV over the TCL TCP $TCL_COMMAND_PORT protocol: handshake, prompt, heartbeat, then the two-shot screenshot request needed by this TV.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = tvIp,
                    onValueChange = { tvIp = it },
                    label = { Text("Selected TV IP address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = !isCapturingTcl,
                    onClick = {
                        if (tvIp.isBlank()) {
                            tclStatus = "Discover a TV first, or enter the TV IP address manually."
                            return@Button
                        }
                        isCapturingTcl = true
                        tclStatus = "Connecting to ${tvIp.trim()}:$TCL_COMMAND_PORT over TCL transport..."
                        coroutineScope.launch {
                            runCatching {
                                captureTcl6553Screenshot(
                                    tvIp = tvIp.trim(),
                                    port = TCL_COMMAND_PORT,
                                    phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                                    uuid = tclUuid.ifBlank { androidId },
                                    phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                                    imageDirectory = screenshotDirectory
                                )
                            }.onSuccess { result ->
                                tclBitmap = result.bitmap
                                selectedScreenshot = result.file
                                galleryBitmap = result.bitmap
                                screenshots = loadScreenshotFiles(context)
                                tclStatus = "Captured ${result.byteCount} bytes and added ${result.file.name} to the gallery."
                                galleryStatus = "Selected latest capture: ${result.file.name}."
                            }.onFailure { error ->
                                tclStatus = "Screenshot failed: ${error.message ?: error::class.java.simpleName}"
                            }
                            isCapturingTcl = false
                        }
                    }
                ) {
                    Text(if (isCapturingTcl) "Capturing..." else "Capture screenshot")
                }
                Text(tclStatus, style = MaterialTheme.typography.bodySmall)
                tclBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "TV screenshot preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("3. Screenshot gallery", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Browse captures saved by this app, share through Android Sharesheet, export to Pictures, or delete with confirmation.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { refreshGallery() }) { Text("Refresh") }
                    Button(
                        enabled = selectedScreenshot != null,
                        onClick = { selectedScreenshot?.let { shareScreenshot(context, it) } }
                    ) { Text("Share selected") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = selectedScreenshot != null && !isExporting,
                        onClick = {
                            val file = selectedScreenshot ?: return@Button
                            isExporting = true
                            galleryStatus = "Exporting ${file.name} to Pictures..."
                            coroutineScope.launch {
                                runCatching { exportScreenshotToPictures(context, file) }
                                    .onSuccess { galleryStatus = "Exported ${file.name} to Pictures." }
                                    .onFailure { error -> galleryStatus = "Export failed: ${error.message ?: error::class.java.simpleName}" }
                                isExporting = false
                            }
                        }
                    ) { Text(if (isExporting) "Exporting..." else "Export selected") }
                    Button(
                        enabled = selectedScreenshot != null,
                        onClick = { selectedScreenshot?.let { deleteCandidate = it } }
                    ) { Text("Delete selected") }
                }
                Text(galleryStatus, style = MaterialTheme.typography.bodySmall)
                selectedScreenshot?.let { file ->
                    Text("Selected: ${file.name} (${formatFileSize(file.length())}, ${formatTimestamp(file.lastModified())})")
                }
                galleryBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected saved screenshot preview",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
                screenshots.forEach { file ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(file.name, fontWeight = FontWeight.Bold)
                            Text("${formatFileSize(file.length())} • ${formatTimestamp(file.lastModified())}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    selectedScreenshot = file
                                    galleryBitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    galleryStatus = "Opened ${file.name}."
                                }) { Text("Open") }
                                Button(onClick = { shareScreenshot(context, file) }) { Text("Share") }
                                Button(onClick = { deleteCandidate = file }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("4. Remote control", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Sends button commands to the selected TV over the same TCP control transport.",
                    style = MaterialTheme.typography.bodyMedium
                )
                fun sendRemoteButton(label: String, keyCode: Int) {
                    if (tvIp.isBlank()) {
                        remoteStatus = "Discover a TV first, or enter the TV IP address manually."
                        return
                    }
                    isSendingRemote = true
                    remoteStatus = "Sending $label to ${tvIp.trim()}..."
                    coroutineScope.launch {
                        runCatching {
                            sendTcl6553RemoteKey(
                                tvIp = tvIp.trim(),
                                port = TCL_COMMAND_PORT,
                                phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                                uuid = tclUuid.ifBlank { androidId },
                                phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                                keyCode = keyCode
                            )
                        }.onSuccess {
                            remoteStatus = "Sent $label."
                        }.onFailure { error ->
                            remoteStatus = "Remote command failed: ${error.message ?: error::class.java.simpleName}"
                        }
                        isSendingRemote = false
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Power", TCL_KEY_POWER, isSendingRemote, ::sendRemoteButton)
                    RemoteButton("Home", TCL_KEY_HOME, isSendingRemote, ::sendRemoteButton)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Back", TCL_KEY_BACK, isSendingRemote, ::sendRemoteButton)
                    RemoteButton("Menu", TCL_KEY_MENU, isSendingRemote, ::sendRemoteButton)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Up", TCL_KEY_UP, isSendingRemote, ::sendRemoteButton)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Left", TCL_KEY_LEFT, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("OK", TCL_KEY_OK, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("Right", TCL_KEY_RIGHT, isSendingRemote, ::sendRemoteButton)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Down", TCL_KEY_DOWN, isSendingRemote, ::sendRemoteButton)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Vol -", TCL_KEY_VOLUME_DOWN, isSendingRemote, ::sendRemoteButton)
                    RemoteButton("Mute", TCL_KEY_MUTE, isSendingRemote, ::sendRemoteButton)
                    RemoteButton("Vol +", TCL_KEY_VOLUME_UP, isSendingRemote, ::sendRemoteButton)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Ch -", TCL_KEY_CHANNEL_DOWN, isSendingRemote, ::sendRemoteButton)
                    RemoteButton("Ch +", TCL_KEY_CHANNEL_UP, isSendingRemote, ::sendRemoteButton)
                }
                Text("Number pad", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton("1", 1, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("2", 2, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("3", 3, isSendingRemote, ::sendRemoteButton)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton("4", 4, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("5", 5, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("6", 6, isSendingRemote, ::sendRemoteButton)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton("7", 7, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("8", 8, isSendingRemote, ::sendRemoteButton)
                        RemoteButton("9", 9, isSendingRemote, ::sendRemoteButton)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton("0", 0, isSendingRemote, ::sendRemoteButton)
                    }
                }
                Text(remoteStatus, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Advanced identity", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "These values are generated by this app and only used to identify the phone to the TV protocol. Leave them unchanged unless testing another identity.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = tclPhoneName,
                    onValueChange = { tclPhoneName = it },
                    label = { Text("Phone name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tclUuid,
                    onValueChange = { tclUuid = it },
                    label = { Text("Handshake UUID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tclPhoneImei,
                    onValueChange = { tclPhoneImei = it },
                    label = { Text("Stable phone ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { tclUuid = androidId.ifBlank { "android-id-unavailable" } }) {
                    Text("Use this app's Android ID")
                }
            }
        }
    }
}

@Composable
private fun RemoteButton(
    label: String,
    keyCode: Int,
    disabled: Boolean,
    onClick: (String, Int) -> Unit
) {
    Button(
        enabled = !disabled,
        onClick = { onClick(label, keyCode) },
        modifier = Modifier.width(80.dp)
    ) {
        Text(label)
    }
}

private data class Tcl6553ScreenshotResult(
    val bitmap: Bitmap,
    val byteCount: Int,
    val file: File,
    val url: String,
    val log: String
)

private data class TclPacket(
    val raw: ByteArray,
    val text: String
)

private data class TclDiscoveryDevice(
    val ip: String,
    val name: String,
    val source: String,
    val mac: String? = null,
    val algorithmType: String? = null,
    val handshake: String? = null
)

private data class SelectedTclDevice(
    val ip: String,
    val name: String,
    val mac: String?,
    val source: String?,
    val algorithmType: String?,
    val handshake: String?,
    val lastVerifiedAtMillis: Long
)

private fun loadSelectedTclDevice(context: Context): SelectedTclDevice? {
    val preferences = context.getSharedPreferences("selected_tcl_device", android.content.Context.MODE_PRIVATE)
    val ip = preferences.getString("ip", null)?.takeIf { it.isNotBlank() } ?: return null
    return SelectedTclDevice(
        ip = ip,
        name = preferences.getString("name", null).orEmpty(),
        mac = preferences.getString("mac", null),
        source = preferences.getString("source", null),
        algorithmType = preferences.getString("algorithm_type", null),
        handshake = preferences.getString("handshake", null),
        lastVerifiedAtMillis = preferences.getLong("last_verified_at", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()
    )
}

private fun saveSelectedTclDevice(context: Context, device: SelectedTclDevice) {
    context.getSharedPreferences("selected_tcl_device", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString("ip", device.ip)
        .putString("name", device.name)
        .putString("mac", device.mac)
        .putString("source", device.source)
        .putString("algorithm_type", device.algorithmType)
        .putString("handshake", device.handshake)
        .putLong("last_verified_at", device.lastVerifiedAtMillis)
        .apply()
}

private fun forgetSelectedTclDevice(context: Context) {
    context.getSharedPreferences("selected_tcl_device", android.content.Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

private fun TclDiscoveryDevice.toSelectedDevice(verifiedAtMillis: Long = System.currentTimeMillis()): SelectedTclDevice {
    return SelectedTclDevice(
        ip = ip,
        name = name,
        mac = mac,
        source = source,
        algorithmType = algorithmType,
        handshake = handshake,
        lastVerifiedAtMillis = verifiedAtMillis
    )
}

private fun loadScreenshotFiles(context: Context): List<File> {
    val directory = File(context.filesDir, "TCast/Images")
    return directory.listFiles()
        ?.filter { file -> file.isFile && file.extension.lowercase() in setOf("jpg", "jpeg", "png", "bin") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()
}

private fun shareScreenshot(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = imageMimeType(file)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share screenshot"))
}

private suspend fun exportScreenshotToPictures(context: Context, file: File): Uri = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.MIME_TYPE, imageMimeType(file))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TCL TV Screenshot")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create Pictures export entry")
    runCatching {
        resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { input -> input.copyTo(output) } }
            ?: error("Could not open Pictures export stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completeValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, completeValues, null, null)
        }
    }.onFailure { error ->
        resolver.delete(uri, null, null)
        throw error
    }
    uri
}

private fun imageMimeType(file: File): String = when (file.extension.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    else -> "application/octet-stream"
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "unknown"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(millis))
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private data class TclDiscoveryPacket(
    val version: String,
    val packetNo: String,
    val senderName: String,
    val senderType: String,
    val commandNo: Int,
    val additionalSection: String
)


private suspend fun discoverTclTvs(
    context: Context,
    phoneName: String,
    phoneImei: String,
    uuid: String
): List<TclDiscoveryDevice> = withContext(Dispatchers.IO) {
    val devices = linkedMapOf<String, TclDiscoveryDevice>()
    val localIps = localIpv4Addresses().toSet()
    val broadcasts = broadcastAddresses(context, localIps)
    val socket = bindDiscoverySocket()
    socket.use { udp ->
        udp.broadcast = true
        udp.soTimeout = TCL_DISCOVERY_RECEIVE_TIMEOUT_MS
        val startedAt = System.currentTimeMillis()
        var nextSendAt = 0L
        while (System.currentTimeMillis() - startedAt < TCL_DISCOVERY_DURATION_MS) {
            val now = System.currentTimeMillis()
            if (now >= nextSendAt) {
                val online = tclDiscoveryPayload(phoneName, phoneImei, commandNo = 1)
                broadcasts.forEach { host -> sendUdp(udp, online, host, TCL_DISCOVERY_PORT) }
                nextSendAt = now + TCL_DISCOVERY_SEND_INTERVAL_MS
            }
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                udp.receive(packet)
                val sourceIp = packet.address.hostAddress ?: continue
                if (sourceIp in localIps || sourceIp == "192.168.43.1") continue
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val parsed = parseTclDiscoveryPacket(text) ?: continue
                if (parsed.senderType != "TV") continue
                if (parsed.commandNo == 1) {
                    sendUdp(
                        udp,
                        tclDiscoveryPayload(phoneName, phoneImei, commandNo = 3),
                        sourceIp,
                        packet.port
                    )
                }
                if (parsed.commandNo == 1 || parsed.commandNo == 3 || parsed.commandNo == 4) {
                    val additional = parsed.additionalSection.split(":")
                    val mac = additional.getOrNull(3)?.replace("&#058", ":")?.ifBlank { null }
                    devices[sourceIp] = TclDiscoveryDevice(
                        ip = sourceIp,
                        name = parsed.senderName,
                        source = "udp:${parsed.commandNo}",
                        mac = mac
                    )
                }
            } catch (_: SocketTimeoutException) {
                // Keep sending until the discovery window closes.
            }
        }
    }

    val verifiedUdpDevices = devices.values.map { device ->
        verifyTcl6553Device(device.ip, phoneName, uuid)?.let { verified ->
            device.copy(
                name = device.name.ifBlank { verified.name },
                source = "${device.source}+tcp6553",
                algorithmType = verified.algorithmType,
                handshake = verified.handshake
            )
        } ?: device
    }.toMutableList()

    val scanDevices = if (verifiedUdpDevices.any { it.algorithmType != null }) {
        emptyList()
    } else {
        val knownIps = verifiedUdpDevices.map { it.ip }.toSet()
        scanLocalSubnetsForTcl6553(localIps, phoneName, uuid, knownIps)
    }
    (verifiedUdpDevices + scanDevices)
        .distinctBy { it.ip }
        .sortedWith(compareBy<TclDiscoveryDevice> { it.source.startsWith("scan") }.thenBy { it.ip })
}

private fun bindDiscoverySocket(): DatagramSocket {
    var port = TCL_DISCOVERY_PORT
    while (port <= TCL_DISCOVERY_PORT + 20) {
        try {
            return DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(port))
            }
        } catch (_: Exception) {
            port += 1
        }
    }
    return DatagramSocket(null).apply {
        reuseAddress = true
        bind(InetSocketAddress(0))
    }
}

private fun sendUdp(socket: DatagramSocket, text: String, host: String, port: Int) {
    runCatching {
        val bytes = text.toByteArray(Charsets.UTF_8)
        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(host), port))
    }
}

private fun tclDiscoveryPayload(phoneName: String, phoneImei: String, commandNo: Int): String {
    val safeName = phoneName.replace(":", "&#058")
    return "1:${System.currentTimeMillis()}:$safeName:PHONE:$commandNo:$safeName:$phoneImei:0:0" + "\u0000"
}

private fun parseTclDiscoveryPacket(text: String): TclDiscoveryPacket? {
    val cleanText = text.trimEnd('\u0000').replace("\\u0000", "")
    val parts = cleanText.split(":")
    if (parts.size < 5) return null
    val commandNo = parts[4].toIntOrNull() ?: return null
    return TclDiscoveryPacket(
        version = parts[0],
        packetNo = parts[1],
        senderName = parts[2].replace("&#058", ":"),
        senderType = parts[3],
        commandNo = commandNo,
        additionalSection = parts.drop(5).joinToString(":")
    )
}

private fun localIpv4Addresses(): List<String> {
    return Collections.list(NetworkInterface.getNetworkInterfaces())
        .filter { it.isUp && !it.isLoopback }
        .flatMap { networkInterface -> Collections.list(networkInterface.inetAddresses) }
        .filter { address -> address is java.net.Inet4Address && !address.isLoopbackAddress }
        .mapNotNull { it.hostAddress }
        .distinct()
}

private fun broadcastAddresses(context: Context, localIps: Set<String>): List<String> {
    val broadcasts = linkedSetOf("255.255.255.255", "192.168.43.255")
    runCatching {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        val dhcp = wifiManager?.dhcpInfo
        if (dhcp != null) {
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val bytes = ByteArray(4) { index -> ((broadcast shr (index * 8)) and 0xff).toByte() }
            broadcasts += InetAddress.getByAddress(bytes).hostAddress.orEmpty()
        }
    }
    runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.interfaceAddresses }
            .mapNotNull { it.broadcast?.hostAddress }
            .forEach { broadcasts += it }
    }
    localIps.mapNotNull { ip -> ip.substringBeforeLast('.', missingDelimiterValue = "").takeIf { it.isNotBlank() }?.let { "$it.255" } }
        .forEach { broadcasts += it }
    return broadcasts.filter { it.isNotBlank() }.distinct()
}

private suspend fun scanLocalSubnetsForTcl6553(
    localIps: Set<String>,
    phoneName: String,
    uuid: String,
    skipIps: Set<String>
): List<TclDiscoveryDevice> = coroutineScope {
    val targets = localIps.flatMap { localIp ->
        val prefix = localIp.substringBeforeLast('.', missingDelimiterValue = "")
        if (prefix.isBlank()) emptyList() else (1..254).map { "$prefix.$it" }
    }.filter { it !in localIps && it !in skipIps }.distinct()

    val found = mutableListOf<TclDiscoveryDevice>()
    targets.chunked(32).forEach { chunk ->
        val chunkFound = chunk.map { ip ->
            async(Dispatchers.IO) { verifyTcl6553Device(ip, phoneName, uuid) }
        }.awaitAll().filterNotNull()
        found += chunkFound
    }
    found
}

private fun verifyTcl6553Device(ip: String, phoneName: String, uuid: String): TclDiscoveryDevice? {
    return runCatching {
        Socket().use { socket ->
            socket.soTimeout = TCL_VERIFY_TIMEOUT_MS
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(ip, TCL_COMMAND_PORT), TCL_VERIFY_TIMEOUT_MS)
            val output = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())
            val inquiry = "159>>$phoneName>>1>>$uuid>>1"
            writeTclText(output, inquiry, encrypted = false)
            val handshake = readTclPacket(input).text
            if (!handshake.startsWith("159>>")) return@runCatching null
            val fields = handshake.split(">>")
            TclDiscoveryDevice(
                ip = ip,
                name = fields.getOrNull(1).orEmpty().ifBlank { "TCL TV" },
                source = "scan+tcp6553",
                algorithmType = fields.getOrNull(6),
                handshake = handshake
            )
        }
    }.getOrNull()
}

private suspend fun captureTcl6553Screenshot(
    tvIp: String,
    port: Int,
    phoneName: String,
    uuid: String,
    phoneImei: String,
    imageDirectory: File
): Tcl6553ScreenshotResult = withContext(Dispatchers.IO) {
    val log = StringBuilder()
    Socket().use { socket ->
        socket.soTimeout = SOCKET_READ_TIMEOUT_MS
        socket.tcpNoDelay = true
        socket.connect(InetSocketAddress(tvIp, port), SOCKET_CONNECT_TIMEOUT_MS)
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val inquiry = "159>>$phoneName>>1>>$uuid>>1"
        log.appendLine("send $inquiry")
        writeTclText(output, inquiry, encrypted = false)
        val handshake = readTclPacket(input)
        log.appendLine("recv ${handshake.text}")
        val fields = handshake.text.split(">>")
        val encrypted = fields.getOrNull(6) == "1"
        log.appendLine("algorithm=${fields.getOrNull(6).orEmpty()} encrypted=$encrypted")

        val prompt = "160>>$phoneImei>>$phoneName"
        log.appendLine("send $prompt encrypted=$encrypted")
        writeTclText(output, prompt, encrypted = encrypted)

        val heartbeat = "150>>"
        log.appendLine("send $heartbeat encrypted=$encrypted")
        writeTclText(output, heartbeat, encrypted = encrypted)

        val shot = "225>>"
        val shotUrls = mutableListOf<String>()
        repeat(TCL_SCREENSHOT_SHOT_COUNT) { index ->
            val shotNumber = index + 1
            log.appendLine("send screenshot $shotNumber/$TCL_SCREENSHOT_SHOT_COUNT $shot encrypted=$encrypted")
            writeTclText(output, shot, encrypted = encrypted)

            val response = readTclScreenshotResponse(input, encrypted, log)
            val shotFields = response.split(">>")
            require(shotFields.size >= 3 && shotFields[0] == "225" && shotFields[1] == "0") {
                "Unexpected screenshot response for shot $shotNumber: $response"
            }
            val shotUrl = shotFields[2]
            shotUrls += shotUrl
            if (shotNumber == 1 && TCL_SCREENSHOT_SHOT_COUNT > 1) {
                log.appendLine("warm-up url $shotUrl (not downloaded; TV may return a stale HTTP port here)")
                Thread.sleep(TCL_SCREENSHOT_SHOT_GAP_MS)
            } else {
                log.appendLine("download url $shotUrl")
            }
        }
        val url = shotUrls.lastOrNull() ?: error("No screenshot URL returned. $log")

        val imageBytes = downloadScreenshotWithRetries(url, log)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: error("Downloaded ${imageBytes.size} bytes, but Android could not decode them as an image")

        imageDirectory.mkdirs()
        val file = File(imageDirectory, "TCast6553-${System.currentTimeMillis()}.${imageExtension(imageBytes)}")
        file.writeBytes(imageBytes)

        Tcl6553ScreenshotResult(
            bitmap = bitmap,
            byteCount = imageBytes.size,
            file = file,
            url = url,
            log = log.toString().trim()
        )
    }
}


private suspend fun sendTcl6553RemoteKey(
    tvIp: String,
    port: Int,
    phoneName: String,
    uuid: String,
    phoneImei: String,
    keyCode: Int
) = withContext(Dispatchers.IO) {
    Socket().use { socket ->
        socket.soTimeout = SOCKET_READ_TIMEOUT_MS
        socket.tcpNoDelay = true
        socket.connect(InetSocketAddress(tvIp, port), SOCKET_CONNECT_TIMEOUT_MS)
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val inquiry = "159>>$phoneName>>1>>$uuid>>1"
        writeTclText(output, inquiry, encrypted = false)
        val handshake = readTclPacket(input)
        require(handshake.text.startsWith("159>>")) { "Unexpected handshake: ${handshake.text}" }
        val fields = handshake.text.split(">>")
        val encrypted = fields.getOrNull(6) == "1"

        writeTclText(output, "160>>$phoneImei>>$phoneName", encrypted = encrypted)
        writeTclText(output, "150>>", encrypted = encrypted)
        writeTclText(output, "$TCL_REMOTE_KEY_COMMAND>>$keyCode", encrypted = encrypted)
    }
}

private fun writeTclText(output: DataOutputStream, text: String, encrypted: Boolean) {
    val payload = if (encrypted) encryptTclAes(text.toByteArray(Charsets.UTF_8)) else text.toByteArray(Charsets.UTF_8)
    output.writeInt(payload.size)
    output.write(payload)
    output.flush()
}

private fun readTclPacket(input: DataInputStream): TclPacket {
    val length = input.readInt()
    require(length in 1..MAX_TCL_PACKET_BYTES) { "Unexpected TCL packet length: $length" }
    val raw = ByteArray(length)
    input.readFully(raw)
    return TclPacket(raw = raw, text = raw.toString(Charsets.UTF_8))
}

private fun readTclScreenshotResponse(
    input: DataInputStream,
    encrypted: Boolean,
    log: StringBuilder
): String {
    val deadline = System.currentTimeMillis() + SOCKET_READ_TIMEOUT_MS
    while (System.currentTimeMillis() < deadline) {
        val packet = readTclPacket(input)
        val text = if (encrypted) decryptTclAes(packet.raw).toString(Charsets.UTF_8) else packet.text
        log.appendLine("recv $text")
        if (text.startsWith("225>>")) {
            return text
        }
    }
    error("Timed out waiting for 225 screenshot response. $log")
}

private fun encryptTclAes(bytes: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(TCL_AES_KEY, "AES"), IvParameterSpec(TCL_AES_IV))
    return cipher.doFinal(bytes)
}

private fun decryptTclAes(bytes: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(TCL_AES_KEY, "AES"), IvParameterSpec(TCL_AES_IV))
    return cipher.doFinal(bytes)
}

private fun downloadScreenshotWithRetries(url: String, log: StringBuilder): ByteArray {
    var lastError: Throwable? = null
    repeat(HTTP_DOWNLOAD_ATTEMPTS) { index ->
        try {
            log.appendLine("download attempt ${index + 1}/$HTTP_DOWNLOAD_ATTEMPTS")
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = HTTP_CONNECT_TIMEOUT_MS
                readTimeout = HTTP_READ_TIMEOUT_MS
                requestMethod = "GET"
                useCaches = false
            }
            connection.inputStream.use { stream ->
                val bytes = stream.readBytes()
                require(bytes.size in 1..MAX_SCREENSHOT_BYTES) { "Unexpected image byte count: ${bytes.size}" }
                return bytes
            }
        } catch (error: Throwable) {
            lastError = error
            log.appendLine("download failed: ${error.message ?: error::class.java.simpleName}")
            Thread.sleep(HTTP_DOWNLOAD_RETRY_DELAY_MS)
        }
    }
    throw IllegalStateException(
        "Downloaded screenshot URL never opened: ${lastError?.message ?: lastError?.javaClass?.simpleName}\n${log.toString().trim()}"
    )
}

private fun imageExtension(bytes: ByteArray): String = when {
    bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "jpg"
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4e.toByte() && bytes[3] == 0x47.toByte() -> "png"
    else -> "bin"
}

@Preview(showBackground = true)
@Composable
private fun ScreenshotWorkbenchPreview() {
    TlcTvScreenshotApp()
}