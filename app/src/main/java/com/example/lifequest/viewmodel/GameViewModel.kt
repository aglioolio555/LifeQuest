package com.example.lifequest.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.FocusMode
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.logic.FocusTimerManager
import com.example.lifequest.logic.SoundManager
import com.example.lifequest.logic.StatisticsCalculator
import com.example.lifequest.logic.DailyQuestManager
import com.example.lifequest.logic.RewardCalculator
import com.example.lifequest.logic.QuestCompletionService
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.repository.GameRepository
import com.example.lifequest.model.StatisticsData
import com.example.lifequest.utils.UsageStatsHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class GameViewModel(
    private val repository: GameRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val timerManager: FocusTimerManager = FocusTimerManager()
) : ViewModel() {

    companion object {
        private const val CYCLE_BONUS_EXP = 15
        private const val BREAK_ACTIVITY_REWARD = 10
    }

    // --- Logic Components ---
    private val statisticsCalculator = StatisticsCalculator()
    private val rewardCalculator = RewardCalculator()
    private val dailyQuestManager = DailyQuestManager(repository, usageStatsHelper)
    private val questCompletionService = QuestCompletionService(repository, dailyQuestManager)

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

    // 統計データ (Delegated to StatisticsCalculator)
    val statistics: StateFlow<StatisticsData> = repository.questLogs
        .map { logs -> statisticsCalculator.calculate(logs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsData()
        )

    // デイリークエスト進捗 (Delegated to DailyQuestManager through Repository)
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

        // Timer auto-mode initialization
        viewModelScope.launch {
            repository.activeQuests.collect { quests ->
                val topQuest = quests.firstOrNull()?.quest
                if (topQuest != null && currentActiveQuestId != topQuest.id) {
                    currentActiveQuestId = topQuest.id
                    timerManager.initializeModeBasedOnQuest(topQuest.estimatedTime)
                }
            }
        }

        performDailyChecks()
        _missingPermission.value = !usageStatsHelper.hasPermission()
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // --- Daily Quest Actions ---
    private fun performDailyChecks() {
        viewModelScope.launch {
            val status = repository.getUserStatusSync() ?: return@launch

            val wakeUpExp = dailyQuestManager.checkWakeUp(status)
            if (wakeUpExp > 0) grantExp(wakeUpExp)

            val bedtimeExp = dailyQuestManager.checkBedtime(status)
            if (bedtimeExp > 0) grantExp(bedtimeExp)
        }
    }

    fun refreshPermissionCheck() {
        _missingPermission.value = !usageStatsHelper.hasPermission()
        if (usageStatsHelper.hasPermission()) {
            performDailyChecks()
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

    // --- Timer & Quest Actions ---
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

        if (sessionTime > 0) {
            viewModelScope.launch {
                val earnedExp = dailyQuestManager.addFocusTime(sessionTime)
                if (earnedExp > 0) grantExp(earnedExp)
            }
        }

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

    // --- CRUD & Helper Wrappers ---

    fun completeQuest(quest: Quest) {
        timerManager.stopTimer()
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)

            // Delegate complex completion logic to Service
            val totalExp = questCompletionService.completeQuest(quest, finalTime)

            if (totalExp > 0) grantExp(totalExp)
        }
    }

    fun addQuest(title: String, note: String, dueDate: Long?, repeatMode: Int, category: Int, estimatedTime: Long, subtasks: List<String>) {
        if (title.isBlank()) return
        val exp = rewardCalculator.calculateExp(estimatedTime)
        viewModelScope.launch {
            repository.insertQuest(
                Quest(
                    title = title,
                    note = note,
                    dueDate = dueDate,
                    estimatedTime = estimatedTime,
                    expReward = exp,
                    repeatMode = repeatMode,
                    category = category
                ),
                subtasks
            )
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

    fun addSubtask(questId: Int, title: String) = viewModelScope.launch { repository.insertSubtask(questId, title) }
    fun toggleSubtask(subtask: Subtask) = viewModelScope.launch { repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted)) }
    fun deleteSubtask(subtask: Subtask) = viewModelScope.launch { repository.deleteSubtask(subtask) }
    fun updateQuest(quest: Quest) = viewModelScope.launch { repository.updateQuest(quest) }
    fun deleteQuest(quest: Quest) = viewModelScope.launch { repository.deleteQuest(quest) }
    fun exportLogsToCsv(context: Context, uri: Uri) = viewModelScope.launch { repository.exportLogsToCsv(context, uri) }

    private fun calculateFinalActualTime(quest: Quest): Long {
        var time = quest.accumulatedTime
        if (quest.lastStartTime != null) time += (System.currentTimeMillis() - quest.lastStartTime)
        return time
    }
}