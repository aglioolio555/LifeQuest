package com.example.lifequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val dao: UserDao) : ViewModel() {
    // 難易度定数
    companion object {
        //難易度
        const val DIFF_EASY = 0
        const val DIFF_NORMAL = 1
        const val DIFF_HARD = 2
        //リピート
        const val REPEAT_NONE = 0
        const val REPEAT_DAILY = 1
        const val REPEAT_WEEKLY = 2
        const val REPEAT_MONTHLY = 3
    }
    private val _uiState = MutableStateFlow(UserStatus())
    val uiState: StateFlow<UserStatus> = _uiState

    // クエストリストの監視用
    private val _questList = MutableStateFlow<List<Quest>>(emptyList())
    val questList: StateFlow<List<Quest>> = _questList

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

        // クエストリストの監視
        viewModelScope.launch {
            dao.getActiveQuests().collect { quests ->
                _questList.value = quests
            }
        }
    }

    // クエストを追加する
    fun addQuest(title: String, note: String, dueDate: Long?, difficulty: Int, repeatMode: Int) {
        if (title.isBlank()) return

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
                expReward = exp,
                goldReward = gold,
                difficulty = difficulty,
                repeatMode = repeatMode // ★保存
            )
            dao.insertQuest(newQuest)
        }
    }

    // クエストを完了（達成）する
    // 完了処理の更新（タイマー動作中に完了した場合の考慮）
    fun completeQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            // ★タイマー時間の確定処理
            var finalAccumulatedTime = quest.accumulatedTime
            if (quest.lastStartTime != null) {
                finalAccumulatedTime += (System.currentTimeMillis() - quest.lastStartTime)
            }

            // 1. 報酬付与 (ここは変更なし)
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

            dao.update(current.copy(
                level = newLevel,
                currentExp = newExp,
                maxExp = newMaxExp,
                gold = newGold
            ))

            // 2. クエスト更新処理 (★ここを修正)
            if (quest.repeatMode == REPEAT_NONE) {
                // 変更前: dao.deleteQuest(quest)

                // 変更後: 完了フラグを立てて、時間も確定させて保存
                // (DAOのクエリが "WHERE isCompleted = 0" なので、trueにすると画面からは消えます)
                dao.updateQuest(quest.copy(
                    isCompleted = true,
                    accumulatedTime = finalAccumulatedTime, // 最終時間を保存
                    lastStartTime = null // タイマー停止
                ))
            } else {
                // リピート時の処理 (変更なし)
                val baseDate = quest.dueDate ?: System.currentTimeMillis()
                val nextDate = calculateNextDueDate(baseDate, quest.repeatMode)

                dao.updateQuest(quest.copy(
                    dueDate = nextDate,
                    accumulatedTime = 0L,   // リピート時は時間をリセット
                    lastStartTime = null
                ))
            }
        }
    }

    // 次の日付を計算するヘルパー関数
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
    //修正機能
    fun updateQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateQuest(quest)
        }
    }
    // 単純削除（間違って登録した時用。報酬は入りません）
    fun deleteQuest(quest: Quest) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteQuest(quest)
        }
    }
    // ★追加: タイマーの再生/停止を切り替える
    fun toggleTimer(quest: Quest) {
        val now = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            if (quest.lastStartTime != null) {
                // ストップ処理: 今までの累積時間に、(現在 - 開始) を足して、開始時刻をnullにする
                val diff = now - quest.lastStartTime
                val newAccumulated = quest.accumulatedTime + diff
                dao.updateQuest(quest.copy(accumulatedTime = newAccumulated, lastStartTime = null))
            } else {
                // スタート処理: 現在時刻を開始時刻として保存
                dao.updateQuest(quest.copy(lastStartTime = now))
            }
        }
    }
}