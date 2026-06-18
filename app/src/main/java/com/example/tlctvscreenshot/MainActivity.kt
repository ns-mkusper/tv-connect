package com.example.tlctvscreenshot

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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
private const val HTTP_DIRECT_ATTEMPTS_BEFORE_PORT_SCAN = 2
private const val HTTP_PORT_SCAN_FIRST_PORT = 32768
private const val HTTP_PORT_SCAN_LAST_PORT = 60999
private const val HTTP_PORT_SCAN_CONNECT_TIMEOUT_MS = 75
private const val HTTP_PORT_SCAN_READ_TIMEOUT_MS = 1_000
private const val HTTP_PORT_SCAN_PARALLELISM = 384
private const val HTTP_PORT_SCAN_START_DELAY_MS = 350L
private const val HTTP_PORT_SCAN_TIMEOUT_MS = 12_000L
private const val TCL_SCREENSHOT_SHOT_COUNT = 1
private const val MAX_SCREENSHOT_BYTES = 25 * 1024 * 1024
private const val MAX_TCL_PACKET_BYTES = 1024 * 1024
private const val LOG_TAG = "TlcTvCapture"

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
            TlcTvScreenshotApp(
                testMode = isAppDebuggable() && intent.getBooleanExtra(EXTRA_UI_TEST_MODE, false)
            )
        }
    }

    private fun isAppDebuggable(): Boolean =
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

private const val EXTRA_UI_TEST_MODE = "com.example.tlctvscreenshot.UI_TEST_MODE"

@Composable
private fun TlcTvScreenshotApp(testMode: Boolean = false) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScreenshotWorkbench(testMode = testMode)
        }
    }
}

