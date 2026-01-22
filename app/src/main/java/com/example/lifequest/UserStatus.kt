package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.floor
import kotlin.math.pow

@Entity(tableName = "user_status")
data class UserStatus(
    @PrimaryKey val id: Int = 0,
    val level: Int = 1,
    val experience: Int = 0
) {
    val nextLevelExp: Int
        get() = floor(100 * 1.5.pow(level - 1)).toInt()

    fun addExperience(exp: Int): UserStatus {
        var newExp = experience + exp
        var newLevel = level

        // レベルアップ計算
        while (newExp >= floor(100 * 1.5.pow(newLevel - 1)).toInt()) {
            newExp -= floor(100 * 1.5.pow(newLevel - 1)).toInt()
            newLevel++
        }
        return this.copy(level = newLevel, experience = newExp)
    }
}