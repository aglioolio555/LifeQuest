package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lifequest.data.local.AppDatabase
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.ui.MainScreen
import com.example.lifequest.ui.theme.LifeQuestTheme
import com.example.lifequest.utils.UsageStatsHelper
import com.example.lifequest.viewmodel.QuestViewModel
import com.example.lifequest.viewmodel.FocusViewModel
import com.example.lifequest.viewmodel.SettingsViewModel
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
    // ViewModelをプロパティとして保持
    private lateinit var questViewModel: QuestViewModel
    private lateinit var focusViewModel: FocusViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = MainRepository(
            db.userStatusDao(),
            db.questDao(),
            db.questLogDao(),
            db.breakActivityDao(),
            db.dailyQuestDao(),
            db.extraQuestDao(),
            db.allowedAppDao()
        )

        val usageStatsHelper = UsageStatsHelper(applicationContext)
        notificationManager = LifeQuestNotificationManager(applicationContext)

        // Factory作成（複数ViewModel対応）
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(QuestViewModel::class.java) -> {
                        QuestViewModel(repository, usageStatsHelper) as T
                    }
                    modelClass.isAssignableFrom(FocusViewModel::class.java) -> {
                        val vm = FocusViewModel(repository, usageStatsHelper)
                        vm.notificationManager = notificationManager // NotificationManagerを渡す
                        vm as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(repository, usageStatsHelper) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        val provider = ViewModelProvider(this, viewModelFactory)
        questViewModel = provider[QuestViewModel::class.java]
        focusViewModel = provider[FocusViewModel::class.java]
        settingsViewModel = provider[SettingsViewModel::class.java]

        // 権限チェック
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        checkRequiredPermissions()

        // アプリ監視サービスの制御
        lifecycleScope.launch {
            focusViewModel.timerState.collect { state ->
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
                MainScreen(
                    questViewModel = questViewModel,
                    focusViewModel = focusViewModel,
                    settingsViewModel = settingsViewModel
                )
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
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "集中モードには「使用状況へのアクセス」許可が必要です", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
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

    private fun stopAppMonitorService() {
        val intent = Intent(this, AppMonitorService::class.java)
        stopService(intent)
    }
}