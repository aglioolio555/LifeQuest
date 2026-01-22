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

    // --- Break Activity Data ---
    val allBreakActivities: Flow<List<BreakActivity>> = breakActivityDao.getAll()

    suspend fun getBreakActivityCount(): Int = withContext(Dispatchers.IO) {
        breakActivityDao.getCount()
    }

    // ★修正: DAOに合わせて戻り値 Long を明示
    suspend fun insertBreakActivity(activity: BreakActivity): Long = withContext(Dispatchers.IO) {
        breakActivityDao.insert(activity)
    }

    // ★修正: DAOに合わせて戻り値 Int を明示
    suspend fun deleteBreakActivity(activity: BreakActivity): Int = withContext(Dispatchers.IO) {
        breakActivityDao.delete(activity)
    }

    // --- UserStatus ---
    // 最新のユーザーステータスを1回だけ同期的に取得する
    suspend fun getUserStatusSync(): UserStatus? = withContext(Dispatchers.IO) {
        userDao.getUserStatusSync()
    }

    suspend fun insertUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userDao.insert(status)
    }

    suspend fun updateUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userDao.update(status)
    }

    // --- Quest ---
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

    // --- Subtask ---
    suspend fun insertSubtask(questId: Int, title: String) = withContext(Dispatchers.IO) {
        userDao.insertSubtask(Subtask(questId = questId, title = title))
    }

    suspend fun updateSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        userDao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        userDao.deleteSubtask(subtask)
    }

    // --- QuestLog ---
    suspend fun insertQuestLog(log: QuestLog) = withContext(Dispatchers.IO) {
        userDao.insertQuestLog(log)
    }

    // --- CSV Export ---
    suspend fun exportLogsToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val logs = userDao.getAllLogsSync()
        CsvExporter(context).export(uri, logs)
    }
}