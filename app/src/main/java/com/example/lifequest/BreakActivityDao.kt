package com.example.lifequest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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