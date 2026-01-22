package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lifequest.data.local.entity.DailyQuestProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {
    @Query("SELECT * FROM daily_quest_progress WHERE date = :date LIMIT 1")
    fun getProgressFlow(date: Long): Flow<DailyQuestProgress?>
    @Query("SELECT * FROM daily_quest_progress WHERE date = :date LIMIT 1")
    fun getProgress(date: Long): DailyQuestProgress?
    @Query("SELECT * FROM daily_quest_progress ORDER BY date DESC")
    fun getAllSync(): List<DailyQuestProgress>
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(progress: DailyQuestProgress): Long
    @Update
    fun update(progress: DailyQuestProgress): Int
}