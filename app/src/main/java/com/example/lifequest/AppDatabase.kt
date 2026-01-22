package com.example.lifequest

import androidx.room.Database
import androidx.room.RoomDatabase

// entities に Quest::class を追加し、version を 2 に上げます
@Database(entities = [UserStatus::class, Quest::class,QuestLog::class,Subtask::class,BreakActivity::class], version =11)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun breakActivityDao(): BreakActivityDao
}