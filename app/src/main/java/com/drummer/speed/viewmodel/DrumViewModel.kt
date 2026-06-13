package com.drummer.speed.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drummer.speed.data.repository.DrumRepository
import com.drummer.speed.data.repository.UpdateRepository
import com.drummer.speed.data.model.SessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class DrumViewModel @Inject constructor(
    private val repository: DrumRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    // Update States
    var showUpdateDialog by mutableStateOf(false)
    var downloadUrl by mutableStateOf("")

    fun checkAppUpdate(currentVersionCode: Int) {
        viewModelScope.launch {
            val updateData = updateRepository.checkForUpdates()
            if (updateData != null) {
                val latestVersionCode = (updateData["versionCode"] as? Double)?.toInt() ?: 0
                val url = updateData["downloadUrl"] as? String ?: ""
                
                if (latestVersionCode > currentVersionCode) {
                    downloadUrl = url
                    showUpdateDialog = true
                }
            }
        }
    }

    // History from Repository
    val history: StateFlow<List<SessionResult>> = repository.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Practice States
    var strokeCount by mutableIntStateOf(0)
    var timerSeconds by mutableIntStateOf(30)
    var timeLeft by mutableIntStateOf(30)
    var timerInput by mutableStateOf("30")
    
    var isRunning by mutableStateOf(false)
    var isCountingDown by mutableStateOf(false)
    var countdownText by mutableStateOf("")
    
    var isMetronomeEnabled by mutableStateOf(false)
    var bpm by mutableIntStateOf(120)
    var bpmInput by mutableStateOf("120")
    
    var sensitivity by mutableFloatStateOf(0.5f)
    var sensitivityInput by mutableStateOf("50")

    // Calibration States
    var isCalibrating by mutableStateOf(false)
    var calibrationStep by mutableIntStateOf(0) // 0: Idle, 1: Silence, 2: Hitting, 3: Finished
    var calibrationProgress by mutableFloatStateOf(0f)
    var calibrationHits by mutableIntStateOf(0)
    var calibrationStatus by mutableStateOf("")

    private var audioJob: Job? = null
    private var timerJob: Job? = null
    private var metronomeJob: Job? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    val currentThreshold: Int
        get() = (15000 - (sensitivity * 14000)).toInt()

    fun startPractice(goText: String = "GO!", onFinished: (SessionResult) -> Unit) {
        if (isRunning) return
        
        viewModelScope.launch {
            isCountingDown = true
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                for (i in 3 downTo 1) {
                    countdownText = i.toString()
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    delay(1000)
                }
                countdownText = goText
                toneGen.startTone(ToneGenerator.TONE_DTMF_D, 400)
                delay(800)
                toneGen.release()
            } catch (_: Exception) { delay(3800) }
            
            isCountingDown = false
            if (timeLeft <= 0) {
                strokeCount = 0
                timeLeft = timerSeconds
            }
            isRunning = true
            
            runPracticeSession(onFinished)
        }
    }

    private fun runPracticeSession(onFinished: (SessionResult) -> Unit) {
        timerJob = viewModelScope.launch {
            while (timeLeft > 0 && isRunning) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft <= 0) {
                stopPractice()
                val result = SessionResult(
                    strokes = strokeCount,
                    duration = timerSeconds,
                    bpm = if (isMetronomeEnabled) bpm else null
                )
                saveResult(result)
                onFinished(result)
            }
        }

        metronomeJob = viewModelScope.launch(Dispatchers.Default) {
            if (isMetronomeEnabled) {
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                    val interval = (60000 / bpm).toLong()
                    while (isRunning && timeLeft > 0) {
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        delay(interval)
                    }
                    toneGen.release()
                } catch (_: Exception) {}
            }
        }

        audioJob = viewModelScope.launch(Dispatchers.IO) {
            try {
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

                while (isRunning && timeLeft > 0) {
                    val read = audioRecord.read(buffer, 0, bufferSize)
                    if (read > 0) {
                        var maxVal = 0
                        for (i in 0 until read) {
                            val absVal = abs(buffer[i].toInt())
                            if (absVal > maxVal) maxVal = absVal
                        }

                        if (maxVal > currentThreshold) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastStrokeTime > debounceTime) {
                                withContext(Dispatchers.Main) {
                                    strokeCount++
                                }
                                lastStrokeTime = currentTime
                            }
                        }
                    }
                }
                try { audioRecord.stop() } catch (_: Exception) {}
                audioRecord.release()
            } catch (_: Exception) {}
        }
    }

    private fun saveResult(result: SessionResult) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertResult(result)
        }
    }

    fun deleteResult(result: SessionResult) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteResult(result)
        }
    }

    fun deleteSelectedResults(ids: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteResults(ids)
        }
    }

    fun stopPractice() {
        isRunning = false
        audioJob?.cancel()
        timerJob?.cancel()
        metronomeJob?.cancel()
    }

    fun resetPractice() {
        stopPractice()
        strokeCount = 0
        timeLeft = timerSeconds
    }

    fun updateTimer(input: String) {
        if (input.all { it.isDigit() } && input.length <= 4) {
            timerInput = input
            val newVal = input.toIntOrNull() ?: 0
            timerSeconds = newVal
            timeLeft = newVal
        }
    }

    fun incrementTimer(amount: Int) {
        timerSeconds += amount
        if (timerSeconds < 0) timerSeconds = 0
        timeLeft = timerSeconds
        timerInput = timerSeconds.toString()
    }

    fun updateBpm(input: String) {
        if (input.all { it.isDigit() } && input.length <= 3) {
            bpmInput = input
            val newVal = input.toIntOrNull() ?: 0
            if (newVal in 1..999) { bpm = newVal }
        }
    }

    fun incrementBpm(amount: Int) {
        val nextBpm = bpm + amount
        if (nextBpm in 40..999) {
            bpm = nextBpm
            bpmInput = bpm.toString()
        }
    }

    fun updateSensitivity(value: Float) {
        sensitivity = value
        sensitivityInput = (value * 100).toInt().toString()
    }

    fun updateSensitivityInput(input: String) {
        if (input.all { it.isDigit() } && input.length <= 3) {
            val percent = input.toIntOrNull() ?: 0
            if (percent <= 100) {
                sensitivityInput = input
                sensitivity = percent / 100f
            }
        }
    }

    fun startSmartCalibration() {
        if (isCalibrating && calibrationStep != 0) return
        isCalibrating = true
        calibrationStep = 1
        calibrationProgress = 0f
        calibrationHits = 0
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                val buffer = ShortArray(bufferSize)
                audioRecord.startRecording()

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
                    calibrationProgress = (System.currentTimeMillis() - startTime).toFloat() / silenceDuration
                }

                // Step 2: Capture 5 hits
                withContext(Dispatchers.Main) {
                    calibrationStep = 2
                    calibrationProgress = 0f
                }

                val collectedHits = mutableListOf<Int>()
                var lastHitTime = 0L
                val debounceTime = 200L
                val hitThreshold = maxNoise + 500 // Initial guess based on noise

                while (collectedHits.size < 5 && isCalibrating) {
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
                                withContext(Dispatchers.Main) {
                                    calibrationHits = collectedHits.size
                                }
                                lastHitTime = now
                            }
                        }
                    }
                }

                // Step 3: Analysis
                if (collectedHits.size == 5) {
                    val avgHit = collectedHits.average().toInt()
                    // Target threshold is 70% of hit strength, but not lower than noise floor + buffer
                    val targetThreshold = (avgHit * 0.7f).toInt().coerceAtLeast(maxNoise + 1000)
                    
                    // Convert back to 0-1.0 sensitivity scale
                    // Formula in ViewModel: threshold = 15000 - (sensitivity * 14000)
                    // sensitivity = (15000 - threshold) / 14000
                    val newSensitivity = ((15000f - targetThreshold) / 14000f).coerceIn(0f, 1f)
                    
                    withContext(Dispatchers.Main) {
                        updateSensitivity(newSensitivity)
                        calibrationStep = 3
                    }
                }

                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isCalibrating = false }
            }
        }
    }

    fun stopCalibration() {
        isCalibrating = false
        calibrationStep = 0
    }
}
