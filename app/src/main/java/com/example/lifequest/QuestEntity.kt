package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val dueDate: Long? = null,
    val expReward: Int,
    val goldReward: Int,
    val difficulty: Int = 1,
    val repeatMode: Int = 0,

    // ★追加: タイマー機能用
    val accumulatedTime: Long = 0L,   // 累積時間 (ms)
    val lastStartTime: Long? = null,  // 計測開始時刻 (nullなら停止中)

    val isCompleted: Boolean = false
)