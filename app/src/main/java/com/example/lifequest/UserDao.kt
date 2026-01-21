package com.example.lifequest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // --- ユーザー用 ---
    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatus(): Flow<UserStatus?>

    @Insert
    fun insert(status: UserStatus): Long

    @Update
    fun update(status: UserStatus): Int

    // --- クエスト（タスク）用 ---
    @Query("""
        SELECT * FROM quests 
        WHERE isCompleted = 0 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC,
            dueDate ASC,
            CASE WHEN repeatMode = 0 THEN 0 ELSE 1 END ASC,
            id DESC
    """)
    fun getActiveQuests(): Flow<List<Quest>>

    @Insert
    fun insertQuest(quest: Quest): Long

    @Delete
    fun deleteQuest(quest: Quest): Int

    @Update
    fun updateQuest(quest: Quest): Int

    // --- 履歴（ログ）用 ---
    @Insert
    fun insertQuestLog(log: QuestLog)

    @Query("SELECT * FROM quest_logs ORDER BY completedAt DESC")
    fun getAllQuestLogs(): Flow<List<QuestLog>>

    // ★重要: CSV出力用
    // @JvmSuppressWildcards をつけることで、生成されるJavaコードとの型の不一致を防ぎます
    @Query("SELECT * FROM quest_logs ORDER BY completedAt DESC")
    @JvmSuppressWildcards
    suspend fun getQuestLogsList(): List<QuestLog>
}