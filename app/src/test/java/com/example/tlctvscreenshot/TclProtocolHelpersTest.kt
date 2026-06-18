package com.example.tlctvscreenshot

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
