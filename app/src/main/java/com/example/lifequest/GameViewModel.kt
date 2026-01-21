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

    // ユーザーの状態
    private val _uiState = MutableStateFlow(UserStatus())
    val uiState: StateFlow<UserStatus> = _uiState

    // クエストリスト (★型を変更)
    private val _questList = MutableStateFlow<List<QuestWithSubtasks>>(emptyList())
    val questList: StateFlow<List<QuestWithSubtasks>> = _questList

    init {
        // ユーザーステータスの監視
        viewModelScope.launch {
            dao.getUserStatus().collect { status ->
                if (status == null) {
                    launch(Dispatchers.IO) { dao.insert(UserStatus()) }
                } else {
                    _uiState.value = status
                }
            }
        }

        // アクティブなクエストリストの監視
        viewModelScope.launch {
            dao.getActiveQuests().collect { quests ->
                _questList.value = quests
            }
        }
    }

    // クエストを追加 (★サブタスクのリスト引数を追加)
    fun addQuest(
        title: String,
        note: String,
        dueDate: Long?,
        difficultyInt: Int,
        repeatModeInt: Int,
        categoryInt: Int,
        estimatedTime: Long,
        subtaskTitles: List<String> = emptyList() // ★追加
    ) {
        if (title.isBlank()) return

        val difficulty = QuestDifficulty.fromInt(difficultyInt)

        viewModelScope.launch(Dispatchers.IO) {
            val newQuest = Quest(
                title = title,
                note = note,
                dueDate = dueDate,
                estimatedTime = estimatedTime,
                expReward = difficulty.exp,
                goldReward = difficulty.gold,
                difficulty = difficulty.value,
                repeatMode = repeatModeInt,
                category = categoryInt
            )
            // クエスト挿入後にIDを取得
            val questId = dao.insertQuest(newQuest).toInt()

            // ★サブタスクの登録
            subtaskTitles.forEach { subTitle ->
                if (subTitle.isNotBlank()) {
                    dao.insertSubtask(Subtask(questId = questId, title = subTitle))
                }
            }
        }
    }

    // サブタスクの追加（編集用）
    fun addSubtask(questId: Int, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertSubtask(Subtask(questId = questId, title = title))
        }
    }

    // サブタスクの完了切替
    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
        }
    }

    // サブタスク削除
    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSubtask(subtask)
        }
    }

    // クエスト更新
    fun updateQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateQuest(quest)
        }
    }

    // クエスト削除
    fun deleteQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteQuest(quest)
        }
    }

    // タイマー切り替え
    fun toggleTimer(quest: Quest) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            if (quest.lastStartTime != null) {
                // 停止
                val diff = now - quest.lastStartTime
                val newAccumulated = quest.accumulatedTime + diff
                dao.updateQuest(quest.copy(accumulatedTime = newAccumulated, lastStartTime = null))
            } else {
                // 開始
                dao.updateQuest(quest.copy(lastStartTime = now))
            }
        }
    }

    // クエスト完了
    fun completeQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalActualTime = quest.accumulatedTime
            if (quest.lastStartTime != null) {
                finalActualTime += (System.currentTimeMillis() - quest.lastStartTime)
            }

            val log = QuestLog(
                title = quest.title,
                difficulty = quest.difficulty,
                estimatedTime = quest.estimatedTime,
                actualTime = finalActualTime,
                category = quest.category,
                completedAt = System.currentTimeMillis()
            )
            dao.insertQuestLog(log)

            val newStatus = _uiState.value.addExperience(quest.expReward, quest.goldReward)
            dao.update(newStatus)

            val repeatMode = RepeatMode.fromInt(quest.repeatMode)
            if (repeatMode == RepeatMode.NONE) {
                dao.deleteQuest(quest)
            } else {
                val baseDate = quest.dueDate ?: System.currentTimeMillis()
                val nextDate = repeatMode.calculateNextDueDate(baseDate)

                // 繰り返し時、サブタスクの状態をリセットするかは要件によりますが、ここではリセットしない（または手動リセット）とします。
                // 必要であればここで `dao.updateSubtask(...)` をループして false に戻す処理を追加できます。

                dao.updateQuest(quest.copy(
                    dueDate = nextDate,
                    accumulatedTime = 0L,
                    lastStartTime = null
                ))
            }
        }
    }

    // CSVエクスポート
    fun exportLogsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val logs = dao.getAllLogsSync()
            CsvExporter(context).export(uri, logs)
        }
    }
}