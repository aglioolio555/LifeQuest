package com.example.lifequest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
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
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC, -- 1. まず「期限あり」を最優先
            dueDate ASC,                                     -- 2. 次に「日付が近い順」
            CASE WHEN repeatMode = 0 THEN 0 ELSE 1 END ASC,  -- 3. 日付が同じなら「通常」を優先
            id DESC                                          -- 4. 最後に「新しい順」
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
}