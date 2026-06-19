package com.drummer.speed.util

object InputValidator {
    fun isValidDigitInput(input: String, maxLength: Int): Boolean {
        return input.all { it.isDigit() } && input.length <= maxLength
    }

    fun isValidTimerInput(input: String): Boolean {
        return isValidDigitInput(input, AudioConfig.TIMER_MAX_LENGTH)
    }

    fun isValidBpmInput(input: String): Boolean {
        return isValidDigitInput(input, AudioConfig.BPM_MAX_LENGTH)
    }

    fun isValidSensitivityInput(input: String): Boolean {
        return isValidDigitInput(input, AudioConfig.SENSITIVITY_INPUT_MAX_LENGTH) &&
                (input.toIntOrNull() ?: 0) <= AudioConfig.SENSITIVITY_PERCENT_MAX
    }

    fun clampBpm(value: Int): Int = value.coerceIn(AudioConfig.BPM_MIN, AudioConfig.BPM_MAX)

    fun clampTimer(value: Int): Int = value.coerceAtLeast(0)
}