@Composable
private fun ScreenshotWorkbench(testMode: Boolean = false) {
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
    var tclStatus by remember { mutableStateOf("Ready to capture from a connected TV.") }
    var remoteStatus by remember { mutableStateOf("Remote ready. Connect a TV before sending commands.") }
    var isCapturingTcl by remember { mutableStateOf(false) }
    var isSendingRemote by remember { mutableStateOf(false) }
    var discoveredDevices by remember { mutableStateOf<List<TclDiscoveryDevice>>(emptyList()) }
    var discoveryStatus by remember { mutableStateOf("Open discovery to find and connect to a TV.") }
    var isDiscovering by remember { mutableStateOf(false) }
    var screenshots by remember { mutableStateOf(loadScreenshotFiles(context)) }
    var selectedScreenshot by remember { mutableStateOf(screenshots.firstOrNull()) }
    var galleryBitmap by remember { mutableStateOf(selectedScreenshot?.let { BitmapFactory.decodeFile(it.absolutePath) }) }
    var galleryStatus by remember {
        mutableStateOf(
            if (screenshots.isEmpty()) "No saved captures yet." else "Loaded ${screenshots.size} saved capture(s)."
        )
    }
    var isExporting by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<File?>(null) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var selectedGalleryTab by remember { mutableStateOf("All") }

    fun currentTvIp() = selectedDevice?.ip?.ifBlank { tvIp } ?: tvIp

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
        galleryStatus = if (loaded.isEmpty()) "No saved captures yet." else "Loaded ${loaded.size} saved capture(s)."
    }

    fun startDiscovery() {
        isDiscovering = true
        discoveredDevices = emptyList()
        discoveryStatus = "Discovering TVs on the local network..."
        if (testMode) {
            val testDevice = testTclDevice()
            discoveredDevices = listOf(testDevice)
            rememberSelectedDevice(testDevice)
            discoveryStatus = "Found 1 test TV candidate. The test TV was selected and remembered."
            isDiscovering = false
            return
        }
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
                    "No TVs found. Confirm the phone and TV are on the same Wi-Fi subnet, or enter the TV IP manually."
                } else {
                    "Found ${devices.size} TV candidate(s). The first one was selected and remembered."
                }
            }.onFailure { error ->
                discoveryStatus = "Discovery failed: ${error.message ?: error::class.java.simpleName}"
            }
            isDiscovering = false
        }
    }

    fun captureTv() {
        if (testMode) {
            isCapturingTcl = true
            tclStatus = "Creating test TV screenshot..."
            coroutineScope.launch {
                runCatching { createTestTclScreenshot(screenshotDirectory) }
                    .onSuccess { result ->
                        selectedScreenshot = result.file
                        galleryBitmap = result.bitmap
                        screenshots = loadScreenshotFiles(context)
                        tclStatus = "Captured test TV screenshot."
                        galleryStatus = "Added ${result.file.name} to Gallery."
                    }
                    .onFailure { error ->
                        tclStatus = "Test capture failed: ${error.message ?: error::class.java.simpleName}"
                    }
                isCapturingTcl = false
            }
            return
        }
        val ip = currentTvIp().trim()
        if (ip.isBlank()) {
            tclStatus = "Please connect your TV first."
            showConnectDialog = true
            return
        }
        isCapturingTcl = true
        Log.i(LOG_TAG, "capture click accepted tv=$ip at=${System.currentTimeMillis()}")
        tclStatus = "Connecting to $ip:$TCL_COMMAND_PORT..."
        coroutineScope.launch {
            runCatching {
                captureTcl6553Screenshot(
                    tvIp = ip,
                    port = TCL_COMMAND_PORT,
                    phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                    uuid = tclUuid.ifBlank { androidId },
                    phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                    imageDirectory = screenshotDirectory
                )
            }.onSuccess { result ->
                selectedScreenshot = result.file
                galleryBitmap = result.bitmap
                screenshots = loadScreenshotFiles(context)
                tclStatus = "Captured ${formatFileSize(result.byteCount.toLong())} from TV."
                galleryStatus = "Added ${result.file.name} to Gallery. Publishing to Pictures..."
                coroutineScope.launch {
                    runCatching { exportScreenshotToPictures(context.applicationContext, result.file) }
                        .onSuccess {
                            if (selectedScreenshot == result.file) {
                                galleryStatus = "Added ${result.file.name} to Gallery and Pictures."
                            }
                        }
                        .onFailure { error ->
                            if (selectedScreenshot == result.file) {
                                galleryStatus = "Added ${result.file.name} to Gallery. Pictures publish failed: ${error.message ?: error::class.java.simpleName}"
                            }
                        }
                }
            }.onFailure { error ->
                Log.e(LOG_TAG, "capture failed", error)
                tclStatus = "Screenshot failed: ${error.message ?: error::class.java.simpleName}"
            }
            isCapturingTcl = false
        }
    }

    fun sendRemoteButton(label: String, keyCode: Int) {
        if (testMode) {
            remoteStatus = "Test remote sent $label."
            return
        }
        val ip = currentTvIp().trim()
        if (ip.isBlank()) {
            remoteStatus = "Please connect your TV first."
            showConnectDialog = true
            return
        }
        isSendingRemote = true
        remoteStatus = "Sending $label to $ip..."
        coroutineScope.launch {
            runCatching {
                sendTcl6553RemoteKey(
                    tvIp = ip,
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

    deleteCandidate?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            modifier = Modifier.testTag("delete_capture_dialog"),
            title = { Text("Delete capture?") },
            text = { Text("Delete ${file.name} from this app's saved gallery?") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("confirm_delete_button"),
                    onClick = {
                        val deleted = runCatching { file.delete() }.getOrDefault(false)
                        deleteCandidate = null
                        refreshGallery()
                        galleryStatus = if (deleted) "Deleted ${file.name}." else "Could not delete ${file.name}."
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(modifier = Modifier.testTag("cancel_delete_button"), onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }

    if (showConnectDialog) {
        ConnectTvDialog(
            selectedDevice = selectedDevice,
            tvIp = tvIp,
            onTvIpChange = { tvIp = it },
            discoveryStatus = discoveryStatus,
            isDiscovering = isDiscovering,
            discoveredDevices = discoveredDevices,
            tclPhoneName = tclPhoneName,
            onTclPhoneNameChange = { tclPhoneName = it },
            tclUuid = tclUuid,
            onTclUuidChange = { tclUuid = it },
            tclPhoneImei = tclPhoneImei,
            onTclPhoneImeiChange = { tclPhoneImei = it },
            onUseAndroidId = { tclUuid = androidId.ifBlank { "android-id-unavailable" } },
            onDiscover = { startDiscovery() },
            onUseDevice = { rememberSelectedDevice(it) },
            onForget = {
                forgetSelectedTclDevice(context)
                selectedDevice = null
                tvIp = ""
                discoveredDevices = emptyList()
                discoveryStatus = "Forgot remembered TV."
            },
            onDismiss = { showConnectDialog = false }
        )
    }

    if (showRemoteDialog) {
        RemoteControlDialog(
            isSendingRemote = isSendingRemote,
            remoteStatus = remoteStatus,
            onSendRemoteButton = ::sendRemoteButton,
            onDismiss = { showRemoteDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_root")
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            MediaHomeHeader(
                selectedDevice = selectedDevice,
                onConnectClick = { showConnectDialog = true }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MediaActionTile(
                    title = "Capture TV",
                    subtitle = if (isCapturingTcl) "Capturing" else "Screenshot",
                    enabled = !isCapturingTcl,
                    modifier = Modifier.weight(1f).testTag("action_capture_tv"),
                    onClick = { captureTv() }
                )
                MediaActionTile(
                    title = "Cast Photo",
                    subtitle = "Gallery",
                    modifier = Modifier.weight(1f).testTag("action_cast_photo"),
                    onClick = { selectedGalleryTab = "Photos"; galleryStatus = "Photo casting is not required for TV capture. Saved screenshots stay available here." }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MediaActionTile(
                    title = "Cast Video",
                    subtitle = "Media",
                    modifier = Modifier.weight(1f).testTag("action_cast_video"),
                    onClick = { selectedGalleryTab = "Videos"; galleryStatus = "Video casting is not configured in this standalone build." }
                )
                MediaActionTile(
                    title = "Cast Music",
                    subtitle = "Audio",
                    modifier = Modifier.weight(1f).testTag("action_cast_music"),
                    onClick = { selectedGalleryTab = "Music"; galleryStatus = "Music casting is not configured in this standalone build." }
                )
            }

            StatusPanel(
                connected = selectedDevice != null || currentTvIp().isNotBlank(),
                selectedDevice = selectedDevice,
                tclStatus = tclStatus,
                galleryStatus = galleryStatus,
                remoteStatus = remoteStatus,
                onConnectClick = { showConnectDialog = true }
            )

            GallerySection(
                selectedTab = selectedGalleryTab,
                onSelectedTabChange = { selectedGalleryTab = it },
                screenshots = screenshots,
                selectedScreenshot = selectedScreenshot,
                galleryBitmap = galleryBitmap,
                isExporting = isExporting,
                onRefresh = { refreshGallery() },
                onOpen = { file ->
                    selectedScreenshot = file
                    galleryBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    galleryStatus = "Opened ${file.name}."
                },
                onShare = { file ->
                    if (testMode) {
                        galleryStatus = "Test shared ${file.name}."
                    } else {
                        shareScreenshot(context, file)
                    }
                },
                onExport = { file ->
                    if (testMode) {
                        galleryStatus = "Test exported ${file.name}."
                    } else {
                        isExporting = true
                        galleryStatus = "Exporting ${file.name} to Pictures..."
                        coroutineScope.launch {
                            runCatching { exportScreenshotToPictures(context, file) }
                                .onSuccess { galleryStatus = "Exported ${file.name} to Pictures." }
                                .onFailure { error -> galleryStatus = "Export failed: ${error.message ?: error::class.java.simpleName}" }
                            isExporting = false
                        }
                    }
                },
                onDelete = { file -> deleteCandidate = file }
            )
        }

        BottomMediaBar(
            connected = selectedDevice != null || currentTvIp().isNotBlank(),
            onConnectClick = { showConnectDialog = true },
            onRemoteClick = { showRemoteDialog = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MediaHomeHeader(
    selectedDevice: SelectedTclDevice?,
    onConnectClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier.testTag("top_tv_button"),
            onClick = onConnectClick,
            colors = ButtonDefaults.buttonColors(containerColor = PanelColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("TV", fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Media Cast", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                selectedDevice?.let { "Connected to ${it.name.ifBlank { "TCL TV" }}" } ?: "Connect a TV to capture and control",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Text("Gallery", style = MaterialTheme.typography.labelLarge, color = MutedText)
    }
}

@Composable
private fun MediaActionTile(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(containerColor = PanelColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(AccentColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(title.take(1), color = AccentColor, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
    }
}

@Composable
private fun StatusPanel(
    connected: Boolean,
    selectedDevice: SelectedTclDevice?,
    tclStatus: String,
    galleryStatus: String,
    remoteStatus: String,
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.testTag("status_panel"),
        colors = CardDefaults.cardColors(containerColor = PanelColor),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (connected) selectedDevice?.let { "${it.name.ifBlank { "TCL TV" }} — ${it.ip}" } ?: "TV IP selected" else "Please connect your TV......",
                fontWeight = FontWeight.Bold,
                color = if (connected) SuccessColor else AccentColor
            )
            Text(tclStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text(galleryStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text(remoteStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            if (!connected) {
                TextButton(modifier = Modifier.testTag("status_connect_button"), onClick = onConnectClick) { Text("Open discovery and connect") }
            }
        }
    }
}

@Composable
private fun GallerySection(
    selectedTab: String,
    onSelectedTabChange: (String) -> Unit,
    screenshots: List<File>,
    selectedScreenshot: File?,
    galleryBitmap: Bitmap?,
    isExporting: Boolean,
    onRefresh: () -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onExport: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    val tabs = listOf("All", "Photos", "Videos", "Favorites")
    Card(
        modifier = Modifier.testTag("gallery_section"),
        colors = CardDefaults.cardColors(containerColor = PanelColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Gallery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(modifier = Modifier.testTag("gallery_refresh_button"), onClick = onRefresh) { Text("Refresh") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { tab ->
                    GalleryTab(tab = tab, selected = tab == selectedTab, onClick = { onSelectedTabChange(tab) })
                }
            }
            selectedScreenshot?.let { file ->
                Text("Selected: ${file.name} (${formatFileSize(file.length())})", style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
            galleryBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected saved TV capture preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_capture_preview")
                        .height(190.dp)
                        .background(AppBackground, RoundedCornerShape(18.dp))
                )
            }
            if (selectedScreenshot != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.testTag("selected_share_button"), onClick = { onShare(selectedScreenshot) }) { Text("Share") }
                    Button(modifier = Modifier.testTag("selected_export_button"), enabled = !isExporting, onClick = { onExport(selectedScreenshot) }) {
                        Text(if (isExporting) "Exporting" else "Export")
                    }
                    Button(modifier = Modifier.testTag("selected_delete_button"), onClick = { onDelete(selectedScreenshot) }) { Text("Delete") }
                }
            }
            if (screenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(AppBackground, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Capture TV to add screenshots here", color = MutedText, textAlign = TextAlign.Center)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    screenshots.chunked(2).forEach { rowFiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            rowFiles.forEach { file ->
                                GalleryItem(
                                    file = file,
                                    selected = file == selectedScreenshot,
                                    onOpen = onOpen,
                                    onShare = onShare,
                                    onDelete = onDelete,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowFiles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryTab(tab: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.testTag("gallery_tab_$tab"),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) AccentColor else AppBackground,
            contentColor = if (selected) Color.White else MutedText
        )
    ) {
        Text(tab)
    }
}

@Composable
private fun GalleryItem(
    file: File,
    selected: Boolean,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { BitmapFactory.decodeFile(file.absolutePath) }
    Card(
        modifier = modifier.testTag("gallery_item"),
        colors = CardDefaults.cardColors(containerColor = if (selected) AccentColor.copy(alpha = 0.24f) else AppBackground),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_item_preview")
                    .height(105.dp)
                    .clickable { onOpen(file) }
                    .background(Color.Black, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Saved TV capture ${file.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Preview unavailable", style = MaterialTheme.typography.bodySmall, color = MutedText, textAlign = TextAlign.Center)
                }
            }
            Text(file.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${formatFileSize(file.length())} • ${formatTimestamp(file.lastModified())}", style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(modifier = Modifier.testTag("gallery_item_open_button"), onClick = { onOpen(file) }) { Text("Open") }
                TextButton(modifier = Modifier.testTag("gallery_item_share_button"), onClick = { onShare(file) }) { Text("Share") }
            }
            TextButton(modifier = Modifier.testTag("gallery_item_delete_button"), onClick = { onDelete(file) }) { Text("Delete") }
        }
    }
}

@Composable
private fun BottomMediaBar(
    connected: Boolean,
    onConnectClick: () -> Unit,
    onRemoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bottom_status_bar")
                .height(34.dp)
                .background(if (connected) SuccessColor else AccentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(if (connected) "TV connected" else "Please connect your TV......", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .background(PanelColor)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(modifier = Modifier.testTag("bottom_connect_button"), onClick = onConnectClick) { Text("Connect") }
            Button(
                modifier = Modifier.testTag("bottom_remote_button"),
                onClick = onRemoteClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) { Text("Remote", fontWeight = FontWeight.Bold) }
            TextButton(modifier = Modifier.testTag("bottom_tv_button"), onClick = onConnectClick) { Text("TV") }
        }
    }
}

@Composable
private fun ConnectTvDialog(
    selectedDevice: SelectedTclDevice?,
    tvIp: String,
    onTvIpChange: (String) -> Unit,
    discoveryStatus: String,
    isDiscovering: Boolean,
    discoveredDevices: List<TclDiscoveryDevice>,
    tclPhoneName: String,
    onTclPhoneNameChange: (String) -> Unit,
    tclUuid: String,
    onTclUuidChange: (String) -> Unit,
    tclPhoneImei: String,
    onTclPhoneImeiChange: (String) -> Unit,
    onUseAndroidId: () -> Unit,
    onDiscover: () -> Unit,
    onUseDevice: (TclDiscoveryDevice) -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("connect_dialog"),
        confirmButton = { TextButton(modifier = Modifier.testTag("connect_done_button"), onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            TextButton(modifier = Modifier.testTag("forget_tv_button"), enabled = selectedDevice != null, onClick = onForget) { Text("Forget TV") }
        },
        title = { Text("Connect TV") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                selectedDevice?.let { current ->
                    Text("Selected: ${current.name.ifBlank { "TCL TV" }} — ${current.ip}", fontWeight = FontWeight.Bold)
                    Text("Last verified: ${formatTimestamp(current.lastVerifiedAtMillis)}", style = MaterialTheme.typography.bodySmall)
                } ?: Text("No TV selected.", color = AccentColor, fontWeight = FontWeight.Bold)
                Button(modifier = Modifier.testTag("discover_button"), enabled = !isDiscovering, onClick = onDiscover) {
                    Text(if (isDiscovering) "Discovering..." else "Discover TV")
                }
                Text(discoveryStatus, style = MaterialTheme.typography.bodySmall)
                discoveredDevices.forEach { device ->
                    Card(
                        modifier = Modifier.testTag("discovered_device_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${device.name.ifBlank { "TCL TV" }} — ${device.ip}", fontWeight = FontWeight.Bold)
                            Text(
                                listOfNotNull(
                                    "source=${device.source}",
                                    device.mac?.let { "mac=$it" },
                                    device.algorithmType?.let { "algorithm=$it" }
                                ).joinToString("  "),
                                style = MaterialTheme.typography.bodySmall
                            )
                            device.handshake?.let { handshake ->
                                Text(handshake, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                            Button(modifier = Modifier.testTag("use_discovered_device_button"), onClick = { onUseDevice(device) }) { Text("Use ${device.ip}") }
                        }
                    }
                }
                OutlinedTextField(
                    value = tvIp,
                    onValueChange = onTvIpChange,
                    label = { Text("Manual TV IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_tv_ip")
                )
                Text("Advanced identity", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = tclPhoneName,
                    onValueChange = onTclPhoneNameChange,
                    label = { Text("Phone name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("phone_name_field")
                )
                OutlinedTextField(
                    value = tclUuid,
                    onValueChange = onTclUuidChange,
                    label = { Text("Handshake UUID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("handshake_uuid_field")
                )
                OutlinedTextField(
                    value = tclPhoneImei,
                    onValueChange = onTclPhoneImeiChange,
                    label = { Text("Stable phone ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stable_phone_id_field")
                )
                TextButton(modifier = Modifier.testTag("use_android_id_button"), onClick = onUseAndroidId) { Text("Use this app's Android ID") }
            }
        }
    )
}

@Composable
private fun RemoteControlDialog(
    isSendingRemote: Boolean,
    remoteStatus: String,
    onSendRemoteButton: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("remote_dialog"),
        confirmButton = { TextButton(modifier = Modifier.testTag("remote_close_button"), onClick = onDismiss) { Text("Close") } },
        title = { Text("Remote") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Power", TCL_KEY_POWER, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Home", TCL_KEY_HOME, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Back", TCL_KEY_BACK, isSendingRemote, onSendRemoteButton)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Up", TCL_KEY_UP, isSendingRemote, onSendRemoteButton)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Left", TCL_KEY_LEFT, isSendingRemote, onSendRemoteButton)
                        RemoteButton("OK", TCL_KEY_OK, isSendingRemote, onSendRemoteButton)
                        RemoteButton("Right", TCL_KEY_RIGHT, isSendingRemote, onSendRemoteButton)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        RemoteButton("Down", TCL_KEY_DOWN, isSendingRemote, onSendRemoteButton)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Vol -", TCL_KEY_VOLUME_DOWN, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Mute", TCL_KEY_MUTE, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Vol +", TCL_KEY_VOLUME_UP, isSendingRemote, onSendRemoteButton)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton("Menu", TCL_KEY_MENU, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Ch -", TCL_KEY_CHANNEL_DOWN, isSendingRemote, onSendRemoteButton)
                    RemoteButton("Ch +", TCL_KEY_CHANNEL_UP, isSendingRemote, onSendRemoteButton)
                }
                Text(remoteStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
    )
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
        modifier = Modifier.width(82.dp).testTag("remote_button_${label.replace(" ", "_").replace("+", "plus").replace("-", "minus")}"),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PanelColor)
    ) {
        Text(label, maxLines = 1)
    }
}

private val AppBackground = Color(0xFF0B0F18)
private val PanelColor = Color(0xFF171D2A)
private val MutedText = Color(0xFFA9B0C2)
private val AccentColor = Color(0xFFE6426E)
private val SuccessColor = Color(0xFF2DAF7D)

private data class Tcl6553ScreenshotResult(
    val bitmap: Bitmap,
    val byteCount: Int,
    val file: File,
    val url: String,
    val log: String
)

private fun testTclDevice(): TclDiscoveryDevice = TclDiscoveryDevice(
    ip = "192.0.2.10",
    name = "Test Living Room TV",
    source = "ui-test",
    mac = "00:11:22:33:44:55",
    algorithmType = "1",
    handshake = "159>>Test Living Room TV>>0>>0>>0>>0>>1"
)

private suspend fun createTestTclScreenshot(imageDirectory: File): Tcl6553ScreenshotResult = withContext(Dispatchers.IO) {
    imageDirectory.mkdirs()
    val width = 96
    val height = 54
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val red = 40 + (x * 160 / width)
            val green = 30 + (y * 120 / height)
            val blue = if ((x + y) % 14 < 7) 110 else 190
            bitmap.setPixel(x, y, AndroidColor.rgb(red, green, blue))
        }
    }
    val output = ByteArrayOutputStream()
    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode test capture" }
    val bytes = output.toByteArray()
    val file = File(imageDirectory, "TestCapture-${System.currentTimeMillis()}.png")
    file.writeBytes(bytes)
    Tcl6553ScreenshotResult(
        bitmap = bitmap,
        byteCount = bytes.size,
        file = file,
        url = "test://capture",
        log = "Generated test capture"
    )
}

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
    val nowMillis = System.currentTimeMillis()
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Images.Media.TITLE, file.nameWithoutExtension)
        put(MediaStore.Images.Media.MIME_TYPE, imageMimeType(file))
        put(MediaStore.Images.Media.DATE_TAKEN, nowMillis)
        put(MediaStore.Images.Media.DATE_ADDED, nowMillis / 1_000L)
        put(MediaStore.Images.Media.DATE_MODIFIED, nowMillis / 1_000L)
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
        log.protocolLog("socket connect start tv=$tvIp:$port")
        socket.connect(InetSocketAddress(tvIp, port), SOCKET_CONNECT_TIMEOUT_MS)
        log.protocolLog("socket connect complete tv=$tvIp:$port")
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val inquiry = "159>>$phoneName>>1>>$uuid>>1"
        log.protocolLog("send $inquiry")
        writeTclText(output, inquiry, encrypted = false)
        val handshake = readTclPacket(input)
        log.protocolLog("recv ${handshake.text}")
        val fields = handshake.text.split(">>")
        val encrypted = fields.getOrNull(6) == "1"
        log.protocolLog("algorithm=${fields.getOrNull(6).orEmpty()} encrypted=$encrypted")

        val prompt = "160>>$phoneImei>>$phoneName"
        log.protocolLog("send $prompt encrypted=$encrypted")
        writeTclText(output, prompt, encrypted = encrypted)

        val heartbeat = "150>>"
        log.protocolLog("send $heartbeat encrypted=$encrypted")
        writeTclText(output, heartbeat, encrypted = encrypted)
        val heartbeatAck = readTclResponse(input, encrypted, log) { response ->
            response.startsWith("150>>") || response.startsWith("225>>")
        }
        require(heartbeatAck == "150>>YES") { "Unexpected heartbeat response: $heartbeatAck" }

        val shot = "225>>"
        val shotUrls = mutableListOf<String>()
        repeat(TCL_SCREENSHOT_SHOT_COUNT) { index ->
            val shotNumber = index + 1
            log.protocolLog("send screenshot $shotNumber/$TCL_SCREENSHOT_SHOT_COUNT $shot encrypted=$encrypted")
            writeTclText(output, shot, encrypted = encrypted)

            val response = readTclScreenshotResponse(input, encrypted, log)
            val shotFields = response.split(">>")
            require(shotFields.size >= 3 && shotFields[0] == "225" && shotFields[1] == "0") {
                "Unexpected screenshot response for shot $shotNumber: $response"
            }
            val shotUrl = shotFields[2]
            shotUrls += shotUrl
            log.protocolLog("download url $shotUrl")
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
): String = readTclResponse(input, encrypted, log) { response ->
    response.startsWith("225>>")
}

private fun readTclResponse(
    input: DataInputStream,
    encrypted: Boolean,
    log: StringBuilder,
    predicate: (String) -> Boolean
): String {
    val deadline = System.currentTimeMillis() + SOCKET_READ_TIMEOUT_MS
    while (System.currentTimeMillis() < deadline) {
        val packet = readTclPacket(input)
        val text = if (encrypted) decryptTclAes(packet.raw).toString(Charsets.UTF_8) else packet.text
        log.protocolLog("recv $text")
        if (predicate(text)) {
            return text
        }
    }
    error("Timed out waiting for TCL response. ${log.toString().trim()}")
}

private fun StringBuilder.protocolLog(message: String) {
    appendLine(message)
    Log.i(LOG_TAG, "${System.currentTimeMillis()} $message")
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
            log.protocolLog("download attempt ${index + 1}/$HTTP_DOWNLOAD_ATTEMPTS")
            val bytes = downloadScreenshotUrl(url)
            require(bytes.size in 1..MAX_SCREENSHOT_BYTES) { "Unexpected image byte count: ${bytes.size}" }
            return bytes
        } catch (error: Throwable) {
            lastError = error
            log.protocolLog("download failed: ${error.message ?: error::class.java.simpleName}")
            if (index + 1 == HTTP_DIRECT_ATTEMPTS_BEFORE_PORT_SCAN) {
                findScreenshotOnFreshHttpPort(url, log)?.let { return it }
            }
            Thread.sleep(HTTP_DOWNLOAD_RETRY_DELAY_MS)
        }
    }
    throw IllegalStateException(
        "Downloaded screenshot URL never opened: ${lastError?.message ?: lastError?.javaClass?.simpleName}\n${log.toString().trim()}"
    )
}

private fun downloadScreenshotUrl(url: String): ByteArray {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = HTTP_CONNECT_TIMEOUT_MS
        readTimeout = HTTP_READ_TIMEOUT_MS
        requestMethod = "GET"
        useCaches = false
    }
    return try {
        connection.inputStream.use { stream -> stream.readBytesBounded(MAX_SCREENSHOT_BYTES) }
    } finally {
        connection.disconnect()
    }
}

private fun findScreenshotOnFreshHttpPort(url: String, log: StringBuilder): ByteArray? {
    val parsed = URL(url)
    val stalePort = parsed.port
    val host = parsed.host.takeIf { it.isNotBlank() } ?: return null
    val path = buildString {
        append(parsed.file.takeIf { it.isNotBlank() } ?: "/")
    }
    val portCount = HTTP_PORT_SCAN_LAST_PORT - HTTP_PORT_SCAN_FIRST_PORT + 1
    var portOrder = randomizedPortOrder(portCount, stalePort)
    val stopped = AtomicBoolean(false)
    val probeLogCount = AtomicInteger(0)
    val workers = minOf(HTTP_PORT_SCAN_PARALLELISM, portOrder.size)
    val executor = Executors.newFixedThreadPool(workers)
    val completion = ExecutorCompletionService<Pair<Int, ByteArray>?>(executor)
    var nextPortIndex = 0
    var submitted = 0
    var completed = 0
    var pass = 1
    var startedAt = 0L

    fun submitNext() {
        while (!stopped.get() && submitted - completed < workers && nextPortIndex < portOrder.size) {
            val port = portOrder[nextPortIndex]
            nextPortIndex += 1
            if (port == stalePort) continue
            completion.submit(Callable {
                if (stopped.get()) null else probeScreenshotPort(host, port, path, probeLogCount)
            })
            submitted += 1
        }
    }

    log.protocolLog(
        "port scan wait ${HTTP_PORT_SCAN_START_DELAY_MS}ms before fallback sweep host=$host stale=$stalePort"
    )
    Thread.sleep(HTTP_PORT_SCAN_START_DELAY_MS)
    startedAt = System.currentTimeMillis()
    log.protocolLog(
        "port scan start host=$host path=$path ports=${HTTP_PORT_SCAN_FIRST_PORT}..$HTTP_PORT_SCAN_LAST_PORT stale=$stalePort workers=$workers"
    )
    try {
        while (System.currentTimeMillis() - startedAt < HTTP_PORT_SCAN_TIMEOUT_MS) {
            if (completed == submitted) {
                if (nextPortIndex >= portOrder.size) {
                    pass += 1
                    nextPortIndex = 0
                    portOrder = randomizedPortOrder(portCount, stalePort)
                    log.protocolLog("port scan retry pass=$pass submitted=$submitted completed=$completed")
                }
                submitNext()
            }

            val remaining = HTTP_PORT_SCAN_TIMEOUT_MS - (System.currentTimeMillis() - startedAt)
            if (remaining <= 0L) break
            val future = completion.poll(remaining.coerceAtMost(250L), TimeUnit.MILLISECONDS)
            if (future == null) {
                submitNext()
                continue
            }
            completed += 1
            val result = runCatching { future.get() }.getOrNull()
            if (result != null) {
                stopped.set(true)
                log.protocolLog(
                    "port scan found screenshot port=${result.first} bytes=${result.second.size} submitted=$submitted completed=$completed pass=$pass"
                )
                return result.second
            }
            submitNext()
        }
    } finally {
        stopped.set(true)
        executor.shutdownNow()
    }
    log.protocolLog("port scan timeout submitted=$submitted completed=$completed next=$nextPortIndex pass=$pass")
    return null
}

private fun randomizedPortOrder(portCount: Int, stalePort: Int): IntArray {
    val ports = IntArray(portCount)
    val staleOffset = (stalePort - HTTP_PORT_SCAN_FIRST_PORT).coerceIn(0, portCount - 1)
    val seed = (System.nanoTime() xor stalePort.toLong()).toPositiveInt()
    val start = (staleOffset + seed).floorMod(portCount)
    var step = (seed or 1).floorMod(portCount).coerceAtLeast(1)
    while (greatestCommonDivisor(step, portCount) != 1) {
        step += 2
        if (step >= portCount) step = 1
    }
    for (index in 0 until portCount) {
        ports[index] = HTTP_PORT_SCAN_FIRST_PORT + ((start + index * step).floorMod(portCount))
    }
    return ports
}

private fun Long.toPositiveInt(): Int = (this and Int.MAX_VALUE.toLong()).toInt()

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

private tailrec fun greatestCommonDivisor(left: Int, right: Int): Int =
    if (right == 0) left else greatestCommonDivisor(right, left % right)

private fun probeScreenshotPort(
    host: String,
    port: Int,
    path: String,
    probeLogCount: AtomicInteger
): Pair<Int, ByteArray>? = runCatching {
    Socket().use { socket ->
        socket.tcpNoDelay = true
        socket.soTimeout = HTTP_PORT_SCAN_READ_TIMEOUT_MS
        socket.connect(InetSocketAddress(host, port), HTTP_PORT_SCAN_CONNECT_TIMEOUT_MS)
        val request = "GET $path HTTP/1.1\r\nHost: $host:$port\r\nConnection: close\r\n\r\n"
        socket.getOutputStream().write(request.toByteArray(Charsets.US_ASCII))
        socket.getOutputStream().flush()

        val input = socket.getInputStream()
        val header = input.readHttpHeadersBounded() ?: return@runCatching null
        val statusCode = header.lineSequence()
            .firstOrNull()
            ?.split(' ')
            ?.getOrNull(1)
            ?.toIntOrNull()
        val contentLength = header.lineSequence()
            .firstNotNullOfOrNull { line ->
                val separator = line.indexOf(':')
                if (separator < 0 || !line.substring(0, separator).equals("Content-Length", ignoreCase = true)) {
                    null
                } else {
                    line.substring(separator + 1).trim().toIntOrNull()
                }
            }
        if (probeLogCount.getAndIncrement() < 24) {
            Log.i(LOG_TAG, "${System.currentTimeMillis()} port probe connected port=$port code=$statusCode length=${contentLength ?: -1}")
        }
        if (statusCode != 200) return@runCatching null
        val body = if (contentLength != null) {
            input.readExactlyBytesBounded(contentLength, MAX_SCREENSHOT_BYTES)
        } else {
            input.readBytesBoundedUntilTimeout(MAX_SCREENSHOT_BYTES)
        }
        if (body.size !in 1..MAX_SCREENSHOT_BYTES || !looksLikeScreenshot(body)) return@runCatching null
        if (BitmapFactory.decodeByteArray(body, 0, body.size) == null) return@runCatching null
        port to body
    }
}.getOrNull()

private fun InputStream.readHttpHeadersBounded(): String? {
    val output = ByteArrayOutputStream()
    var matched = 0
    while (output.size() < 8 * 1024) {
        val value = read()
        if (value == -1) return null
        output.write(value)
        matched = when {
            matched == 0 && value == '\r'.code -> 1
            matched == 1 && value == '\n'.code -> 2
            matched == 2 && value == '\r'.code -> 3
            matched == 3 && value == '\n'.code -> return output.toByteArray().decodeToString()
            value == '\r'.code -> 1
            else -> 0
        }
    }
    return null
}

private fun InputStream.readExactlyBytesBounded(size: Int, maxBytes: Int): ByteArray {
    require(size in 0..maxBytes) { "Response exceeded $maxBytes bytes" }
    val bytes = ByteArray(size)
    var offset = 0
    while (offset < size) {
        val read = read(bytes, offset, size - offset)
        if (read == -1) error("Response ended after $offset of $size bytes")
        offset += read
    }
    return bytes
}

private fun InputStream.readBytesBoundedUntilTimeout(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = try {
            read(buffer)
        } catch (_: SocketTimeoutException) {
            break
        }
        if (read == -1) break
        total += read
        require(total <= maxBytes) { "Response exceeded $maxBytes bytes" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun InputStream.readBytesBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read == -1) break
        total += read
        require(total <= maxBytes) { "Response exceeded $maxBytes bytes" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun looksLikeScreenshot(bytes: ByteArray): Boolean =
    bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() ||
        bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4e.toByte() && bytes[3] == 0x47.toByte() &&
        bytes[4] == 0x0d.toByte() && bytes[5] == 0x0a.toByte() &&
        bytes[6] == 0x1a.toByte() && bytes[7] == 0x0a.toByte()

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