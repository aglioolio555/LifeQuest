package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_logs")
data class QuestLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,          // クエスト名
    val difficulty: Int,        // 難易度
    val estimatedTime: Long,    // 目安時間 (目標)
    val actualTime: Long,       // 実際にかかった時間 (実績)
    val category: Int = 0,
    val completedAt: Long = System.currentTimeMillis() // 完了日時
)