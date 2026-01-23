package com.example.lifequest.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifequest.utils.CsvExporter
import com.example.lifequest.data.local.dao.*
import com.example.lifequest.data.local.entity.*
import com.example.lifequest.model.QuestWithSubtasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MainRepository(
    private val userStatusDao: UserStatusDao, // 変更
    private val questDao: QuestDao,           // 変更
    private val questLogDao: QuestLogDao,     // 変更
    private val breakActivityDao: BreakActivityDao,
    private val dailyQuestDao: DailyQuestDao,
    private val extraQuestDao: ExtraQuestDao,
) {

    // --- User Status Data ---
    val userStatus: Flow<UserStatus?> = userStatusDao.getUserStatus()

    // --- Quest Data ---
    val activeQuests: Flow<List<QuestWithSubtasks>> = questDao.getActiveQuests()

    // --- Log Data ---
    val questLogs: Flow<List<QuestLog>> = questLogDao.getAllQuestLogs()

    // --- Daily Quest ---
    fun getDailyProgressFlow(date: Long): Flow<DailyQuestProgress?> = dailyQuestDao.getProgressFlow(date)

    suspend fun getDailyProgress(date: Long): DailyQuestProgress? = withContext(Dispatchers.IO) {
        dailyQuestDao.getProgress(date)
    }

    suspend fun insertDailyProgress(progress: DailyQuestProgress): Long = withContext(Dispatchers.IO) {
        dailyQuestDao.insert(progress)
    }

    suspend fun updateDailyProgress(progress: DailyQuestProgress): Int = withContext(Dispatchers.IO) {
        dailyQuestDao.update(progress)
    }

    // --- Break Activity Data ---
    val allBreakActivities: Flow<List<BreakActivity>> = breakActivityDao.getAll()

    suspend fun getBreakActivityCount(): Int = withContext(Dispatchers.IO) {
        breakActivityDao.getCount()
    }

    suspend fun insertBreakActivity(activity: BreakActivity): Long = withContext(Dispatchers.IO) {
        breakActivityDao.insert(activity)
    }

    suspend fun deleteBreakActivity(activity: BreakActivity): Int = withContext(Dispatchers.IO) {
        breakActivityDao.delete(activity)
    }

    // --- UserStatus Logic ---
    suspend fun getUserStatusSync(): UserStatus? = withContext(Dispatchers.IO) {
        userStatusDao.getUserStatusSync()
    }

    suspend fun insertUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userStatusDao.insert(status)
    }

    suspend fun updateUserStatus(status: UserStatus) = withContext(Dispatchers.IO) {
        userStatusDao.update(status)
    }

    // --- Quest Logic ---
    suspend fun insertQuest(quest: Quest, subtasks: List<String>) = withContext(Dispatchers.IO) {
        val questId = questDao.insertQuest(quest).toInt()
        subtasks.forEach { title ->
            if (title.isNotBlank()) {
                questDao.insertSubtask(Subtask(questId = questId, title = title))
            }
        }
    }

    suspend fun updateQuest(quest: Quest) = withContext(Dispatchers.IO) {
        questDao.updateQuest(quest)
    }

    suspend fun deleteQuest(quest: Quest) = withContext(Dispatchers.IO) {
        questDao.deleteQuest(quest)
    }

    // --- Subtask Logic ---
    suspend fun insertSubtask(questId: Int, title: String) = withContext(Dispatchers.IO) {
        questDao.insertSubtask(Subtask(questId = questId, title = title))
    }

    suspend fun updateSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        questDao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        questDao.deleteSubtask(subtask)
    }

    // --- QuestLog Logic ---
    suspend fun insertQuestLog(log: QuestLog) = withContext(Dispatchers.IO) {
        questLogDao.insertQuestLog(log)
    }

    // --- CSV Export ---
    suspend fun exportLogsToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val logs = questLogDao.getAllLogsSync()
        CsvExporter(context).exportQuestLog(uri, logs)
    }
    suspend fun exportDailyQuestsToCsv(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        val allProgress = dailyQuestDao.getAllSync()
        CsvExporter(context).exportDailyProgress(uri, allProgress)
    }

    // --- Extra Quest Logic ---
    val allExtraQuests: Flow<List<ExtraQuest>> = extraQuestDao.getAll()

    suspend fun getRandomExtraQuest(): ExtraQuest? = withContext(Dispatchers.IO) {
        extraQuestDao.getRandom()
    }

    suspend fun insertExtraQuest(quest: ExtraQuest) = withContext(Dispatchers.IO) {
        extraQuestDao.insert(quest)
    }

    suspend fun deleteExtraQuest(quest: ExtraQuest) = withContext(Dispatchers.IO) {
        extraQuestDao.delete(quest)
    }
}