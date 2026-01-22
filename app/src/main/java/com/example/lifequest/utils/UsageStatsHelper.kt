package com.example.lifequest.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    // 権限が許可されているか確認
    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 指定期間の画面点灯時間をミリ秒で取得
    fun getScreenOnTime(startTime: Long, endTime: Long): Long {
        if (!hasPermission()) return 0L

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return 0L

        var totalScreenOnTime = 0L
        var lastScreenOnTimestamp: Long? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                // 画面点灯（操作開始）
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    lastScreenOnTimestamp = event.timeStamp
                }
                // 画面消灯（操作終了）
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (lastScreenOnTimestamp != null) {
                        totalScreenOnTime += (event.timeStamp - lastScreenOnTimestamp)
                        lastScreenOnTimestamp = null
                    }
                }
            }
        }

        // 期間終了時にまだ画面が点灯していた場合、期間終了までの時間を加算
        if (lastScreenOnTimestamp != null && lastScreenOnTimestamp < endTime) {
            totalScreenOnTime += (endTime - lastScreenOnTimestamp)
        }

        return totalScreenOnTime
    }
}