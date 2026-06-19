package com.drummer.speed.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drummer.speed.R
import com.drummer.speed.audio.engine.AudioFocusManager
import com.drummer.speed.audio.engine.CalibrationEngine
import com.drummer.speed.audio.engine.CountdownEngine
import com.drummer.speed.audio.engine.MetronomeEngine
import com.drummer.speed.audio.engine.StrokeDetector
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.domain.model.DownloadResult
import com.drummer.speed.domain.model.UpdateCheckResult
import com.drummer.speed.domain.usecase.CheckUpdateUseCase
import com.drummer.speed.domain.usecase.DeleteResultUseCase
import com.drummer.speed.domain.usecase.DownloadUpdateUseCase
import com.drummer.speed.domain.usecase.GetHistoryUseCase
import com.drummer.speed.domain.usecase.SaveResultUseCase
import com.drummer.speed.util.AudioConfig
import com.drummer.speed.util.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.TimeoutCancellationException
import java.io.File
import javax.inject.Inject

data class PracticeUiState(
    val strokeCount: Int = 0,
    val timeLeft: Int = 30,
    val isRunning: Boolean = false,
    val isCountingDown: Boolean = false,
    val countdownText: String = "",
    val isMetronomeEnabled: Boolean = false,
    val bpm: Int = 120,
    val sensitivity: Float = 0.5f,
    val isCalibrating: Boolean = false,
    val calibrationStep: Int = 0,
    val calibrationProgress: Float = 0f,
    val calibrationHits: Int = 0,
    val timerInput: String = "30",
    val bpmInput: String = "120",
    val sensitivityInput: String = "50"
)

