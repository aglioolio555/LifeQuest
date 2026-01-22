package com.example.lifequest

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository(
    private val userDao: UserDao,
    private val breakActivityDao: BreakActivityDao
) {

    // --- User / Quest Data ---
    val userStatus: Flow<UserStatus?> = userDao.getUserStatus()
    val activeQuests: Flow<List<QuestWithSubtasks>> = userDao.getActiveQuests()

    // ★追加: 全クエストログの監視
    val questLogs: Flow<List<QuestLog>> = userDao.getAllQuestLogs()

    // --- Break Activity Data ---
    val allBreakActivities: Flow<List<BreakActivity>> = breakActivityDao.getAll()

    suspend fun getBreakActivityCount(): Int = withContext(Dispatchers.IO) {
        breakActivityDao.getCount()
    }
    // ... (以下、既存コードと同じため省略)
    suspend fun insertBreakActivity(activity: BreakActivity): Long = withContext(Dispatchers.IO) {
        breakActivityDao.insert(activity)
    }

    suspend fun deleteBreakActivity(activity: BreakActivity): Int = withContext(Dispatchers.IO) {
        breakActivityDao.delete(activity)
    }

    suspend fun getUserStatusSync(): UserStatus? = withContext(Dispatchers.IO) {
        userDao.getUserStatusSync()
    }

    suspend fun insertUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userDao.insert(status)
    }

    suspend fun updateUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userDao.update(status)
    }

    suspend fun insertQuest(quest: Quest, subtasks: List<String>) = withContext(Dispatchers.IO) {
        val questId = userDao.insertQuest(quest).toInt()
        subtasks.forEach { title ->
            if (title.isNotBlank()) {
                userDao.insertSubtask(Subtask(questId = questId, title = title))
            }
        }
    }

    suspend fun updateQuest(quest: Quest) = withContext(Dispatchers.IO) {
        userDao.updateQuest(quest)
    }

    suspend fun deleteQuest(quest: Quest) = withContext(Dispatchers.IO) {
        userDao.deleteQuest(quest)
    }

    suspend fun insertSubtask(questId: Int, title: String) = withContext(Dispatchers.IO) {
        userDao.insertSubtask(Subtask(questId = questId, title = title))
    }

    suspend fun updateSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        userDao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        userDao.deleteSubtask(subtask)
    }

    suspend fun insertQuestLog(log: QuestLog) = withContext(Dispatchers.IO) {
        userDao.insertQuestLog(log)
    }

    suspend fun exportLogsToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val logs = userDao.getAllLogsSync()
        CsvExporter(context).export(uri, logs)
    }
}