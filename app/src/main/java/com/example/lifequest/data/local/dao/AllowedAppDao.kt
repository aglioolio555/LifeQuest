package com.example.lifequest.data.local.dao

import androidx.room.*
import com.example.lifequest.data.local.entity.AllowedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedAppDao {
    @Query("SELECT * FROM allowed_apps")
    fun getAllAllowedApps(): Flow<List<AllowedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(app: AllowedApp):Long

    @Delete
    fun delete(app: AllowedApp):Int

    @Query("SELECT packageName FROM allowed_apps")
    fun getAllPackageNames(): List<String>
}