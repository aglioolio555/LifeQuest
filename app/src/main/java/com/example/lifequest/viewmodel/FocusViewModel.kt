package com.example.lifequest.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.lifequest.DailyQuestType
import com.example.lifequest.DailyQuestEvent
import com.example.lifequest.FocusMode
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.logic.DailyQuestManager
import com.example.lifequest.logic.FocusTimerManager
import com.example.lifequest.logic.LifeQuestNotificationManager
import com.example.lifequest.logic.QuestCompletionService
import com.example.lifequest.logic.SoundManager
import com.example.lifequest.logic.SoundType
import com.example.lifequest.utils.UsageStatsHelper
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FocusViewModel(
    private val repository: MainRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel(), DefaultLifecycleObserver {

    private val dailyQuestManager = DailyQuestManager(repository, usageStatsHelper)
    private val questCompletionService = QuestCompletionService(repository, dailyQuestManager)

    // --- Timer State ---
    val timerState = FocusTimerManager.timerState

    // --- Break Activity State ---
    val breakActivities: StateFlow<List<BreakActivity>> = repository.allBreakActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentBreakActivity = MutableStateFlow<BreakActivity?>(null)
    val currentBreakActivity: StateFlow<BreakActivity?> = _currentBreakActivity.asStateFlow()

    // --- Events ---
    private val _soundEvent = Channel<SoundType>(Channel.BUFFERED)
    val soundEvent = _soundEvent.receiveAsFlow()

    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    // 完了ポップアップ用
    private val _popupQueue = MutableStateFlow<List<DailyQuestEvent>>(emptyList())
    val popupQueue: StateFlow<List<DailyQuestEvent>> = _popupQueue.asStateFlow()

    // ボーナスミッション判定用
    var isBonusMissionRunning = false
    private val _isBonusMissionLoading = MutableStateFlow(false)
    val isBonusMissionLoading: StateFlow<Boolean> = _isBonusMissionLoading.asStateFlow()

    // --- Notification & Lifecycle ---
    var notificationManager: LifeQuestNotificationManager? = null
    private val _isInterrupted = MutableStateFlow(false)
    val isInterrupted: StateFlow<Boolean> = _isInterrupted.asStateFlow()

    init {
        // ViewModel作成時にライフサイクル監視を開始
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onCleared() {
        super.onCleared()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (timerState.value.isRunning) {
            _isInterrupted.value = true
            // 現在のクエスト名は取得できないため汎用メッセージ（本来はQuest情報を持つべきだが簡易化）
            notificationManager?.showReturnNotification("クエスト進行中")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        notificationManager?.cancelNotification()
    }

    fun resumeFromInterruption() {
        _isInterrupted.value = false
    }

    // --- Timer Control ---
    fun toggleTimer(quest: Quest, soundManager: SoundManager? = null) {
        if (timerState.value.isRunning) {
            FocusTimerManager.stopTimer()
            updateQuestAccumulatedTime(quest)
            triggerSound(SoundType.TIMER_PAUSE)
            triggerSound(SoundType.BGM_PAUSE)
        } else {
            updateQuestAccumulatedTime(quest)
            updateQuestStartTime(quest)
            triggerSound(SoundType.TIMER_START)
            triggerSound(SoundType.BGM_START)
            triggerSound(SoundType.BGM_RESUME)

            val appScope = ProcessLifecycleOwner.get().lifecycleScope
            FocusTimerManager.startTimer(
                scope = appScope,
                onFinish = { handleTimerFinish(quest) }
            )
        }
    }

    fun toggleTimerMode() = FocusTimerManager.toggleMode()

    fun stopSession(quest: Quest) {
        if (timerState.value.isRunning) {
            FocusTimerManager.stopTimer()
            updateQuestAccumulatedTime(quest)
            triggerSound(SoundType.TIMER_PAUSE)
        }
        triggerSound(SoundType.BGM_STOP)
    }

    private fun handleTimerFinish(quest: Quest) {
        updateQuestAccumulatedTime(quest)
        triggerSound(SoundType.TIMER_FINISH)
        triggerSound(SoundType.BGM_STOP)
        grantExp(15) // Cycle Bonus

        val sessionTime = if (timerState.value.mode == FocusMode.COUNT_UP) 0L else timerState.value.initialSeconds * 1000
        if (sessionTime > 0) {
            viewModelScope.launch {
                val earnedExp = dailyQuestManager.addFocusTime(sessionTime)
                if (earnedExp > 0) {
                    grantExp(earnedExp)
                    addToPopupQueue(DailyQuestType.FOCUS, earnedExp)
                }
            }
        }
        val appScope = ProcessLifecycleOwner.get().lifecycleScope
        if (!timerState.value.isBreak) {
            shuffleBreakActivity()
            FocusTimerManager.startBreak(
                scope = appScope,
                onFinish = {
                    triggerSound(SoundType.TIMER_FINISH)
                    FocusTimerManager.initializeModeBasedOnQuest(quest.estimatedTime)
                    _currentBreakActivity.value = null
                }
            )
        } else {
            FocusTimerManager.initializeModeBasedOnQuest(quest.estimatedTime)
            _currentBreakActivity.value = null
        }
    }

    // --- Quest Completion ---
    fun completeQuest(quest: Quest) {
        FocusTimerManager.stopTimer()
        triggerSound(SoundType.BGM_STOP)
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)
            val result = questCompletionService.completeQuest(quest, finalTime)

            if (result.totalExp > 0) grantExp(result.totalExp)

            if (isBonusMissionRunning) {
                triggerSound(SoundType.BONUS)
                addToPopupQueue(DailyQuestType.BONUS, quest.expReward)
                isBonusMissionRunning = false
            } else {
                triggerSound(SoundType.QUEST_COMPLETE)
            }

            if (result.dailyQuestType != null) {
                addToPopupQueue(result.dailyQuestType, 20)
            }

            if (result.nextDueDate != null) {
                val dateStr = formatDate(result.nextDueDate)
                _toastEvent.send("次回は $dateStr に表示されます")
            }
        }
    }

    // --- Break Activities ---
    fun shuffleBreakActivity() {
        val activities = breakActivities.value
        if (activities.isNotEmpty()) _currentBreakActivity.value = activities.random()
    }

    fun completeBreakActivity() {
        grantExp(10) // Break Reward
        triggerSound(SoundType.QUEST_COMPLETE)
        _currentBreakActivity.value = null
    }

    // --- Helpers ---
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

    private fun calculateFinalActualTime(quest: Quest): Long {
        var time = quest.accumulatedTime
        if (quest.lastStartTime != null) time += (System.currentTimeMillis() - quest.lastStartTime)
        return time
    }

    private fun grantExp(amount: Int) = viewModelScope.launch {
        val currentStatus = repository.getUserStatusSync() ?: return@launch
        val newStatus = currentStatus.addExperience(amount)
        if (newStatus.level > currentStatus.level) {
            triggerSound(SoundType.LEVEL_UP)
        }
        repository.updateUserStatus(newStatus)
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

    private fun triggerSound(type: SoundType) {
        viewModelScope.launch { _soundEvent.send(type) }
    }

    // ボーナスミッションのローディング状態管理用
    fun setBonusMissionLoading(loading: Boolean) {
        _isBonusMissionLoading.value = loading
    }
}