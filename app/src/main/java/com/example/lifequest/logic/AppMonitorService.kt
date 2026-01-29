package com.example.lifequest.logic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.lifequest.R
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.ui.BlockActivity
import com.example.lifequest.utils.AppUtils
import kotlinx.coroutines.*
import com.example.lifequest.data.local.AppDatabase
import android.provider.Settings
import android.util.Log
class AppMonitorService : Service() {
    lateinit var repository: MainRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isMonitoring = false
    private var allowedPackages = setOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        loadAllowedApps()
        startMonitoring()
        return START_STICKY
    }
    override fun onCreate() {
        super.onCreate()
        // ★追加: 手動でRepositoryを初期化
        // LifeQuestDatabase.getDatabase(context) は一般的な実装を想定しています
        val db = AppDatabase.getDatabase(applicationContext)
        repository = MainRepository(
            userStatusDao = db.userStatusDao(),
            questDao = db.questDao(),
            questLogDao = db.questLogDao(),
            breakActivityDao = db.breakActivityDao(),
            dailyQuestDao = db.dailyQuestDao(),
            extraQuestDao = db.extraQuestDao(),
            allowedAppDao = db.allowedAppDao()
        )
    }

    private fun startForegroundService() {
        val channelId = "FocusMonitorChannel"
        val channelName = "Focus Mode Monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("集中モード稼働中")
            .setContentText("他のアプリの使用を監視しています")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 適切なアイコンに
            .build()

        startForeground(1, notification)
    }

    private fun loadAllowedApps() {
        serviceScope.launch {
            // DBから許可リスト取得
            val dbAllowed = repository.getAllAllowedPackageNames() // Daoに追加したメソッド経由
            val launcher = AppUtils.getDefaultLauncherPackage(applicationContext)
            val myPackage = packageName // 自アプリ

            allowedPackages = dbAllowed.toSet() + listOfNotNull(launcher, myPackage)
        }
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            while (isMonitoring) {
                val currentTime = System.currentTimeMillis()
                // 取得範囲を「直近1分」などに広げる
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 60, // 1分前〜現在
                    currentTime
                )

                if (stats != null && stats.isNotEmpty()) {
                    val topApp = stats.maxByOrNull { it.lastTimeUsed }
                    val currentPackage = topApp?.packageName

                    //currentPackage != packageName (自分自身ではない) を条件に追加
                    if (currentPackage != null && currentPackage != packageName && !allowedPackages.contains(currentPackage)) {
                        blockApp()
                    }
                }
                delay(500) // 0.5秒間隔
            }
        }
    }

    private fun blockApp() {
        // 権限があるかチェック
        if (!Settings.canDrawOverlays(this)) {
            Log.e("AppMonitor", "Block failed: Overlay permission not granted.")
            return
        }

        val intent = Intent(this, BlockActivity::class.java).apply {
            // バックグラウンドからの起動には NEW_TASK が必須（これはOK）
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // アニメーションを消すとよりブロックらしくなります
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }

        try {
            Log.d("AppMonitor", "Attempting to start BlockActivity...")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppMonitor", "Failed to start BlockActivity", e)
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        isMonitoring = false
        serviceScope.cancel()
        super.onDestroy()
    }
}