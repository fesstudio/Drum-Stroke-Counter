package com.drummer.speed.audio.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.drummer.speed.util.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

class StrokeDetector(
    private val sampleRate: Int = AudioConfig.SAMPLE_RATE
) {
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        .coerceAtLeast(AudioConfig.MIN_BUFFER_SIZE)

    @SuppressLint("MissingPermission")
    fun startDetection(threshold: Int): Flow<Unit> = flow {
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val buffer = ShortArray(bufferSize)
        audioRecord.startRecording()

        var lastStrokeTime = 0L
        val debounceTime = AudioConfig.DEBOUNCE_MS

        try {
            while (true) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var maxVal = 0
                    for (i in 0 until read) {
                        val absVal = abs(buffer[i].toInt())
                        if (absVal > maxVal) maxVal = absVal
                    }

                    if (maxVal > threshold) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastStrokeTime > debounceTime) {
                            emit(Unit)
                            lastStrokeTime = currentTime
                        }
                    }
                }
                delay(10)
            }
        } finally {
            try { audioRecord.stop() } catch (_: Exception) {}
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Melakukan kalibrasi penuh (noise floor + collect hits) dalam satu sesi AudioRecord.
     * Seluruh proses berjalan di [Dispatchers.IO] agar tidak memblokir Main thread.
     */
    @SuppressLint("MissingPermission")
    suspend fun calibrate(
        onProgress: (Float) -> Unit,
        onHits: (Int) -> Unit
    ): CalibrationData = withContext(Dispatchers.IO) {
        withTimeout(AudioConfig.CALIBRATION_TIMEOUT_MS) {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()

            try {
                // Step 1: Measure Noise Floor
                var maxNoise = 0
                val silenceStart = System.currentTimeMillis()
                while (System.currentTimeMillis() - silenceStart < AudioConfig.CALIBRATION_SILENCE_DURATION_MS) {
                    val read = audioRecord.read(buffer, 0, bufferSize)
                    if (read > 0) {
                        for (i in 0 until read) {
                            val absVal = abs(buffer[i].toInt())
                            if (absVal > maxNoise) maxNoise = absVal
                        }
                    }
                    onProgress((System.currentTimeMillis() - silenceStart).toFloat() / AudioConfig.CALIBRATION_SILENCE_DURATION_MS)
                }

                // Step 2: Capture hits
                val collectedHits = mutableListOf<Int>()
                var lastHitTime = 0L
                val hitThreshold = maxNoise + AudioConfig.CALIBRATION_HIT_OFFSET

                while (collectedHits.size < AudioConfig.CALIBRATION_HITS) {
                    val read = audioRecord.read(buffer, 0, bufferSize)
                    if (read > 0) {
                        var currentMax = 0
                        for (i in 0 until read) {
                            val absVal = abs(buffer[i].toInt())
                            if (absVal > currentMax) currentMax = absVal
                        }

                        if (currentMax > hitThreshold) {
                            val now = System.currentTimeMillis()
                            if (now - lastHitTime > AudioConfig.CALIBRATION_DEBOUNCE_MS) {
                                collectedHits.add(currentMax)
                                onHits(collectedHits.size)
                                lastHitTime = now
                            }
                        }
                    }
                }

                CalibrationData(
                    noiseFloor = maxNoise,
                    collectedHits = collectedHits
                )
            } finally {
                try { audioRecord.stop() } catch (_: Exception) {}
                audioRecord.release()
            }
        }
    }

    data class CalibrationData(
        val noiseFloor: Int,
        val collectedHits: List<Int>
    )
}
