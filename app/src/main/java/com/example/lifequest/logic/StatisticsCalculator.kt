package com.example.lifequest.logic

import com.example.lifequest.QuestCategory
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.model.CategoryStats
import com.example.lifequest.model.DailyStats
import com.example.lifequest.model.StatisticsData
import java.util.Calendar

class StatisticsCalculator {

    fun calculate(logs: List<QuestLog>): StatisticsData {
        if (logs.isEmpty()) return StatisticsData()

        // 1. 全体累計
        val totalQuests = logs.size
        val totalTime = logs.sumOf { it.actualTime }

        // 2. カテゴリー別集計
        val categoryMap = logs.groupBy { it.category }
        val breakdown = QuestCategory.entries.map { category ->
            val categoryLogs = categoryMap[category.id] ?: emptyList()
            val duration = categoryLogs.sumOf { it.actualTime }
            CategoryStats(
                category,
                categoryLogs.size,
                duration,
                if (totalTime > 0) duration.toFloat() / totalTime.toFloat() else 0f
            )
        }.sortedByDescending { it.duration }

        // 3. 週次グラフデータ (直近7日間)
        val calendar = Calendar.getInstance()
        // 時間をリセット
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 6日前から開始
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val weeklyData = mutableListOf<DailyStats>()
        for (i in 0..6) {
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val end = calendar.timeInMillis

            val dailyLogs = logs.filter { it.completedAt in start until end }
            val dailyTotal = dailyLogs.sumOf { it.actualTime }
            val dailyCatMap = dailyLogs.groupBy { it.category }
                .mapKeys { QuestCategory.fromInt(it.key) }
                .mapValues { it.value.sumOf { log -> log.actualTime } }

            // 日付ラベル生成用にカレンダーを戻して取得
            val dCal = Calendar.getInstance().apply { timeInMillis = start }
            val label = "${dCal.get(Calendar.MONTH) + 1}/${dCal.get(Calendar.DAY_OF_MONTH)}"
            val dow = when(dCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"; Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"; Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"; else -> ""
            }
            weeklyData.add(DailyStats(label, dow, dailyTotal, dailyCatMap))
        }

        return StatisticsData(totalQuests, totalTime, breakdown, weeklyData)
    }
}