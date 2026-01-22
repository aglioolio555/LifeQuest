package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val note: String = "",
    val dueDate: Long? = null,
    val estimatedTime: Long = 0L,
    val accumulatedTime: Long = 0L,
    val lastStartTime: Long? = null,
    val expReward: Int,
    val repeatMode: Int = 0,
    val category: Int = 0,
    val isCompleted: Boolean = false
)