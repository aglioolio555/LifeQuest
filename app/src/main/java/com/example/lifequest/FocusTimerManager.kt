package com.example.lifequest

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

class FocusTimerManager {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    companion object {
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
        private const val ONE_SECOND_MILLIS = 1000L
    }

    // --- 初期化・設定 ---

    // クエストの目安時間に基づいてモードを自動設定（タイマー停止中のみ）
    fun initializeModeBasedOnQuest(estimatedTime: Long) {
        if (_timerState.value.isRunning) return

        val mode = if (estimatedTime < ONE_HOUR_MILLIS) FocusMode.RUSH else FocusMode.DEEP_DIVE
        resetTimerToMode(mode)
    }

    // 手動モード切り替え
    fun toggleMode() {
        if (_timerState.value.isRunning) return
        val nextMode = _timerState.value.mode.next()
        resetTimerToMode(nextMode)
    }

    private fun resetTimerToMode(mode: FocusMode) {
        val seconds = mode.minutes * 60L
        _timerState.value = _timerState.value.copy(
            mode = mode,
            initialSeconds = seconds,
            remainingSeconds = seconds,
            isBreak = false,
            isRunning = false
        )
    }

    // --- タイマー制御 ---

    fun startTimer(scope: CoroutineScope, onTick: () -> Unit = {}, onFinish: () -> Unit) {
        if (_timerState.value.isRunning) return

        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(ONE_SECOND_MILLIS)
                onTick()

                val currentState = _timerState.value

                // カウントアップモードの場合は時間経過のみ（UI側で計算するためState操作なしでも良いが、一応保持）
                if (currentState.mode == FocusMode.COUNT_UP) {
                    continue
                }

                // カウントダウン処理
                if (currentState.remainingSeconds > 0) {
                    _timerState.value = currentState.copy(
                        remainingSeconds = currentState.remainingSeconds - 1
                    )
                } else {
                    // タイマー終了
                    stopTimer()
                    onFinish()
                    break
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    // 休憩モードを開始
    fun startBreak(scope: CoroutineScope, onFinish: () -> Unit) {
        val currentMode = _timerState.value.mode
        val breakMinutes = currentMode.breakMinutes
        val breakSeconds = breakMinutes * 60L

        _timerState.value = _timerState.value.copy(
            remainingSeconds = breakSeconds,
            initialSeconds = breakSeconds,
            isBreak = true,
            isRunning = true,
            mode = FocusMode.BREAK
        )

        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _timerState.value.remainingSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.remainingSeconds - 1
                )
            }
            if (_timerState.value.remainingSeconds <= 0L) {
                stopTimer()
                _timerState.value = _timerState.value.copy(isBreak = false)
                onFinish()
            }
        }
    }
}