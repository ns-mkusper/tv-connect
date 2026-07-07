package com.example.tlctvscreenshot

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.system.measureNanoTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class MockTargetBenchmarkTest {
    private val helpers = Class.forName("com.example.tlctvscreenshot.MainActivityKt")
    private val pngBytes = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAFgwJ/lwQnWQAAAABJRU5ErkJggg=="
    )

    @Before
    fun runOnlyWhenBenchmarkTaskEnablesIt() {
        assumeTrue(System.getProperty("tvconnect.benchmarks") == "true")
    }

    @Test
    fun mockTargetScreenshotRoundTripStaysInsideLocalBudget() {
        val durationsMs = List(24) {
            MockScreenshotTarget(pngBytes).use { target ->
                val elapsedNanos = measureNanoTime {
                    val downloaded = runScreenshotRoundTrip(target.commandPort, target.screenshotUrl)
                    assertEquals(pngBytes.size, downloaded.size)
                }
                TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
            }
        }

        val medianMs = percentile(durationsMs, 50)
        val p95Ms = percentile(durationsMs, 95)
        assertTrue("Median mock target screenshot round trip was ${medianMs}ms", medianMs <= 80)
        assertTrue("p95 mock target screenshot round trip was ${p95Ms}ms", p95Ms <= 220)
    }

    @Test
    fun screenshotPortOrderBenchmarkKeepsPreferredLookupCheap() {
        val preferredPorts = listOf(41000, 41001, 41000, 32768, 60999)
        val elapsedNanos = measureNanoTime {
            repeat(200) {
                val order = invoke("prioritizedPortOrder", 41002, preferredPorts) as IntArray
                assertEquals(41000, order[0])
                assertEquals(41001, order[1])
                assertEquals(32768, order[2])
                assertEquals(60999, order[3])
                assertEquals(41002, order[4])
            }
        }
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos)

        assertTrue("200 screenshot port order builds took ${elapsedMs}ms", elapsedMs <= 700)
    }

    private fun runScreenshotRoundTrip(commandPort: Int, screenshotUrl: String): ByteArray {
        Socket(InetAddress.getLoopbackAddress(), commandPort).use { socket ->
            socket.tcpNoDelay = true
            socket.soTimeout = 2_000
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            writeText(output, "159>>Benchmark Phone>>1>>${UUID.randomUUID()}>>1")
            assertTrue(readText(input).startsWith("159>>"))
            writeText(output, "160>>benchmark-phone-id>>Benchmark Phone")
            writeText(output, "150>>")
            assertEquals("150>>YES", readText(input))
            writeText(output, "225>>")
            assertEquals("225>>0>>$screenshotUrl", readText(input))
        }

        val connection = (URL(screenshotUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 1_000
            readTimeout = 1_000
            requestMethod = "GET"
            useCaches = false
        }
        return connection.inputStream.use { it.readBytes() }.also { connection.disconnect() }
    }

    private fun writeText(output: DataOutputStream, text: String) {
        output.writeInt(text.toByteArray(Charsets.UTF_8).size)
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun readText(input: DataInputStream): String {
        val length = input.readInt()
        require(length in 1..1024 * 1024) { "Unexpected packet length: $length" }
        val payload = ByteArray(length)
        input.readFully(payload)
        return payload.toString(Charsets.UTF_8)
    }

    private fun percentile(values: List<Long>, percentile: Int): Long {
        val sorted = values.sorted()
        val index = (ceil(percentile / 100.0 * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index]
    }

    private fun invoke(name: String, vararg args: Any?): Any? {
        val method = helpers.declaredMethods.single { method ->
            method.name == name && method.parameterTypes.size == args.size
        }
        method.isAccessible = true
        return method.invoke(null, *args)
    }
}

private class MockScreenshotTarget(
    private val screenshotBytes: ByteArray
) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()
    private val commandSocket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val httpSocket = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val ready = CountDownLatch(2)

    val commandPort: Int = commandSocket.localPort
    val screenshotUrl: String

    init {
        val path = "/screenShot/tohsneercs.PNG"
        screenshotUrl = "http://${InetAddress.getLoopbackAddress().hostAddress}:${httpSocket.localPort}$path"
        executor.execute {
            commandSocket.use { server ->
                ready.countDown()
                server.accept().use(::handleCommandConnection)
            }
        }
        executor.execute {
            httpSocket.use { server ->
                ready.countDown()
                server.accept().use(::handleHttpConnection)
            }
        }
        check(ready.await(2, TimeUnit.SECONDS)) { "Mock target did not start" }
    }

    private fun handleCommandConnection(socket: Socket) {
        val input = DataInputStream(socket.getInputStream())
        val output = DataOutputStream(socket.getOutputStream())
        readText(input)
        writeText(output, "159>>Mock TV>>mock-version>>mock-firmware>>mock-serial>>mock-link>>0")
        readText(input)
        readText(input)
        writeText(output, "150>>YES")
        readText(input)
        writeText(output, "225>>0>>$screenshotUrl")
    }

    private fun writeText(output: DataOutputStream, text: String) {
        output.writeInt(text.toByteArray(Charsets.UTF_8).size)
        output.write(text.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private fun readText(input: DataInputStream): String {
        val length = input.readInt()
        require(length in 1..1024 * 1024) { "Unexpected packet length: $length" }
        val payload = ByteArray(length)
        input.readFully(payload)
        return payload.toString(Charsets.UTF_8)
    }

    private fun handleHttpConnection(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val request = StringBuilder()
        while (!request.endsWith("\r\n\r\n") && request.length < 8 * 1024) {
            val next = input.read()
            if (next == -1) break
            request.append(next.toChar())
        }
        val headers = "HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: ${screenshotBytes.size}\r\nConnection: close\r\n\r\n"
        output.write(headers.toByteArray(Charsets.US_ASCII))
        output.write(screenshotBytes)
        output.flush()
    }

    override fun close() {
        runCatching { commandSocket.close() }
        runCatching { httpSocket.close() }
        executor.shutdownNow()
    }
}
