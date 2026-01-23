package com.example.lifequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_quests")
data class ExtraQuest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val estimatedTime: Long = 15 * 60 * 1000L, // デフォルト15分
    val expReward: Int = 50 // 高めの報酬
)