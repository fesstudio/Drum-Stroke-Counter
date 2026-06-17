package com.drummer.speed.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class DrumViewModelThresholdTest {

    @Test
    fun `test currentThreshold calculation for sensitivity 0`() {
        val sensitivity = 0f
        val threshold = (15000 - (sensitivity * 14000)).toInt()
        assertEquals(15000, threshold)
    }

    @Test
    fun `test currentThreshold calculation for sensitivity 1`() {
        val sensitivity = 1f
        val threshold = (15000 - (sensitivity * 14000)).toInt()
        assertEquals(1000, threshold)
    }

    @Test
    fun `test currentThreshold calculation for sensitivity 0_5`() {
        val sensitivity = 0.5f
        val threshold = (15000 - (sensitivity * 14000)).toInt()
        assertEquals(8000, threshold)
    }
}
