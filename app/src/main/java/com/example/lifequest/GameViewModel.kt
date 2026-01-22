package com.example.lifequest

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val dao: UserDao) : ViewModel() {

    private val _uiState = MutableStateFlow(UserStatus())
    val uiState: StateFlow<UserStatus> = _uiState

    private val _questList = MutableStateFlow<List<QuestWithSubtasks>>(emptyList())
    val questList: StateFlow<List<QuestWithSubtasks>> = _questList

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
            }
        }
    }

    // クエスト追加：難易度・Goldを廃止し、時間に基づくEXPを計算
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

        // EXP計算: 60分 = 100EXP (約1.67 EXP/分)
        val calculatedExp = if (estimatedTime > 0) {
            val minutes = estimatedTime / (1000 * 60)
            (minutes * 1.67).toInt().coerceAtLeast(10)
        } else {
            25 // 時間設定なしのデフォルト
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

    fun toggleTimer(quest: Quest) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            if (quest.lastStartTime != null) {
                val diff = now - quest.lastStartTime
                dao.updateQuest(quest.copy(accumulatedTime = quest.accumulatedTime + diff, lastStartTime = null))
            } else {
                dao.updateQuest(quest.copy(lastStartTime = now))
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