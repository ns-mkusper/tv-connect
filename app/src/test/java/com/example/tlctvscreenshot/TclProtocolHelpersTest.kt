package com.example.tlctvscreenshot

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TclProtocolHelpersTest {
    private val helpers = Class.forName("com.example.tlctvscreenshot.MainActivityKt")

    private data class RemoteButtonExpectation(
        val label: String,
        val keyCode: Int,
        val displayLabel: String,
        val testTag: String
    )

    @Test
    fun discoveryPayloadEscapesPhoneNameAndParserRestoresTvName() {
        val payload = invoke("tclDiscoveryPayload", "Phone:One", "stable-id", 1) as String

        assertTrue(payload.endsWith('\u0000'))
        assertTrue(payload.contains("Phone&#058One:PHONE:1:Phone&#058One:stable-id:0:0"))

        val packet = invoke("parseTclDiscoveryPacket", "1:42:Living&#058Room:TV:4:model:version:serial:08&#058C3&#058B3\u0000")
            ?: error("Expected packet")

        assertEquals("1", field(packet, "version"))
        assertEquals("42", field(packet, "packetNo"))
        assertEquals("Living:Room", field(packet, "senderName"))
        assertEquals("TV", field(packet, "senderType"))
        assertEquals(4, field(packet, "commandNo"))
        assertEquals("model:version:serial:08&#058C3&#058B3", field(packet, "additionalSection"))
    }

    @Test
    fun aesEncryptionRoundTripsProtocolText() {
        val text = "149>>15"
        val encrypted = invoke("encryptTclAes", text.toByteArray(Charsets.UTF_8)) as ByteArray
        val decrypted = invoke("decryptTclAes", encrypted) as ByteArray

        assertEquals(text, decrypted.toString(Charsets.UTF_8))
    }

    @Test
    fun writesAndReadsPlainLengthPrefixedTclPacket() {
        val bytes = ByteArrayOutputStream()
        invoke("writeTclText", DataOutputStream(bytes), "225>>", false)

        val packetBytes = bytes.toByteArray()
        assertEquals(9, packetBytes.size)
        assertEquals(0, packetBytes[0].toInt())
        assertEquals(0, packetBytes[1].toInt())
        assertEquals(0, packetBytes[2].toInt())
        assertEquals(5, packetBytes[3].toInt())

        val packet = invoke("readTclPacket", DataInputStream(ByteArrayInputStream(packetBytes)))
            ?: error("Expected packet")
        assertEquals("225>>", field(packet, "text"))
        assertArrayEquals("225>>".toByteArray(Charsets.UTF_8), field(packet, "raw") as ByteArray)
    }

    @Test
    fun writeEncryptedPacketStoresEncryptedPayloadAfterLengthPrefix() {
        val text = "160>>phone-id>>Armor 22"
        val bytes = ByteArrayOutputStream()
        invoke("writeTclText", DataOutputStream(bytes), text, true)

        val packet = invoke("readTclPacket", DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
            ?: error("Expected packet")
        val raw = field(packet, "raw") as ByteArray
        val decrypted = invoke("decryptTclAes", raw) as ByteArray

        assertEquals(text, decrypted.toString(Charsets.UTF_8))
    }


    @Test
    fun remoteButtonSpecsCoverEverySupportedButtonWithoutDrift() {
        val specs = invoke("tclRemoteButtonSpecs") as List<*>
        val expected = listOf(
            RemoteButtonExpectation("Power", 20, "⏻", "remote_button_Power"),
            RemoteButtonExpectation("Home", 19, "🏠", "remote_button_Home"),
            RemoteButtonExpectation("Back", 16, "↩", "remote_button_Back"),
            RemoteButtonExpectation("Up", 11, "⬆", "remote_button_Up"),
            RemoteButtonExpectation("Left", 13, "⬅", "remote_button_Left"),
            RemoteButtonExpectation("OK", 15, "OK", "remote_button_OK"),
            RemoteButtonExpectation("Right", 14, "➡", "remote_button_Right"),
            RemoteButtonExpectation("Down", 12, "⬇", "remote_button_Down"),
            RemoteButtonExpectation("Vol -", 22, "🔉", "remote_button_Vol_minus"),
            RemoteButtonExpectation("Mute", 23, "🔇", "remote_button_Mute"),
            RemoteButtonExpectation("Vol +", 21, "🔊", "remote_button_Vol_plus"),
            RemoteButtonExpectation("Menu", 18, "☰", "remote_button_Menu"),
            RemoteButtonExpectation("Ch -", 28, "CH−", "remote_button_Ch_minus"),
            RemoteButtonExpectation("Ch +", 27, "CH+", "remote_button_Ch_plus")
        )

        assertEquals(expected.size, specs.size)
        expected.zip(specs).forEach { (expectedSpec, actualSpec) ->
            assertEquals(expectedSpec.label, field(actualSpec ?: error("Missing spec"), "label"))
            assertEquals(expectedSpec.keyCode, field(actualSpec, "keyCode"))
            assertEquals(expectedSpec.displayLabel, field(actualSpec, "displayLabel"))
            assertEquals(expectedSpec.testTag, field(actualSpec, "testTag"))
        }
        assertEquals(expected.size, specs.map { field(it ?: error("Missing spec"), "label") }.distinct().size)
        assertEquals(expected.size, specs.map { field(it ?: error("Missing spec"), "testTag") }.distinct().size)
    }

    @Test
    fun remoteButtonTagSanitizesLabelsForComposeTests() {
        assertEquals("remote_button_Vol_minus", invoke("remoteButtonTag", "Vol -"))
        assertEquals("remote_button_Vol_plus", invoke("remoteButtonTag", "Vol +"))
        assertEquals("remote_button_Ch_minus", invoke("remoteButtonTag", "Ch -"))
        assertEquals("remote_button_Ch_plus", invoke("remoteButtonTag", "Ch +"))
    }

    @Test
    fun remoteKeyCommandsUseExpectedTclPayloads() {
        val specs = invoke("tclRemoteButtonSpecs") as List<*>

        specs.forEach { spec ->
            val keyCode = field(spec ?: error("Missing spec"), "keyCode") as Int
            assertEquals("149>>$keyCode", invoke("tclRemoteKeyCommand", keyCode))
        }
    }

    @Test
    fun preferredScreenshotPortsLeadFallbackScanOrder() {
        val order = invoke("prioritizedPortOrder", 32769, listOf(32770, 32768, 32770, 9999)) as IntArray

        assertEquals(28_232, order.size)
        assertEquals(32770, order[0])
        assertEquals(32768, order[1])
        assertEquals(32769, order[2])
        assertEquals(32771, order[3])
        assertFalse(order.contains(9999))
    }

    @Test
    fun staleScreenshotPortIsNotPreferredDuringFallbackScan() {
        val order = invoke("prioritizedPortOrder", 32770, listOf(32770, 32768)) as IntArray

        assertEquals(32768, order[0])
        assertEquals(32770, order[1])
        assertEquals(32769, order[2])
    }

    @Test
    fun fastCaptureUiStatusShowsReadyAndDisconnectedStates() {
        val constants = Class.forName("com.example.tlctvscreenshot.Tcl6553SessionState").enumConstants
            ?: error("Expected enum constants")
        val ready = constants.single { (it as Enum<*>).name == "READY" }

        val readyStatus = invoke("fastCaptureUiStatus", true, ready) ?: error("Expected ready status")
        assertEquals("TV fully connected — fast capture ready", field(readyStatus, "title"))
        assertEquals("Fast ready", field(readyStatus, "captureSubtitle"))
        assertEquals(true, field(readyStatus, "ready"))

        val disconnectedStatus = invoke("fastCaptureUiStatus", false, ready) ?: error("Expected disconnected status")
        assertEquals("Connect TV for fast capture", field(disconnectedStatus, "title"))
        assertEquals("Connect TV", field(disconnectedStatus, "captureSubtitle"))
        assertEquals(false, field(disconnectedStatus, "ready"))
    }

    @Test
    fun fastCaptureUiStatusShowsPreparingAndFallbackStates() {
        val constants = Class.forName("com.example.tlctvscreenshot.Tcl6553SessionState").enumConstants
            ?: error("Expected enum constants")
        val warming = constants.single { (it as Enum<*>).name == "WARMING" }
        val fallback = constants.single { (it as Enum<*>).name == "FALLBACK_ONLY" }

        val warmingStatus = invoke("fastCaptureUiStatus", true, warming) ?: error("Expected warming status")
        assertEquals("TV connected — preparing fast capture", field(warmingStatus, "title"))
        assertEquals("Preparing", field(warmingStatus, "captureSubtitle"))
        assertEquals(false, field(warmingStatus, "ready"))

        val fallbackStatus = invoke("fastCaptureUiStatus", true, fallback) ?: error("Expected fallback status")
        assertEquals("TV connected — fallback capture only", field(fallbackStatus, "title"))
        assertEquals("Fallback", field(fallbackStatus, "captureSubtitle"))
        assertEquals(false, field(fallbackStatus, "ready"))
    }

    @Test
    fun captureTimingSummaryShowsTotalAndBars() {
        val segmentClass = Class.forName("com.example.tlctvscreenshot.TclCaptureTimingSegment")
        val constructor = segmentClass.declaredConstructors.single()
        constructor.isAccessible = true
        val segments = listOf(
            constructor.newInstance("TV wait", 1_200L),
            constructor.newInstance("Download", 100L)
        )

        val summary = invoke("formatCaptureTimingSummary", segments, 1_350L) as String

        assertTrue(summary.contains("Done in 1.4s"))
        assertTrue(summary.contains("TV wait: 1.2s"))
        assertTrue(summary.contains("Download: 100ms"))
        assertTrue(summary.contains("█"))
    }

    @Test
    fun imageAndDisplayHelpersClassifySupportedFiles() {
        assertEquals("jpg", invoke("imageExtension", byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x00)))
        assertEquals("png", invoke("imageExtension", byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x00, 0x00, 0x00, 0x00)))
        assertEquals("bin", invoke("imageExtension", byteArrayOf(0x01, 0x02)))

        assertEquals("image/jpeg", invoke("imageMimeType", File("capture.JPG")))
        assertEquals("image/png", invoke("imageMimeType", File("capture.png")))
        assertEquals("application/octet-stream", invoke("imageMimeType", File("capture.bin")))

        assertEquals("68 B", invoke("formatFileSize", 68L))
        assertEquals("1.5 KB", invoke("formatFileSize", 1536L))
        assertEquals("2.0 MB", invoke("formatFileSize", 2L * 1024L * 1024L))
    }

    @Test
    fun streamProbeTargetsCoverLikelyAvDiscoveryPorts() {
        val targets = invoke("tvStreamProbeTargets") as List<*>
        val ports = targets.map { field(it ?: error("Missing target"), "port") as Int }
        assertTrue(ports.contains(554))
        assertTrue(ports.contains(8008))
        assertTrue(ports.contains(8009))
        assertTrue(ports.contains(8443))
        assertTrue(ports.contains(6553))
    }

    @Test
    fun confirmedPlaybackEvidenceRecognizesRealAvDescriptors() {
        assertEquals(true, invoke("isConfirmedPlaybackEvidence", "HTTP 200 application/vnd.apple.mpegurl confirmed-playback url=http://example.test/live.m3u8"))
        assertEquals(true, invoke("isConfirmedPlaybackEvidence", "HTTP 200 video/mp2t"))
        assertEquals(false, invoke("isConfirmedPlaybackEvidence", "HTTP 200 application/json"))
    }

    @Test
    fun confirmedPlaybackUrlExtractsMarkedUrls() {
        assertEquals(
            "http://example.test/live.m3u8",
            invoke("confirmedPlaybackUrl", "HTTP 200 application/vnd.apple.mpegurl confirmed-playback url=http://example.test/live.m3u8 #EXTM3U")
        )
        assertEquals(null, invoke("confirmedPlaybackUrl", "HTTP 200 application/json"))
    }

    @Test
    fun playbackPlaylistParserResolvesSegmentsAndVariants() {
        val mediaPlaylist = """
            #EXTM3U
            #EXTINF:3.5,
            seg001.ts
            #EXTINF:4.0,
            ../base/seg002.ts?session=abc
        """.trimIndent()
        val segments = invoke("parsePlaybackMediaSegments", mediaPlaylist, "http://example.test/live/base/index.m3u8") as List<*>
        val first = segments[0] ?: error("Missing segment")
        val second = segments[1] ?: error("Missing segment")

        assertEquals("http://example.test/live/base/seg001.ts", field(first, "url"))
        assertEquals(3_500L, field(first, "durationMs"))
        assertEquals("http://example.test/live/base/seg002.ts?session=abc", field(second, "url"))
        assertEquals(4_000L, field(second, "durationMs"))

        val masterPlaylist = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=3000000
            high/index.m3u8
        """.trimIndent()
        val variants = invoke("parsePlaybackVariantReferences", masterPlaylist, "http://example.test/live/master.m3u8") as List<*>
        assertEquals(listOf("http://example.test/live/high/index.m3u8"), variants)
    }

    @Test(expected = IllegalArgumentException::class)
    fun readPacketRejectsOversizedLengths() {
        invoke("readTclPacket", DataInputStream(ByteArrayInputStream(byteArrayOf(0x00, 0x10, 0x00, 0x01))))
    }

    private fun invoke(name: String, vararg args: Any): Any? {
        val method = helpers.declaredMethods.single { method ->
            method.name == name && method.parameterCount == args.size
        }
        method.isAccessible = true
        return try {
            method.invoke(null, *args)
        } catch (error: java.lang.reflect.InvocationTargetException) {
            throw error.targetException
        }
    }

    private fun field(instance: Any, name: String): Any? {
        val field = instance.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(instance)
    }
}
