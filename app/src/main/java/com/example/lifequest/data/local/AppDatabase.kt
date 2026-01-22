package com.example.lifequest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.data.local.dao.BreakActivityDao
import com.example.lifequest.data.local.dao.DailyQuestDao
import com.example.lifequest.data.local.dao.UserStatusDao
import com.example.lifequest.data.local.dao.QuestDao
import com.example.lifequest.data.local.dao.QuestLogDao
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.QuestLog
import com.example.lifequest.data.local.entity.Subtask

// entities に Quest::class を追加し、version を 2 に上げます
@Database(entities =
    [UserStatus::class, Quest::class, QuestLog::class,
        Subtask::class, BreakActivity::class, DailyQuestProgress::class], version =12)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userStatusDao(): UserStatusDao
    abstract fun questDao(): QuestDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun breakActivityDao(): BreakActivityDao
    abstract fun dailyQuestDao(): DailyQuestDao
}