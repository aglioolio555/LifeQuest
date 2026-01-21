package com.example.lifequest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // --- ユーザーステータス ---
    // Flowはもともと非同期ストリームなので suspend は不要（そのままでOK）
    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatus(): Flow<UserStatus?>

    // ★修正: suspend を削除し、戻り値を Long にする
    @Insert
    fun insert(status: UserStatus): Long

    // ★修正: suspend を削除し、戻り値を Int にする
    @Update
    fun update(status: UserStatus): Int

    // ★修正: suspend を削除し、戻り値を Int にする
    @Query("DELETE FROM user_status")
    fun deleteUserStatus(): Int

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

    // ★修正: suspend を削除、戻り値 Long
    @Insert
    fun insertQuest(quest: Quest): Long

    // ★修正: suspend を削除、戻り値 Int
    @Update
    fun updateQuest(quest: Quest): Int

    // ★修正: suspend を削除、戻り値 Int
    @Delete
    fun deleteQuest(quest: Quest): Int

    // ★修正: suspend を削除、戻り値 Int
    @Query("DELETE FROM quests")
    fun deleteAllQuests(): Int

    // --- 履歴（ログ）用 ---

    // ★修正: suspend を削除、戻り値 Long
    @Insert
    fun insertQuestLog(log: QuestLog): Long

    @Query("SELECT * FROM quest_logs ORDER BY completedAt DESC")
    fun getAllQuestLogs(): Flow<List<QuestLog>>

    // --- バックアップ・復元用（同期取得） ---
    // これらは suspend なしでリストを返す通常のメソッドにします

    @Query("SELECT * FROM quests")
    fun getAllQuestsSync(): List<Quest>

    @Query("SELECT * FROM quest_logs")
    fun getAllLogsSync(): List<QuestLog>

    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatusSync(): UserStatus?

    // ★修正: suspend を削除、戻り値 Int
    @Query("DELETE FROM quest_logs")
    fun deleteAllLogs(): Int

    // ★修正: suspend を削除、戻り値 List<Long>
    @Insert
    fun insertQuests(quests: List<Quest>): List<Long>

    // ★修正: suspend を削除、戻り値 List<Long>
    @Insert
    fun insertLogs(logs: List<QuestLog>): List<Long>
}