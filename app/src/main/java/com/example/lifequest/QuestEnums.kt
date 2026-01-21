package com.example.lifequest

import java.util.Calendar

// 難易度と報酬定義
enum class QuestDifficulty(val value: Int, val exp: Int, val gold: Int) {
    EASY(0, 10, 5),
    NORMAL(1, 30, 15),
    HARD(2, 100, 50);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: NORMAL
    }
}

// リピートモードと次回日付計算
enum class RepeatMode(val value: Int) {
    NONE(0),
    DAILY(1),
    WEEKLY(2),
    MONTHLY(3);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: NONE
    }

    // 次回の期限日を計算するメソッド
    fun calculateNextDueDate(currentDate: Long): Long {
        if (this == NONE) return currentDate

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        when (this) {
            DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            MONTHLY -> calendar.add(Calendar.MONTH, 1)
            else -> {}
        }
        return calendar.timeInMillis
    }
}