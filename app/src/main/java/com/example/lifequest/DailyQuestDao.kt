package com.example.lifequest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {
    @Query("SELECT * FROM daily_quest_progress WHERE date = :date LIMIT 1")
    fun getProgressFlow(date: Long): Flow<DailyQuestProgress?>

    @Query("SELECT * FROM daily_quest_progress WHERE date = :date LIMIT 1")
    fun getProgress(date: Long): DailyQuestProgress?

    // ★修正: 戻り値の型 (: Long) を明示
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(progress: DailyQuestProgress): Long

    // ★修正: 戻り値の型 (: Int) を明示
    @Update
    fun update(progress: DailyQuestProgress): Int
}