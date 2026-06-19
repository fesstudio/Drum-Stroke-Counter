package com.drummer.speed.audio.engine

import com.drummer.speed.domain.model.CalibrationResult
import com.drummer.speed.util.AudioConfig
import javax.inject.Inject

class CalibrationEngine @Inject constructor(
    private val strokeDetector: StrokeDetector
) {
    suspend fun calibrate(
        onProgress: (Float) -> Unit,
        onHits: (Int) -> Unit
    ): CalibrationResult {
        // Seluruh proses kalibrasi (noise floor + hits) dalam satu sesi AudioRecord
        val data = strokeDetector.calibrate(
            onProgress = onProgress,
            onHits = onHits
        )

        // Analysis
        val avgHit = data.collectedHits.average().toInt()
        val threshold = (avgHit * AudioConfig.THRESHOLD_MULTIPLIER).toInt()
            .coerceAtLeast(data.noiseFloor + AudioConfig.THRESHOLD_MIN_OFFSET)
        val sensitivity = ((AudioConfig.SENSITIVITY_MAX - threshold) / AudioConfig.SENSITIVITY_RANGE)
            .coerceIn(0f, 1f)

        return CalibrationResult(
            sensitivity = sensitivity,
            noiseFloor = data.noiseFloor,
            averageHit = avgHit,
            threshold = threshold
        )
    }
}
