package app.birdo.vpn.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SpeedTestResult(
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val latencyMs: Int = 0,
    val jitterMs: Int = 0,
)

enum class SpeedTestPhase {
    Idle,
    Latency,
    Download,
    Upload,
    Done,
    Error,
}

@HiltViewModel
class SpeedTestViewModel @Inject constructor() : ViewModel() {

    private val _phase = MutableStateFlow(SpeedTestPhase.Idle)
    val phase: StateFlow<SpeedTestPhase> = _phase.asStateFlow()

    private val _result = MutableStateFlow(SpeedTestResult())
    val result: StateFlow<SpeedTestResult> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val SPEED_TEST_URL = "https://birdo.app/api/speed-test"
        private const val DOWNLOAD_SIZE = 10 * 1024 * 1024 // 10 MB
        private const val UPLOAD_SIZE = 5 * 1024 * 1024 // 5 MB
        private const val PING_COUNT = 5
    }

    fun runTest() {
        if (_phase.value != SpeedTestPhase.Idle && _phase.value != SpeedTestPhase.Done && _phase.value != SpeedTestPhase.Error) return

        viewModelScope.launch {
            try {
                _error.value = null
                _result.value = SpeedTestResult()

                // Phase 1: Latency
                _phase.value = SpeedTestPhase.Latency
                val (latency, jitter) = withContext(Dispatchers.IO) { measureLatency() }
                _result.value = _result.value.copy(latencyMs = latency, jitterMs = jitter)

                // Phase 2: Download
                _phase.value = SpeedTestPhase.Download
                val downloadMbps = withContext(Dispatchers.IO) { measureDownload() }
                _result.value = _result.value.copy(downloadMbps = downloadMbps)

                // Phase 3: Upload
                _phase.value = SpeedTestPhase.Upload
                val uploadMbps = withContext(Dispatchers.IO) { measureUpload() }
                _result.value = _result.value.copy(uploadMbps = uploadMbps)

                _phase.value = SpeedTestPhase.Done
            } catch (e: Exception) {
                _error.value = e.message ?: "Speed test failed"
                _phase.value = SpeedTestPhase.Error
            }
        }
    }

    private fun measureLatency(): Pair<Int, Int> {
        val samples = mutableListOf<Long>()
        repeat(PING_COUNT) {
            val request = Request.Builder()
                .url("$SPEED_TEST_URL/ping")
                .build()
            val start = System.nanoTime()
            client.newCall(request).execute().use { /* discard body */ }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            samples.add(elapsed)
        }
        val avg = samples.average().toInt()
        val jitter = if (samples.size > 1) {
            val mean = samples.average()
            val variance = samples.map { (it - mean) * (it - mean) }.average()
            kotlin.math.sqrt(variance).toInt()
        } else 0
        return avg to jitter
    }

    private fun measureDownload(): Double {
        val request = Request.Builder()
            .url("$SPEED_TEST_URL/download?size=$DOWNLOAD_SIZE")
            .build()
        val start = System.nanoTime()
        var totalBytes = 0L
        client.newCall(request).execute().use { response ->
            val body = response.body ?: throw Exception("Empty response")
            val buffer = ByteArray(8192)
            body.byteStream().use { stream ->
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    totalBytes += read
                }
            }
        }
        val elapsed = (System.nanoTime() - start) / 1_000_000_000.0
        return if (elapsed > 0) (totalBytes * 8.0) / (elapsed * 1_000_000) else 0.0
    }

    private fun measureUpload(): Double {
        val payload = ByteArray(UPLOAD_SIZE) // zero-filled — no user data
        val request = Request.Builder()
            .url("$SPEED_TEST_URL/upload")
            .post(payload.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val start = System.nanoTime()
        client.newCall(request).execute().use { /* discard */ }
        val elapsed = (System.nanoTime() - start) / 1_000_000_000.0
        return if (elapsed > 0) (UPLOAD_SIZE * 8.0) / (elapsed * 1_000_000) else 0.0
    }
}
