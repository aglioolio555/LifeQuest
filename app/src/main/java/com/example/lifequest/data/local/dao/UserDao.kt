package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.local.entity.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // --- ユーザーステータス ---
    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatus(): Flow<UserStatus?>

    @Insert
    fun insert(status: UserStatus): Long

    @Update
    fun update(status: UserStatus): Int

    @Query("DELETE FROM user_status")
    fun deleteUserStatus(): Int

    // --- クエスト（タスク）用 ---

    // ★修正: 戻り値を QuestWithSubtasks に変更し、@Transactionを追加
    @Transaction
    @Query("""
        SELECT * FROM quests 
        WHERE isCompleted = 0 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, 
            dueDate ASC, 
            CASE WHEN repeatMode = 0 THEN 0 ELSE 1 END ASC, 
            id DESC
    """)
    fun getActiveQuests(): Flow<List<QuestWithSubtasks>>

    @Insert
    fun insertQuest(quest: Quest): Long

    @Update
    fun updateQuest(quest: Quest): Int

    @Delete
    fun deleteQuest(quest: Quest): Int

    @Query("DELETE FROM quests")
    fun deleteAllQuests(): Int

    // --- ★追加: サブタスク用 ---
    @Insert
    fun insertSubtask(subtask: Subtask): Long

    @Update
    fun updateSubtask(subtask: Subtask): Int

    @Delete
    fun deleteSubtask(subtask: Subtask): Int

    // --- 履歴（ログ）用 ---
    @Insert
    fun insertQuestLog(log: QuestLog): Long

    @Query("SELECT * FROM quest_logs ORDER BY completedAt DESC")
    fun getAllQuestLogs(): Flow<List<QuestLog>>

    // --- バックアップ・復元用 ---
    @Query("SELECT * FROM quests")
    fun getAllQuestsSync(): List<Quest>

    @Query("SELECT * FROM quest_logs")
    fun getAllLogsSync(): List<QuestLog>

    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatusSync(): UserStatus?

    @Query("DELETE FROM quest_logs")
    fun deleteAllLogs(): Int

    @Insert
    fun insertQuests(quests: List<Quest>): List<Long>

    @Insert
    fun insertLogs(logs: List<QuestLog>): List<Long>
}