package com.example.lifequest.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifequest.data.local.entity.AllowedApp
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.entity.ExtraQuest
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.logic.SoundType
import com.example.lifequest.logic.StatisticsCalculator
import com.example.lifequest.model.StatisticsData
import com.example.lifequest.utils.UsageStatsHelper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: MainRepository,
    private val usageStatsHelper: UsageStatsHelper
) : ViewModel() {

    private val statisticsCalculator = StatisticsCalculator()

    val statistics: StateFlow<StatisticsData> = repository.questLogs
        .map { logs -> statisticsCalculator.calculate(logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsData())

    val breakActivities: StateFlow<List<BreakActivity>> = repository.allBreakActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extraQuests: StateFlow<List<ExtraQuest>> = repository.allExtraQuests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowedApps: StateFlow<List<AllowedApp>> = repository.getAllAllowedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<UserStatus> = repository.userStatus
        .map { it ?: UserStatus() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStatus())

    private val _missingPermission = MutableStateFlow(false)
    val missingPermission: StateFlow<Boolean> = _missingPermission.asStateFlow()

    // --- Sound Events ---
    private val _soundEvent = Channel<SoundType>(Channel.BUFFERED)
    val soundEvent = _soundEvent.receiveAsFlow()

    private fun triggerSound(type: SoundType) {
        viewModelScope.launch { _soundEvent.send(type) }
    }

    init {
        refreshPermissionCheck()
    }

    fun refreshPermissionCheck() {
        _missingPermission.value = !usageStatsHelper.hasPermission()
    }

    // --- Break Activity CRUD ---
    fun addBreakActivity(title: String, description: String) {
        if (title.isBlank()) return
        triggerSound(SoundType.REQUEST)
        viewModelScope.launch { repository.insertBreakActivity(BreakActivity(title = title, description = description)) }
    }

    fun deleteBreakActivity(activity: BreakActivity) {
        triggerSound(SoundType.DELETE)
        viewModelScope.launch { repository.deleteBreakActivity(activity) }
    }

    fun updateBreakActivity(activity: BreakActivity) {
        if (activity.title.isBlank()) return
        triggerSound(SoundType.REQUEST)
        viewModelScope.launch { repository.updateBreakActivity(activity) }
    }

    // --- Extra Quest CRUD ---
    fun addExtraQuest(title: String, desc: String, minutes: Int, category: Int) {
        if (title.isBlank()) return
        triggerSound(SoundType.REQUEST)
        viewModelScope.launch {
            repository.insertExtraQuest(
                ExtraQuest(
                    title = title,
                    description = desc,
                    estimatedTime = minutes * 60 * 1000L,
                    category = category
                )
            )
        }
    }

    fun deleteExtraQuest(extra: ExtraQuest) {
        triggerSound(SoundType.DELETE)
        viewModelScope.launch { repository.deleteExtraQuest(extra) }
    }

    fun updateExtraQuest(quest: ExtraQuest) {
        if (quest.title.isBlank()) return
        triggerSound(SoundType.REQUEST)
        viewModelScope.launch { repository.updateExtraQuest(quest) }
    }

    // --- Allowed Apps CRUD ---
    fun addAllowedApp(app: AllowedApp) = viewModelScope.launch {
        repository.insertAllowedApp(app)
    }

    fun removeAllowedApp(app: AllowedApp) = viewModelScope.launch {
        repository.deleteAllowedApp(app)
    }

    // --- Config & Export ---
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

    fun exportLogsToCsv(context: Context, uri: Uri) = viewModelScope.launch { repository.exportLogsToCsv(context, uri) }
    fun exportDailyQuestsToCsv(context: Context, uri: Uri) = viewModelScope.launch { repository.exportDailyQuestsToCsv(context, uri) }
}