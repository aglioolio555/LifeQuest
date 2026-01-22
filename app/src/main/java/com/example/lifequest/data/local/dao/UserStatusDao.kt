package com.example.lifequest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lifequest.data.local.entity.UserStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatusDao {
    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatus(): Flow<UserStatus?>

    @Query("SELECT * FROM user_status WHERE id = 0")
    fun getUserStatusSync(): UserStatus?

    @Insert
    fun insert(status: UserStatus): Long

    @Update
    fun update(status: UserStatus): Int

    @Query("DELETE FROM user_status")
    fun deleteUserStatus(): Int
}