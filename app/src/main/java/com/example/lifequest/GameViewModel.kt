package com.example.lifequest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// タイマーの状態管理用クラス
data class TimerState(
    val mode: FocusMode = FocusMode.RUSH,
    val initialSeconds: Long = 25 * 60L,
    val remainingSeconds: Long = 25 * 60L,
    val isRunning: Boolean = false,
    val isBreak: Boolean = false
)

class GameViewModel(private val dao: UserDao) : ViewModel() {

    private val _uiState = MutableStateFlow(UserStatus())
    val uiState: StateFlow<UserStatus> = _uiState

    private val _questList = MutableStateFlow<List<QuestWithSubtasks>>(emptyList())
    val questList: StateFlow<List<QuestWithSubtasks>> = _questList

    // ★ポモドーロ用ステート
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState

    private var timerJob: Job? = null
    private var currentActiveQuestId: Int? = null

    init {
        viewModelScope.launch {
            dao.getUserStatus().collect { status ->
                if (status == null) {
                    launch(Dispatchers.IO) { dao.insert(UserStatus()) }
                } else {
                    _uiState.value = status
                }
            }
        }

        viewModelScope.launch {
            dao.getActiveQuests().collect { quests ->
                _questList.value = quests

                // クエストリストが更新されたら、一番上のクエストに合わせてモードを自動設定
                // (すでにタイマーが動いている場合は変更しない)
                val topQuest = quests.firstOrNull()?.quest
                if (topQuest != null && currentActiveQuestId != topQuest.id && !_timerState.value.isRunning) {
                    currentActiveQuestId = topQuest.id
                    initializeTimerMode(topQuest.estimatedTime)
                }
            }
        }
    }

    // ★モード自動判定ロジック
    private fun initializeTimerMode(estimatedTime: Long) {
        // 60分(3600000ms)未満ならRush, 以上ならDeep Dive
        val mode = if (estimatedTime < 60 * 60 * 1000) FocusMode.RUSH else FocusMode.DEEP_DIVE
        val seconds = mode.minutes * 60L
        _timerState.value = _timerState.value.copy(
            mode = mode,
            initialSeconds = seconds,
            remainingSeconds = seconds,
            isBreak = false,
            isRunning = false
        )
    }

    // ★モード手動切り替え
    fun toggleTimerMode() {
        if (_timerState.value.isRunning) return // 実行中は変更不可

        val nextMode = _timerState.value.mode.next()
        val seconds = nextMode.minutes * 60L
        _timerState.value = _timerState.value.copy(
            mode = nextMode,
            initialSeconds = seconds,
            remainingSeconds = seconds,
            isBreak = false
        )
    }

