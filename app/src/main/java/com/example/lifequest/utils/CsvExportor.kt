package com.example.lifequest.utils

import android.content.Context
import android.net.Uri
import com.example.lifequest.data.local.entity.QuestLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class CsvExporter(private val context: Context) {

    suspend fun export(uri: Uri, logs: List<QuestLog>) = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                val writer = BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8))
                // BOM (文字化け防止)
                writer.write("\uFEFF")
                // ヘッダー
                writer.write("ID,クエスト名,難易度,目標時間(秒),実績時間(秒),完了日\n")

                // データ
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
}