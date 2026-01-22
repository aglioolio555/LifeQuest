package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_logs")
data class QuestLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val estimatedTime: Long,
    val actualTime: Long,
    val category: Int,
    val completedAt: Long
)