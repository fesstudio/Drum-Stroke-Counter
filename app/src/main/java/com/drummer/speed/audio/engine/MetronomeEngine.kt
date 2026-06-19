package com.drummer.speed.audio.engine

import android.media.AudioManager
import android.media.ToneGenerator
import com.drummer.speed.util.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class MetronomeEngine {

    suspend fun start(
        bpm: Int,
        isRunning: () -> Boolean,
        timeLeft: () -> Int
    ) = withContext(Dispatchers.Default) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, AudioConfig.METRONOME_VOLUME)
        val interval = (60000 / bpm).toLong()
        try {
            while (isActive && isRunning() && timeLeft() > 0) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, AudioConfig.METRONOME_BEEP_DURATION_MS.toInt())
                delay(interval)
            }
        } finally {
            toneGen.release()
        }
    }
}
