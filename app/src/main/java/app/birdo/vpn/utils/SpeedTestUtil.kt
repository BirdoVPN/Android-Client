package app.birdo.vpn.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * On-device speed test utility.
 * Measures throughput through the VPN tunnel by downloading/uploading test data.
 *
 * Privacy: No user data transmitted. Only random/zero-filled payloads.
 */
object SpeedTestUtil {

    data class SpeedTestResult(
        val downloadMbps: Double,
        val uploadMbps: Double,
        val latencyMs: Int,
        val jitterMs: Int,
        val bytesDownloaded: Long,
        val bytesUploaded: Long,
        val durationSeconds: Double,
        val serverEndpoint: String,
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Run a complete speed test against the given endpoint.
     */
    suspend fun runSpeedTest(
        speedTestUrl: String,
        serverEndpoint: String,
    ): Result<SpeedTestResult> = withContext(Dispatchers.IO) {
        runCatching {
            val startTime = System.nanoTime()

            // 1. Latency (5 samples)
            val (latencyMs, jitterMs) = measureLatency(speedTestUrl, 5)

            // 2. Download (10MB)
            val (downloadMbps, bytesDownloaded) = measureDownload(speedTestUrl, 10 * 1024 * 1024L)

            // 3. Upload (5MB)
            val (uploadMbps, bytesUploaded) = measureUpload(speedTestUrl, 5 * 1024 * 1024)

            val durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0

            SpeedTestResult(
                downloadMbps = downloadMbps,
                uploadMbps = uploadMbps,
                latencyMs = latencyMs,
                jitterMs = jitterMs,
                bytesDownloaded = bytesDownloaded,
                bytesUploaded = bytesUploaded,
                durationSeconds = durationSeconds,
                serverEndpoint = serverEndpoint,
            )
        }
    }

    private fun measureDownload(baseUrl: String, sizeBytes: Long): Pair<Double, Long> {
        val request = Request.Builder()
            .url("$baseUrl/download?size=$sizeBytes")
            .get()
            .build()

        val start = System.nanoTime()
        val response = client.newCall(request).execute()
        val bytes = response.body?.bytes() ?: ByteArray(0)
        response.close()
        val elapsed = (System.nanoTime() - start) / 1_000_000_000.0

        val mbps = if (elapsed > 0) (bytes.size.toLong() * 8.0) / (elapsed * 1_000_000.0) else 0.0
        return Pair(mbps, bytes.size.toLong())
    }

    private fun measureUpload(baseUrl: String, sizeBytes: Int): Pair<Double, Long> {
        val payload = ByteArray(sizeBytes) // zeros
        val request = Request.Builder()
            .url("$baseUrl/upload")
            .post(payload.toRequestBody())
            .build()

        val start = System.nanoTime()
        val response = client.newCall(request).execute()
        response.close()
        val elapsed = (System.nanoTime() - start) / 1_000_000_000.0

        val mbps = if (elapsed > 0) (sizeBytes.toLong() * 8.0) / (elapsed * 1_000_000.0) else 0.0
        return Pair(mbps, sizeBytes.toLong())
    }

    private fun measureLatency(baseUrl: String, samples: Int): Pair<Int, Int> {
        val latencies = mutableListOf<Double>()

        repeat(samples) {
            val request = Request.Builder()
                .url("$baseUrl/ping")
                .get()
                .build()
            val start = System.nanoTime()
            try {
                val response = client.newCall(request).execute()
                response.close()
                latencies.add((System.nanoTime() - start) / 1_000_000.0)
            } catch (_: Exception) {
                // Skip failed samples
            }
        }

        if (latencies.isEmpty()) return Pair(0, 0)

        val avg = latencies.average()
        val variance = latencies.map { (it - avg) * (it - avg) }.average()
        val jitter = kotlin.math.sqrt(variance)

        return Pair(avg.toInt(), jitter.toInt())
    }
}
