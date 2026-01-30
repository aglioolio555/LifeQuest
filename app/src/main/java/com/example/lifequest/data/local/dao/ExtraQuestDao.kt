package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifequest.data.local.entity.ExtraQuest
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtraQuestDao {
    @Query("SELECT * FROM extra_quests ORDER BY id DESC")
    fun getAll(): Flow<List<ExtraQuest>>

    @Query("SELECT * FROM extra_quests ORDER BY RANDOM() LIMIT 1")
    fun getRandom(): ExtraQuest?

    @Insert
    fun insert(quest: ExtraQuest): Long

    @Update
    fun update(quest: ExtraQuest): Int

    @Delete
    fun delete(quest: ExtraQuest): Int

    @Query("SELECT COUNT(*) FROM extra_quests")
    fun getCount(): Int
}