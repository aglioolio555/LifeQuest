package com.example.lifequest.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.lifequest.MainActivity
import com.example.lifequest.logic.LocalSoundManager
import com.example.lifequest.logic.SoundManager
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.ui.dialogs.DailyQuestCompletionDialog
import com.example.lifequest.ui.dialogs.QuestEditDialog
import com.example.lifequest.ui.screens.*
import com.example.lifequest.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// --- Tech Noir Animated Background ---
@Composable
fun TechNoirAnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridAnimation")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f, // グリッドの間隔分移動
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    Canvas(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val gridSize = 100f // グリッドのサイズ
        val width = size.width
        val height = size.height
        val gridColor = Color(0xFF40E0FF).copy(alpha = 0.05f) // 薄いシアン

        // 縦線
        for (x in 0..width.toInt() step gridSize.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), height),
                strokeWidth = 1f
            )
        }

        // 横線 (動く)
        val startY = (offsetY % gridSize) - gridSize
        for (y in startY.toInt()..height.toInt() step gridSize.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y.toFloat()),
                end = Offset(width, y.toFloat()),
                strokeWidth = 1f
            )
        }
    }
}

enum class ExportType { LOGS, DAILY_QUESTS }

@Composable
fun MainScreen(viewModel: MainViewModel) {
    // ... (既存の状態取得コード) ...
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()
    val futureQuests by viewModel.futureQuestList.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val breakActivities by viewModel.breakActivities.collectAsState()
    val currentBreakActivity by viewModel.currentBreakActivity.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState()
    val missingPermission by viewModel.missingPermission.collectAsState()
    val isInterrupted by viewModel.isInterrupted.collectAsState()
    val suggestedExtraQuest by viewModel.suggestedExtraQuest.collectAsState()
    val extraQuests by viewModel.extraQuests.collectAsState()
    val popupQueue by viewModel.popupQueue.collectAsState()

    // ★追加: ボーナスミッションのロード状態
    val isBonusMissionLoading by viewModel.isBonusMissionLoading.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val activity = context as? MainActivity
    val soundManager = remember { SoundManager(context) }
    var exportType by remember { mutableStateOf<ExportType?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var editingQuestData by remember { mutableStateOf<QuestWithSubtasks?>(null) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissionCheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundManager.release()
        }
    }

    // ★ViewModelからの音イベントを監視して再生
    LaunchedEffect(Unit) {
        viewModel.soundEvent.collect { soundType ->
            soundManager.play(soundType)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }
    // ★レベルアップ検知 & SE再生
    LaunchedEffect(status.level) {
        if (status.level > 1) { // 初回起動時などを除く
            soundManager.playLevelUp()
        }
    }
    //サブタスク追加等の変更を即座にダイアログに反映させるための同期処理
    LaunchedEffect(quests, futureQuests) {
        editingQuestData?.let { current ->
            // 現在編集中のクエストIDと同じものを最新リストから探す
            val updated = quests.find { it.quest.id == current.quest.id }
                ?: futureQuests.find { it.quest.id == current.quest.id }

            // 最新データが見つかり、内容が更新されていれば差し替える
            if (updated != null && updated != current) {
                editingQuestData = updated
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && exportType != null) {
            when (exportType) {
                ExportType.LOGS -> viewModel.exportLogsToCsv(context, uri)
                ExportType.DAILY_QUESTS -> viewModel.exportDailyQuestsToCsv(context, uri)
                else -> { /* 何もしない */
                }
            }

            exportType = null
        }
    }
    CompositionLocalProvider(LocalSoundManager provides soundManager) {
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.FOCUS && currentScreen != Screen.SETTINGS && currentScreen != Screen.WHITELIST) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // 少し透過
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Screen.entries.filter { it != Screen.FOCUS && it != Screen.SETTINGS && it != Screen.WHITELIST }
                            .forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                                    label = { Text(screen.label) },
                                    selected = currentScreen == screen,
                                    onClick = {
                                        currentScreen = screen
                                        soundManager.playClick()
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                    }
                }
            }
        ) { innerPadding ->
            // ★背景を適用
            Box(modifier = Modifier.fillMaxSize()) {
                TechNoirAnimatedBackground()

                Box(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
                    when (currentScreen) {
                        Screen.HOME -> {
                            // ... (HomeScreen呼び出し)
                            HomeScreen(
                                status = status,
                                urgentQuestData = quests.firstOrNull(),
                                suggestedExtraQuest = suggestedExtraQuest,
                                onStartBonusMission = { extra ->
                                    viewModel.startBonusMission(extra, soundManager)
                                    activity?.startPinning()
                                    currentScreen = Screen.FOCUS
                                },
                                timerState = timerState,
                                currentTime = currentTime,
                                onOpenSettings = {
                                    soundManager.playClick()
                                    currentScreen = Screen.SETTINGS
                                                 },
                                onEdit = { editingQuestData = it },
                                onToggleTimer = { quest ->
                                    if (!timerState.isRunning) {
                                        viewModel.toggleTimer(quest, soundManager)
                                        activity?.startPinning()
                                    }
                                    currentScreen = Screen.FOCUS
                                },
                                onComplete = { quest ->
                                    soundManager.playCoinSound()
                                    viewModel.completeQuest(quest)
                                },
                                onSubtaskToggle = { viewModel.toggleSubtask(it) },
                            )
                        }

                        Screen.LIST -> {
                            QuestListContent(
                                quests = quests,
                                futureQuests = futureQuests,
                                dailyProgress = dailyProgress,
                                currentTime = currentTime,
                                onEdit = { editingQuestData = it },
                                onToggleTimer = { quest ->
                                    viewModel.toggleTimer(quest, soundManager)
                                    activity?.startPinning()
                                    currentScreen = Screen.FOCUS
                                },
                                onComplete = { quest ->
                                    soundManager.playCoinSound()
                                    viewModel.completeQuest(quest)
                                },
                                onDelete = { viewModel.deleteQuest(it) },
                                onSubtaskToggle = { viewModel.toggleSubtask(it) }
                            )
                        }

                        Screen.ADD -> {
                            AddQuestScreen(
                                onAddQuest = { title, note, date, repeat, category, time, subtasks ->
                                    viewModel.addQuest(
                                        title,
                                        note,
                                        date,
                                        repeat,
                                        category,
                                        time,
                                        subtasks
                                    )
                                    currentScreen = Screen.LIST
                                }, soundManager = soundManager
                            )
                        }
                        // ... (FOCUS, STATISTICS, SETTINGS も同様に) ...
                        Screen.FOCUS -> {
                            val activeQuest = quests.firstOrNull()
                            if (activeQuest != null) {
                                FocusScreen(
                                    questWithSubtasks = activeQuest,
                                    timerState = timerState,
                                    currentBreakActivity = currentBreakActivity,
                                    currentTime = currentTime,
                                    isInterrupted = isInterrupted,
                                    onResumeFromInterruption = { viewModel.resumeFromInterruption() },
                                    onToggleTimer = {
                                        viewModel.toggleTimer(
                                            activeQuest.quest,
                                            soundManager
                                        )
                                    },
                                    onModeToggle = { viewModel.toggleTimerMode() },
                                    onComplete = {
                                        soundManager.playCoinSound()
                                        viewModel.completeQuest(activeQuest.quest)
                                        currentScreen = Screen.HOME
                                    },
                                    onSubtaskToggle = { viewModel.toggleSubtask(it) },
                                    onExit = {
                                        if (timerState.isRunning) viewModel.toggleTimer(
                                            activeQuest.quest,
                                            soundManager
                                        )
                                        currentScreen = Screen.HOME
                                    },
                                    onRerollBreakActivity = { viewModel.shuffleBreakActivity() },
                                    onCompleteBreakActivity = {
                                        viewModel.completeBreakActivity(
                                            soundManager
                                        )
                                    }
                                )
                            } else if (isBonusMissionLoading) {
                                // ★追加: ボーナスミッション準備中のローディング表示
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                currentScreen = Screen.HOME
                            }
                        }

                        Screen.STATISTICS -> StatisticsScreen(statistics = statistics)
                        Screen.SETTINGS -> {
                            SettingScreen(
                                activities = breakActivities,
                                userStatus = status,
                                onAddActivity = { title, desc ->
                                    viewModel.addBreakActivity(
                                        title,
                                        desc
                                    )
                                },
                                onDeleteActivity = { viewModel.deleteBreakActivity(it) },
                                onUpdateTargetTimes = { wh, wm, bh, bm ->
                                    viewModel.updateTargetTimes(
                                        wh,
                                        wm,
                                        bh,
                                        bm
                                    )
                                },
                                onExportQuestLogs = {
                                    exportType = ExportType.LOGS
                                    exportLauncher.launch("quest_logs_backup.csv")
                                },
                                onExportDailyQuests = {
                                    exportType = ExportType.DAILY_QUESTS
                                    exportLauncher.launch("daily_quests_backup.csv")
                                },
                                extraQuests = extraQuests,
                                onAddExtraQuest = { title, desc, minutes ->
                                    viewModel.addExtraQuest(
                                        title,
                                        desc,
                                        minutes
                                    )
                                },
                                onDeleteExtraQuest = { extra -> viewModel.deleteExtraQuest(extra) },
                                onNavigateToWhitelist = {
                                    soundManager.playClick()
                                    currentScreen = Screen.WHITELIST
                                },
                                onBack = {
                                    soundManager.playClick()
                                    currentScreen = Screen.HOME
                                }
                            )
                            BackHandler {
                                soundManager.playClick()
                                currentScreen = Screen.HOME
                            }
                        }
                        Screen.WHITELIST -> {
                            AppWhitelistScreen(
                                viewModel = viewModel,
                                onBack = {
                                    soundManager.playClick()
                                    currentScreen = Screen.SETTINGS
                                }
                            )
                            BackHandler {
                                soundManager.playClick()
                                currentScreen = Screen.SETTINGS
                            }
                        }
                    }
                }
            }
        }

        // ... (Popup, Dialog handling) ...
        if (popupQueue.isNotEmpty()) {
            val currentEvent = popupQueue.first()
            DailyQuestCompletionDialog(
                type = currentEvent.type,
                expEarned = currentEvent.expEarned,
                onDismiss = {
                    soundManager.playCoinSound()
                    viewModel.dismissCurrentPopup()
                }
            )
        }

        if (editingQuestData != null) {
            QuestEditDialog(
                questWithSubtasks = editingQuestData!!,
                onDismiss = { editingQuestData = null },
                onConfirm = { updatedQuest ->
                    viewModel.updateQuest(updatedQuest)
                    editingQuestData = null
                },
                onAddSubtask = { title ->
                    viewModel.addSubtask(
                        editingQuestData!!.quest.id,
                        title
                    )
                },
                onDeleteSubtask = { subtask -> viewModel.deleteSubtask(subtask) }
            )
        }
    }
}