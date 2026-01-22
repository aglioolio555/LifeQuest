package com.example.lifequest.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.dao.BreakActivityDao
import com.example.lifequest.utils.CsvExporter
import com.example.lifequest.data.local.dao.DailyQuestDao
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.local.dao.UserDao
import com.example.lifequest.data.local.entity.UserStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MainRepository(
    private val userDao: UserDao,
    private val breakActivityDao: BreakActivityDao,
    private val dailyQuestDao: DailyQuestDao
) {

    // --- User / Quest Data ---
    val userStatus: Flow<UserStatus?> = userDao.getUserStatus()
    val activeQuests: Flow<List<QuestWithSubtasks>> = userDao.getActiveQuests()
    val questLogs: Flow<List<QuestLog>> = userDao.getAllQuestLogs()
    // --- Daily Quest (★修正箇所) ---
    fun getDailyProgressFlow(date: Long): Flow<DailyQuestProgress?> = dailyQuestDao.getProgressFlow(date)

    suspend fun getDailyProgress(date: Long): DailyQuestProgress? = withContext(Dispatchers.IO) {
        dailyQuestDao.getProgress(date)
    }

    // ★修正: 戻り値 (: Long) を明示
    suspend fun insertDailyProgress(progress: DailyQuestProgress): Long =
        withContext(Dispatchers.IO) {
            dailyQuestDao.insert(progress)
        }

    // ★修正: 戻り値 (: Int) を明示
    suspend fun updateDailyProgress(progress: DailyQuestProgress): Int =
        withContext(Dispatchers.IO) {
            dailyQuestDao.update(progress)
        }

    // --- Break Activity Data ---
    val allBreakActivities: Flow<List<BreakActivity>> = breakActivityDao.getAll()

    // ... (以下、既存コードはそのまま) ...

    suspend fun getBreakActivityCount(): Int = withContext(Dispatchers.IO) {
        breakActivityDao.getCount()
    }

    suspend fun insertBreakActivity(activity: BreakActivity): Long = withContext(Dispatchers.IO) {
        breakActivityDao.insert(activity)
    }

    suspend fun deleteBreakActivity(activity: BreakActivity): Int = withContext(Dispatchers.IO) {
        breakActivityDao.delete(activity)
    }

    // --- UserStatus ---
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