package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.lifequest.data.local.AppDatabase
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.ui.MainScreen
import com.example.lifequest.ui.theme.LifeQuestTheme
import com.example.lifequest.utils.UsageStatsHelper
import com.example.lifequest.viewmodel.MainViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.lifequest.logic.LifeQuestNotificationManager
import android.Manifest

class MainActivity : ComponentActivity() {
    private lateinit var notificationManager: LifeQuestNotificationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "lifequest-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val repository = MainRepository(db.userStatusDao(), questDao = db.questDao(), questLogDao = db.questLogDao(), db.breakActivityDao(), db.dailyQuestDao(),db.extraQuestDao())

        val usageStatsHelper = UsageStatsHelper(applicationContext)

        notificationManager = LifeQuestNotificationManager(applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    val vm = MainViewModel(repository, usageStatsHelper)
                    vm.notificationManager = notificationManager // VMに渡す
                    @Suppress("UNCHECKED_CAST")
                    return vm as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        // ★追加: アプリ全体のライフサイクル監視
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // アプリがバックグラウンドへ
                viewModel.onAppBackgrounded()
            }

            override fun onStart(owner: LifecycleOwner) {
                // アプリがフォアグラウンドへ
                viewModel.onAppForegrounded()
            }
        })

        // 通知権限の要求 (Android 13+) - 簡易実装
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContent {
            LifeQuestTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    // ★追加: 画面固定の開始
    fun startPinning() {
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ★追加: 画面固定の終了
    fun stopPinning() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            // 固定されていない状態で呼ぶと例外が出る場合があるため
        }
    }
}