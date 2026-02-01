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
//todo delete underline if application is completed
private const val SECONDS_PER_MINUTE = 1L
//private const val SECONDS_PER_MINUTE = 60L
private const val ONE_SECOND_MILLIS = 1000L
private const val DEFAULT_BREAK_DURATION_MINUTES = 5L

// タイマーの詳細なステータス
enum class TimerStatus {
    IDLE,           // 待機中（初期状態・完了後）
    FOCUS,          // 集中モード実行中
    PAUSE_FOCUS,    // 集中モード一時停止中
    BREAK,          // 休憩モード実行中
    PAUSE_BREAK     // 休憩モード一時停止中
}

// タイマーの状態
data class TimerState(
    val mode: FocusMode = FocusMode.RUSH,
    val initialSeconds: Long = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE,
    val remainingSeconds: Long = FocusMode.RUSH.minutes * SECONDS_PER_MINUTE,
    val status: TimerStatus = TimerStatus.IDLE
) {
    // UI互換性のためのヘルパープロパティ
    val isRunning: Boolean
        get() = status == TimerStatus.FOCUS || status == TimerStatus.BREAK

    val isBreak: Boolean
        get() = status == TimerStatus.BREAK || status == TimerStatus.PAUSE_BREAK

    val isPaused: Boolean
        get() = status == TimerStatus.PAUSE_FOCUS || status == TimerStatus.PAUSE_BREAK
}

object FocusTimerManager {

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    // モード切り替え（IDLE時のみ可能）
    fun toggleMode() {
        if (_timerState.value.status != TimerStatus.IDLE) return

        val currentModes = FocusMode.entries
        val nextIndex = (_timerState.value.mode.ordinal + 1) % currentModes.size
        val nextMode = currentModes[nextIndex]

        _timerState.value = _timerState.value.copy(
            mode = nextMode,
            initialSeconds = nextMode.minutes * SECONDS_PER_MINUTE,
            remainingSeconds = nextMode.minutes * SECONDS_PER_MINUTE
        )
    }

    // クエストの予想時間に基づいてモードを初期化（IDLE時のみ）
    fun initializeModeBasedOnQuest(estimatedTimeMillis: Long) {
        // IDLE以外のときは初期化しない（実行中やポーズ中の誤操作防止）
        if (_timerState.value.status != TimerStatus.IDLE) return
        val rush = FocusMode.RUSH
        val targetMode = if (estimatedTimeMillis < 2*(rush.minutes+rush.breakMinutes) *SECONDS_PER_MINUTE* ONE_SECOND_MILLIS) FocusMode.RUSH else FocusMode.DEEP_DIVE

        _timerState.value = _timerState.value.copy(
            mode = targetMode,
            initialSeconds = targetMode.minutes * SECONDS_PER_MINUTE,
            remainingSeconds = targetMode.minutes * SECONDS_PER_MINUTE,
            status = TimerStatus.IDLE
        )
    }

    // 集中タイマー開始・再開
    fun startTimer(scope: CoroutineScope, onFinish: () -> Unit) {
        if (timerJob?.isActive == true) return

        val currentStatus = _timerState.value.status

        // 休憩中や休憩ポーズ中は開始できない（明示的に終了させる必要がある）
        if (currentStatus == TimerStatus.BREAK || currentStatus == TimerStatus.PAUSE_BREAK) return

        // IDLEまたはPAUSE_FOCUSからFOCUSへ遷移
        _timerState.value = _timerState.value.copy(status = TimerStatus.FOCUS)

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
                // 完了時処理
                finishTimer()
                onFinish()
            }
        }
    }

    // タイマー一時停止（FOCUS -> PAUSE_FOCUS, BREAK -> PAUSE_BREAK）
    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null

        val newStatus = when (_timerState.value.status) {
            TimerStatus.FOCUS -> TimerStatus.PAUSE_FOCUS
            TimerStatus.BREAK -> TimerStatus.PAUSE_BREAK
            else -> _timerState.value.status // IDLE等の場合はそのまま
        }

        _timerState.value = _timerState.value.copy(status = newStatus)
    }

    // セッション強制終了・リセット（IDLEに戻す）
    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = _timerState.value.copy(status = TimerStatus.IDLE)
    }

    // 内部的な完了処理
    private fun finishTimer() {
        timerJob?.cancel()
        timerJob = null
        // 完了後はIDLEに戻す（休憩への遷移はViewModel側でstartBreakを呼ぶことで制御する想定）
        _timerState.value = _timerState.value.copy(status = TimerStatus.IDLE)
    }

    // 休憩タイマー開始・再開
    fun startBreak(scope: CoroutineScope, onFinish: () -> Unit) {
        if (timerJob?.isActive == true) return

        val currentStatus = _timerState.value.status

        // 集中モード中は開始できない
        if (currentStatus == TimerStatus.FOCUS || currentStatus == TimerStatus.PAUSE_FOCUS) return

        // 初期設定（IDLEから開始する場合のみ時間をセット）
        if (currentStatus == TimerStatus.IDLE) {
            val breakDuration = DEFAULT_BREAK_DURATION_MINUTES * SECONDS_PER_MINUTE
            _timerState.value = _timerState.value.copy(
                initialSeconds = breakDuration,
                remainingSeconds = breakDuration
            )
        }

        // BREAKへ遷移
        _timerState.value = _timerState.value.copy(status = TimerStatus.BREAK)

        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(ONE_SECOND_MILLIS)
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.remainingSeconds - 1
                )
            }
            finishTimer() // 完了したらIDLEへ
            onFinish()
        }
    }
}