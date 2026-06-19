package com.drummer.speed.audio.engine

import android.media.AudioManager
import android.media.ToneGenerator
import com.drummer.speed.util.AudioConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CountdownEngine {

    suspend fun execute(
        onCountdownText: (String) -> Unit,
        goText: String
    ) = withContext(kotlinx.coroutines.Dispatchers.Default) {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, AudioConfig.METRONOME_VOLUME)
        try {
            for (i in AudioConfig.COUNTDOWN_START downTo AudioConfig.COUNTDOWN_END) {
                onCountdownText(i.toString())
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, AudioConfig.COUNTDOWN_BEEP_DURATION_MS.toInt())
                delay(1000)
            }
            onCountdownText(goText)
            toneGen.startTone(ToneGenerator.TONE_DTMF_D, AudioConfig.GO_BEEP_DURATION_MS.toInt())
            delay(AudioConfig.GO_DELAY_MS)
        } finally {
            toneGen.release()
        }
    }
}
