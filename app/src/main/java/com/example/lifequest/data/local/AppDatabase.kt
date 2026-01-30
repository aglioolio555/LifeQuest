package com.example.lifequest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lifequest.data.local.dao.*
import com.example.lifequest.data.local.entity.*

@Database(
    entities = [
        UserStatus::class,
        Quest::class,
        Subtask::class,
        QuestLog::class,
        BreakActivity::class,
        DailyQuestProgress::class,
        ExtraQuest::class,
        AllowedApp::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userStatusDao(): UserStatusDao
    abstract fun questDao(): QuestDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun breakActivityDao(): BreakActivityDao
    abstract fun dailyQuestDao(): DailyQuestDao
    abstract fun extraQuestDao(): ExtraQuestDao
    abstract fun allowedAppDao(): AllowedAppDao // ★追加

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifequest-db"
                )
                    // ★Migration設定: バージョンが上がった際に、古いデータを破棄して再作成する設定
                    // (本番リリース後は適切なMigrationスクリプトを書くべきですが、開発中はこれでOKです)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}