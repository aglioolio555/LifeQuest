package com.example.lifequest.logic

import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.data.repository.GameRepository
import com.example.lifequest.utils.UsageStatsHelper
import java.util.Calendar

class DailyQuestManager(
    private val repository: GameRepository,
    private val usageStatsHelper: UsageStatsHelper
) {
    companion object {
        private const val WAKE_UP_EXP = 30
        private const val BEDTIME_EXP = 50
        private const val CATEGORY_EXP = 20
        private const val EARLIEST_WAKE_UP_WINDOW_MINUTES = 180L // 3時間
    }

    // 今日の0:00を取得
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 起床クエストの判定
     * @return 獲得経験値（クリア時のみ正の値）
     */
    suspend fun checkWakeUp(status: UserStatus): Int {
        val todayStart = getTodayStartMillis()
        // DBから取得、なければ新規作成（insertはViewModelの初期化フローで行われる前提だが、念のため）
        var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

        if (progress.isWakeUpCleared) return 0

        val now = Calendar.getInstance()
        val targetTimeMinutes = status.targetWakeUpHour * 60 + status.targetWakeUpMinute
        val currentTimeMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // 目標時刻の3時間前 ～ 目標時刻 までを「起床成功」とみなす
        val startTimeWindow = targetTimeMinutes - EARLIEST_WAKE_UP_WINDOW_MINUTES

        if (currentTimeMinutes in startTimeWindow..targetTimeMinutes.toLong()) {
            progress = progress.copy(isWakeUpCleared = true)
            // 既に存在する場合はupdate、なければinsertが必要だが、
            // ViewModelのFlowで初期データを作成済みと仮定してupdateを使用
            if (repository.getDailyProgress(todayStart) != null) {
                repository.updateDailyProgress(progress)
            } else {
                repository.insertDailyProgress(progress)
            }
            return WAKE_UP_EXP
        }
        return 0
    }

    /**
     * 就寝クエストの判定
     */
    suspend fun checkBedtime(status: UserStatus): Int {
        if (!usageStatsHelper.hasPermission()) return 0

        val todayStart = getTodayStartMillis()
        var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

        if (progress.isBedTimeCleared) return 0

        val calendar = Calendar.getInstance()
        val wakeUpTarget = calendar.apply {
            timeInMillis = todayStart
            set(Calendar.HOUR_OF_DAY, status.targetWakeUpHour)
            set(Calendar.MINUTE, status.targetWakeUpMinute)
        }.timeInMillis

        val bedTimeTarget = calendar.apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, status.targetBedTimeHour)
            set(Calendar.MINUTE, status.targetBedTimeMinute)
        }.timeInMillis

        val now = System.currentTimeMillis()
        // まだ今日の起床目標時刻を過ぎていなければ判定しない（朝起きたタイミングで判定する想定）
        if (now < wakeUpTarget) return 0

        val screenOnTimeMillis = usageStatsHelper.getScreenOnTime(bedTimeTarget, wakeUpTarget)
        val limitMillis = 5 * 60 * 1000L // 5分

        if (screenOnTimeMillis < limitMillis) {
            progress = progress.copy(isBedTimeCleared = true)
            if (repository.getDailyProgress(todayStart) != null) {
                repository.updateDailyProgress(progress)
            } else {
                repository.insertDailyProgress(progress)
            }
            return BEDTIME_EXP
        }
        return 0
    }

    /**
     * 集中時間の加算と報酬判定
     */
    suspend fun addFocusTime(addedTime: Long): Int {
        if (addedTime <= 0) return 0

        val todayStart = getTodayStartMillis()
        val progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

        val newTotalTime = progress.totalFocusTime + addedTime
        var newTier = progress.focusRewardTier
        var expToAdd = 0

        val hours = newTotalTime / (1000 * 60 * 60)
        if (newTier < 1 && hours >= 1) { newTier = 1; expToAdd += 50 }
        if (newTier < 2 && hours >= 3) { newTier = 2; expToAdd += 100 }
        if (newTier < 3 && hours >= 5) { newTier = 3; expToAdd += 200 }
        if (newTier < 4 && hours >= 10) { newTier = 4; expToAdd += 500 }

        if (newTier != progress.focusRewardTier || addedTime > 0) {
            val newProgress = progress.copy(
                totalFocusTime = newTotalTime,
                focusRewardTier = newTier
            )
            if (repository.getDailyProgress(todayStart) != null) {
                repository.updateDailyProgress(newProgress)
            } else {
                repository.insertDailyProgress(newProgress)
            }
        }
        return expToAdd
    }

    /**
     * カテゴリー達成判定
     */
    suspend fun checkCategoryComplete(category: Int): Int {
        val todayStart = getTodayStartMillis()
        var progress = repository.getDailyProgress(todayStart) ?: DailyQuestProgress(date = todayStart)

        if (!progress.hasCategoryCleared(category)) {
            progress = progress.addClearedCategory(category)
            if (repository.getDailyProgress(todayStart) != null) {
                repository.updateDailyProgress(progress)
            } else {
                repository.insertDailyProgress(progress)
            }
            return CATEGORY_EXP
        }
        return 0
    }
}