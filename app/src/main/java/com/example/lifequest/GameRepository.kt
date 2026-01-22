package com.example.lifequest

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(private val dao: UserDao) {

    // データの監視
    val userStatus: Flow<UserStatus?> = dao.getUserStatus()
    val activeQuests: Flow<List<QuestWithSubtasks>> = dao.getActiveQuests()

    // --- UserStatus ---
    suspend fun insertUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        dao.insert(status)
    }

    suspend fun updateUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        dao.update(status)
    }
    // 最新のユーザーステータスを1回だけ同期的に取得する
    suspend fun getUserStatusSync(): UserStatus? = withContext(Dispatchers.IO) {
        dao.getUserStatusSync()
    }

    // --- Quest ---
    suspend fun insertQuest(quest: Quest, subtasks: List<String>) = withContext(Dispatchers.IO) {
        val questId = dao.insertQuest(quest).toInt()
        subtasks.forEach { title ->
            if (title.isNotBlank()) {
                dao.insertSubtask(Subtask(questId = questId, title = title))
            }
        }
    }

    suspend fun updateQuest(quest: Quest) = withContext(Dispatchers.IO) {
        dao.updateQuest(quest)
    }

    suspend fun deleteQuest(quest: Quest) = withContext(Dispatchers.IO) {
        dao.deleteQuest(quest)
    }

    // --- Subtask ---
    suspend fun insertSubtask(questId: Int, title: String) = withContext(Dispatchers.IO) {
        dao.insertSubtask(Subtask(questId = questId, title = title))
    }

    suspend fun updateSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        dao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        dao.deleteSubtask(subtask)
    }

    // --- QuestLog ---
    suspend fun insertQuestLog(log: QuestLog) = withContext(Dispatchers.IO) {
        dao.insertQuestLog(log)
    }

    // --- CSV Export ---
    suspend fun exportLogsToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val logs = dao.getAllLogsSync()
        CsvExporter(context).export(uri, logs)
    }
}