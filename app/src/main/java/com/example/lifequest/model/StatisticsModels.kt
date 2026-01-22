package com.example.lifequest.model

import com.example.lifequest.QuestCategory

// 統計表示用のデータ構造
data class StatisticsData(
    val totalQuests: Int = 0,
    val totalTime: Long = 0L,
    val categoryBreakdown: List<CategoryStats> = emptyList(),
    val weeklyActivity: List<DailyStats> = emptyList()
)

data class CategoryStats(
    val category: QuestCategory,
    val count: Int,
    val duration: Long,
    val percentage: Float
)

data class DailyStats(
    val dateLabel: String,
    val dayOfWeek: String,
    val totalTime: Long,
    val categoryTimes: Map<QuestCategory, Long>
)