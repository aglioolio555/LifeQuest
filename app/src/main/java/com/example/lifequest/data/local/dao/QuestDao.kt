package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    // --- クエスト ---
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

    @Query("SELECT * FROM quests")
    fun getAllQuestsSync(): List<Quest>

    @Insert
    fun insertQuest(quest: Quest): Long

    @Insert
    fun insertQuests(quests: List<Quest>): List<Long>

    @Update
    fun updateQuest(quest: Quest): Int

    @Delete
    fun deleteQuest(quest: Quest): Int

    @Query("DELETE FROM quests")
    fun deleteAllQuests(): Int

    // --- サブタスク ---
    @Insert
    fun insertSubtask(subtask: Subtask): Long

    @Update
    fun updateSubtask(subtask: Subtask): Int

    @Delete
    fun deleteSubtask(subtask: Subtask): Int
}