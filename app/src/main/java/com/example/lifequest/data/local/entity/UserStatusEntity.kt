package com.example.lifequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.floor
import kotlin.math.pow

@Entity(tableName = "user_status")
data class UserStatus(
    @PrimaryKey val id: Int = 0,
    val level: Int = 1,
    val experience: Int = 0,
    //デイリークエスト設定用 (デフォルト 7:00 起床, 23:00 就寝)
    val targetWakeUpHour: Int = 7,
    val targetWakeUpMinute: Int = 0,
    val targetBedTimeHour: Int = 23,
    val targetBedTimeMinute: Int = 0
) {
    val nextLevelExp: Int
        get() = floor(100 * 1.5.pow(level - 1)).toInt()

    fun addExperience(exp: Int): UserStatus {
        var newExp = experience + exp
        var newLevel = level

        while (newExp >= floor(100 * 1.5.pow(newLevel - 1)).toInt()) {
            newExp -= floor(100 * 1.5.pow(newLevel - 1)).toInt()
            newLevel++
        }
        return this.copy(level = newLevel, experience = newExp)
    }
}