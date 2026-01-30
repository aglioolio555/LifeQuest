package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifequest.data.local.entity.BreakActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakActivityDao {
    @Query("SELECT * FROM break_activities ORDER BY id DESC")
    fun getAll(): Flow<List<BreakActivity>>

    @Query("SELECT COUNT(*) FROM break_activities")
    fun getCount(): Int

    @Insert
    fun insert(activity: BreakActivity): Long

    @Update
    fun update(activity: BreakActivity): Int

    @Delete
    fun delete(activity: BreakActivity): Int
}