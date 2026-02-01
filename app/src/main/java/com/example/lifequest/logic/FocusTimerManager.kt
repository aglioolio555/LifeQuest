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

// 定数定義
//todo delete underline
private const val SECONDS_PER_MINUTE = 1L
//private const val SECONDS_PER_MINUTE = 60L
private const val ONE_SECOND_MILLIS = 1000L
private const val DEFAULT_BREAK_DURATION_MINUTES = 5L

// タイマーの状態
data class TimerState(
    val mode: FocusMode = FocusMode.RUSH,
    val initialSeconds: Long = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE,
    val remainingSeconds: Long = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE,
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
            initialSeconds = nextMode.minutes * SECONDS_PER_MINUTE,
            remainingSeconds = nextMode.minutes * SECONDS_PER_MINUTE
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
                    initialSeconds = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE,
                    remainingSeconds = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE
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
                    delay(ONE_SECOND_MILLIS)
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds + 1
                    )
                }
            } else {
                // カウントダウン
                while (_timerState.value.remainingSeconds > 0) {
                    delay(ONE_SECOND_MILLIS)
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

        val breakDuration = DEFAULT_BREAK_DURATION_MINUTES * SECONDS_PER_MINUTE

        _timerState.value = _timerState.value.copy(
            isRunning = true,
            isBreak = true,
            initialSeconds = breakDuration,
            remainingSeconds = breakDuration
        )

        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
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