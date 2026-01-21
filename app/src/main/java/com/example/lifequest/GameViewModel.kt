package com.example.lifequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context // 追加
import android.net.Uri // 追加
import java.io.BufferedWriter // 追加
import java.io.OutputStreamWriter // 追加
import com.example.lifequest.utils.formatDate // 既存の日付フォーマットを利用

class GameViewModel(private val dao: UserDao) : ViewModel() {

    // ユーザーの状態（レベル、経験値、ゴールド）
    private val _uiState = MutableStateFlow(UserStatus())
    val uiState: StateFlow<UserStatus> = _uiState

    // アクティブなクエストリスト
    private val _questList = MutableStateFlow<List<Quest>>(emptyList())
    val questList: StateFlow<List<Quest>> = _questList

    // 定数定義
    companion object {
        // 難易度
        const val DIFF_EASY = 0
        const val DIFF_NORMAL = 1
        const val DIFF_HARD = 2

        // 繰り返しモード
        const val REPEAT_NONE = 0
        const val REPEAT_DAILY = 1
        const val REPEAT_WEEKLY = 2
        const val REPEAT_MONTHLY = 3
    }

    init {
        // ユーザーステータスの監視
        viewModelScope.launch {
            dao.getUserStatus().collect { status ->
                if (status == null) {
                    // 初回起動時などデータがない場合は作成
                    launch(Dispatchers.IO) { dao.insert(UserStatus()) }
                } else {
                    _uiState.value = status
                }
            }
        }

        // クエストリストの監視
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
        difficulty: Int,
        repeatMode: Int,
        estimatedTime: Long // 目安時間
    ) {
        if (title.isBlank()) return

        // 難易度に応じた報酬計算
        val (exp, gold) = when (difficulty) {
            DIFF_EASY -> Pair(10, 5)
            DIFF_NORMAL -> Pair(30, 15)
            DIFF_HARD -> Pair(100, 50)
            else -> Pair(30, 15)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val newQuest = Quest(
                title = title,
                note = note,
                dueDate = dueDate,
                estimatedTime = estimatedTime,
                expReward = exp,
                goldReward = gold,
                difficulty = difficulty,
                repeatMode = repeatMode
            )
            dao.insertQuest(newQuest)
        }
    }

    // クエストの内容を更新
    fun updateQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateQuest(quest)
        }
    }

    // クエストを削除（誤登録時など）
    fun deleteQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteQuest(quest)
        }
    }

    // タイマーの再生/停止切り替え
    fun toggleTimer(quest: Quest) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            if (quest.lastStartTime != null) {
                // 停止: 経過時間を累積時間に加算し、開始時間をクリア
                val diff = now - quest.lastStartTime
                val newAccumulated = quest.accumulatedTime + diff
                dao.updateQuest(quest.copy(accumulatedTime = newAccumulated, lastStartTime = null))
            } else {
                // 開始: 現在時刻を記録
                dao.updateQuest(quest.copy(lastStartTime = now))
            }
        }
    }

    // クエスト完了処理
    fun completeQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 最終的な実績時間を計算（タイマー動作中の分も加算）
            var finalActualTime = quest.accumulatedTime
            if (quest.lastStartTime != null) {
                finalActualTime += (System.currentTimeMillis() - quest.lastStartTime)
            }

            // 2. 履歴（QuestLog）へ保存
            val log = QuestLog(
                title = quest.title,
                difficulty = quest.difficulty,
                estimatedTime = quest.estimatedTime,
                actualTime = finalActualTime,
                completedAt = System.currentTimeMillis()
            )
            dao.insertQuestLog(log)

            // 3. 報酬付与とレベルアップ計算
            val current = _uiState.value
            var newExp = current.currentExp + quest.expReward
            var newLevel = current.level
            var newMaxExp = current.maxExp
            val newGold = current.gold + quest.goldReward

            while (newExp >= newMaxExp) {
                newExp -= newMaxExp
                newLevel++
                newMaxExp = (newLevel * 100 * 1.2).toInt()
            }

            // ステータス更新
            dao.update(current.copy(
                level = newLevel,
                currentExp = newExp,
                maxExp = newMaxExp,
                gold = newGold
            ))

            // 4. 元クエストの処理（リピート判定）
            if (quest.repeatMode == REPEAT_NONE) {
                // リピートなし：履歴に移したので、アクティブリストからは削除
                dao.deleteQuest(quest)
            } else {
                // リピートあり：次回の期限を設定し、時間はリセットして維持
                val baseDate = quest.dueDate ?: System.currentTimeMillis()
                val nextDate = calculateNextDueDate(baseDate, quest.repeatMode)

                dao.updateQuest(quest.copy(
                    dueDate = nextDate,
                    accumulatedTime = 0L,   // 時間リセット
                    lastStartTime = null    // タイマーリセット
                ))
            }
        }
    }

    // 次回期限日の計算ロジック
    private fun calculateNextDueDate(currentDate: Long, mode: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentDate

        when (mode) {
            REPEAT_DAILY -> calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            REPEAT_WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            REPEAT_MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
        }
        return calendar.timeInMillis
    }
    fun exportLogsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. データを取得
                val logs = dao.getQuestLogsList()

                // 2. ファイルへの書き込み準備
                val outputStream = context.contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    val writer = BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8))

                    // BOM (Excelでの文字化け防止) を書き込む
                    writer.write("\uFEFF")

                    // 3. ヘッダー行を書き込み
                    writer.write("ID,クエスト名,難易度,目標時間(秒),実績時間(秒),完了日\n")

                    // 4. データ行を書き込み
                    for (log in logs) {
                        // カンマが含まれる場合に備えてダブルクォーテーションで囲む処理
                        val safeTitle = "\"${log.title.replace("\"", "\"\"")}\""
                        val difficultyText = when (log.difficulty) {
                            DIFF_EASY -> "EASY"
                            DIFF_HARD -> "HARD"
                            else -> "NORMAL"
                        }
                        val estimatedSec = log.estimatedTime / 1000
                        val actualSec = log.actualTime / 1000
                        val dateStr = formatDate(log.completedAt)

                        val line = "${log.id},$safeTitle,$difficultyText,$estimatedSec,$actualSec,$dateStr\n"
                        writer.write(line)
                    }
                    writer.flush()
                }
            } catch (e: Exception) {
                e.printStackTrace() // エラーハンドリング（本番ではToastなどで通知すると良い）
            }
        }
    }
}