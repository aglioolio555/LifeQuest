package com.example.lifequest.logic

import com.example.lifequest.RepeatMode
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.DailyQuestType

data class QuestCompletionResult(
    val totalExp: Int,
    val dailyQuestType: DailyQuestType? = null, // デイリー達成があればセットされる
    val nextDueDate: Long? = null
)
class QuestCompletionService(
    private val repository: MainRepository,
    private val dailyQuestManager: DailyQuestManager,

) {

    /**
     * クエスト完了処理を実行する
     * @return QuestCompletionResult 獲得した総経験値 (クエスト報酬 + デイリーボーナス)
     */
    suspend fun completeQuest(quest: Quest, actualTime: Long): QuestCompletionResult {
        var totalExpGained = 0

        // 1. ログの保存
        val log = QuestLog(
            title = quest.title,
            estimatedTime = quest.estimatedTime,
            actualTime = actualTime,
            category = quest.category,
            completedAt = System.currentTimeMillis()
        )
        repository.insertQuestLog(log)

        // 2. クエスト報酬の加算
        totalExpGained += quest.expReward

        // 3. デイリークエスト（カテゴリー達成）の判定と加算
        val categoryBonus = dailyQuestManager.checkCategoryComplete(quest.category)
        totalExpGained += categoryBonus

        val dailyType = if (categoryBonus > 0) DailyQuestType.BALANCE else null

        // 4. 繰り返し設定に基づく 次回の作成 または 削除
        var nextDueDate: Long? = null
        val repeat = RepeatMode.fromInt(quest.repeatMode)
        if (repeat == RepeatMode.NONE) {
            repository.deleteQuest(quest)
        } else {
            val nextDate = repeat.calculateNextDueDate(quest.dueDate ?: System.currentTimeMillis())
            nextDueDate = nextDate
            // 繰り返し時は期限を更新し、累積時間をリセットして更新
            val nextQuest = quest.copy(
                dueDate = nextDate,
                accumulatedTime = 0L,
                lastStartTime = null
            )
            repository.updateQuest(nextQuest)
        }

        return QuestCompletionResult(totalExpGained, dailyType,nextDueDate)
    }
}