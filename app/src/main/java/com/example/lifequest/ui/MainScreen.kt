package com.example.lifequest.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.lifequest.viewmodel.QuestViewModel
import com.example.lifequest.viewmodel.FocusViewModel
import com.example.lifequest.viewmodel.SettingsViewModel
import com.example.lifequest.logic.FocusTimerManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

// --- Tech Noir Animated Background ---
@Composable
fun TechNoirAnimatedBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gridAnimation")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    Canvas(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val gridSize = 100f
        val width = size.width
        val height = size.height
        val gridColor = Color(0xFF40E0FF).copy(alpha = 0.05f)

        for (x in 0..width.toInt() step gridSize.toInt()) {
            drawLine(
                color = gridColor,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), height),
                strokeWidth = 1f
            )
        }

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
fun MainScreen(
    questViewModel: QuestViewModel,
    focusViewModel: FocusViewModel,
    settingsViewModel: SettingsViewModel
) {
    // --- State from ViewModels ---
    val status by questViewModel.uiState.collectAsState()
    val quests by questViewModel.questList.collectAsState()
    val urgentQuest by questViewModel.urgentQuest.collectAsState()
    val futureQuests by questViewModel.futureQuestList.collectAsState()
    val dailyProgress by questViewModel.dailyProgress.collectAsState()
    val suggestedExtraQuest by questViewModel.suggestedExtraQuest.collectAsState()
    // ポップアップは両方から来る可能性があるので統合が必要だが、簡易的に両方監視
    val questPopupQueue by questViewModel.popupQueue.collectAsState()
    val focusPopupQueue by focusViewModel.popupQueue.collectAsState()
    // リスト結合
    val popupQueue = questPopupQueue + focusPopupQueue

    val timerState by focusViewModel.timerState.collectAsState()
    val breakActivities by settingsViewModel.breakActivities.collectAsState() // SettingsVMから
    val currentBreakActivity by focusViewModel.currentBreakActivity.collectAsState()
    val isInterrupted by focusViewModel.isInterrupted.collectAsState()
    val isBonusMissionLoading by focusViewModel.isBonusMissionLoading.collectAsState()

    val statistics by settingsViewModel.statistics.collectAsState()
    val extraQuests by settingsViewModel.extraQuests.collectAsState()
    val allowedApps by settingsViewModel.allowedApps.collectAsState()

    var focusingQuestId by remember { mutableStateOf<Int?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val activity = context as? MainActivity
    val soundManager = remember { SoundManager(context) }
    var exportType by remember { mutableStateOf<ExportType?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var editingQuestData by remember { mutableStateOf<QuestWithSubtasks?>(null) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val coroutineScope = rememberCoroutineScope()

    // Toast Event
    LaunchedEffect(Unit) {
        merge(focusViewModel.toastEvent).collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                questViewModel.refreshPermissionCheck()
                settingsViewModel.refreshPermissionCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundManager.release()
        }
    }

    // Sound Event: 3つのViewModelからのイベントをマージして監視
    LaunchedEffect(Unit) {
        merge(questViewModel.soundEvent, focusViewModel.soundEvent, settingsViewModel.soundEvent).collect { soundType ->
            soundManager.play(soundType)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // 編集ダイアログ同期
    LaunchedEffect(quests, futureQuests) {
        editingQuestData?.let { current ->
            val updated = quests.find { it.quest.id == current.quest.id }
                ?: futureQuests.find { it.quest.id == current.quest.id }
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
                ExportType.LOGS -> settingsViewModel.exportLogsToCsv(context, uri)
                ExportType.DAILY_QUESTS -> settingsViewModel.exportDailyQuestsToCsv(context, uri)
                else -> {}
            }
            exportType = null
        }
    }

    CompositionLocalProvider(LocalSoundManager provides soundManager) {
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.FOCUS && currentScreen != Screen.SETTINGS && currentScreen != Screen.WHITELIST) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
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
            Box(modifier = Modifier.fillMaxSize()) {
                TechNoirAnimatedBackground()

                Box(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                status = status,
                                urgentQuestData = urgentQuest,
                                suggestedExtraQuest = suggestedExtraQuest,
                                onStartBonusMission = { extra ->
                                    coroutineScope.launch {
                                        focusViewModel.setBonusMissionLoading(true)
                                        focusViewModel.isBonusMissionRunning = true // フラグセット
                                        val newQuest = questViewModel.prepareBonusMission(extra)
                                        // Timer初期化と開始
                                        FocusTimerManager.initializeModeBasedOnQuest(newQuest.estimatedTime)
                                        focusViewModel.toggleTimer(newQuest, soundManager)

                                        focusingQuestId = newQuest.id
                                        activity?.startPinning()

                                        delay(200) // DB反映待ち
                                        focusViewModel.setBonusMissionLoading(false)
                                        currentScreen = Screen.FOCUS
                                    }
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
                                        focusingQuestId = quest.id
                                        FocusTimerManager.initializeModeBasedOnQuest(quest.estimatedTime)
                                        focusViewModel.toggleTimer(quest, soundManager)
                                        activity?.startPinning()
                                    }
                                    currentScreen = Screen.FOCUS
                                },
                                onComplete = { quest ->
                                    focusViewModel.completeQuest(quest) // 完了処理はFocusVMへ
                                },
                                onSubtaskToggle = { questViewModel.toggleSubtask(it) },
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
                                    focusingQuestId = quest.id
                                    FocusTimerManager.initializeModeBasedOnQuest(quest.estimatedTime)
                                    focusViewModel.toggleTimer(quest, soundManager)
                                    activity?.startPinning()
                                    currentScreen = Screen.FOCUS
                                },
                                onComplete = { quest -> focusViewModel.completeQuest(quest) },
                                onDelete = { questViewModel.deleteQuest(it) },
                                onSubtaskToggle = { questViewModel.toggleSubtask(it) }
                            )
                        }

                        Screen.ADD -> {
                            AddQuestScreen(
                                onAddQuest = { title, note, date, repeat, category, time, subtasks ->
                                    questViewModel.addQuest(title, note, date, repeat, category, time, subtasks)
                                    currentScreen = Screen.LIST
                                }, soundManager = soundManager
                            )
                        }

                        Screen.FOCUS -> {
                            val activeQuest = quests.find { it.quest.id == focusingQuestId }
                                ?: quests.find { it.quest.lastStartTime != null }
                                ?: quests.firstOrNull()

                            if (activeQuest != null) {
                                FocusScreen(
                                    questWithSubtasks = activeQuest,
                                    timerState = timerState,
                                    currentBreakActivity = currentBreakActivity,
                                    currentTime = currentTime,
                                    isInterrupted = isInterrupted,
                                    onResumeFromInterruption = { focusViewModel.resumeFromInterruption() },
                                    onToggleTimer = {
                                        focusViewModel.toggleTimer(activeQuest.quest, soundManager)
                                    },
                                    onModeToggle = { focusViewModel.toggleTimerMode() },
                                    onComplete = {
                                        focusViewModel.completeQuest(activeQuest.quest)
                                        focusingQuestId = null
                                        currentScreen = Screen.HOME
                                    },
                                    onSubtaskToggle = { questViewModel.toggleSubtask(it) },
                                    onExit = {
                                        focusViewModel.stopSession(activeQuest.quest)
                                        focusingQuestId = null
                                        currentScreen = Screen.HOME
                                    },
                                    onRerollBreakActivity = { focusViewModel.shuffleBreakActivity() },
                                    onCompleteBreakActivity = { focusViewModel.completeBreakActivity() }
                                )
                            } else if (isBonusMissionLoading) {
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
                                activities = breakActivities, // SettingsVMから取得するよう修正済み
                                userStatus = status,
                                onAddActivity = { title, desc -> settingsViewModel.addBreakActivity(title, desc) },
                                onDeleteActivity = { settingsViewModel.deleteBreakActivity(it) },
                                onUpdateActivity = { settingsViewModel.updateBreakActivity(it) },
                                onUpdateTargetTimes = { wh, wm, bh, bm -> settingsViewModel.updateTargetTimes(wh, wm, bh, bm) },
                                onExportQuestLogs = {
                                    exportType = ExportType.LOGS
                                    exportLauncher.launch("quest_logs_backup.csv")
                                },
                                onExportDailyQuests = {
                                    exportType = ExportType.DAILY_QUESTS
                                    exportLauncher.launch("daily_quests_backup.csv")
                                },
                                extraQuests = extraQuests,
                                onAddExtraQuest = { title, desc, minutes, category -> settingsViewModel.addExtraQuest(title, desc, minutes, category) },
                                onDeleteExtraQuest = { settingsViewModel.deleteExtraQuest(it) },
                                onUpdateExtraQuest = { settingsViewModel.updateExtraQuest(it) },
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
                                viewModel = settingsViewModel, // AppWhitelistScreenの引数も修正が必要だが、一旦SettingsVMを渡す前提
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

        // Popup Handling
        if (popupQueue.isNotEmpty()) {
            val currentEvent = popupQueue.first()
            DailyQuestCompletionDialog(
                type = currentEvent.type,
                expEarned = currentEvent.expEarned,
                onDismiss = {
                    questViewModel.dismissCurrentPopup()
                    focusViewModel.dismissCurrentPopup()
                }
            )
        }

        if (editingQuestData != null) {
            QuestEditDialog(
                questWithSubtasks = editingQuestData!!,
                onDismiss = { editingQuestData = null },
                onConfirm = { updatedQuest ->
                    questViewModel.updateQuest(updatedQuest)
                    editingQuestData = null
                },
                onAddSubtask = { title ->
                    questViewModel.addSubtask(editingQuestData!!.quest.id, title)
                },
                onDeleteSubtask = { subtask -> questViewModel.deleteSubtask(subtask) }
            )
        }
    }
}