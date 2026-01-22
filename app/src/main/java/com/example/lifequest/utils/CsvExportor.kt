package com.example.lifequest.utils

import android.content.Context
import android.net.Uri
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.QuestLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class CsvExporter(private val context: Context) {

    // 既存のクエストログ出力
    suspend fun exportQuestLog(uri: Uri, logs: List<QuestLog>) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8))
                writer.write("\uFEFF") // BOM
                writer.write("ID,クエスト名,難易度,目標時間(秒),実績時間(秒),完了日\n")

                for (log in logs) {
                    val safeTitle = "\"${log.title.replace("\"", "\"\"")}\""
                    val estimatedSec = log.estimatedTime / 1000
                    val actualSec = log.actualTime / 1000
                    val dateStr = formatDate(log.completedAt)

                    val line = "${log.id},$safeTitle,$estimatedSec,$actualSec,$dateStr\n"
                    writer.write(line)
                }
                writer.flush()
            }
        }.onFailure { e ->
            e.printStackTrace()
        }
    }

    suspend fun exportDailyProgress(uri: Uri, progressList: List<DailyQuestProgress>) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8))
                writer.write("\uFEFF") // BOM
                // ヘッダー: 日付, 起床達成, 就寝達成, 集中時間(秒), 報酬ランク, 達成カテゴリID
                writer.write("日付,起床達成,就寝達成,集中時間(秒),報酬ランク,達成カテゴリID\n")

                for (item in progressList) {
                    val dateStr = formatDate(item.date)
                    val wakeUp = if (item.isWakeUpCleared) "1" else "0"
                    val bedTime = if (item.isBedTimeCleared) "1" else "0"
                    val focusSec = item.totalFocusTime / 1000
                    val tier = item.focusRewardTier
                    val categories = "\"${item.clearedCategoryIds}\"" // カンマを含む可能性があるためクォート

                    val line = "$dateStr,$wakeUp,$bedTime,$focusSec,$tier,$categories\n"
                    writer.write(line)
                }
                writer.flush()
            }
        }.onFailure { e ->
            e.printStackTrace()
        }
    }
}