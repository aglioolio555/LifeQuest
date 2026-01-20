package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_status")
data class UserStatus(
    @PrimaryKey val id: Int = 0, // プレイヤーは1人なのでID固定
    val level: Int = 1,
    val currentExp: Int = 0,
    val maxExp: Int = 100, // 次のレベルまでに必要な経験値
    val gold: Int = 0
)
