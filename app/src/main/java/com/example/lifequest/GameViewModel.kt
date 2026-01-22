package com.example.lifequest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// RepositoryとManagerを依存性として受け取る
class GameViewModel(
    private val repository: GameRepository,
    private val timerManager: FocusTimerManager = FocusTimerManager()
) : ViewModel() {

    companion object {
        private const val EXP_PER_MINUTE_FACTOR = 1.67
        private const val MIN_EXP_REWARD = 10
        private const val DEFAULT_EXP_REWARD = 25
        private const val CYCLE_BONUS_EXP = 15
    }

    // --- State ---

    // ★修正: RepositoryのFlowをStateFlowに変換します。
    // これにより GameScreen で collectAsState() を初期値なしで呼べるようになります。
    val uiState: StateFlow<UserStatus> = repository.userStatus
        .map { it ?: UserStatus() } // nullの場合は初期値を返す
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStatus()
        )

    val questList: StateFlow<List<QuestWithSubtasks>> = repository.activeQuests
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // タイマー状態はManagerから委譲
    val timerState = timerManager.timerState

    private var currentActiveQuestId: Int? = null

    init {
        // 初期データの監視などはRepositoryのFlowを利用するため、initでの明示的なcollectは
        // ComposeのcollectAsStateを利用する場合は不要になる場合もあるが、
        // ここではデータがない場合の初期作成ロジックだけ残す
        viewModelScope.launch {
            repository.userStatus.collect {
                if (it == null) repository.insertUserStatus(UserStatus())
            }
        }

        // クエスト切り替え時のタイマー自動設定の監視
        viewModelScope.launch {
            repository.activeQuests.collect { quests ->
                val topQuest = quests.firstOrNull()?.quest
                if (topQuest != null && currentActiveQuestId != topQuest.id) {
                    currentActiveQuestId = topQuest.id
                    timerManager.initializeModeBasedOnQuest(topQuest.estimatedTime)
                }
            }
        }
    }

    // --- Timer Actions ---

    fun toggleTimer(quest: Quest, soundManager: SoundManager? = null) {
        if (timerState.value.isRunning) {
            // 停止: タイマーを止めて、経過時間をDB保存
            timerManager.stopTimer()
            updateQuestAccumulatedTime(quest)
        } else {
            // 開始: 開始時間をDB保存し、タイマーを起動
            updateQuestStartTime(quest)

            timerManager.startTimer(
                scope = viewModelScope,
                onFinish = {
                    // タイマー終了時のコールバック（ViewModelが具体的な処理を決定）
                    handleTimerFinish(quest, soundManager)
                }
            )
        }
    }

    fun toggleTimerMode() {
        timerManager.toggleMode()
    }

    private fun handleTimerFinish(quest: Quest, soundManager: SoundManager?) {
        // 1. 経過時間を確定
        updateQuestAccumulatedTime(quest)

        // 2. 演出と報酬
        soundManager?.playTimerFinishSound()
        grantCycleBonusExp()

        // 3. 休憩モードへ移行
        if (!timerState.value.isBreak) {
            timerManager.startBreak(
                scope = viewModelScope,
                onFinish = {
                    // 休憩終了時
                    soundManager?.playTimerFinishSound()
                    // 次の集中に向けてリセット
                    timerManager.initializeModeBasedOnQuest(quest.estimatedTime)
                }
            )
        } else {
            // 休憩が終わっていた場合のリセット
            timerManager.initializeModeBasedOnQuest(quest.estimatedTime)
        }
    }

    // --- Quest / User Actions (Delegated to Repository) ---

    private fun updateQuestStartTime(quest: Quest) {
        viewModelScope.launch {
            repository.updateQuest(quest.copy(lastStartTime = System.currentTimeMillis()))
        }
    }

    private fun updateQuestAccumulatedTime(quest: Quest) {
        val now = System.currentTimeMillis()
        if (quest.lastStartTime != null) {
            val diff = now - quest.lastStartTime
            viewModelScope.launch {
                repository.updateQuest(quest.copy(accumulatedTime = quest.accumulatedTime + diff, lastStartTime = null))
            }
        }
    }

    private fun grantCycleBonusExp() {
        viewModelScope.launch {
            val currentStatus = repository.getUserStatusSync() // 同期取得メソッドが必要、あるいはFlowから取る
            currentStatus?.let {
                repository.updateUserStatus(it.addExperience(CYCLE_BONUS_EXP))
            }
        }
    }

    // --- CRUD Wrappers ---

    fun addQuest(title: String, note: String, dueDate: Long?, repeatMode: Int, category: Int, estimatedTime: Long, subtasks: List<String>) {
        if (title.isBlank()) return
        val exp = calculateExpReward(estimatedTime)
        viewModelScope.launch {
            repository.insertQuest(
                Quest(title = title, note = note, dueDate = dueDate, estimatedTime = estimatedTime, expReward = exp, repeatMode = repeatMode, category = category),
                subtasks
            )
        }
    }

    fun completeQuest(quest: Quest) {
        timerManager.stopTimer() // クエスト完了時はタイマー強制停止
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)

            // ログ保存
            repository.insertQuestLog(
                QuestLog(title = quest.title, estimatedTime = quest.estimatedTime, actualTime = finalTime, category = quest.category, completedAt = System.currentTimeMillis())
            )

            // 経験値付与
            val status = repository.getUserStatusSync()
            status?.let { repository.updateUserStatus(it.addExperience(quest.expReward)) }

            // 繰り返し or 削除
            val repeat = RepeatMode.fromInt(quest.repeatMode)
            if (repeat == RepeatMode.NONE) {
                repository.deleteQuest(quest)
            } else {
                val nextDate = repeat.calculateNextDueDate(quest.dueDate ?: System.currentTimeMillis())
                repository.updateQuest(quest.copy(dueDate = nextDate, accumulatedTime = 0L, lastStartTime = null))
            }
        }
    }

    fun addSubtask(questId: Int, title: String) = viewModelScope.launch { repository.insertSubtask(questId, title) }
    fun toggleSubtask(subtask: Subtask) = viewModelScope.launch { repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted)) }
    fun deleteSubtask(subtask: Subtask) = viewModelScope.launch { repository.deleteSubtask(subtask) }
    fun updateQuest(quest: Quest) = viewModelScope.launch { repository.updateQuest(quest) }
    fun deleteQuest(quest: Quest) = viewModelScope.launch { repository.deleteQuest(quest) }
    fun exportLogsToCsv(context: Context, uri: Uri) = viewModelScope.launch { repository.exportLogsToCsv(context, uri) }

    // --- Helper Logic ---
    private fun calculateExpReward(estimatedTime: Long): Int {
        return if (estimatedTime > 0) {
            val minutes = estimatedTime / (1000 * 60)
            (minutes * EXP_PER_MINUTE_FACTOR).toInt().coerceAtLeast(MIN_EXP_REWARD)
        } else DEFAULT_EXP_REWARD
    }

    private fun calculateFinalActualTime(quest: Quest): Long {
        var time = quest.accumulatedTime
        if (quest.lastStartTime != null) {
            time += (System.currentTimeMillis() - quest.lastStartTime)
        }
        return time
    }
}