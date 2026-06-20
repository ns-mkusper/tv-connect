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
