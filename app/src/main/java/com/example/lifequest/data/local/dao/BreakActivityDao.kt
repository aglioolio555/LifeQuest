package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.lifequest.data.local.entity.BreakActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakActivityDao {
    @Query("SELECT * FROM break_activities ORDER BY id DESC")
    fun getAll(): Flow<List<BreakActivity>>

    @Insert
    fun insert(activity: BreakActivity): Long

    @Delete
    fun delete(activity: BreakActivity): Int

    @Query("SELECT COUNT(*) FROM break_activities")
    fun getCount(): Int
}