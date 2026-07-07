package com.example.tlctvscreenshot

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.ArrayDeque
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
private const val HTTP_DIRECT_ATTEMPTS_BEFORE_PORT_SCAN = 1
private const val HTTP_PORT_SCAN_FIRST_PORT = 32768
private const val HTTP_PORT_SCAN_LAST_PORT = 60999
private const val HTTP_PORT_SCAN_CONNECT_TIMEOUT_MS = 75
private const val HTTP_PORT_SCAN_READ_TIMEOUT_MS = 1_000
private const val HTTP_PORT_SCAN_PARALLELISM = 384
private const val HTTP_PORT_SCAN_START_DELAY_MS = 0L
private const val HTTP_PORT_SCAN_TIMEOUT_MS = 12_000L
private const val TCL_SCREENSHOT_SHOT_COUNT = 1
private const val TCL_SESSION_HEARTBEAT_INTERVAL_MS = 15_000L
private const val TCL_SESSION_RETRY_DELAY_MS = 2_000L
private const val VIDEO_CAPTURE_TARGET_FRAME_INTERVAL_MS = 550L
private const val VIDEO_CAPTURE_MAX_DURATION_MS = 30_000L
private const val VIDEO_CAPTURE_FRAME_RATE = 12
private const val VIDEO_CAPTURE_BIT_RATE = 4_000_000
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

private data class TclRemoteButtonSpec(
    val label: String,
    val keyCode: Int,
    val displayLabel: String,
    val testTag: String = remoteButtonTag(label)
)

private val TCL_REMOTE_BUTTONS = listOf(
    TclRemoteButtonSpec("Power", TCL_KEY_POWER, "⏻"),
    TclRemoteButtonSpec("Home", TCL_KEY_HOME, "🏠"),
    TclRemoteButtonSpec("Back", TCL_KEY_BACK, "↩"),
    TclRemoteButtonSpec("Up", TCL_KEY_UP, "⬆"),
    TclRemoteButtonSpec("Left", TCL_KEY_LEFT, "⬅"),
    TclRemoteButtonSpec("OK", TCL_KEY_OK, "OK"),
    TclRemoteButtonSpec("Right", TCL_KEY_RIGHT, "➡"),
    TclRemoteButtonSpec("Down", TCL_KEY_DOWN, "⬇"),
    TclRemoteButtonSpec("Vol -", TCL_KEY_VOLUME_DOWN, "🔉"),
    TclRemoteButtonSpec("Mute", TCL_KEY_MUTE, "🔇"),
    TclRemoteButtonSpec("Vol +", TCL_KEY_VOLUME_UP, "🔊"),
    TclRemoteButtonSpec("Menu", TCL_KEY_MENU, "☰"),
    TclRemoteButtonSpec("Ch -", TCL_KEY_CHANNEL_DOWN, "CH−"),
    TclRemoteButtonSpec("Ch +", TCL_KEY_CHANNEL_UP, "CH+")
)

private fun tclRemoteButtonSpecs(): List<TclRemoteButtonSpec> = TCL_REMOTE_BUTTONS

private fun tclRemoteButton(label: String): TclRemoteButtonSpec =
    tclRemoteButtonSpecs().single { it.label == label }

private val TCL_REMOTE_POWER_ROW = listOf("Power", "Home", "Back").map(::tclRemoteButton)

private val TCL_REMOTE_DPAD_CENTER_ROW = listOf("Left", "OK", "Right").map(::tclRemoteButton)

private val TCL_REMOTE_VOLUME_ROW = listOf("Vol -", "Mute", "Vol +").map(::tclRemoteButton)

private val TCL_REMOTE_CHANNEL_ROW = listOf("Menu", "Ch -", "Ch +").map(::tclRemoteButton)

private val SquareComponentShapes = Shapes(
    extraSmall = CutCornerShape(0.dp),
    small = CutCornerShape(0.dp),
    medium = CutCornerShape(0.dp),
    large = CutCornerShape(0.dp),
    extraLarge = CutCornerShape(0.dp)
)

private fun remoteButtonTag(label: String): String =
    "remote_button_${label.replace(" ", "_").replace("+", "plus").replace("-", "minus")}"

private fun tclRemoteKeyCommand(keyCode: Int): String = "$TCL_REMOTE_KEY_COMMAND>>$keyCode"

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
    MaterialTheme(colorScheme = darkColorScheme(), shapes = SquareComponentShapes) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScreenshotWorkbench(testMode = testMode)
        }
    }
}