    // ★タイマー制御 (カウントダウン / カウントアップ)
    fun toggleTimer(quest: Quest, soundManager: SoundManager? = null) {
        if (_timerState.value.isRunning) {
            // 停止処理
            timerJob?.cancel()
            _timerState.value = _timerState.value.copy(isRunning = false)
            // 経過時間をQuestに保存
            updateQuestAccumulatedTime(quest)
        } else {
            // 開始処理
            _timerState.value = _timerState.value.copy(isRunning = true)
            // 最終開始時刻を記録
            updateQuestStartTime(quest)

            timerJob = viewModelScope.launch(Dispatchers.Default) {
                while (isActive) {
                    delay(1000L)
                    val currentState = _timerState.value

                    if (currentState.mode == FocusMode.COUNT_UP) {
                        // カウントアップモード
                        // (UI表示はQuestEntityのaccumulatedTime + 経過時間を使うため、ここではState更新のみ)
                        // 実装簡略化のため、COUNT_UP時はremainingSecondsを増やして「経過時間」として扱うことも可能だが
                        // UrgentQuestCard側で existing logic を使う
                    } else {
                        // カウントダウンモード (Rush / Deep / Break)
                        if (currentState.remainingSeconds > 0) {
                            _timerState.value = currentState.copy(
                                remainingSeconds = currentState.remainingSeconds - 1
                            )
                        } else {
                            // ★タイマー終了時の処理
                            handleTimerFinish(quest, soundManager)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun updateQuestStartTime(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateQuest(quest.copy(lastStartTime = System.currentTimeMillis()))
        }
    }

    private fun updateQuestAccumulatedTime(quest: Quest) {
        val now = System.currentTimeMillis()
        if (quest.lastStartTime != null) {
            val diff = now - quest.lastStartTime
            viewModelScope.launch(Dispatchers.IO) {
                dao.updateQuest(quest.copy(accumulatedTime = quest.accumulatedTime + diff, lastStartTime = null))
            }
        }
    }

    // ★集中終了時の報酬と休憩への遷移
    private fun handleTimerFinish(quest: Quest, soundManager: SoundManager?) {
        val currentState = _timerState.value

        // タイマー停止
        timerJob?.cancel()
        updateQuestAccumulatedTime(quest) // 時間を保存

        if (!currentState.isBreak) {
            // 集中終了 -> 休憩へ
            soundManager?.playTimerFinishSound() // 効果音

            // 即時報酬 (小EXP付与)
            viewModelScope.launch(Dispatchers.IO) {
                val currentStatus = _uiState.value
                val bonusExp = 15 // 1サイクルの報酬
                dao.update(currentStatus.addExperience(bonusExp))
            }

            // 休憩モード設定
            val breakMinutes = currentState.mode.breakMinutes
            val breakSeconds = breakMinutes * 60L

            _timerState.value = currentState.copy(
                remainingSeconds = breakSeconds,
                initialSeconds = breakSeconds,
                isBreak = true,
                isRunning = true,
                mode = FocusMode.BREAK // UI表示用
            )

            // 休憩タイマー自動開始
            timerJob = viewModelScope.launch(Dispatchers.Default) {
                while (isActive && _timerState.value.remainingSeconds > 0) {
                    delay(1000L)
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds - 1
                    )
                }
                if (_timerState.value.remainingSeconds <= 0L) {
                    // 休憩終了
                    soundManager?.playTimerFinishSound()
                    _timerState.value = _timerState.value.copy(isRunning = false, isBreak = false)
                    // 次の集中モードへ戻す (自動判定ロジックで戻るか、手動でRushに戻す)
                    initializeTimerMode(quest.estimatedTime)
                }
            }

        } else {
            // 休憩終了 (手動停止された場合など)
            _timerState.value = currentState.copy(isRunning = false, isBreak = false)
            initializeTimerMode(quest.estimatedTime)
        }
    }

    // --- 以下、既存のメソッド ---

    fun addQuest(
        title: String,
        note: String,
        dueDate: Long?,
        repeatModeInt: Int,
        categoryInt: Int,
        estimatedTime: Long,
        subtaskTitles: List<String> = emptyList()
    ) {
        if (title.isBlank()) return

        val calculatedExp = if (estimatedTime > 0) {
            val minutes = estimatedTime / (1000 * 60)
            (minutes * 1.67).toInt().coerceAtLeast(10)
        } else {
            25
        }

        viewModelScope.launch(Dispatchers.IO) {
            val newQuest = Quest(
                title = title,
                note = note,
                dueDate = dueDate,
                estimatedTime = estimatedTime,
                expReward = calculatedExp,
                repeatMode = repeatModeInt,
                category = categoryInt
            )
            val questId = dao.insertQuest(newQuest).toInt()

            subtaskTitles.forEach { subTitle ->
                if (subTitle.isNotBlank()) {
                    dao.insertSubtask(Subtask(questId = questId, title = subTitle))
                }
            }
        }
    }

    fun completeQuest(quest: Quest) {
        timerJob?.cancel() // タイマー停止
        _timerState.value = _timerState.value.copy(isRunning = false)

        viewModelScope.launch(Dispatchers.IO) {
            var finalActualTime = quest.accumulatedTime
            if (quest.lastStartTime != null) {
                finalActualTime += (System.currentTimeMillis() - quest.lastStartTime)
            }

            val log = QuestLog(
                title = quest.title,
                estimatedTime = quest.estimatedTime,
                actualTime = finalActualTime,
                category = quest.category,
                completedAt = System.currentTimeMillis()
            )
            dao.insertQuestLog(log)

            val newStatus = _uiState.value.addExperience(quest.expReward)
            dao.update(newStatus)

            val repeatMode = RepeatMode.fromInt(quest.repeatMode)
            if (repeatMode == RepeatMode.NONE) {
                dao.deleteQuest(quest)
            } else {
                val baseDate = quest.dueDate ?: System.currentTimeMillis()
                val nextDate = repeatMode.calculateNextDueDate(baseDate)
                dao.updateQuest(quest.copy(
                    dueDate = nextDate,
                    accumulatedTime = 0L,
                    lastStartTime = null
                ))
            }
        }
    }

    fun addSubtask(questId: Int, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { dao.insertSubtask(Subtask(questId = questId, title = title)) }
    }

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted)) }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteSubtask(subtask) }
    }

    fun updateQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) { dao.updateQuest(quest) }
    }

    fun deleteQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteQuest(quest) }
    }

    fun exportLogsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val logs = dao.getAllLogsSync()
            CsvExporter(context).export(uri, logs)
        }
    }
}