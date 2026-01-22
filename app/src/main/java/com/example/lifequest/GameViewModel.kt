package com.example.lifequest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// ★追加: 統計用のデータクラス
data class StatisticsData(
    val totalQuests: Int = 0,
    val totalTime: Long = 0L,
    val categoryBreakdown: List<CategoryStats> = emptyList(),
    val weeklyActivity: List<DailyStats> = emptyList()
)

data class CategoryStats(
    val category: QuestCategory,
    val count: Int,
    val duration: Long,
    val percentage: Float
)

data class DailyStats(
    val dateLabel: String, // "1/23"
    val dayOfWeek: String, // "Mon"
    val totalTime: Long,
    val categoryTimes: Map<QuestCategory, Long> // 積み上げグラフ用
)

class GameViewModel(
    private val repository: GameRepository,
    private val timerManager: FocusTimerManager = FocusTimerManager()
) : ViewModel() {

    companion object {
        private const val EXP_PER_MINUTE_FACTOR = 1.67
        private const val MIN_EXP_REWARD = 10
        private const val DEFAULT_EXP_REWARD = 25
        private const val CYCLE_BONUS_EXP = 15
        private const val BREAK_ACTIVITY_REWARD = 10
    }

    // --- State ---

    val uiState: StateFlow<UserStatus> = repository.userStatus
        .map { it ?: UserStatus() }
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

    val breakActivities: StateFlow<List<BreakActivity>> = repository.allBreakActivities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentBreakActivity = MutableStateFlow<BreakActivity?>(null)
    val currentBreakActivity: StateFlow<BreakActivity?> = _currentBreakActivity.asStateFlow()

    val timerState = timerManager.timerState

    // ★追加: 統計データのStateFlow
    val statistics: StateFlow<StatisticsData> = repository.questLogs
        .map { logs -> calculateStatistics(logs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsData()
        )

    private var currentActiveQuestId: Int? = null

    init {
        viewModelScope.launch {
            repository.userStatus.collect {
                if (it == null) repository.insertUserStatus(UserStatus())
            }
        }

        viewModelScope.launch {
            if (repository.getBreakActivityCount() == 0) {
                val defaults = listOf(
                    BreakActivity(title = "深呼吸", description = "目を閉じて、4秒吸って、4秒止めて、4秒で吐く。", isDefault = true),
                    BreakActivity(title = "遠くを見る", description = "窓の外や部屋の端など、20メートル先を20秒間ぼんやり見る。", isDefault = true),
                    BreakActivity(title = "首のストレッチ", description = "ゆっくりと首を回し、緊張をほぐす。", isDefault = true),
                    BreakActivity(title = "水分補給", description = "コップ一杯の水を飲んでリフレッシュ。", isDefault = true),
                    BreakActivity(title = "背伸び", description = "椅子から立ち上がり、天井に向かって大きく背伸びをする。", isDefault = true)
                )
                defaults.forEach { repository.insertBreakActivity(it) }
            }
        }

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

    // ★追加: 統計計算ロジック
    private fun calculateStatistics(logs: List<QuestLog>): StatisticsData {
        if (logs.isEmpty()) return StatisticsData()

        // 1. 全体累計
        val totalQuests = logs.size
        val totalTime = logs.sumOf { it.actualTime }

        // 2. カテゴリー別集計
        val categoryMap = logs.groupBy { it.category }
        val breakdown = QuestCategory.entries.map { category ->
            val categoryLogs = categoryMap[category.id] ?: emptyList()
            val count = categoryLogs.size
            val duration = categoryLogs.sumOf { it.actualTime }
            val percentage = if (totalTime > 0) (duration.toFloat() / totalTime.toFloat()) else 0f
            CategoryStats(category, count, duration, percentage)
        }.sortedByDescending { it.duration }

        // 3. 週次グラフデータ (直近7日間)
        val calendar = Calendar.getInstance()
        // 時間を00:00:00にリセットして今日の日付を取得
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 6日前まで遡る
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val weeklyData = mutableListOf<DailyStats>()

        for (i in 0..6) {
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis

            // その日のログを抽出
            val dailyLogs = logs.filter { it.completedAt in startOfDay until endOfDay }
            val dailyTotal = dailyLogs.sumOf { it.actualTime }

            // カテゴリごとの時間
            val dailyCategoryMap = dailyLogs.groupBy { it.category }
                .mapKeys { QuestCategory.fromInt(it.key) }
                .mapValues { entry -> entry.value.sumOf { it.actualTime } }

            // 日付ラベル生成
            val dayCalendar = Calendar.getInstance().apply { timeInMillis = startOfDay }
            val dateLabel = "${dayCalendar.get(Calendar.MONTH) + 1}/${dayCalendar.get(Calendar.DAY_OF_MONTH)}"
            val dayOfWeek = when(dayCalendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> ""
            }

            weeklyData.add(DailyStats(dateLabel, dayOfWeek, dailyTotal, dailyCategoryMap))
        }

        return StatisticsData(totalQuests, totalTime, breakdown, weeklyData)
    }

    // --- Timer Actions ---
    // ... (既存コードはそのまま) ...
    fun toggleTimer(quest: Quest, soundManager: SoundManager? = null) {
        if (timerState.value.isRunning) {
            timerManager.stopTimer()
            updateQuestAccumulatedTime(quest)
        } else {
            updateQuestStartTime(quest)
            timerManager.startTimer(
                scope = viewModelScope,
                onFinish = { handleTimerFinish(quest, soundManager) }
            )
        }
    }

    fun toggleTimerMode() {
        timerManager.toggleMode()
    }

    private fun handleTimerFinish(quest: Quest, soundManager: SoundManager?) {
        updateQuestAccumulatedTime(quest)
        soundManager?.playTimerFinishSound()
        grantExp(CYCLE_BONUS_EXP)

        if (!timerState.value.isBreak) {
            shuffleBreakActivity()
            timerManager.startBreak(
                scope = viewModelScope,
                onFinish = {
                    soundManager?.playTimerFinishSound()
                    timerManager.initializeModeBasedOnQuest(quest.estimatedTime)
                    _currentBreakActivity.value = null
                }
            )
        } else {
            timerManager.initializeModeBasedOnQuest(quest.estimatedTime)
            _currentBreakActivity.value = null
        }
    }

    // --- Break Activity Actions ---
    fun shuffleBreakActivity() {
        viewModelScope.launch {
            val activities = breakActivities.value
            if (activities.isNotEmpty()) {
                _currentBreakActivity.value = activities.random()
            }
        }
    }

    fun completeBreakActivity(soundManager: SoundManager?) {
        grantExp(BREAK_ACTIVITY_REWARD)
        soundManager?.playCoinSound()
        _currentBreakActivity.value = null
    }

    fun addBreakActivity(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertBreakActivity(BreakActivity(title = title, description = description))
        }
    }

    fun deleteBreakActivity(activity: BreakActivity) {
        viewModelScope.launch {
            repository.deleteBreakActivity(activity)
        }
    }

    // --- Quest / User Actions ---
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

    private fun grantExp(amount: Int) {
        viewModelScope.launch {
            val currentStatus = repository.getUserStatusSync()
            currentStatus?.let {
                repository.updateUserStatus(it.addExperience(amount))
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
        timerManager.stopTimer()
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)
            repository.insertQuestLog(
                QuestLog(title = quest.title, estimatedTime = quest.estimatedTime, actualTime = finalTime, category = quest.category, completedAt = System.currentTimeMillis())
            )
            grantExp(quest.expReward)

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