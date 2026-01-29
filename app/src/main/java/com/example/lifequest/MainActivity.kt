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
import android.provider.Settings
import android.net.Uri
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.widget.Toast
import com.example.lifequest.logic.AppMonitorService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {
    private lateinit var notificationManager: LifeQuestNotificationManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // シングルトンインスタンスを取得するように変更
        val db = AppDatabase.getDatabase(applicationContext)

        val repository = MainRepository(db.userStatusDao(), questDao = db.questDao(), questLogDao = db.questLogDao(), db.breakActivityDao(), db.dailyQuestDao(),db.extraQuestDao(), db.allowedAppDao())

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
        //追加: 権限チェックと要求
        checkRequiredPermissions()
        lifecycleScope.launch {
            // repeatOnLifecycle(Lifecycle.State.STARTED) を使うとより丁寧ですが、ここでは簡易的にcollectします
            viewModel.timerState.collect { state ->
                // 集中モード中(isRunning) かつ 休憩中ではない(!isBreak) 場合に監視
                if (state.isRunning && !state.isBreak) {
                    startAppMonitorService()
                } else {
                    stopAppMonitorService()
                }
            }
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
    private fun checkRequiredPermissions() {
        // 1. 使用状況アクセス権限のチェック
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "集中モードには「使用状況へのアクセス」許可が必要です", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // 2. オーバーレイ権限のチェック (Android 6.0+)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "ブロック画面を表示するために「他のアプリの上に重ねて表示」許可が必要です", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    private fun startAppMonitorService() {
        val intent = Intent(this, AppMonitorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // ★追加: サービス停止メソッド
    private fun stopAppMonitorService() {
        val intent = Intent(this, AppMonitorService::class.java)
        stopService(intent)
    }
}