@Composable
private fun ScreenshotWorkbench(testMode: Boolean = false) {
    val context = LocalContext.current
    SideEffect { ScreenshotPortCache.bind(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val screenshotDirectory = remember { screenshotDirectory(context) }
    val videoDraftDirectory = remember { videoDraftDirectory(context) }
    val tclSessionManager = remember { Tcl6553SessionManager() }
    val fastCaptureState by tclSessionManager.state.collectAsState()
    DisposableEffect(Unit) {
        onDispose { tclSessionManager.close() }
    }
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
    var streamProbeStatus by remember { mutableStateOf("TV stream probe not run.") }
    var isProbingStreams by remember { mutableStateOf(false) }
    var isCapturingTcl by remember { mutableStateOf(false) }
    var activeRemoteSends by remember { mutableStateOf(0) }
    var discoveredDevices by remember { mutableStateOf<List<TclDiscoveryDevice>>(emptyList()) }
    var discoveryStatus by remember { mutableStateOf("Open Connect TV to search for nearby TVs on this network.") }
    var isDiscovering by remember { mutableStateOf(false) }
    var currentWifiName by remember { mutableStateOf(if (testMode) "Test Wi-Fi" else currentWifiDisplayName(context)) }
    var screenshots by remember { mutableStateOf(loadScreenshotFiles(context)) }
    var selectedScreenshot by remember { mutableStateOf(screenshots.firstOrNull()) }
    var galleryBitmap by remember { mutableStateOf(selectedScreenshot?.let { loadCapturePreview(it) }) }
    var galleryStatus by remember {
        mutableStateOf(
            if (screenshots.isEmpty()) "No saved captures yet." else "Loaded ${screenshots.size} saved capture(s)."
        )
    }
    val appSettings = remember(context) { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var debugModeEnabled by remember { mutableStateOf(appSettings.getBoolean("debug_mode_enabled", false)) }
    var deleteCandidate by remember { mutableStateOf<File?>(null) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showRemoteDialog by remember { mutableStateOf(false) }
    var showVideoCaptureDialog by remember { mutableStateOf(false) }
    var videoCaptureState by remember { mutableStateOf(VideoCaptureUiState()) }
    var videoRecordingJob by remember { mutableStateOf<Job?>(null) }
    val settingsDrawerState = rememberDrawerState(DrawerValue.Closed)
    var selectedGalleryTab by remember { mutableStateOf("All") }

    fun currentTvIp() = selectedDevice?.ip?.ifBlank { tvIp } ?: tvIp

    val connectedToTv = selectedDevice != null || currentTvIp().isNotBlank()
    val fastCaptureUiStatus = fastCaptureUiStatus(connectedToTv, fastCaptureState)

    fun rememberSelectedDevice(device: TclDiscoveryDevice) {
        val remembered = device.toSelectedDevice()
        selectedDevice = remembered
        tvIp = remembered.ip
        saveSelectedTclDevice(context, remembered)
    }

    val warmTvIp = currentTvIp().trim()
    LaunchedEffect(testMode, warmTvIp, tclPhoneName, tclUuid, tclPhoneImei) {
        if (testMode || warmTvIp.isBlank()) {
            tclSessionManager.clear()
        } else {
            tclSessionManager.configure(
                scope = coroutineScope,
                config = Tcl6553SessionConfig(
                    tvIp = warmTvIp,
                    port = TCL_COMMAND_PORT,
                    phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                    uuid = tclUuid.ifBlank { androidId },
                    phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } }
                )
            )
        }
    }

    fun refreshGallery(preferredFile: File? = selectedScreenshot) {
        val loaded = loadScreenshotFiles(context)
        screenshots = loaded
        val nextSelected = preferredFile?.takeIf { it.exists() } ?: loaded.firstOrNull()
        selectedScreenshot = nextSelected
        galleryBitmap = nextSelected?.let { loadCapturePreview(it) }
        galleryStatus = if (loaded.isEmpty()) "No saved captures yet." else "Loaded ${loaded.size} saved capture(s)."
    }

    fun startDiscovery() {
        isDiscovering = true
        discoveredDevices = emptyList()
        currentWifiName = if (testMode) "Test Wi-Fi" else currentWifiDisplayName(context)
        discoveryStatus = "Searching for nearby TVs on this network..."
        if (testMode) {
            val testDevice = testTclDevice()
            discoveredDevices = listOf(testDevice)
            discoveryStatus = "Found 1 TV candidate. Select it to connect."
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
                discoveryStatus = if (devices.isEmpty()) {
                    "No TVs found. Check that the phone and TV are on the same local network, then refresh."
                } else {
                    "Found ${devices.size} TV candidate(s). Select one to connect."
                }
            }.onFailure { error ->
                discoveryStatus = "Discovery failed: ${error.message ?: error::class.java.simpleName}"
            }
            isDiscovering = false
        }
    }

    fun openConnectDialog() {
        showConnectDialog = true
        if (!isDiscovering) {
            startDiscovery()
        }
    }

    fun retryFastConnection() {
        val ip = currentTvIp().trim()
        if (ip.isBlank()) {
            tclStatus = "Please connect your TV first."
            openConnectDialog()
            return
        }
        tclStatus = "Retrying fast TV connection..."
        tclSessionManager.reconnect()
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
            openConnectDialog()
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
                    imageDirectory = screenshotDirectory,
                    sessionManager = tclSessionManager
                )
            }.onSuccess { result ->
                selectedScreenshot = result.file
                galleryBitmap = result.bitmap
                screenshots = loadScreenshotFiles(context)
                tclStatus = buildString {
                    append("Captured ${formatFileSize(result.byteCount.toLong())} from TV")
                    if (result.timingSummary.isNotBlank()) {
                        append("\n")
                        append(result.timingSummary)
                    } else {
                        append(".")
                    }
                }
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

    fun openVideoCaptureDialog() {
        showVideoCaptureDialog = true
        videoCaptureState = VideoCaptureUiState(
            includeAudio = false,
            audioStatus = "TV audio stream unavailable",
            status = "Ready to record TV video."
        )
    }

    fun toggleVideoAudio() {
        videoCaptureState = videoCaptureState.copy(
            includeAudio = false,
            audioStatus = "TV audio stream unavailable"
        )
    }

    fun stopVideoRecording() {
        videoRecordingJob?.cancel(CancellationException("Stopped by user"))
    }

    fun discardVideoCapture() {
        videoRecordingJob?.cancel(CancellationException("Discarded by user"))
        videoCaptureState.file?.delete()
        videoCaptureState.audioFile?.delete()
        videoRecordingJob = null
        videoCaptureState = VideoCaptureUiState(status = "Recording discarded.")
        showVideoCaptureDialog = false
        refreshGallery()
    }

    fun keepVideoCapture() {
        val draftFile = videoCaptureState.file ?: return
        val audioFile = videoCaptureState.audioFile
        val includeAudio = videoCaptureState.includeAudio
        videoCaptureState = videoCaptureState.copy(phase = VideoCapturePhase.SAVING, status = "Saving video...")
        coroutineScope.launch {
            runCatching {
                val finalFile = uniqueVideoCaptureFile(screenshotDirectory, System.currentTimeMillis())
                if (includeAudio && audioFile?.exists() == true) {
                    muxVideoAndOptionalAudio(draftFile, audioFile, finalFile, 0L, videoDurationMs(draftFile), includeAudio = true)
                    draftFile.delete()
                    audioFile.delete()
                } else {
                    moveCaptureFile(draftFile, finalFile)
                    audioFile?.delete()
                }
                if (!testMode) exportVideoToMovies(context.applicationContext, finalFile)
                finalFile
            }.onSuccess { file ->
                selectedScreenshot = file
                galleryBitmap = loadCapturePreview(file)
                refreshGallery(file)
                galleryStatus = "Saved ${file.name} to Gallery and Movies."
                showVideoCaptureDialog = false
                videoCaptureState = VideoCaptureUiState(status = "Video saved.")
            }.onFailure { error ->
                videoCaptureState = videoCaptureState.copy(
                    phase = VideoCapturePhase.REVIEW,
                    status = "Save failed: ${error.message ?: error::class.java.simpleName}"
                )
            }
        }
    }

    fun saveEditedVideoCapture(startText: String, endText: String) {
        val sourceFile = videoCaptureState.file ?: return
        val audioFile = videoCaptureState.audioFile
        val includeAudio = videoCaptureState.includeAudio
        val startMs = (startText.toDoubleOrNull()?.times(1_000.0)?.toLong() ?: 0L).coerceAtLeast(0L)
        val endMs = (endText.toDoubleOrNull()?.times(1_000.0)?.toLong() ?: videoCaptureState.durationMs)
            .coerceAtLeast(startMs + 250L)
        videoCaptureState = videoCaptureState.copy(phase = VideoCapturePhase.SAVING, status = "Saving edited video...")
        coroutineScope.launch {
            runCatching {
                val outputFile = uniqueVideoCaptureFile(screenshotDirectory, System.currentTimeMillis(), suffix = "edited")
                muxVideoAndOptionalAudio(sourceFile, audioFile, outputFile, startMs, endMs, includeAudio)
                if (sourceFile.parentFile?.canonicalPath == videoDraftDirectory.canonicalPath) {
                    sourceFile.delete()
                }
                audioFile?.delete()
                if (!testMode) exportVideoToMovies(context.applicationContext, outputFile)
                outputFile
            }.onSuccess { editedFile ->
                selectedScreenshot = editedFile
                galleryBitmap = loadCapturePreview(editedFile)
                refreshGallery(editedFile)
                galleryStatus = "Saved edited ${editedFile.name} to Gallery and Movies."
                showVideoCaptureDialog = false
                videoCaptureState = VideoCaptureUiState(status = "Edited video saved.")
            }.onFailure { error ->
                videoCaptureState = videoCaptureState.copy(
                    phase = VideoCapturePhase.REVIEW,
                    status = "Edit failed: ${error.message ?: error::class.java.simpleName}"
                )
            }
        }
    }

    fun startVideoRecording() {
        if (videoRecordingJob?.isActive == true) return
        val ip = currentTvIp().trim()
        if (!testMode && ip.isBlank()) {
            tclStatus = "Please connect your TV first."
            videoCaptureState = VideoCaptureUiState(status = "Connect your TV before recording.")
            openConnectDialog()
            return
        }
        val outputFile = uniqueVideoCaptureFile(videoDraftDirectory, System.currentTimeMillis(), suffix = "draft")
        val startedAt = System.currentTimeMillis()
        videoCaptureState = videoCaptureState.copy(
            phase = VideoCapturePhase.RECORDING,
            startedAtMillis = startedAt,
            elapsedMs = 0L,
            frameCount = 0,
            status = "Recording TV video...",
            file = outputFile,
            audioFile = null,
            includeAudio = false,
            audioStatus = "TV audio stream unavailable",
            previewFrame = null,
            timelineFrames = emptyList()
        )
        tclStatus = "Recording TV video..."
        videoRecordingJob = coroutineScope.launch {
            runCatching {
                val result = if (testMode) {
                    recordTestTvVideo(
                        outputFile = outputFile,
                        onProgress = { frameCount, elapsedMs ->
                            videoCaptureState = videoCaptureState.copy(
                                elapsedMs = elapsedMs,
                                frameCount = frameCount,
                                status = "Recording test video: ${formatDurationMs(elapsedMs)} • $frameCount frames"
                            )
                        },
                        onPreviewFrame = { frame ->
                            videoCaptureState = videoCaptureState.withPreviewFrame(frame)
                        }
                    )
                } else {
                    recordTcl6553Video(
                        tvIp = ip,
                        port = TCL_COMMAND_PORT,
                        phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                        uuid = tclUuid.ifBlank { androidId },
                        phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                        outputFile = outputFile,
                        sessionManager = tclSessionManager,
                        isActive = { videoRecordingJob?.isActive == true },
                        onProgress = { frameCount, elapsedMs ->
                            videoCaptureState = videoCaptureState.copy(
                                elapsedMs = elapsedMs,
                                frameCount = frameCount,
                                status = "Recording: ${formatDurationMs(elapsedMs)} • $frameCount frames"
                            )
                        },
                        onPreviewFrame = { frame ->
                            videoCaptureState = videoCaptureState.withPreviewFrame(frame)
                        }
                    )
                }
                result
            }.onSuccess { result ->
                videoCaptureState = videoCaptureState.copy(
                    phase = VideoCapturePhase.REVIEW,
                    elapsedMs = result.durationMs,
                    frameCount = result.frameCount,
                    file = result.file,
                    audioFile = null,
                    durationMs = result.durationMs,
                    trimStartText = "0.0",
                    trimEndText = formatSecondsText(result.durationMs),
                    status = "Recorded ${formatDurationMs(result.durationMs)} with ${result.frameCount} frame(s). Review before saving.",
                    audioStatus = "TV audio stream unavailable"
                )
                tclStatus = "Recorded video in ${formatDurationMs(result.durationMs)}."
                galleryStatus = "Review video, then keep or save an edit."
            }.onFailure { error ->
                if (error is CancellationException && outputFile.exists()) {
                    val durationMs = videoDurationMs(outputFile).takeIf { it > 0L }
                        ?: (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                    val frameCount = videoCaptureState.frameCount
                    videoCaptureState = videoCaptureState.copy(
                        phase = VideoCapturePhase.REVIEW,
                        elapsedMs = durationMs,
                        frameCount = frameCount,
                        file = outputFile,
                        audioFile = null,
                        durationMs = durationMs,
                        trimStartText = "0.0",
                        trimEndText = formatSecondsText(durationMs),
                        status = "Recording stopped at ${formatDurationMs(durationMs)} with $frameCount frame(s).",
                        audioStatus = "TV audio stream unavailable"
                    )
                    tclStatus = "Recording stopped."
                    galleryStatus = "Review video, then keep or save an edit."
                } else {
                    outputFile.delete()
                    videoCaptureState = VideoCaptureUiState(status = "Video recording failed: ${error.message ?: error::class.java.simpleName}")
                    tclStatus = "Video recording failed: ${error.message ?: error::class.java.simpleName}"
                }
            }
            videoRecordingJob = null
        }
    }

    fun probeTvStreams() {
        val ip = currentTvIp().trim()
        if (ip.isBlank()) {
            streamProbeStatus = "Connect a TV before probing streams."
            openConnectDialog()
            return
        }
        if (isProbingStreams) return
        isProbingStreams = true
        streamProbeStatus = "Probing TV stream services..."
        coroutineScope.launch {
            runCatching { probeTvStreamServices(ip) }
                .onSuccess { results ->
                    streamProbeStatus = formatStreamProbeSummary(results)
                }
                .onFailure { error ->
                    streamProbeStatus = "TV stream probe failed: ${error.message ?: error::class.java.simpleName}"
                }
            isProbingStreams = false
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
            openConnectDialog()
            return
        }
        activeRemoteSends += 1
        remoteStatus = "Sending $label to TV..."
        coroutineScope.launch {
            val started = System.currentTimeMillis()
            val log = StringBuilder()
            runCatching {
                val sentWarm = tclSessionManager.sendRemoteKey(keyCode, log)
                if (!sentWarm) {
                    sendTcl6553RemoteKey(
                        tvIp = ip,
                        port = TCL_COMMAND_PORT,
                        phoneName = tclPhoneName.ifBlank { Build.MODEL ?: "Android" },
                        uuid = tclUuid.ifBlank { androidId },
                        phoneImei = tclPhoneImei.ifBlank { tclUuid.ifBlank { androidId } },
                        keyCode = keyCode
                    )
                }
                sentWarm
            }.onSuccess { sentWarm ->
                val durationMs = System.currentTimeMillis() - started
                val path = if (sentWarm) "warm" else "cold"
                Log.i(LOG_TAG, "remote $label sent via $path session in ${durationMs}ms")
                remoteStatus = "Sent $label in ${formatDurationMs(durationMs)}."
            }.onFailure { error ->
                remoteStatus = "Remote command failed: ${error.message ?: error::class.java.simpleName}"
            }
            activeRemoteSends = (activeRemoteSends - 1).coerceAtLeast(0)
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
            currentWifiName = currentWifiName,
            debugModeEnabled = debugModeEnabled,
            onOpenWifiSettings = {
                if (!testMode) {
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            onDiscover = { startDiscovery() },
            onUseDevice = { device ->
                rememberSelectedDevice(device)
                discoveryStatus = "Connected to ${device.name.ifBlank { "TV" }}."
            },
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
            remoteStatus = remoteStatus,
            onSendRemoteButton = ::sendRemoteButton,
            onDismiss = { showRemoteDialog = false }
        )
    }

    if (showVideoCaptureDialog) {
        VideoCaptureDialog(
            state = videoCaptureState,
            onStart = { startVideoRecording() },
            onStop = { stopVideoRecording() },
            onTrimStartChange = { videoCaptureState = videoCaptureState.copy(trimStartText = it, activeTrimHandle = VideoTrimHandle.START) },
            onTrimEndChange = { videoCaptureState = videoCaptureState.copy(trimEndText = it, activeTrimHandle = VideoTrimHandle.END) },
            onActiveTrimHandleChange = { videoCaptureState = videoCaptureState.copy(activeTrimHandle = it) },
            onAudioToggle = { toggleVideoAudio() },
            onSaveEdited = { saveEditedVideoCapture(videoCaptureState.trimStartText, videoCaptureState.trimEndText) },
            onKeepOriginal = { keepVideoCapture() },
            onDiscard = { discardVideoCapture() },
            onDismiss = {
                when (videoCaptureState.phase) {
                    VideoCapturePhase.RECORDING -> stopVideoRecording()
                    VideoCapturePhase.REVIEW -> discardVideoCapture()
                    VideoCapturePhase.SAVING -> Unit
                    VideoCapturePhase.READY -> showVideoCaptureDialog = false
                }
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = settingsDrawerState,
            gesturesEnabled = settingsDrawerState.isOpen,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    SettingsDrawer(
                        debugModeEnabled = debugModeEnabled,
                        onDebugModeEnabledChange = { enabled ->
                            debugModeEnabled = enabled
                            appSettings.edit().putBoolean("debug_mode_enabled", enabled).apply()
                        },
                        onDismiss = { coroutineScope.launch { settingsDrawerState.close() } }
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            onSettingsClick = { coroutineScope.launch { settingsDrawerState.open() } }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MediaActionTile(
                                title = "Capture Photo",
                                subtitle = if (isCapturingTcl) "Capturing" else fastCaptureUiStatus.captureSubtitle,
                                enabled = !isCapturingTcl,
                                modifier = Modifier.weight(1f).testTag("action_capture_photo"),
                                onClick = { captureTv() }
                            )
                            MediaActionTile(
                                title = "Capture Video",
                                subtitle = "Video",
                                modifier = Modifier.weight(1f).testTag("action_capture_video"),
                                onClick = { openVideoCaptureDialog() }
                            )
                            CastDropdownTile(
                                modifier = Modifier.weight(1f),
                                onPhotoClick = {
                                    selectedGalleryTab = "Photos"
                                    galleryStatus = "Photo casting is not required for TV capture. Saved screenshots stay available here."
                                },
                                onVideoClick = {
                                    selectedGalleryTab = "Videos"
                                    galleryStatus = "Video casting is not configured in this standalone build."
                                },
                                onMusicClick = {
                                    selectedGalleryTab = "Music"
                                    galleryStatus = "Music casting is not configured in this standalone build."
                                }
                            )
                        }

                        if (debugModeEnabled) {
                            ActivityStatusPanel(
                                tclStatus = tclStatus,
                                galleryStatus = galleryStatus,
                                remoteStatus = remoteStatus,
                                streamProbeStatus = streamProbeStatus,
                                isProbingStreams = isProbingStreams,
                                onProbeStreams = { probeTvStreams() }
                            )
                        }

                        GallerySection(
                            selectedTab = selectedGalleryTab,
                            onSelectedTabChange = { selectedGalleryTab = it },
                            screenshots = screenshots,
                            selectedScreenshot = selectedScreenshot,
                            galleryBitmap = galleryBitmap,
                            onRefresh = { refreshGallery() },
                            onOpen = { file ->
                                selectedScreenshot = file
                                galleryBitmap = loadCapturePreview(file)
                                galleryStatus = "Opened ${file.name}."
                            },
                            onShare = { file ->
                                if (testMode) {
                                    galleryStatus = "Test shared ${file.name}."
                                } else {
                                    shareScreenshot(context, file)
                                }
                            },
                            onDelete = { file -> deleteCandidate = file }
                        )
                    }

                    BottomMediaBar(
                        fastCaptureStatus = fastCaptureUiStatus,
                        onConnectClick = { openConnectDialog() },
                        onFastConnectClick = { retryFastConnection() },
                        onRemoteClick = { showRemoteDialog = true },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaHomeHeader(
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("top_status_area"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            modifier = Modifier.testTag("settings_menu_button"),
            onClick = onSettingsClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("☰", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsDrawer(
    debugModeEnabled: Boolean,
    onDebugModeEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .testTag("settings_drawer"),
        drawerShape = RectangleShape,
        drawerContainerColor = PanelColor,
        drawerContentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Settings",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(modifier = Modifier.testTag("settings_done_button"), onClick = onDismiss) { Text("Done") }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDebugModeEnabledChange(!debugModeEnabled) }
                    .testTag("debug_mode_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Debug mode", fontWeight = FontWeight.Bold)
                    Text(
                        "Show the activity pane with capture, gallery, and remote status messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                }
                Switch(
                    modifier = Modifier.testTag("debug_mode_switch"),
                    checked = debugModeEnabled,
                    onCheckedChange = onDebugModeEnabledChange
                )
            }
        }
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(112.dp),
        color = Color.Transparent,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
                    .background(AccentColor.copy(alpha = 0.18f), RectangleShape),
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
private fun CastDropdownTile(
    modifier: Modifier = Modifier,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onMusicClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MediaActionTile(
            title = "Cast...",
            subtitle = "Photo • Video • Music",
            modifier = Modifier.fillMaxWidth().testTag("action_cast_menu"),
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("cast_options_menu")
        ) {
            DropdownMenuItem(
                modifier = Modifier.testTag("cast_option_photo"),
                text = { Text("Photo") },
                onClick = {
                    expanded = false
                    onPhotoClick()
                }
            )
            DropdownMenuItem(
                modifier = Modifier.testTag("cast_option_video"),
                text = { Text("Video") },
                onClick = {
                    expanded = false
                    onVideoClick()
                }
            )
            DropdownMenuItem(
                modifier = Modifier.testTag("cast_option_music"),
                text = { Text("Music") },
                onClick = {
                    expanded = false
                    onMusicClick()
                }
            )
        }
    }
}

@Composable
private fun ActivityStatusPanel(
    tclStatus: String,
    galleryStatus: String,
    remoteStatus: String,
    streamProbeStatus: String,
    isProbingStreams: Boolean,
    onProbeStreams: () -> Unit
) {
    Surface(
        modifier = Modifier.testTag("status_panel"),
        color = Color.Transparent,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Activity", fontWeight = FontWeight.Bold, color = MutedText)
            Text(tclStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text(galleryStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text(remoteStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            Text("TV stream probe", fontWeight = FontWeight.Bold, color = MutedText)
            Text(streamProbeStatus, modifier = Modifier.testTag("stream_probe_status"), style = MaterialTheme.typography.bodySmall, color = MutedText)
            TextButton(
                modifier = Modifier.testTag("stream_probe_button"),
                enabled = !isProbingStreams,
                onClick = onProbeStreams
            ) { Text(if (isProbingStreams) "Probing..." else "Find TV streams") }
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
    onRefresh: () -> Unit,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    val tabs = listOf("All", "Photos", "Videos", "Favorites")
    var showGalleryPane by remember { mutableStateOf(false) }

    if (showGalleryPane) {
        GalleryPaneDialog(
            screenshots = screenshots,
            selectedScreenshot = selectedScreenshot,
            onOpen = { file ->
                onOpen(file)
                showGalleryPane = false
            },
            onShare = onShare,
            onDelete = onDelete,
            onDismiss = { showGalleryPane = false }
        )
    }

    Surface(
        modifier = Modifier.testTag("gallery_section"),
        color = Color.Transparent,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Gallery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(modifier = Modifier.testTag("gallery_open_pane_button"), enabled = screenshots.isNotEmpty(), onClick = { showGalleryPane = true }) { Text("View all") }
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
                        .background(Color.Transparent, RectangleShape)
                )
            }
            if (selectedScreenshot != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.testTag("selected_share_button"), onClick = { onShare(selectedScreenshot) }) { Text("Share") }
                    Button(modifier = Modifier.testTag("selected_delete_button"), onClick = { onDelete(selectedScreenshot) }) { Text("Delete") }
                }
            }
            if (screenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.Transparent, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Capture photo to add screenshots here", color = MutedText, textAlign = TextAlign.Center)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recent captures", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("gallery_strip"),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(screenshots, key = { it.absolutePath }) { file ->
                            GalleryStripItem(
                                file = file,
                                selected = file == selectedScreenshot,
                                onOpen = onOpen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPaneDialog(
    screenshots: List<File>,
    selectedScreenshot: File?,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("gallery_pane_dialog"),
        title = { Text("All captures") },
        text = {
            if (screenshots.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved captures yet.", color = MutedText, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .testTag("gallery_pane_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(screenshots, key = { it.absolutePath }) { file ->
                        GalleryPaneItem(
                            file = file,
                            selected = file == selectedScreenshot,
                            onOpen = onOpen,
                            onShare = onShare,
                            onDelete = onDelete
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.testTag("gallery_pane_done_button"), onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun GalleryStripItem(
    file: File,
    selected: Boolean,
    onOpen: (File) -> Unit
) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { loadCapturePreview(file) }
    Surface(
        modifier = Modifier
            .width(154.dp)
            .testTag("gallery_item"),
        color = Color.Transparent,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_item_preview")
                    .height(86.dp)
                    .clickable { onOpen(file) }
                    .background(Color.Black, RectangleShape),
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
            Text(formatTimestamp(file.lastModified()), style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1)
        }
    }
}

@Composable
private fun GalleryPaneItem(
    file: File,
    selected: Boolean,
    onOpen: (File) -> Unit,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit
) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { loadCapturePreview(file) }
    Surface(
        modifier = Modifier.testTag("gallery_pane_item"),
        color = Color.Transparent,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(Color.Black, RectangleShape)
                    .clickable { onOpen(file) },
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
                    Text("No preview", style = MaterialTheme.typography.labelSmall, color = MutedText, textAlign = TextAlign.Center)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(file.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${formatFileSize(file.length())} • ${formatTimestamp(file.lastModified())}", style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(modifier = Modifier.testTag("gallery_pane_item_open_button"), onClick = { onOpen(file) }) { Text("Open") }
                    TextButton(modifier = Modifier.testTag("gallery_pane_item_share_button"), onClick = { onShare(file) }) { Text("Share") }
                    TextButton(modifier = Modifier.testTag("gallery_pane_item_delete_button"), onClick = { onDelete(file) }) { Text("🗑️") }
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
private fun BottomMediaBar(
    fastCaptureStatus: FastCaptureUiStatus,
    onConnectClick: () -> Unit,
    onFastConnectClick: () -> Unit,
    onRemoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusClick = if (fastCaptureStatus.retryAvailable) onFastConnectClick else onConnectClick
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bottom_status_bar")
                .height(34.dp)
                .clickable(onClick = statusClick)
                .background(if (fastCaptureStatus.ready) SuccessColor else AccentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(fastCaptureStatus.title, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .background(Color.Transparent)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(modifier = Modifier.testTag("bottom_connect_button"), onClick = onConnectClick) { Text("Connect") }
            Button(
                modifier = Modifier.testTag("bottom_remote_button"),
                onClick = onRemoteClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                shape = RectangleShape,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
            ) { Text("Remote", fontWeight = FontWeight.Bold) }
            TextButton(
                modifier = Modifier.testTag(if (fastCaptureStatus.retryAvailable) "bottom_fast_retry_button" else "bottom_tv_button"),
                onClick = statusClick
            ) {
                Text(if (fastCaptureStatus.retryAvailable) "Retry fast" else "TV")
            }
        }
    }
}

@Composable
private fun rememberSearchingLabel(isSearching: Boolean): String {
    var dotCount by remember { mutableStateOf(1) }
    LaunchedEffect(isSearching) {
        dotCount = 1
        while (isSearching) {
            delay(450)
            dotCount = if (dotCount >= 3) 1 else dotCount + 1
        }
    }
    return if (isSearching) "Searching${".".repeat(dotCount)}" else "Refresh"
}

@Composable
private fun SearchingTvIcon(isSearching: Boolean) {
    val haloProgress = if (isSearching) {
        val transition = rememberInfiniteTransition(label = "connect-search")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_200),
                repeatMode = RepeatMode.Restart
            ),
            label = "connect-search-halo"
        )
        progress
    } else {
        0f
    }
    val haloSize = if (isSearching) 48.dp + (24 * haloProgress).dp else 54.dp
    val haloAlpha = if (isSearching) (0.36f * (1f - haloProgress)).coerceIn(0.08f, 0.36f) else 0.16f

    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(haloSize)
                .alpha(haloAlpha)
                .testTag("connect_tv_halo"),
            shape = RectangleShape,
            color = AccentColor,
            content = {}
        )
        Text("▭", fontSize = 44.sp, color = AccentColor, modifier = Modifier.testTag("connect_tv_icon"))
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
    currentWifiName: String,
    debugModeEnabled: Boolean,
    onOpenWifiSettings: () -> Unit,
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
                val searchingLabel = rememberSearchingLabel(isDiscovering)
                Surface(
                    modifier = Modifier,
                    color = Color.Transparent,
                    shape = RectangleShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SearchingTvIcon(isSearching = isDiscovering)
                        Text("Choose a device on this network", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        TextButton(modifier = Modifier.testTag("wifi_settings_button"), onClick = onOpenWifiSettings) {
                            Text("Current network: $currentWifiName")
                        }
                    }
                }

                selectedDevice?.let { current ->
                    Text("Selected: ${current.name.ifBlank { "TCL TV" }} — ${current.ip}", fontWeight = FontWeight.Bold)
                    Text("Last verified: ${formatTimestamp(current.lastVerifiedAtMillis)}", style = MaterialTheme.typography.bodySmall)
                } ?: Text("No TV selected.", color = AccentColor, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(modifier = Modifier.width(128.dp).testTag("discover_button"), enabled = !isDiscovering, onClick = onDiscover) {
                        Text(
                            text = if (isDiscovering) searchingLabel else "Refresh",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(discoveryStatus, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }

                if (discoveredDevices.isEmpty() && !isDiscovering) {
                    Text("No devices listed yet. Refresh after checking the local network.", style = MaterialTheme.typography.bodySmall)
                }

                discoveredDevices.forEach { device ->
                    val isSelected = selectedDevice?.ip == device.ip
                    Surface(
                        onClick = { onUseDevice(device) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discovered_device_card"),
                        color = Color.Transparent,
                        shape = RectangleShape,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(deviceTypeIcon(device.deviceType), fontSize = 28.sp, modifier = Modifier.testTag("device_type_icon"))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${device.name.ifBlank { "TCL TV" }} — ${device.ip}", fontWeight = FontWeight.Bold)
                                Text(
                                    listOfNotNull(
                                        device.deviceType.ifBlank { "TV" },
                                        if (isSelected) "Connected" else "Tap to connect",
                                        device.algorithmType?.let { "Verified" }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(if (isSelected) "Connected" else "Connect", color = AccentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (debugModeEnabled) {
                    Text("Debug connection fallback", style = MaterialTheme.typography.titleMedium)
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
                    selectedDevice?.handshake?.let { handshake ->
                        Text(handshake, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    )
}

@Composable
private fun VideoCaptureDialog(
    state: VideoCaptureUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTrimStartChange: (String) -> Unit,
    onTrimEndChange: (String) -> Unit,
    onActiveTrimHandleChange: (VideoTrimHandle) -> Unit,
    onAudioToggle: () -> Unit,
    onSaveEdited: () -> Unit,
    onKeepOriginal: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("video_capture_dialog"),
        title = { Text("Capture Video") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .background(Color.Black, RectangleShape)
                        .border(1.dp, AccentColor.copy(alpha = 0.45f), RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val displayFrame = state.editorPreviewFrame()
                    displayFrame?.let { frame ->
                        Image(
                            bitmap = frame.asImageBitmap(),
                            contentDescription = "Video recording preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().testTag("video_preview_frame")
                        )
                    }
                    if (state.phase != VideoCapturePhase.RECORDING) {
                        Text("▶", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .testTag("video_audio_toggle_button"),
                        onClick = onAudioToggle,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(if (state.includeAudio) "🔊" else "🔇", color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.62f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${formatDurationMs(state.elapsedMs)} • ${state.frameCount} frame(s)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Text(state.status, color = MutedText, style = MaterialTheme.typography.bodySmall)
                Text(state.audioStatus, color = MutedText, style = MaterialTheme.typography.bodySmall)
                if (state.timelineFrames.isNotEmpty()) {
                    if (state.phase == VideoCapturePhase.REVIEW || state.phase == VideoCapturePhase.SAVING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                modifier = Modifier.testTag("video_trim_start_handle"),
                                onClick = { onActiveTrimHandleChange(VideoTrimHandle.START) },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (state.activeTrimHandle == VideoTrimHandle.START) AccentColor else AppBackground
                                )
                            ) { Text("Start") }
                            TextButton(
                                modifier = Modifier.testTag("video_trim_end_handle"),
                                onClick = { onActiveTrimHandleChange(VideoTrimHandle.END) },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (state.activeTrimHandle == VideoTrimHandle.END) AccentColor else AppBackground
                                )
                            ) { Text("End") }
                            Text(state.activeTrimLabel(), color = MutedText, style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            modifier = Modifier.testTag("video_trim_seek_slider"),
                            value = state.activeTrimSeconds(),
                            onValueChange = { value ->
                                val text = "%.1f".format(Locale.US, value)
                                if (state.activeTrimHandle == VideoTrimHandle.START) onTrimStartChange(text) else onTrimEndChange(text)
                            },
                            valueRange = 0f..state.durationSeconds().coerceAtLeast(0.1f)
                        )
                    }
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .testTag("video_timeline_strip")
                            .border(2.dp, Color.White, RectangleShape),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        items(state.timelineFrames) { frame ->
                            Image(
                                bitmap = frame.asImageBitmap(),
                                contentDescription = "Video timeline frame",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(80.dp).fillMaxHeight()
                            )
                        }
                    }
                }
                if (state.phase == VideoCapturePhase.REVIEW || state.phase == VideoCapturePhase.SAVING) {
                    Text("Review and edit", fontWeight = FontWeight.Bold)
                    Text(
                        state.file?.let { "${it.name} • ${formatFileSize(it.length())}" }.orEmpty(),
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.trimStartText,
                            onValueChange = onTrimStartChange,
                            label = { Text("Start s") },
                            enabled = state.phase != VideoCapturePhase.SAVING,
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("video_trim_start_field")
                        )
                        OutlinedTextField(
                            value = state.trimEndText,
                            onValueChange = onTrimEndChange,
                            label = { Text("End s") },
                            enabled = state.phase != VideoCapturePhase.SAVING,
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("video_trim_end_field")
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                VideoCapturePhase.READY -> Button(modifier = Modifier.testTag("video_record_start_button"), onClick = onStart) { Text("Record") }
                VideoCapturePhase.RECORDING -> Button(modifier = Modifier.testTag("video_record_stop_button"), onClick = onStop) { Text("Stop") }
                VideoCapturePhase.REVIEW -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier = Modifier.testTag("video_keep_original_button"), onClick = onKeepOriginal) { Text("Keep") }
                    Button(modifier = Modifier.testTag("video_save_edit_button"), onClick = onSaveEdited) { Text("Save edit") }
                }
                VideoCapturePhase.SAVING -> TextButton(enabled = false, onClick = {}) { Text("Saving") }
            }
        },
        dismissButton = {
            when (state.phase) {
                VideoCapturePhase.RECORDING, VideoCapturePhase.REVIEW -> {
                    TextButton(modifier = Modifier.testTag("video_record_cancel_button"), onClick = onDiscard) { Text("Discard") }
                }
                VideoCapturePhase.READY -> TextButton(modifier = Modifier.testTag("video_dialog_close_button"), onClick = onDismiss) { Text("Close") }
                VideoCapturePhase.SAVING -> TextButton(enabled = false, onClick = {}) { Text("Discard") }
            }
        }
    )
}

@Composable
private fun RemoteControlDialog(
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TCL_REMOTE_POWER_ROW.forEach { spec ->
                        RemoteButton(spec, onSendRemoteButton)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        RemoteButton(tclRemoteButton("Up"), onSendRemoteButton)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        TCL_REMOTE_DPAD_CENTER_ROW.forEach { spec ->
                            RemoteButton(spec, onSendRemoteButton)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        RemoteButton(tclRemoteButton("Down"), onSendRemoteButton)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TCL_REMOTE_VOLUME_ROW.forEach { spec ->
                        RemoteButton(spec, onSendRemoteButton)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    TCL_REMOTE_CHANNEL_ROW.forEach { spec ->
                        RemoteButton(spec, onSendRemoteButton)
                    }
                }
                Text(remoteStatus, style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
        }
    )
}

@Composable
private fun RemoteButton(
    spec: TclRemoteButtonSpec,
    onClick: (String, Int) -> Unit
) {
    Button(
        enabled = true,
        onClick = { onClick(spec.label, spec.keyCode) },
        modifier = Modifier
            .width(86.dp)
            .height(58.dp)
            .semantics { contentDescription = "${spec.label} remote button" }
            .testTag(spec.testTag),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RemoteButtonColor,
            contentColor = RemoteButtonContentColor
        )
    ) {
        Text(
            spec.displayLabel,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            color = RemoteButtonContentColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

private val AppBackground = Color(0xFF0B0F18)
private val PanelColor = Color(0xFF171D2A)
private val RemoteButtonColor = Color(0xFFFFF3B0)
private val RemoteButtonContentColor = Color(0xFF05070D)
private val MutedText = Color(0xFFA9B0C2)
private val AccentColor = Color(0xFFE6426E)
private val SuccessColor = Color(0xFF2DAF7D)

private data class Tcl6553ScreenshotResult(
    val bitmap: Bitmap,
    val byteCount: Int,
    val file: File,
    val url: String,
    val log: String,
    val timingSummary: String = ""
)

private data class Tcl6553ScreenshotBytes(
    val bytes: ByteArray,
    val url: String,
    val log: String,
    val timingSummary: String = ""
)

private data class VideoCaptureResult(
    val file: File,
    val durationMs: Long,
    val frameCount: Int
)

private enum class VideoCapturePhase {
    READY,
    RECORDING,
    REVIEW,
    SAVING
}

private enum class VideoTrimHandle {
    START,
    END
}

private data class VideoCaptureUiState(
    val phase: VideoCapturePhase = VideoCapturePhase.READY,
    val startedAtMillis: Long = 0L,
    val elapsedMs: Long = 0L,
    val frameCount: Int = 0,
    val status: String = "Ready to record TV video.",
    val file: File? = null,
    val durationMs: Long = 0L,
    val trimStartText: String = "0.0",
    val trimEndText: String = "0.0",
    val includeAudio: Boolean = false,
    val audioFile: File? = null,
    val audioStatus: String = "TV audio stream unavailable",
    val activeTrimHandle: VideoTrimHandle = VideoTrimHandle.END,
    val previewFrame: Bitmap? = null,
    val timelineFrames: List<Bitmap> = emptyList()
)

private fun VideoCaptureUiState.editorPreviewFrame(): Bitmap? {
    if (timelineFrames.isEmpty()) return previewFrame
    if (phase != VideoCapturePhase.REVIEW && phase != VideoCapturePhase.SAVING) return previewFrame ?: timelineFrames.lastOrNull()
    val duration = durationSeconds().coerceAtLeast(0.1f)
    val fraction = (activeTrimSeconds() / duration).coerceIn(0f, 1f)
    val index = (fraction * (timelineFrames.lastIndex.coerceAtLeast(0))).toInt().coerceIn(timelineFrames.indices)
    return timelineFrames[index]
}

private fun VideoCaptureUiState.durationSeconds(): Float = (durationMs.coerceAtLeast(elapsedMs) / 1_000f).coerceAtLeast(0.1f)

private fun VideoCaptureUiState.activeTrimSeconds(): Float = when (activeTrimHandle) {
    VideoTrimHandle.START -> trimStartText.toFloatOrNull() ?: 0f
    VideoTrimHandle.END -> trimEndText.toFloatOrNull() ?: durationSeconds()
}.coerceIn(0f, durationSeconds())

private fun VideoCaptureUiState.activeTrimLabel(): String = when (activeTrimHandle) {
    VideoTrimHandle.START -> "Previewing start at ${trimStartText.ifBlank { "0.0" }}s"
    VideoTrimHandle.END -> "Previewing end at ${trimEndText.ifBlank { formatSecondsText(durationMs) }}s"
}

private fun VideoCaptureUiState.withPreviewFrame(frame: Bitmap): VideoCaptureUiState {
    val preview = frame.copy(Bitmap.Config.ARGB_8888, false)
    val nextTimeline = if (timelineFrames.size < 12 || frameCount % 3 == 0) {
        (timelineFrames + Bitmap.createScaledBitmap(frame, 96, 54, true)).takeLast(16)
    } else {
        timelineFrames
    }
    return copy(previewFrame = preview, timelineFrames = nextTimeline)
}

private data class TclCaptureTimingSegment(
    val label: String,
    val durationMs: Long
)

private class TclCaptureTrace(
    private val startedAt: Long = System.currentTimeMillis()
) {
    private var lastMarkAt = startedAt
    private val segments = mutableListOf<TclCaptureTimingSegment>()

    fun mark(label: String) {
        val now = System.currentTimeMillis()
        val duration = (now - lastMarkAt).coerceAtLeast(0L)
        if (duration > 0L || segments.isNotEmpty()) {
            segments += TclCaptureTimingSegment(label, duration)
        }
        lastMarkAt = now
    }

    fun totalMs(): Long = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)

    fun summary(): String = formatCaptureTimingSummary(segments, totalMs())
}

private data class Tcl6553SessionConfig(
    val tvIp: String,
    val port: Int,
    val phoneName: String,
    val uuid: String,
    val phoneImei: String
)

private enum class Tcl6553SessionState {
    DISCONNECTED,
    WARMING,
    READY,
    RECONNECTING,
    FALLBACK_ONLY
}

private data class FastCaptureUiStatus(
    val title: String,
    val detail: String,
    val captureSubtitle: String,
    val ready: Boolean,
    val retryAvailable: Boolean
)

private fun fastCaptureUiStatus(connected: Boolean, state: Tcl6553SessionState): FastCaptureUiStatus = when {
    !connected -> FastCaptureUiStatus(
        title = "Connect TV for fast capture",
        detail = "Connect to your TV before capturing.",
        captureSubtitle = "Connect TV",
        ready = false,
        retryAvailable = false
    )
    state == Tcl6553SessionState.READY -> FastCaptureUiStatus(
        title = "TV fully connected — fast capture ready",
        detail = "The TV is ready for the fastest screenshots.",
        captureSubtitle = "Fast ready",
        ready = true,
        retryAvailable = false
    )
    state == Tcl6553SessionState.WARMING -> FastCaptureUiStatus(
        title = "TV connected — preparing fast capture",
        detail = "The app is getting the TV ready. Capture can still work.",
        captureSubtitle = "Preparing",
        ready = false,
        retryAvailable = false
    )
    state == Tcl6553SessionState.RECONNECTING -> FastCaptureUiStatus(
        title = "TV connected — fast capture reconnecting",
        detail = "The app is reconnecting. Capture can still work.",
        captureSubtitle = "Reconnecting",
        ready = false,
        retryAvailable = true
    )
    else -> FastCaptureUiStatus(
        title = "TV connected — fallback capture only",
        detail = "Fast capture is not ready. Capture can still work.",
        captureSubtitle = "Fallback",
        ready = false,
        retryAvailable = true
    )
}

private fun testTclDevice(): TclDiscoveryDevice = TclDiscoveryDevice(
    ip = "192.0.2.10",
    name = "Test Living Room TV",
    deviceType = "TV",
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
    val deviceType: String = "TV",
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
    val directory = screenshotDirectory(context)
    return directory.listFiles()
        ?.filter { file -> file.isFile && file.extension.lowercase() in setOf("jpg", "jpeg", "png", "bin", "mp4") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()
}

private fun shareScreenshot(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = captureMimeType(file)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share capture"))
}

private suspend fun exportVideoToMovies(context: Context, file: File): Uri = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val nowMillis = System.currentTimeMillis()
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
        put(MediaStore.Video.Media.TITLE, file.nameWithoutExtension)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.DATE_TAKEN, nowMillis)
        put(MediaStore.Video.Media.DATE_ADDED, nowMillis / 1_000L)
        put(MediaStore.Video.Media.DATE_MODIFIED, nowMillis / 1_000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/TCL TV Screenshot")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create Movies export entry")
    runCatching {
        resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { input -> input.copyTo(output) } }
            ?: error("Could not open Movies export stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completeValues = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            resolver.update(uri, completeValues, null, null)
        }
    }.onFailure { error ->
        resolver.delete(uri, null, null)
        throw error
    }
    uri
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

private fun captureMimeType(file: File): String = when (file.extension.lowercase()) {
    "mp4" -> "video/mp4"
    else -> imageMimeType(file)
}

private fun loadCapturePreview(file: File): Bitmap? {
    if (file.extension.lowercase() != "mp4") return BitmapFactory.decodeFile(file.absolutePath)
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun screenshotDirectory(context: Context): File = File(context.filesDir, "Screenshots")

private fun videoDraftDirectory(context: Context): File = File(context.cacheDir, "VideoDrafts")

private fun captureFilename(nowMillis: Long, extension: String): String {
    val safeExtension = extension.lowercase().ifBlank { "bin" }
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    return "tcl-tv-screenshot_$timestamp.$safeExtension"
}

private fun uniqueCaptureFile(directory: File, nowMillis: Long, extension: String): File {
    val first = File(directory, captureFilename(nowMillis, extension))
    if (!first.exists()) return first

    val safeExtension = extension.lowercase().ifBlank { "bin" }
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    var suffix = 2
    while (true) {
        val candidate = File(directory, "tcl-tv-screenshot_${timestamp}_$suffix.$safeExtension")
        if (!candidate.exists()) return candidate
        suffix++
    }
}

private fun videoCaptureFilename(nowMillis: Long, suffix: String? = null): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    val suffixPart = suffix?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    return "tcl-tv-video_${timestamp}$suffixPart.mp4"
}

private fun audioCaptureFilename(nowMillis: Long, suffix: String? = null): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    val suffixPart = suffix?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    return "tcl-tv-audio_${timestamp}$suffixPart.m4a"
}

private fun uniqueAudioCaptureFile(directory: File, nowMillis: Long, suffix: String? = null): File {
    directory.mkdirs()
    val first = File(directory, audioCaptureFilename(nowMillis, suffix))
    if (!first.exists()) return first
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    val suffixPart = suffix?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    var index = 2
    while (true) {
        val candidate = File(directory, "tcl-tv-audio_${timestamp}${suffixPart}_$index.m4a")
        if (!candidate.exists()) return candidate
        index++
    }
}

private fun moveCaptureFile(source: File, destination: File): File {
    destination.parentFile?.mkdirs()
    if (source.renameTo(destination)) return destination
    source.inputStream().use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
    source.delete()
    return destination
}

private fun uniqueVideoCaptureFile(directory: File, nowMillis: Long, suffix: String? = null): File {
    directory.mkdirs()
    val first = File(directory, videoCaptureFilename(nowMillis, suffix))
    if (!first.exists()) return first
    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS_Z", Locale.US).format(Date(nowMillis))
    val suffixPart = suffix?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()
    var index = 2
    while (true) {
        val candidate = File(directory, "tcl-tv-video_${timestamp}${suffixPart}_$index.mp4")
        if (!candidate.exists()) return candidate
        index++
    }
}

private fun formatSecondsText(durationMs: Long): String = "%.1f".format(Locale.US, durationMs / 1_000.0)

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0L) return "unknown"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(millis))
}

private fun currentWifiDisplayName(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    return wifiManager?.connectionInfo?.ssid
        ?.removeSurrounding("\"")
        ?.takeUnless { it.isBlank() || it == "<unknown ssid>" }
        ?: "Check network"
}

private fun deviceTypeIcon(deviceType: String): String = when (deviceType.trim().uppercase()) {
    "TV", "TCL TV" -> "▭"
    "SPEAKER", "AUDIO" -> "♪"
    "PHONE" -> "▯"
    else -> "•"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatDurationMs(durationMs: Long): String = when {
    durationMs >= 1_000L -> "%.1fs".format(durationMs / 1_000.0)
    else -> "${durationMs}ms"
}

private fun formatCaptureTimingSummary(segments: List<TclCaptureTimingSegment>, totalMs: Long): String {
    if (segments.isEmpty()) return "Done in ${formatDurationMs(totalMs)}."
    val maxDuration = segments.maxOf { it.durationMs }.coerceAtLeast(1L)
    val rows = segments.joinToString("\n") { segment ->
        val barLength = ((segment.durationMs * 12L) / maxDuration).coerceIn(1L, 12L).toInt()
        val bar = "█".repeat(barLength)
        "${segment.label}: ${formatDurationMs(segment.durationMs)} $bar"
    }
    return "Done in ${formatDurationMs(totalMs)}\n$rows"
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
                        deviceType = parsed.senderType.ifBlank { "TV" },
                        source = "udp:${parsed.commandNo}",
                        mac = mac
                    )
                }
            } catch (_: SocketTimeoutException) {
                // Keep looking until time runs out.
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

private data class TvStreamProbeTarget(
    val port: Int,
    val label: String,
    val paths: List<String> = emptyList(),
    val tls: Boolean = false
)

private data class TvStreamProbeResult(
    val port: Int,
    val label: String,
    val reachable: Boolean,
    val evidence: String
)

private fun tvStreamProbeTargets(): List<TvStreamProbeTarget> = listOf(
    TvStreamProbeTarget(5555, "ADB/debug service"),
    TvStreamProbeTarget(554, "RTSP", listOf("/", "/live", "/stream", "/video")),
    TvStreamProbeTarget(6466, "TCL private SSL"),
    TvStreamProbeTarget(6467, "TCL private SSL"),
    TvStreamProbeTarget(6553, "TCL control/screenshot"),
    TvStreamProbeTarget(8008, "Cast/DIAL HTTP", listOf("/setup/eureka_info", "/ssdp/device-desc.xml", "/apps", "/")),
    TvStreamProbeTarget(8009, "Cast V2 TLS", tls = true),
    TvStreamProbeTarget(8443, "HTTPS", listOf("/setup/eureka_info", "/ssdp/device-desc.xml", "/"), tls = true),
    TvStreamProbeTarget(8554, "RTSP alternate", listOf("/", "/live", "/stream", "/video")),
    TvStreamProbeTarget(9000, "private TLS", tls = true),
    TvStreamProbeTarget(1900, "UPnP/SSDP TCP check"),
    TvStreamProbeTarget(38463, "ephemeral HTTP", listOf("/", "/screenShot/tohsneercs.PNG")),
    TvStreamProbeTarget(40647, "ephemeral HTTP", listOf("/", "/screenShot/tohsneercs.PNG"))
)

private suspend fun probeTvStreamServices(tvIp: String): List<TvStreamProbeResult> = withContext(Dispatchers.IO) {
    tvStreamProbeTargets().map { target ->
        async { probeTvStreamTarget(tvIp, target) }
    }.awaitAll()
}

private fun probeTvStreamTarget(tvIp: String, target: TvStreamProbeTarget): TvStreamProbeResult {
    val reachable = runCatching {
        Socket().use { socket ->
            socket.soTimeout = 1_000
            socket.connect(InetSocketAddress(tvIp, target.port), 1_000)
        }
    }.isSuccess
    if (!reachable) return TvStreamProbeResult(target.port, target.label, false, "closed")

    val httpEvidence = target.paths.firstNotNullOfOrNull { path ->
        probeHttpPath(tvIp, target.port, path)?.let { "$path $it" }
    }
    val evidence = when {
        httpEvidence != null -> httpEvidence
        target.tls -> "open TLS; no plain stream descriptor found"
        target.label.contains("RTSP") -> "open; RTSP stream path not confirmed"
        else -> "open"
    }
    return TvStreamProbeResult(target.port, target.label, true, evidence)
}

private fun probeHttpPath(tvIp: String, port: Int, path: String): String? = runCatching {
    val connection = (URL("http://$tvIp:$port$path").openConnection() as HttpURLConnection).apply {
        connectTimeout = 800
        readTimeout = 800
        requestMethod = "GET"
        useCaches = false
    }
    try {
        val code = connection.responseCode
        val contentType = connection.contentType.orEmpty().ifBlank { "unknown" }
        val sample = runCatching {
            connection.inputStream.use { stream ->
                stream.readBytesBounded(512).decodeToString().lineSequence().firstOrNull().orEmpty().take(80)
            }
        }.getOrDefault("")
        "HTTP $code $contentType ${sample}".trim()
    } finally {
        connection.disconnect()
    }
}.getOrNull()

private fun formatStreamProbeSummary(results: List<TvStreamProbeResult>): String {
    val open = results.filter { it.reachable }
    if (open.isEmpty()) return "No candidate TV stream services responded."
    val streamLike = open.filter { result ->
        result.evidence.contains("m3u8", ignoreCase = true) ||
            result.evidence.contains("rtsp", ignoreCase = true) ||
            result.evidence.contains("video", ignoreCase = true) ||
            result.evidence.contains("audio", ignoreCase = true) ||
            result.evidence.contains("Cast", ignoreCase = true)
    }
    val rows = open.joinToString("\n") { result ->
        "${result.port} ${result.label}: ${result.evidence.take(110)}"
    }
    val header = if (streamLike.isEmpty()) {
        "Open services found, but no confirmed AV stream endpoint yet."
    } else {
        "Possible stream-related services found."
    }
    return "$header\n$rows"
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

private class Tcl6553SessionManager {
    private val stateLock = Any()
    private val _state = MutableStateFlow(Tcl6553SessionState.DISCONNECTED)
    val state: StateFlow<Tcl6553SessionState> = _state

    private var config: Tcl6553SessionConfig? = null
    private var scope: CoroutineScope? = null
    private var session: Tcl6553WarmSession? = null
    private var warmJob: Job? = null
    private var keepAliveJob: Job? = null
    private var warmAttemptId = 0L

    fun configure(scope: CoroutineScope, config: Tcl6553SessionConfig) {
        synchronized(stateLock) {
            this.scope = scope
            if (this.config == config && (session != null || warmJob?.isActive == true)) return
            closeLocked(updateState = false)
            this.config = config
            startWarmLocked(scope, config, Tcl6553SessionState.WARMING)
        }
    }

    private fun startWarmLocked(
        scope: CoroutineScope,
        config: Tcl6553SessionConfig,
        startingState: Tcl6553SessionState
    ) {
        warmJob?.cancel()
        warmAttemptId += 1
        val attemptId = warmAttemptId
        _state.value = startingState
        warmJob = scope.launch(Dispatchers.IO) {
            val log = StringBuilder()
            val opened = runCatching { openTcl6553Session(config, log) }
                .onFailure { error -> Log.i(LOG_TAG, "warm session failed: ${error.message ?: error::class.java.simpleName}") }
                .getOrNull()
            synchronized(stateLock) {
                if (attemptId != warmAttemptId || this@Tcl6553SessionManager.config != config) {
                    opened?.close()
                } else if (opened != null) {
                    session = opened
                    _state.value = Tcl6553SessionState.READY
                    startKeepAliveLocked(scope)
                    Log.i(LOG_TAG, "${System.currentTimeMillis()} warm session ready")
                } else if (session == null) {
                    _state.value = Tcl6553SessionState.FALLBACK_ONLY
                    scheduleWarmRetryLocked(scope, config)
                }
            }
        }
    }

    suspend fun captureScreenshotUrl(log: StringBuilder): String? = withContext(Dispatchers.IO) {
        val opened = synchronized(stateLock) { session }
        if (opened == null) {
            log.protocolLog("warm session unavailable; using cold capture path")
            return@withContext null
        }
        runCatching { opened.captureScreenshot(log) }
            .onFailure { error ->
                log.protocolLog("warm session capture failed: ${error.message ?: error::class.java.simpleName}")
                dropSession(opened)
            }
            .getOrNull()
    }

    suspend fun sendRemoteKey(keyCode: Int, log: StringBuilder): Boolean = withContext(Dispatchers.IO) {
        val opened = synchronized(stateLock) { session }
        if (opened == null) {
            log.protocolLog("warm session unavailable; using cold remote path")
            return@withContext false
        }
        runCatching { opened.sendRemoteKey(keyCode, log) }
            .onFailure { error ->
                log.protocolLog("warm session remote failed: ${error.message ?: error::class.java.simpleName}")
                dropSession(opened)
            }
            .isSuccess
    }

    fun reconnect() {
        synchronized(stateLock) {
            warmJob?.cancel()
            warmJob = null
            keepAliveJob?.cancel()
            keepAliveJob = null
            val opened = session
            if (opened != null) {
                session = null
                opened.close()
            }
            val nextConfig = config
            val nextScope = scope
            if (nextConfig != null && nextScope != null) {
                startWarmLocked(nextScope, nextConfig, Tcl6553SessionState.RECONNECTING)
            } else {
                _state.value = Tcl6553SessionState.FALLBACK_ONLY
            }
        }
    }

    fun clear() {
        synchronized(stateLock) {
            config = null
            scope = null
            closeLocked(updateState = true)
        }
    }

    fun close() = clear()

    private fun scheduleWarmRetryLocked(scope: CoroutineScope, config: Tcl6553SessionConfig) {
        warmJob?.cancel()
        warmAttemptId += 1
        val attemptId = warmAttemptId
        warmJob = scope.launch(Dispatchers.IO) {
            delay(TCL_SESSION_RETRY_DELAY_MS)
            synchronized(stateLock) {
                if (attemptId == warmAttemptId && this@Tcl6553SessionManager.config == config && session == null) {
                    startWarmLocked(scope, config, Tcl6553SessionState.RECONNECTING)
                }
            }
        }
    }

    private fun startKeepAliveLocked(scope: CoroutineScope) {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(TCL_SESSION_HEARTBEAT_INTERVAL_MS)
                val opened = synchronized(stateLock) { session } ?: continue
                val heartbeatLog = StringBuilder()
                runCatching { opened.heartbeat(heartbeatLog) }
                    .onFailure { dropSession(opened) }
            }
        }
    }

    private fun dropSession(opened: Tcl6553WarmSession) {
        synchronized(stateLock) {
            if (session === opened) {
                session = null
                opened.close()
                val nextConfig = config
                val nextScope = scope
                if (nextConfig != null && nextScope != null) {
                    startWarmLocked(nextScope, nextConfig, Tcl6553SessionState.RECONNECTING)
                } else {
                    _state.value = Tcl6553SessionState.FALLBACK_ONLY
                }
            }
        }
    }

    private fun closeLocked(updateState: Boolean) {
        warmAttemptId += 1
        warmJob?.cancel()
        warmJob = null
        keepAliveJob?.cancel()
        keepAliveJob = null
        session?.close()
        session = null
        if (updateState) {
            _state.value = Tcl6553SessionState.DISCONNECTED
        }
    }
}

private class Tcl6553WarmSession(
    private val socket: Socket,
    private val input: DataInputStream,
    private val output: DataOutputStream,
    private val encrypted: Boolean
) {
    private val ioLock = Any()

    fun captureScreenshot(log: StringBuilder): String = synchronized(ioLock) {
        val shot = "225>>"
        log.protocolLog("send warm screenshot $shot encrypted=$encrypted")
        writeTclText(output, shot, encrypted = encrypted)
        val response = readTclScreenshotResponse(input, encrypted, log)
        val fields = response.split(">>")
        require(fields.size >= 3 && fields[0] == "225" && fields[1] == "0") {
            "Unexpected warm screenshot response: $response"
        }
        fields[2]
    }

    fun heartbeat(log: StringBuilder) = synchronized(ioLock) {
        writeTclText(output, "150>>", encrypted = encrypted)
        val heartbeatAck = readTclResponse(input, encrypted, log) { response ->
            response.startsWith("150>>") || response.startsWith("225>>")
        }
        require(heartbeatAck == "150>>YES") { "Unexpected heartbeat response: $heartbeatAck" }
    }

    fun sendRemoteKey(keyCode: Int, log: StringBuilder) = synchronized(ioLock) {
        val command = tclRemoteKeyCommand(keyCode)
        log.protocolLog("send warm remote $command encrypted=$encrypted")
        writeTclText(output, command, encrypted = encrypted)
    }

    fun close() {
        runCatching { socket.close() }
    }
}

private fun openTcl6553Session(config: Tcl6553SessionConfig, log: StringBuilder): Tcl6553WarmSession {
    val socket = Socket()
    try {
        socket.soTimeout = SOCKET_READ_TIMEOUT_MS
        socket.tcpNoDelay = true
        log.protocolLog("warm socket connect start")
        socket.connect(InetSocketAddress(config.tvIp, config.port), SOCKET_CONNECT_TIMEOUT_MS)
        log.protocolLog("warm socket connect complete")
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())

        val inquiry = "159>>${config.phoneName}>>1>>${config.uuid}>>1"
        log.protocolLog("send warm $inquiry")
        writeTclText(output, inquiry, encrypted = false)
        val handshake = readTclPacket(input)
        log.protocolLog("recv ${handshake.text}")
        require(handshake.text.startsWith("159>>")) { "Unexpected handshake: ${handshake.text}" }
        val fields = handshake.text.split(">>")
        val encrypted = fields.getOrNull(6) == "1"
        log.protocolLog("warm algorithm=${fields.getOrNull(6).orEmpty()} encrypted=$encrypted")

        val prompt = "160>>${config.phoneImei}>>${config.phoneName}"
        log.protocolLog("send warm $prompt encrypted=$encrypted")
        writeTclText(output, prompt, encrypted = encrypted)

        val session = Tcl6553WarmSession(socket, input, output, encrypted)
        session.heartbeat(log)
        return session
    } catch (error: Throwable) {
        runCatching { socket.close() }
        throw error
    }
}

private suspend fun captureTcl6553Screenshot(
    tvIp: String,
    port: Int,
    phoneName: String,
    uuid: String,
    phoneImei: String,
    imageDirectory: File,
    sessionManager: Tcl6553SessionManager? = null
): Tcl6553ScreenshotResult = withContext(Dispatchers.IO) {
    val result = captureTcl6553ScreenshotBytes(tvIp, port, phoneName, uuid, phoneImei, sessionManager)
    val log = StringBuilder(result.log)
    saveTclScreenshotResult(result.url, result.bytes, imageDirectory, log, timingSummary = result.timingSummary)
}

private suspend fun captureTcl6553ScreenshotBytes(
    tvIp: String,
    port: Int,
    phoneName: String,
    uuid: String,
    phoneImei: String,
    sessionManager: Tcl6553SessionManager? = null
): Tcl6553ScreenshotBytes = withContext(Dispatchers.IO) {
    val log = StringBuilder()
    val trace = TclCaptureTrace()
    sessionManager?.captureScreenshotUrl(log)?.let { url ->
        trace.mark("Fast TV command")
        log.protocolLog("download warm url $url")
        runCatching {
            val bytes = downloadScreenshotWithRetries(url, log, trace, allowPortScan = false)
            return@withContext Tcl6553ScreenshotBytes(bytes, url, log.toString().trim(), trace.summary())
        }.onFailure { error ->
            log.protocolLog("warm download failed; retrying with cold capture: ${error.message ?: error::class.java.simpleName}")
            trace.mark("Warm download failed")
        }
    }
    trace.mark("Fast connection check")
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
        trace.mark("Cold setup")

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
        trace.mark("TV screenshot wait")
        val url = shotUrls.lastOrNull() ?: error("No screenshot URL returned. $log")

        val bytes = downloadScreenshotWithRetries(url, log, trace)
        Tcl6553ScreenshotBytes(bytes, url, log.toString().trim(), trace.summary())
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
        writeTclText(output, tclRemoteKeyCommand(keyCode), encrypted = encrypted)
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

private fun saveTclScreenshotResult(
    url: String,
    imageBytes: ByteArray,
    imageDirectory: File,
    log: StringBuilder,
    trace: TclCaptureTrace? = null,
    timingSummary: String? = null
): Tcl6553ScreenshotResult {
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        ?: error("Downloaded ${imageBytes.size} bytes, but Android could not decode them as an image")

    imageDirectory.mkdirs()
    val file = uniqueCaptureFile(
        directory = imageDirectory,
        nowMillis = System.currentTimeMillis(),
        extension = imageExtension(imageBytes)
    )
    file.writeBytes(imageBytes)

    trace?.mark("Decode and save")
    return Tcl6553ScreenshotResult(
        bitmap = bitmap,
        byteCount = imageBytes.size,
        file = file,
        url = url,
        log = log.toString().trim(),
        timingSummary = timingSummary ?: trace?.summary().orEmpty()
    )
}

private suspend fun recordTcl6553Video(
    tvIp: String,
    port: Int,
    phoneName: String,
    uuid: String,
    phoneImei: String,
    outputFile: File,
    sessionManager: Tcl6553SessionManager,
    isActive: () -> Boolean,
    onProgress: (Int, Long) -> Unit,
    onPreviewFrame: (Bitmap) -> Unit
): VideoCaptureResult = withContext(Dispatchers.IO) {
    val startedAt = System.currentTimeMillis()
    var recorder: Mp4FrameRecorder? = null
    var frameCount = 0
    try {
        while (isActive() && System.currentTimeMillis() - startedAt < VIDEO_CAPTURE_MAX_DURATION_MS) {
            val frameStartedAt = System.currentTimeMillis()
            val capture = captureTcl6553ScreenshotBytes(tvIp, port, phoneName, uuid, phoneImei, sessionManager)
            val bitmap = BitmapFactory.decodeByteArray(capture.bytes, 0, capture.bytes.size)
                ?: error("TV frame could not be decoded")
            if (recorder == null) {
                recorder = Mp4FrameRecorder(
                    outputFile = outputFile,
                    width = evenVideoDimension(bitmap.width),
                    height = evenVideoDimension(bitmap.height),
                    frameRate = VIDEO_CAPTURE_FRAME_RATE,
                    bitRate = VIDEO_CAPTURE_BIT_RATE
                )
            }
            recorder?.writeFrame(bitmap)
            frameCount += 1
            onPreviewFrame(bitmap)
            onProgress(frameCount, System.currentTimeMillis() - startedAt)
            val waitMs = VIDEO_CAPTURE_TARGET_FRAME_INTERVAL_MS - (System.currentTimeMillis() - frameStartedAt)
            if (waitMs > 0L) delay(waitMs)
        }
        val finalRecorder = recorder ?: error("No video frames captured")
        val durationMs = finalRecorder.finish()
            .coerceAtLeast(System.currentTimeMillis() - startedAt)
        VideoCaptureResult(outputFile, durationMs, frameCount)
    } catch (error: Throwable) {
        runCatching { recorder?.finish() }
        if (error is CancellationException && frameCount > 0 && outputFile.exists()) {
            VideoCaptureResult(outputFile, videoDurationMs(outputFile).takeIf { it > 0L } ?: (System.currentTimeMillis() - startedAt), frameCount)
        } else {
            throw error
        }
    } finally {
        recorder?.releaseQuietly()
    }
}

private suspend fun recordTestTvVideo(
    outputFile: File,
    onProgress: (Int, Long) -> Unit,
    onPreviewFrame: (Bitmap) -> Unit
): VideoCaptureResult = withContext(Dispatchers.IO) {
    val startedAt = System.currentTimeMillis()
    val recorder = Mp4FrameRecorder(outputFile, width = 320, height = 180, frameRate = VIDEO_CAPTURE_FRAME_RATE, bitRate = 1_200_000)
    try {
        repeat(10) { index ->
            val bitmap = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.rgb(20 + index * 8, 30, 70 + index * 10))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 34f
            }
            canvas.drawText("Test recording ${index + 1}", 28f, 96f, paint)
            recorder.writeFrame(bitmap)
            onPreviewFrame(bitmap)
            onProgress(index + 1, System.currentTimeMillis() - startedAt)
            delay(90)
        }
        VideoCaptureResult(outputFile, recorder.finish(), 10)
    } finally {
        recorder.releaseQuietly()
    }
}

private class Mp4FrameRecorder(
    private val outputFile: File,
    private val width: Int,
    private val height: Int,
    frameRate: Int,
    bitRate: Int
) {
    private val bufferInfo = MediaCodec.BufferInfo()
    private val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val muxer: MediaMuxer
    private val inputSurface: android.view.Surface
    private var muxerStarted = false
    private var trackIndex = -1
    private var finished = false
    private val startedAt = System.currentTimeMillis()

    init {
        outputFile.parentFile?.mkdirs()
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = encoder.createInputSurface()
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        encoder.start()
    }

    fun writeFrame(bitmap: Bitmap) {
        check(!finished) { "Recorder already finished" }
        drainEncoder(endOfStream = false)
        val canvas = inputSurface.lockCanvas(null)
        try {
            drawBitmapLetterboxed(canvas, bitmap, width, height)
        } finally {
            inputSurface.unlockCanvasAndPost(canvas)
        }
    }

    fun finish(): Long {
        if (finished) return videoDurationMs(outputFile)
        finished = true
        encoder.signalEndOfInputStream()
        drainEncoder(endOfStream = true)
        releaseQuietly()
        return videoDurationMs(outputFile).takeIf { it > 0L } ?: (System.currentTimeMillis() - startedAt)
    }

    private fun drainEncoder(endOfStream: Boolean) {
        while (true) {
            val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
            when {
                encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }
                encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!muxerStarted) { "Encoder format changed twice" }
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                encoderStatus >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(encoderStatus) ?: error("Encoder output buffer unavailable")
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size > 0) {
                        check(muxerStarted) { "Muxer has not started" }
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    val end = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    encoder.releaseOutputBuffer(encoderStatus, false)
                    if (end) return
                }
            }
        }
    }

    fun releaseQuietly() {
        runCatching { inputSurface.release() }
        runCatching { encoder.stop() }
        runCatching { encoder.release() }
        runCatching { if (muxerStarted) muxer.stop() }
        runCatching { muxer.release() }
    }
}

private fun drawBitmapLetterboxed(canvas: Canvas, bitmap: Bitmap, width: Int, height: Int) {
    canvas.drawColor(AndroidColor.BLACK)
    val scale = minOf(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
    val drawWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val drawHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    val left = (width - drawWidth) / 2
    val top = (height - drawHeight) / 2
    canvas.drawBitmap(bitmap, null, Rect(left, top, left + drawWidth, top + drawHeight), null)
}

private fun evenVideoDimension(value: Int): Int = value.coerceAtLeast(2).let { if (it % 2 == 0) it else it - 1 }

private fun videoDurationMs(file: File): Long = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } finally {
        retriever.release()
    }
}.getOrDefault(0L)

private fun trimMp4Video(inputFile: File, outputFile: File, startMs: Long, endMs: Long): File =
    muxVideoAndOptionalAudio(inputFile, audioFile = null, outputFile, startMs, endMs, includeAudio = false)

private fun muxVideoAndOptionalAudio(
    videoFile: File,
    audioFile: File?,
    outputFile: File,
    startMs: Long,
    endMs: Long,
    includeAudio: Boolean
): File {
    require(videoFile.exists()) { "Source video is missing" }
    val safeEndMs = endMs.takeIf { it > startMs } ?: videoDurationMs(videoFile).coerceAtLeast(startMs + 250L)
    outputFile.parentFile?.mkdirs()
    val videoExtractor = MediaExtractor()
    val audioExtractor = if (includeAudio && audioFile?.exists() == true) MediaExtractor() else null
    var muxer: MediaMuxer? = null
    try {
        videoExtractor.setDataSource(videoFile.absolutePath)
        audioExtractor?.setDataSource(audioFile!!.absolutePath)
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val videoTrack = selectFirstTrack(videoExtractor, "video/") ?: error("Source video has no video track")
        val muxerVideoTrack = muxer.addTrack(videoExtractor.getTrackFormat(videoTrack))
        val audioTrack = audioExtractor?.let { selectFirstTrack(it, "audio/") }
        val muxerAudioTrack = if (audioExtractor != null && audioTrack != null) {
            muxer.addTrack(audioExtractor.getTrackFormat(audioTrack))
        } else {
            -1
        }
        muxer.start()
        copySelectedTrack(videoExtractor, muxer, muxerVideoTrack, startMs, safeEndMs)
        if (audioExtractor != null && audioTrack != null && muxerAudioTrack >= 0) {
            copySelectedTrack(audioExtractor, muxer, muxerAudioTrack, startMs, safeEndMs)
        }
    } finally {
        videoExtractor.release()
        audioExtractor?.release()
        runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
    }
    return outputFile
}

private fun selectFirstTrack(extractor: MediaExtractor, mimePrefix: String): Int? {
    for (track in 0 until extractor.trackCount) {
        val mime = extractor.getTrackFormat(track).getString(MediaFormat.KEY_MIME).orEmpty()
        if (mime.startsWith(mimePrefix)) {
            extractor.selectTrack(track)
            return track
        }
    }
    return null
}

private fun copySelectedTrack(
    extractor: MediaExtractor,
    muxer: MediaMuxer,
    muxerTrack: Int,
    startMs: Long,
    endMs: Long
) {
    val buffer = java.nio.ByteBuffer.allocate(2 * 1024 * 1024)
    val info = MediaCodec.BufferInfo()
    val startUs = startMs * 1_000L
    val endUs = endMs * 1_000L
    extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
    while (true) {
        val track = extractor.sampleTrackIndex
        if (track < 0) break
        val sampleTime = extractor.sampleTime
        if (sampleTime > endUs) break
        buffer.clear()
        val sampleSize = extractor.readSampleData(buffer, 0)
        if (sampleSize < 0) break
        val sampleFlags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            MediaCodec.BUFFER_FLAG_KEY_FRAME
        } else {
            0
        }
        info.set(0, sampleSize, (sampleTime - startUs).coerceAtLeast(0L), sampleFlags)
        muxer.writeSampleData(muxerTrack, buffer, info)
        extractor.advance()
    }
}

private object ScreenshotPortCache {
    private const val MAX_PORTS_PER_PATH = 8
    private const val PREFERENCES_NAME = "screenshot_port_cache"
    private val portsByTarget = mutableMapOf<String, ArrayDeque<Int>>()
    private var preferences: SharedPreferences? = null

    fun bind(context: Context) {
        synchronized(portsByTarget) {
            if (preferences != null) return
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).also { stored ->
                stored.all.forEach { (key, value) ->
                    val ports = (value as? String)
                        ?.split(',')
                        ?.mapNotNull { it.toIntOrNull() }
                        ?.filter { it in HTTP_PORT_SCAN_FIRST_PORT..HTTP_PORT_SCAN_LAST_PORT }
                        ?.distinct()
                        .orEmpty()
                    if (ports.isNotEmpty()) portsByTarget[key] = ArrayDeque(ports)
                }
            }
        }
    }

    fun remember(host: String, path: String, port: Int) {
        if (port !in HTTP_PORT_SCAN_FIRST_PORT..HTTP_PORT_SCAN_LAST_PORT) return
        val key = cacheKey(host, path)
        synchronized(portsByTarget) {
            val ports = portsByTarget.getOrPut(key) { ArrayDeque() }
            ports.remove(port)
            ports.addFirst(port)
            while (ports.size > MAX_PORTS_PER_PATH) ports.removeLast()
            preferences?.edit()?.putString(key, ports.joinToString(","))?.commit()
        }
    }

    fun portsFor(host: String, path: String): List<Int> = synchronized(portsByTarget) {
        portsByTarget[cacheKey(host, path)]?.toList().orEmpty()
    }

    private fun cacheKey(host: String, path: String): String = "$host|$path"
}

private fun downloadScreenshotWithRetries(
    url: String,
    log: StringBuilder,
    trace: TclCaptureTrace? = null,
    allowPortScan: Boolean = true
): ByteArray {
    findScreenshotOnCachedHttpPort(url, log)?.let { bytes ->
        trace?.mark("Cached download")
        return bytes
    }

    var lastError: Throwable? = null
    repeat(HTTP_DOWNLOAD_ATTEMPTS) { index ->
        try {
            log.protocolLog("download attempt ${index + 1}/$HTTP_DOWNLOAD_ATTEMPTS")
            val bytes = downloadScreenshotUrl(url)
            require(bytes.size in 1..MAX_SCREENSHOT_BYTES) { "Unexpected image byte count: ${bytes.size}" }
            rememberScreenshotPort(url)
            trace?.mark("Direct download")
            return bytes
        } catch (error: Throwable) {
            lastError = error
            log.protocolLog("download failed: ${error.message ?: error::class.java.simpleName}")
            if (!allowPortScan) {
                log.protocolLog("direct download missed; skipping port scan")
                throw screenshotDownloadFailure(lastError, log)
            }
            if (index + 1 == HTTP_DIRECT_ATTEMPTS_BEFORE_PORT_SCAN) {
                findScreenshotOnFreshHttpPort(url, log, trace)?.let { return it }
                log.protocolLog("port scan missed screenshot; stopping stale URL retries")
                throw screenshotDownloadFailure(lastError, log)
            }
            Thread.sleep(HTTP_DOWNLOAD_RETRY_DELAY_MS)
        }
    }
    throw screenshotDownloadFailure(lastError, log)
}

private fun screenshotDownloadFailure(lastError: Throwable?, log: StringBuilder): IllegalStateException =
    IllegalStateException(
        "Downloaded screenshot URL never opened: ${lastError?.message ?: lastError?.javaClass?.simpleName}\n${log.toString().trim()}"
    )

private fun findScreenshotOnCachedHttpPort(url: String, log: StringBuilder): ByteArray? {
    val parsed = URL(url)
    val stalePort = parsed.port
    val host = parsed.host.takeIf { it.isNotBlank() } ?: return null
    val path = parsed.file.takeIf { it.isNotBlank() } ?: "/"
    val ports = ScreenshotPortCache.portsFor(host, path).filter { it != stalePort }
    if (ports.isEmpty()) return null

    log.protocolLog("cached port probe start host=$host path=$path ports=${ports.joinToString(",")}")
    for (port in ports) {
        val result = probeScreenshotPort(host, port, path, AtomicInteger(24)) ?: continue
        ScreenshotPortCache.remember(host, path, result.first)
        log.protocolLog("cached port found screenshot port=${result.first} bytes=${result.second.size}")
        return result.second
    }
    log.protocolLog("cached port probe missed count=${ports.size}")
    return null
}

private fun rememberScreenshotPort(url: String) {
    runCatching {
        val parsed = URL(url)
        val port = parsed.port
        val host = parsed.host.takeIf { it.isNotBlank() } ?: return
        val path = parsed.file.takeIf { it.isNotBlank() } ?: "/"
        ScreenshotPortCache.remember(host, path, port)
    }
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

private fun findScreenshotOnFreshHttpPort(
    url: String,
    log: StringBuilder,
    trace: TclCaptureTrace? = null
): ByteArray? {
    val parsed = URL(url)
    val stalePort = parsed.port
    val host = parsed.host.takeIf { it.isNotBlank() } ?: return null
    val path = buildString {
        append(parsed.file.takeIf { it.isNotBlank() } ?: "/")
    }
    val preferredPorts = ScreenshotPortCache.portsFor(host, path)
    var portOrder = prioritizedPortOrder(stalePort, preferredPorts)
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
                    portOrder = prioritizedPortOrder(stalePort, preferredPorts)
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
                ScreenshotPortCache.remember(host, path, result.first)
                log.protocolLog(
                    "port scan found screenshot port=${result.first} bytes=${result.second.size} submitted=$submitted completed=$completed pass=$pass"
                )
                trace?.mark("Port scan")
                return result.second
            }
            submitNext()
        }
    } finally {
        stopped.set(true)
        executor.shutdownNow()
    }
    log.protocolLog("port scan timeout submitted=$submitted completed=$completed next=$nextPortIndex pass=$pass")
    trace?.mark("Port scan timeout")
    return null
}

private fun prioritizedPortOrder(stalePort: Int, preferredPorts: List<Int>): IntArray {
    val preferred = preferredPorts
        .asSequence()
        .filter { it in HTTP_PORT_SCAN_FIRST_PORT..HTTP_PORT_SCAN_LAST_PORT && it != stalePort }
        .distinct()
        .toList()
    val preferredSet = preferred.toSet()
    val directPortRetry = sequenceOf(stalePort)
        .filter { it in HTTP_PORT_SCAN_FIRST_PORT..HTTP_PORT_SCAN_LAST_PORT }
    val ascendingTail = (HTTP_PORT_SCAN_FIRST_PORT..HTTP_PORT_SCAN_LAST_PORT)
        .asSequence()
        .filter { it != stalePort && it !in preferredSet }
    return (preferred.asSequence() + directPortRetry + ascendingTail).toList().toIntArray()
}

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
