package com.example.lifequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_quest_progress")
data class DailyQuestProgress(
    // その日の0:00:00のタイムスタンプをIDとする
    @PrimaryKey val date: Long,

    // 各クエストの達成状況
    val isWakeUpCleared: Boolean = false,
    val isBedTimeCleared: Boolean = false,

    // 累計集中時間 (ミリ秒)
    val totalFocusTime: Long = 0L,
    // 報酬を受け取った集中時間の段階 (例: 0=未達成, 1=1時間達成済み, 2=3時間達成済み...)
    val focusRewardTier: Int = 0,

    // 達成したカテゴリーIDをカンマ区切り文字列で保存 (例: "0,1,3")
    val clearedCategoryIds: String = ""
) {
    fun hasCategoryCleared(categoryId: Int): Boolean {
        if (clearedCategoryIds.isEmpty()) return false
        return clearedCategoryIds.split(",").map { it.toInt() }.contains(categoryId)
    }

    fun addClearedCategory(categoryId: Int): DailyQuestProgress {
        if (hasCategoryCleared(categoryId)) return this
        val currentIds = if (clearedCategoryIds.isEmpty()) emptyList() else clearedCategoryIds.split(",")
        val newIds = currentIds + categoryId.toString()
        return this.copy(clearedCategoryIds = newIds.joinToString(","))
    }
}