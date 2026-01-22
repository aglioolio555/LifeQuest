package com.example.lifequest

import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// 難易度と報酬定義
enum class QuestDifficulty(val value: Int, val exp: Int, val gold: Int) {
    EASY(0, 10, 5),
    NORMAL(1, 30, 15),
    HARD(2, 100, 50);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: NORMAL
    }
}
// カテゴリ定義
enum class QuestCategory(val id: Int, val label: String, val icon: ImageVector, val color: Color) {
    LIFE(0, "生活", Icons.Default.Home, Color(0xFF4CAF50)),       // 緑
    WORK(1, "仕事/学習", Icons.Default.Edit, Color(0xFF2196F3)),   // 青
    HEALTH(2, "健康", Icons.Default.Favorite, Color(0xFFE91E63)), // ピンク
    HOBBY(3, "趣味", Icons.Default.Star, Color(0xFFFFC107)),      // 黄色
    OTHER(4, "その他", Icons.Default.Face, Color(0xFF9E9E9E));    // グレー

    companion object {
        fun fromInt(value: Int) = entries.find { it.id == value } ?: OTHER
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
enum class FocusMode(val minutes: Int, val breakMinutes: Int, val label: String) {
    RUSH(25, 5, "Rush (25m)"),       // 25分集中 + 5分休憩
    DEEP_DIVE(45, 10, "Deep (45m)"), // 45分集中 + 10分休憩
    COUNT_UP(0, 0, "Free"),          // カウントアップ（無制限）
    BREAK(0, 0, "Break");            // 休憩中

    // 次のモードへ切り替え（ユーザー操作用）
    fun next(): FocusMode {
        return when (this) {
            RUSH -> DEEP_DIVE
            DEEP_DIVE -> COUNT_UP
            COUNT_UP -> RUSH
            BREAK -> RUSH // 休憩明けはRushへ
        }
    }
}
