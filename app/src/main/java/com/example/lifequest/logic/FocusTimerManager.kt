package com.example.lifequest.logic

import com.example.lifequest.FocusMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// タイマーの状態
data class TimerState(
    val mode: FocusMode = FocusMode.RUSH,
    val initialSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val isBreak: Boolean = false
)

object FocusTimerManager {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // モード切り替え
    fun toggleMode() {
        if (_timerState.value.isRunning) return // 実行中は変更不可

        val currentModes = FocusMode.entries
        val nextIndex = (_timerState.value.mode.ordinal + 1) % currentModes.size
        val nextMode = currentModes[nextIndex]

        _timerState.value = _timerState.value.copy(
            mode = nextMode,
            initialSeconds = nextMode.minutes * 60L,
            remainingSeconds = nextMode.minutes * 60L
        )
    }

    // クエストの予想時間に基づいてモードを初期化
    fun initializeModeBasedOnQuest(estimatedTimeMillis: Long) {
        if (!_timerState.value.isRunning && !_timerState.value.isBreak) {
            if (estimatedTimeMillis == 0L) {
                _timerState.value = _timerState.value.copy(
                    mode = FocusMode.COUNT_UP,
                    initialSeconds = 0,
                    remainingSeconds = 0
                )
            } else {
                _timerState.value = _timerState.value.copy(
                    mode = FocusMode.RUSH,
                    initialSeconds = FocusMode.RUSH.minutes * 60L,
                    remainingSeconds = FocusMode.RUSH.minutes * 60L
                )
            }
        }
    }

    //外部からスコープを受け取れるようにする
    fun startTimer(scope: CoroutineScope, onFinish: () -> Unit) {
        if (timerJob?.isActive == true) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = scope.launch {
            val mode = _timerState.value.mode

            if (mode == FocusMode.COUNT_UP) {
                // カウントアップ
                while (true) {
                    delay(1000L)
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds + 1
                    )
                }
            } else {
                // カウントダウン
                while (_timerState.value.remainingSeconds > 0) {
                    delay(1000L)
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds - 1
                    )
                }
                stopTimer()
                onFinish()
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun startBreak(scope: CoroutineScope, onFinish: () -> Unit) {
        if (timerJob?.isActive == true) return

        val breakDuration = 5 * 60L

        _timerState.value = _timerState.value.copy(
            isRunning = true,
            isBreak = true,
            initialSeconds = breakDuration,
            remainingSeconds = breakDuration
        )

        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.remainingSeconds - 1
                )
            }
            stopTimer()
            _timerState.value = _timerState.value.copy(isBreak = false)
            onFinish()
        }
    }
}