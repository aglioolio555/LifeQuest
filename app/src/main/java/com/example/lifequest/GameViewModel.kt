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

    // クエストリスト
    private val _questList = MutableStateFlow<List<Quest>>(emptyList())
    val questList: StateFlow<List<Quest>> = _questList

    init {
        // ユーザーステータスの監視
        viewModelScope.launch {
            dao.getUserStatus().collect { status ->
                if (status == null) {
                    // 初回データ作成（insertの戻り値は無視）
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

    // クエストを追加
    fun addQuest(
        title: String,
        note: String,
        dueDate: Long?,
        difficultyInt: Int,
        repeatModeInt: Int,
        estimatedTime: Long
    ) {
        if (title.isBlank()) return

        // Enumを利用して報酬を取得（if文やwhen文が消えました）
        val difficulty = QuestDifficulty.fromInt(difficultyInt)

        viewModelScope.launch(Dispatchers.IO) {
            val newQuest = Quest(
                title = title,
                note = note,
                dueDate = dueDate,
                estimatedTime = estimatedTime,
                expReward = difficulty.exp,   // Enumから取得
                goldReward = difficulty.gold, // Enumから取得
                difficulty = difficulty.value,
                repeatMode = repeatModeInt
            )
            dao.insertQuest(newQuest)
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
            // 1. 実績時間計算
            var finalActualTime = quest.accumulatedTime
            if (quest.lastStartTime != null) {
                finalActualTime += (System.currentTimeMillis() - quest.lastStartTime)
            }

            // 2. 履歴保存
            val log = QuestLog(
                title = quest.title,
                difficulty = quest.difficulty,
                estimatedTime = quest.estimatedTime,
                actualTime = finalActualTime,
                completedAt = System.currentTimeMillis()
            )
            dao.insertQuestLog(log)

            // 3. レベルアップ計算とステータス更新
            // ★ロジックをUserStatusに移動したため、ここは1行で済みます
            val newStatus = _uiState.value.addExperience(quest.expReward, quest.goldReward)
            dao.update(newStatus)

            // 4. 次回クエストまたは削除
            val repeatMode = RepeatMode.fromInt(quest.repeatMode)
            if (repeatMode == RepeatMode.NONE) {
                dao.deleteQuest(quest)
            } else {
                // ★日付計算ロジックもEnumに移動済み
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

    // CSVエクスポート
    fun exportLogsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            val logs = dao.getQuestLogsList()
            // ★処理をCsvExporterに移譲
            CsvExporter(context).export(uri, logs)
        }
    }
}