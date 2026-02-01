package com.example.lifequest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.DailyQuestType
import com.example.lifequest.DailyQuestEvent
import com.example.lifequest.RepeatMode
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.local.entity.ExtraQuest
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.logic.DailyQuestManager
import com.example.lifequest.logic.RewardCalculator
import com.example.lifequest.logic.SoundType
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.utils.UsageStatsHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class QuestViewModel(
    private val repository: MainRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val rewardCalculator = RewardCalculator()
    // DailyQuestManagerはここでも必要（起床・就寝判定のため）
    private val dailyQuestManager = DailyQuestManager(repository, usageStatsHelper)

    // --- User Status & Daily Progress ---
    val uiState: StateFlow<UserStatus> = repository.userStatus
        .map { it ?: UserStatus() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus())

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val dailyProgress: StateFlow<DailyQuestProgress> = repository.getDailyProgressFlow(getTodayStartMillis())
        .map { it ?: DailyQuestProgress(date = getTodayStartMillis()) }
        .onEach { progress ->
            if (repository.getDailyProgress(getTodayStartMillis()) == null) {
                repository.insertDailyProgress(progress)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyQuestProgress(date = getTodayStartMillis()))

    // --- Quest Lists ---
    private val endOfTodayFlow = flow {
        while (true) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            emit(calendar.timeInMillis)
            delay(60_000)
        }
    }

    private val allActiveQuests = repository.activeQuests

    val questList: StateFlow<List<QuestWithSubtasks>> = combine(allActiveQuests, endOfTodayFlow) { quests, endOfToday ->
        quests.filter { item ->
            val q = item.quest
            q.repeatMode == RepeatMode.NONE.value || (q.dueDate ?: 0L) <= endOfToday
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val futureQuestList: StateFlow<List<QuestWithSubtasks>> = combine(allActiveQuests, endOfTodayFlow) { quests, endOfToday ->
        quests.filter { item ->
            val q = item.quest
            q.repeatMode != RepeatMode.NONE.value && (q.dueDate ?: 0L) > endOfToday
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val URGENT_WINDOW_HOURS = 72
    val urgentQuest: StateFlow<QuestWithSubtasks?> = allActiveQuests
        .map { quests ->
            val now = System.currentTimeMillis()
            val limitTime = now + (URGENT_WINDOW_HOURS * 60 * 60 * 1000L)
            val candidates = quests.filter { item ->
                val due = item.quest.dueDate
                due != null && due <= limitTime
            }
            if (candidates.isEmpty()) null
            else candidates.shuffled().sortedWith(
                compareBy<QuestWithSubtasks> { it.quest.dueDate }
                    .thenByDescending { it.quest.accumulatedTime }
            ).first()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Bonus Mission Logic ---
    private val _suggestedExtraQuest = MutableStateFlow<ExtraQuest?>(null)
    val suggestedExtraQuest: StateFlow<ExtraQuest?> = _suggestedExtraQuest.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userStatus.collect {
                if (it == null) repository.insertUserStatus(UserStatus())
            }
        }
        viewModelScope.launch {
            urgentQuest.collect { urgent ->
                if (urgent == null) {
                    if (_suggestedExtraQuest.value == null) {
                        _suggestedExtraQuest.value = repository.getRandomExtraQuest()
                    }
                } else {
                    _suggestedExtraQuest.value = null
                }
            }
        }
        performDailyChecks()
    }

    // --- Events ---
    // ポップアップやサウンドイベントはQuestViewModelからも発生しうる
    private val _popupQueue = MutableStateFlow<List<DailyQuestEvent>>(emptyList())
    val popupQueue: StateFlow<List<DailyQuestEvent>> = _popupQueue.asStateFlow()

    private val _soundEvent = Channel<SoundType>(Channel.BUFFERED)
    val soundEvent = _soundEvent.receiveAsFlow()

    private fun triggerSound(type: SoundType) {
        viewModelScope.launch { _soundEvent.send(type) }
    }

    private fun addToPopupQueue(type: DailyQuestType, exp: Int) {
        val currentList = _popupQueue.value.toMutableList()
        currentList.add(DailyQuestEvent(type, exp))
        _popupQueue.value = currentList
    }

    fun dismissCurrentPopup() {
        val currentList = _popupQueue.value.toMutableList()
        if (currentList.isNotEmpty()) {
            currentList.removeAt(0)
            _popupQueue.value = currentList
        }
    }

    // --- Daily Checks ---
    private fun performDailyChecks() {
        viewModelScope.launch {
            val status = repository.getUserStatusSync() ?: return@launch
            val wakeUpExp = dailyQuestManager.checkWakeUp(status)
            if (wakeUpExp > 0) {
                grantExp(wakeUpExp)
                addToPopupQueue(DailyQuestType.WAKE_UP, wakeUpExp)
            }
            val bedtimeExp = dailyQuestManager.checkBedtime(status)
            if (bedtimeExp > 0) {
                grantExp(bedtimeExp)
                addToPopupQueue(DailyQuestType.BEDTIME, bedtimeExp)
            }
        }
    }

    fun refreshPermissionCheck() {
        if (usageStatsHelper.hasPermission()) {
            performDailyChecks()
        }
    }

    private fun grantExp(amount: Int) = viewModelScope.launch {
        val currentStatus = repository.getUserStatusSync() ?: return@launch
        val newStatus = currentStatus.addExperience(amount)
        if (newStatus.level > currentStatus.level) {
            triggerSound(SoundType.LEVEL_UP)
        }
        repository.updateUserStatus(newStatus)
    }

    // --- CRUD Operations ---
    fun addQuest(title: String, note: String, dueDate: Long?, repeatMode: Int, category: Int, estimatedTime: Long, subtasks: List<String>) {
        if (title.isBlank()) return
        val exp = rewardCalculator.calculateExp(estimatedTime)
        triggerSound(SoundType.REQUEST)
        viewModelScope.launch {
            repository.insertQuest(
                Quest(
                    title = title, note = note, dueDate = dueDate, estimatedTime = estimatedTime,
                    expReward = exp, repeatMode = repeatMode, category = category
                ),
                subtasks
            )
        }
    }

    fun updateQuest(quest: Quest) = viewModelScope.launch { repository.updateQuest(quest) }
    fun deleteQuest(quest: Quest) {
        triggerSound(SoundType.DELETE)
        viewModelScope.launch { repository.deleteQuest(quest) }
    }

    fun addSubtask(questId: Int, title: String) = viewModelScope.launch { repository.insertSubtask(questId, title) }
    fun toggleSubtask(subtask: Subtask) = viewModelScope.launch { repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted)) }
    fun deleteSubtask(subtask: Subtask) = viewModelScope.launch { repository.deleteSubtask(subtask) }

    // ボーナスミッション用: ここではクエスト生成までを行い、タイマー開始はUI/FocusViewModelへ委譲する前提
    // もしくはここでDBに入れてIDを返す
    suspend fun prepareBonusMission(extra: ExtraQuest): Quest {
        val newQuest = Quest(
            title = extra.title,
            note = extra.description,
            expReward = extra.expReward,
            estimatedTime = extra.estimatedTime,
            repeatMode = 0,
            category = extra.category,
            dueDate = System.currentTimeMillis()
        )
        val newId = repository.insertQuest(newQuest, emptyList())
        _suggestedExtraQuest.value = null // 提案クリア
        return newQuest.copy(id = newId)
    }
}