@HiltViewModel
class DrumViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val saveResultUseCase: SaveResultUseCase,
    private val deleteResultUseCase: DeleteResultUseCase,
    private val checkUpdateUseCase: CheckUpdateUseCase,
    private val downloadUpdateUseCase: DownloadUpdateUseCase,
    private val strokeDetector: StrokeDetector,
    private val calibrationEngine: CalibrationEngine,
    private val metronomeEngine: MetronomeEngine,
    private val countdownEngine: CountdownEngine,
    private val audioFocusManager: AudioFocusManager,
    application: Application
) : ViewModel() {

    // MVI State - Single source of truth
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    // Transient UI states
    var showUpdateDialog by mutableStateOf(false)
        private set
    var downloadUrl by mutableStateOf("")
        private set
    var downloadProgress by mutableIntStateOf(-1)
        private set
    var lastSessionResult by mutableStateOf<SessionResult?>(null)
        private set

    private var practiceJob: Job? = null
    private var metronomeJob: Job? = null

    val history: StateFlow<List<SessionResult>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== UPDATE ====================

    fun checkAppUpdate(currentVersionCode: Int, showToastIfNoUpdate: Boolean = false, context: Context? = null) {
        viewModelScope.launch {
            when (val result = checkUpdateUseCase(currentVersionCode)) {
                is UpdateCheckResult.Available -> {
                    downloadUrl = result.downloadUrl
                    showUpdateDialog = true
                }
                is UpdateCheckResult.UpToDate -> {
                    if (showToastIfNoUpdate && context != null) {
                        Toast.makeText(context, context.getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (showToastIfNoUpdate && context != null) {
                        Toast.makeText(context, context.getString(R.string.no_update_available), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        showUpdateDialog = false
    }

    fun startUpdateDownload(context: Context) {
        val targetFile = File(context.getExternalFilesDir(null), AudioConfig.UPDATE_FILE_NAME)
        if (targetFile.exists()) targetFile.delete()
        viewModelScope.launch {
            downloadUpdateUseCase(downloadUrl, targetFile.absolutePath).collect { status ->
                when (status) {
                    is DownloadResult.Progress -> downloadProgress = status.percentage
                    is DownloadResult.Success -> {
                        downloadProgress = -1
                        showUpdateDialog = false
                        installApk(context, status.file)
                    }
                    is DownloadResult.Error -> {
                        downloadProgress = -1
                        Toast.makeText(context, context.getString(R.string.download_failed, status.message), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, AudioConfig.MIME_TYPE_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ==================== PRACTICE ====================

    fun startPractice(goText: String, onFinished: (SessionResult) -> Unit) {
        if (_uiState.value.isRunning) return

        viewModelScope.launch {
            // Countdown phase
            _uiState.update { it.copy(isCountingDown = true) }
            countdownEngine.execute(
                onCountdownText = { text -> _uiState.update { it.copy(countdownText = text) } },
                goText = goText
            )

            // Start practice
            val duration = _uiState.value.timerInput.toIntOrNull() ?: 30
            _uiState.update {
                it.copy(
                    isCountingDown = false,
                    isRunning = true,
                    strokeCount = 0,
                    timeLeft = duration
                )
            }

            audioFocusManager.request { stopPractice() }
            runPracticeSession(onFinished)
        }
    }

    private fun runPracticeSession(onFinished: (SessionResult) -> Unit) {
        val threshold = (AudioConfig.SENSITIVITY_MAX - (_uiState.value.sensitivity * AudioConfig.SENSITIVITY_RANGE)).toInt()

        practiceJob = viewModelScope.launch {
            // Timer coroutine
            launch {
                while (_uiState.value.timeLeft > 0 && _uiState.value.isRunning) {
                    delay(1000)
                    _uiState.update { it.copy(timeLeft = it.timeLeft - 1) }
                }
                if (_uiState.value.timeLeft <= 0) {
                    val result = SessionResult(
                        strokes = _uiState.value.strokeCount,
                        duration = _uiState.value.timerInput.toIntOrNull() ?: 30,
                        bpm = if (_uiState.value.isMetronomeEnabled) _uiState.value.bpm else null
                    )
                    stopPractice()
                    onFinished(result)
                }
            }

            // Stroke detection coroutine
            launch {
                strokeDetector.startDetection(threshold).collect {
                    _uiState.update { it.copy(strokeCount = it.strokeCount + 1) }
                }
            }
        }

        // Metronome
        if (_uiState.value.isMetronomeEnabled) {
            metronomeJob = viewModelScope.launch {
                metronomeEngine.start(
                    bpm = _uiState.value.bpm,
                    isRunning = { _uiState.value.isRunning },
                    timeLeft = { _uiState.value.timeLeft }
                )
            }
        }
    }

    fun stopPractice() {
        val currentState = _uiState.value
        if (currentState.isRunning && currentState.strokeCount > 0) {
            val result = SessionResult(
                strokes = currentState.strokeCount,
                duration = currentState.timerInput.toIntOrNull() ?: 30,
                bpm = if (currentState.isMetronomeEnabled) currentState.bpm else null
            )
            lastSessionResult = result
            viewModelScope.launch { saveResultUseCase(result) }
        }
        _uiState.update { it.copy(isRunning = false) }
        practiceJob?.cancel()
        metronomeJob?.cancel()
        audioFocusManager.release()
    }

    fun resetPractice() {
        stopPractice()
        _uiState.update {
            it.copy(
                strokeCount = 0,
                timeLeft = it.timerInput.toIntOrNull() ?: 30
            )
        }
    }

    // ==================== INPUT HANDLERS ====================

    fun updateTimer(input: String) {
        if (InputValidator.isValidTimerInput(input)) {
            _uiState.update {
                it.copy(
                    timerInput = input,
                    timeLeft = input.toIntOrNull() ?: 0
                )
            }
        }
    }

    fun incrementTimer(amount: Int) {
        val current = _uiState.value.timerInput.toIntOrNull() ?: 0
        val next = InputValidator.clampTimer(current + amount)
        _uiState.update {
            it.copy(
                timerInput = next.toString(),
                timeLeft = next
            )
        }
    }

    fun updateBpm(input: String) {
        if (InputValidator.isValidBpmInput(input)) {
            _uiState.update { it.copy(bpmInput = input) }
            input.toIntOrNull()?.let { bpm ->
                if (bpm in 1..AudioConfig.BPM_MAX) {
                    _uiState.update { it.copy(bpm = bpm) }
                }
            }
        }
    }

    fun incrementBpm(amount: Int) {
        val next = InputValidator.clampBpm(_uiState.value.bpm + amount)
        _uiState.update {
            it.copy(
                bpm = next,
                bpmInput = next.toString()
            )
        }
    }

    fun toggleMetronome(enabled: Boolean) {
        _uiState.update { it.copy(isMetronomeEnabled = enabled) }
    }

    fun updateSensitivity(value: Float) {
        _uiState.update {
            it.copy(
                sensitivity = value,
                sensitivityInput = (value * 100).toInt().toString()
            )
        }
    }

    fun updateSensitivityInput(input: String) {
        if (InputValidator.isValidSensitivityInput(input)) {
            val percent = input.toIntOrNull() ?: 0
            _uiState.update {
                it.copy(
                    sensitivityInput = input,
                    sensitivity = percent / 100f
                )
            }
        }
    }

    // ==================== CALIBRATION ====================

    fun startSmartCalibration() {
        if (_uiState.value.isCalibrating) return
        _uiState.update {
            it.copy(
                isCalibrating = true,
                calibrationStep = 1,
                calibrationProgress = 0f,
                calibrationHits = 0
            )
        }

        viewModelScope.launch {
            try {
                val result = calibrationEngine.calibrate(
                    onProgress = { p -> _uiState.update { it.copy(calibrationProgress = p) } },
                    onHits = { h -> _uiState.update { it.copy(calibrationHits = h, calibrationStep = 2) } }
                )
                updateSensitivity(result.sensitivity)
                _uiState.update { it.copy(calibrationStep = 3) }
            } catch (e: TimeoutCancellationException) {
                stopCalibration()
            } catch (e: Exception) {
                stopCalibration()
            }
        }
    }

    fun stopCalibration() {
        _uiState.update { it.copy(isCalibrating = false, calibrationStep = 0) }
    }

    // ==================== HISTORY ====================

    fun deleteResult(result: SessionResult) = viewModelScope.launch {
        deleteResultUseCase(result)
    }

    fun deleteSelectedResults(ids: List<String>) = viewModelScope.launch {
        deleteResultUseCase.deleteMultiple(ids)
    }
}
