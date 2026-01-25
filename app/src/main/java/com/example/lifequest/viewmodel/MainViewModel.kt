package com.example.lifequest.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.DailyQuestType
import com.example.lifequest.FocusMode
import com.example.lifequest.RepeatMode // 追加
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
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.local.entity.ExtraQuest
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.model.StatisticsData
import com.example.lifequest.utils.UsageStatsHelper
import com.example.lifequest.utils.formatDate // 追加
import kotlinx.coroutines.delay // 追加
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.lifequest.logic.LifeQuestNotificationManager
import kotlinx.coroutines.channels.Channel // 追加

data class DailyQuestEvent(
    val type: DailyQuestType,
    val expEarned: Int
)

class MainViewModel(
    private val repository: MainRepository,
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

    // ★変更: 今日の終わり（判定基準）を定期的に更新するFlow
    private val endOfTodayFlow = flow {
        while (true) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            emit(calendar.timeInMillis)
            delay(60_000) // 1分ごとに更新
        }
    }

    // ★変更: 全てのアクティブクエストを取得
    private val allActiveQuests = repository.activeQuests

    // ★変更: 「今日やるべきクエスト」のみをフィルタリングして公開
    // 条件: リピートなし OR (期限日が設定されており、かつ期限が今日の終わり以前)
    val questList: StateFlow<List<QuestWithSubtasks>> = combine(allActiveQuests, endOfTodayFlow) { quests, endOfToday ->
        quests.filter { item ->
            val q = item.quest
            // リピートなし(0) は常に表示。リピートありなら期限チェック。
            q.repeatMode == RepeatMode.NONE.value || (q.dueDate ?: 0L) <= endOfToday
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ★追加: 「未来の待機中クエスト」リスト（明日以降に出現予定のもの）
    val futureQuestList: StateFlow<List<QuestWithSubtasks>> = combine(allActiveQuests, endOfTodayFlow) { quests, endOfToday ->
        quests.filter { item ->
            val q = item.quest
            // リピートあり かつ 期限が明日以降
            q.repeatMode != RepeatMode.NONE.value && (q.dueDate ?: 0L) > endOfToday
        }
    }.stateIn(
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

    val statistics: StateFlow<StatisticsData> = repository.questLogs
        .map { logs -> statisticsCalculator.calculate(logs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsData()
        )

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

    private val _missingPermission = MutableStateFlow(false)
    val missingPermission: StateFlow<Boolean> = _missingPermission.asStateFlow()

    private var currentActiveQuestId: Int? = null

    // ★追加: トースト通知用のイベントチャンネル
    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private val _popupQueue = MutableStateFlow<List<DailyQuestEvent>>(emptyList())
    val popupQueue: StateFlow<List<DailyQuestEvent>> = _popupQueue.asStateFlow()
    // ★追加: 管理画面用のエキストラクエスト一覧
    val extraQuests: StateFlow<List<ExtraQuest>> = repository.allExtraQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ★追加: ホーム画面に提案するエキストラクエスト
    private val _suggestedExtraQuest = MutableStateFlow<ExtraQuest?>(null)
    val suggestedExtraQuest: StateFlow<ExtraQuest?> = _suggestedExtraQuest.asStateFlow()

    // ★追加: 現在実行中のクエストがエキストラかどうか
    var isBonusMissionRunning = false

    // ★追加: ボーナスミッション開始処理中のロード状態
    private val _isBonusMissionLoading = MutableStateFlow(false)
    val isBonusMissionLoading: StateFlow<Boolean> = _isBonusMissionLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userStatus.collect {
                if (it == null) repository.insertUserStatus(UserStatus())
            }
        }

        // Timer auto-mode initialization
        viewModelScope.launch {
            // ここは activeQuests 全体を見て判定しても良いが、表示中のものに合わせるなら questList を使う
            questList.collect { quests ->
                val topQuest = quests.firstOrNull()?.quest
                if (topQuest != null && currentActiveQuestId != topQuest.id) {
                    currentActiveQuestId = topQuest.id
                    timerManager.initializeModeBasedOnQuest(topQuest.estimatedTime)
                }
            }
        }
        viewModelScope.launch {
            questList.collect { activeQuests ->
                if (activeQuests.isEmpty()) {
                    // タスクがない時だけ抽選（まだ抽選していなければ）
                    if (_suggestedExtraQuest.value == null) {
                        _suggestedExtraQuest.value = repository.getRandomExtraQuest()
                    }
                } else {
                    // タスクが復活したら提案をクリア
                    _suggestedExtraQuest.value = null
                }
            }
        }

        performDailyChecks()
        _missingPermission.value = !usageStatsHelper.hasPermission()
    }

    // ... (既存メソッド getTodayStartMillis, performDailyChecks, addToPopupQueue, dismissCurrentPopup, refreshPermissionCheck, updateTargetTimes, toggleTimer, toggleTimerMode, handleTimerFinish) ...
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

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

    fun toggleTimer(quest: Quest, soundManager: SoundManager? = null) {
        if (timerState.value.isRunning) {
            timerManager.stopTimer()
            updateQuestAccumulatedTime(quest)
            soundManager?.playClick()
        } else {
            updateQuestStartTime(quest)
            soundManager?.playTimerStart()
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
                if (earnedExp > 0) {
                    grantExp(earnedExp)
                    addToPopupQueue(DailyQuestType.FOCUS, earnedExp)
                }
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

    // ★修正: completeQuest を拡張してボーナスミッション対応
    fun completeQuest(quest: Quest) {
        timerManager.stopTimer()
        viewModelScope.launch {
            val finalTime = calculateFinalActualTime(quest)

            // 結果を受け取る
            val result = questCompletionService.completeQuest(quest, finalTime)

            // 経験値付与
            if (result.totalExp > 0) grantExp(result.totalExp)

            // 通常のデイリー/カテゴリポップアップ
            if (result.dailyQuestType != null) {
                addToPopupQueue(result.dailyQuestType, 20)
            }

            // ★追加: ボーナスミッションだった場合の特別処理
            if (isBonusMissionRunning) {
                // ボーナス達成ポップアップを表示
                addToPopupQueue(DailyQuestType.BONUS, quest.expReward)
                // 提案をリセット（次の抽選のため）
                _suggestedExtraQuest.value = null
                isBonusMissionRunning = false
            }

            if (result.nextDueDate != null) {
                val dateStr = formatDate(result.nextDueDate)
                _toastEvent.send("次回は $dateStr に表示されます")
            }
        }
    }

    // ★修正: ボーナスミッションを開始する（正規クエスト変換＆タイマー開始）
    fun startBonusMission(extra: ExtraQuest, soundManager: SoundManager) {
        viewModelScope.launch {
            _isBonusMissionLoading.value = true // ロード開始
            isBonusMissionRunning = true

            // 1. ExtraQuest から通常の Quest データを作成
            // IDは自動生成させるため0
            val newQuest = Quest(
                title = extra.title,
                note = extra.description,
                expReward = extra.expReward,
                estimatedTime = extra.estimatedTime,
                repeatMode = 0, // リピートなし
                category = 4,   // その他カテゴリ
                dueDate = System.currentTimeMillis() // 今日
            )

            // 2. DBに保存し、発行されたIDを取得
            val newQuestId = repository.insertQuest(newQuest, emptyList())


            // 4. IDを持った完全なクエストオブジェクトを作成してタイマー開始
            val insertedQuest = newQuest.copy(id = newQuestId)

            // 既存タイマー停止＆新クエストで開始
            if (timerState.value.isRunning) {
                timerManager.stopTimer()
            }
            toggleTimer(insertedQuest, soundManager)

            // 提案用変数をクリア
            _suggestedExtraQuest.value = null

            // DB反映のラグを考慮して少し待機してからロード解除
            delay(200)
            _isBonusMissionLoading.value = false
        }
    }

    // ★追加: エキストラクエスト管理
    fun addExtraQuest(title: String, desc: String, minutes: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertExtraQuest(
                ExtraQuest(
                    title = title,
                    description = desc,
                    estimatedTime = minutes * 60 * 1000L
                )
            )
        }
    }

    fun deleteExtraQuest(extra: ExtraQuest) = viewModelScope.launch {
        repository.deleteExtraQuest(extra)
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
    fun exportDailyQuestsToCsv(context: Context, uri: Uri) = viewModelScope.launch { repository.exportDailyQuestsToCsv(context, uri) }
    private fun calculateFinalActualTime(quest: Quest): Long {
        var time = quest.accumulatedTime
        if (quest.lastStartTime != null) time += (System.currentTimeMillis() - quest.lastStartTime)
        return time
    }

    private val _isInterrupted = MutableStateFlow(false)
    val isInterrupted: StateFlow<Boolean> = _isInterrupted.asStateFlow()

    var notificationManager: LifeQuestNotificationManager? = null

    fun onAppBackgrounded() {
        if (timerState.value.isRunning) {
            _isInterrupted.value = true
            val currentQuest = questList.value.firstOrNull()?.quest
            val title = currentQuest?.title ?: "クエスト"
            notificationManager?.showReturnNotification(title)
        }
    }

    fun onAppForegrounded() {
        notificationManager?.cancelNotification()
    }

    fun resumeFromInterruption() {
        _isInterrupted.value = false
    }
}