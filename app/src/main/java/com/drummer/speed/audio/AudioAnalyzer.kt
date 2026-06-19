package com.drummer.speed.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AudioAnalyzer @Inject constructor() {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

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
        val debounceTime = 80L 

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
                delay(10) // Small delay to avoid tight loop
            }
        } finally {
            try { audioRecord.stop() } catch (_: Exception) {}
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    suspend fun calibrate(
        onProgress: (Float) -> Unit,
        onHits: (Int) -> Unit
    ): Float = withContext(Dispatchers.IO) {
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
            // Step 1: Measure Noise Floor (Silence)
            var maxNoise = 0
            val silenceDuration = 2000L
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < silenceDuration) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    for (i in 0 until read) {
                        val absVal = abs(buffer[i].toInt())
                        if (absVal > maxNoise) maxNoise = absVal
                    }
                }
                onProgress((System.currentTimeMillis() - startTime).toFloat() / silenceDuration)
            }

            // Step 2: Capture 5 hits
            val collectedHits = mutableListOf<Int>()
            var lastHitTime = 0L
            val debounceTime = 200L
            val hitThreshold = maxNoise + 500

            while (collectedHits.size < 5) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var currentMax = 0
                    for (i in 0 until read) {
                        val absVal = abs(buffer[i].toInt())
                        if (absVal > currentMax) currentMax = absVal
                    }

                    if (currentMax > hitThreshold) {
                        val now = System.currentTimeMillis()
                        if (now - lastHitTime > debounceTime) {
                            collectedHits.add(currentMax)
                            onHits(collectedHits.size)
                            lastHitTime = now
                        }
                    }
                }
            }

            // Step 3: Analysis
            val avgHit = collectedHits.average().toInt()
            val targetThreshold = (avgHit * 0.7f).toInt().coerceAtLeast(maxNoise + 1000)
            ((15000f - targetThreshold) / 14000f).coerceIn(0f, 1f)

        } finally {
            try { audioRecord.stop() } catch (_: Exception) {}
            audioRecord.release()
        }
    }
}
