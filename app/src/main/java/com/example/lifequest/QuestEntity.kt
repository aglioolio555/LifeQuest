package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val dueDate: Long? = null,

    // ★追加: 目安時間 (ミリ秒)
    val estimatedTime: Long = 0L,

    val expReward: Int,
    val goldReward: Int,
    val difficulty: Int = 1,
    val repeatMode: Int = 0,
    val accumulatedTime: Long = 0L,
    val lastStartTime: Long? = null,
    val isCompleted: Boolean = false
)