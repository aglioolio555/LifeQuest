package com.example.lifequest.logic

class RewardCalculator {
    companion object {
        private const val EXP_PER_MINUTE_FACTOR = 1.67
        private const val MIN_EXP_REWARD = 10
        private const val DEFAULT_EXP_REWARD = 25
    }

    /**
     * 見積もり時間に基づいて獲得経験値を計算する
     */
    fun calculateExp(estimatedTime: Long): Int {
        return if (estimatedTime > 0) {
            val minutes = estimatedTime / (1000 * 60)
            (minutes * EXP_PER_MINUTE_FACTOR).toInt().coerceAtLeast(MIN_EXP_REWARD)
        } else {
            DEFAULT_EXP_REWARD
        }
    }
}