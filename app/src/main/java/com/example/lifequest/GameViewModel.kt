package com.example.lifequest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.utils.UsageStatsHelper //
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

// 統計表示用のデータ構造
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
    val dateLabel: String,
    val dayOfWeek: String,
    val totalTime: Long,
    val categoryTimes: Map<QuestCategory, Long>
)

class GameViewModel(
    private val repository: GameRepository,
    private val usageStatsHelper: UsageStatsHelper, //
    private val timerManager: FocusTimerManager = FocusTimerManager()
) : ViewModel() {

    companion object {
        private const val EXP_PER_MINUTE_FACTOR = 1.67
        private const val MIN_EXP_REWARD = 10
        private const val DEFAULT_EXP_REWARD = 25
        private const val CYCLE_BONUS_EXP = 15
        private const val BREAK_ACTIVITY_REWARD = 10

        private const val WAKE_UP_EXP = 30
        private const val BEDTIME_EXP = 50 //
        private const val EARLIEST_WAKE_UP_WINDOW_MINUTES = 180L // 3時間
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

    // 統計データ
    val statistics: StateFlow<StatisticsData> = repository.questLogs
        .map { logs -> calculateStatistics(logs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsData()
        )

    // デイリークエスト進捗
    val dailyProgress: StateFlow<DailyQuestProgress> = repository.getDailyProgressFlow(getTodayStartMillis())
        .map { it ?: DailyQuestProgress(date = getTodayStartMillis()) }
        .onEach { progress ->
            if (repository.getDailyProgress(getTodayStartMillis()) == null) {
                repository.insertDailyProgress(progress)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailyQuestProgress(date = getTodayStartMillis())
        )

    // 権限不足通知用
    private val _missingPermission = MutableStateFlow(false)
    val missingPermission: StateFlow<Boolean> = _missingPermission.asStateFlow()

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

        checkWakeUpQuest()
        checkBedtimeQuest() //
        _missingPermission.value = !usageStatsHelper.hasPermission() //
    }

    // --- Helper ---
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // --- Daily Quest Logic ---

    private fun checkWakeUpQuest() {
        viewModelScope.launch {
            val status = repository.getUserStatusSync() ?: return@launch
            val todayStart = getTodayStartMillis()
            var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

            if (!progress.isWakeUpCleared) {
                val now = Calendar.getInstance()
                val targetTimeMinutes = status.targetWakeUpHour * 60 + status.targetWakeUpMinute
                val currentTimeMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startTimeWindow = targetTimeMinutes - EARLIEST_WAKE_UP_WINDOW_MINUTES

                if (currentTimeMinutes in startTimeWindow..targetTimeMinutes.toLong()) {
                    progress = progress.copy(isWakeUpCleared = true)
                    repository.updateDailyProgress(progress)
                    grantExp(WAKE_UP_EXP)
                }
            }
        }
    }

    // 就寝クエスト判定
    fun checkBedtimeQuest() {
        if (!usageStatsHelper.hasPermission()) {
            _missingPermission.value = true
            return
        }
        _missingPermission.value = false

        viewModelScope.launch {
            val status = repository.getUserStatusSync() ?: return@launch
            val todayStart = getTodayStartMillis()
            var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

            if (progress.isBedTimeCleared) return@launch

            val calendar = Calendar.getInstance()
            val wakeUpTarget = calendar.apply {
                timeInMillis = todayStart
                set(Calendar.HOUR_OF_DAY, status.targetWakeUpHour)
                set(Calendar.MINUTE, status.targetWakeUpMinute)
            }.timeInMillis

            val bedTimeTarget = calendar.apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, status.targetBedTimeHour)
                set(Calendar.MINUTE, status.targetBedTimeMinute)
            }.timeInMillis

            val now = System.currentTimeMillis()
            if (now < wakeUpTarget) return@launch

            val screenOnTimeMillis = usageStatsHelper.getScreenOnTime(bedTimeTarget, wakeUpTarget)
            val limitMillis = 5 * 60 * 1000L // 5分

            if (screenOnTimeMillis < limitMillis) {
                progress = progress.copy(isBedTimeCleared = true)
                repository.updateDailyProgress(progress)
                grantExp(BEDTIME_EXP)
            }
        }
    }

    fun refreshPermissionCheck() {
        _missingPermission.value = !usageStatsHelper.hasPermission()
        if (usageStatsHelper.hasPermission()) {
            checkBedtimeQuest()
        }
    }

    private fun updateDailyFocusTime(addedTime: Long) {
        viewModelScope.launch {
            val todayStart = getTodayStartMillis()
            var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

            val newTotalTime = progress.totalFocusTime + addedTime
            var newTier = progress.focusRewardTier
            var expToAdd = 0

            val hours = newTotalTime / (1000 * 60 * 60)
            if (newTier < 1 && hours >= 1) { newTier = 1; expToAdd += 50 }
            if (newTier < 2 && hours >= 3) { newTier = 2; expToAdd += 100 }
            if (newTier < 3 && hours >= 5) { newTier = 3; expToAdd += 200 }
            if (newTier < 4 && hours >= 10) { newTier = 4; expToAdd += 500 }

            if (newTier != progress.focusRewardTier || addedTime > 0) {
                repository.updateDailyProgress(progress.copy(
                    totalFocusTime = newTotalTime,
                    focusRewardTier = newTier
                ))
                if (expToAdd > 0) grantExp(expToAdd)
            }
        }
    }

    private fun checkCategoryDailyComplete(category: Int) {
        viewModelScope.launch {
            val todayStart = getTodayStartMillis()
            var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

            if (!progress.hasCategoryCleared(category)) {
                progress = progress.addClearedCategory(category)
                repository.updateDailyProgress(progress)
                grantExp(20)
            }
        }
    }

    fun updateTargetTimes(wakeUpHour: Int, wakeUpMinute: Int, bedTimeHour: Int, bedTimeMinute: Int) {
        viewModelScope.launch {
            val currentStatus = repository.getUserStatusSync() ?: return@launch
            repository.updateUserStatus(currentStatus.copy(
                targetWakeUpHour = wakeUpHour,
                targetWakeUpMinute = wakeUpMinute,
                targetBedTimeHour = bedTimeHour,
                targetBedTimeMinute = bedTimeMinute
            ))
        }
    }

    // --- Statistics Logic ---

    private fun calculateStatistics(logs: List<QuestLog>): StatisticsData {
        if (logs.isEmpty()) return StatisticsData()

        val totalQuests = logs.size
        val totalTime = logs.sumOf { it.actualTime }

        val categoryMap = logs.groupBy { it.category }
        val breakdown = QuestCategory.entries.map { category ->
            val categoryLogs = categoryMap[category.id] ?: emptyList()
            val duration = categoryLogs.sumOf { it.actualTime }
            CategoryStats(
                category,
                categoryLogs.size,
                duration,
                if (totalTime > 0) duration.toFloat() / totalTime.toFloat() else 0f
            )
        }.sortedByDescending { it.duration }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val weeklyData = mutableListOf<DailyStats>()
        for (i in 0..6) {
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val end = calendar.timeInMillis

            val dailyLogs = logs.filter { it.completedAt in start until end }
            val dailyTotal = dailyLogs.sumOf { it.actualTime }
            val dailyCatMap = dailyLogs.groupBy { it.category }
                .mapKeys { QuestCategory.fromInt(it.key) }
                .mapValues { it.value.sumOf { log -> log.actualTime } }

            val dCal = Calendar.getInstance().apply { timeInMillis = start }
            val label = "${dCal.get(Calendar.MONTH) + 1}/${dCal.get(Calendar.DAY_OF_MONTH)}"
            val dow = when(dCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"; Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"; Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"; else -> ""
            }
            weeklyData.add(DailyStats(label, dow, dailyTotal, dailyCatMap))
        }

        return StatisticsData(totalQuests, totalTime, breakdown, weeklyData)
    }

    // --- Actions ---

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

    fun toggleTimerMode() = timerManager.toggleMode()

    private fun handleTimerFinish(quest: Quest, soundManager: SoundManager?) {
        updateQuestAccumulatedTime(quest)
        soundManager?.playTimerFinishSound()
        grantExp(CYCLE_BONUS_EXP)

        val sessionTime = if(timerState.value.mode == FocusMode.COUNT_UP) 0L else timerState.value.initialSeconds * 1000
        if (sessionTime > 0) updateDailyFocusTime(sessionTime)

        if (!timerState.value.isBreak) {
            shuffleBreakActivity()
            timerManager.startBreak(
                scope = viewModelScope,
                onFinish = {
                    soundManager?.playTimerFinishSound()
                    timerManager.initializeModeBasedOnQuest(quest.estimatedTime ?: 0L)
                    _currentBreakActivity.value = null
                }
            )
        } else {
            timerManager.initializeModeBasedOnQuest(quest.estimatedTime)
            _currentBreakActivity.value = null
        }
    }

    fun shuffleBreakActivity() {
        val activities = breakActivities.value
        if (activities.isNotEmpty()) _currentBreakActivity.value = activities.random()
    }

    fun completeBreakActivity(soundManager: SoundManager?) {
        grantExp(BREAK_ACTIVITY_REWARD)
        soundManager?.playCoinSound()
        _currentBreakActivity.value = null
    }

    fun addBreakActivity(title: String, description: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.insertBreakActivity(BreakActivity(title = title, description = description)) }
    }

    fun deleteBreakActivity(activity: BreakActivity) = viewModelScope.launch { repository.deleteBreakActivity(activity) }

    private fun updateQuestStartTime(quest: Quest) = viewModelScope.launch {
        repository.updateQuest(quest.copy(lastStartTime = System.currentTimeMillis()))
    }

    private fun updateQuestAccumulatedTime(quest: Quest) {
        val now = System.currentTimeMillis()
        if (quest.lastStartTime != null) {
            val diff = now - quest.lastStartTime
            viewModelScope.launch { repository.updateQuest(quest.copy(accumulatedTime = quest.accumulatedTime + diff, lastStartTime = null)) }
        }
    }

    private fun grantExp(amount: Int) = viewModelScope.launch {
        repository.getUserStatusSync()?.let { repository.updateUserStatus(it.addExperience(amount)) }
    }

    fun addQuest(title: String, note: String, dueDate: Long?, repeatMode: Int, category: Int, estimatedTime: Long, subtasks: List<String>) {
        if (title.isBlank()) return
        val exp = calculateExpReward(estimatedTime)
        viewModelScope.launch {
            repository.insertQuest(Quest(title = title, note = note, dueDate = dueDate, estimatedTime = estimatedTime, expReward = exp, repeatMode = repeatMode, category = category), subtasks)
        }
    }

    fun completeQuest(quest: Quest) {
        timerManager.stopTimer()
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)
            repository.insertQuestLog(QuestLog(title = quest.title, estimatedTime = quest.estimatedTime, actualTime = finalTime, category = quest.category, completedAt = System.currentTimeMillis()))
            grantExp(quest.expReward)
            checkCategoryDailyComplete(quest.category)

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
        if (quest.lastStartTime != null) time += (System.currentTimeMillis() - quest.lastStartTime)
        return time
    }
}