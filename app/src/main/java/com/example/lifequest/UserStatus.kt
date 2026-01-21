package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_status")
data class UserStatus(
    @PrimaryKey val id: Int = 0,
    val level: Int = 1,
    val currentExp: Int = 0,
    val maxExp: Int = 100,
    val gold: Int = 0
) {
    // 経験値を加算し、レベルアップ後の新しいステータスを返す
    fun addExperience(amount: Int, goldReward: Int): UserStatus {
        var newExp = this.currentExp + amount
        var newLevel = this.level
        var newMaxExp = this.maxExp
        val newGold = this.gold + goldReward

        // レベルアップ計算
        while (newExp >= newMaxExp) {
            newExp -= newMaxExp
            newLevel++
            // 必要経験値の増加計算 (例: レベル * 100 * 1.2)
            newMaxExp = (newLevel * 100 * 1.2).toInt()
        }

        return this.copy(
            level = newLevel,
            currentExp = newExp,
            maxExp = newMaxExp,
            gold = newGold
        )
    }
}