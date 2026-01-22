package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lifequest.data.local.entity.QuestLog
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestLogDao {
    @Insert
    fun insertQuestLog(log: QuestLog): Long

    @Insert
    fun insertLogs(logs: List<QuestLog>): List<Long>

    @Query("SELECT * FROM quest_logs ORDER BY completedAt DESC")
    fun getAllQuestLogs(): Flow<List<QuestLog>>

    @Query("SELECT * FROM quest_logs")
    fun getAllLogsSync(): List<QuestLog>

    @Query("DELETE FROM quest_logs")
    fun deleteAllLogs(): Int
}