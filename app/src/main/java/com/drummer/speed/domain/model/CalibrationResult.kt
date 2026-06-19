package com.drummer.speed.domain.model

data class CalibrationResult(
    val sensitivity: Float,
    val noiseFloor: Int,
    val averageHit: Int,
    val threshold: Int
)
