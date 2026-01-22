package com.example.lifequest.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.lifequest.logic.SoundManager
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.ui.dialogs.QuestEditDialog
import com.example.lifequest.ui.screens.FocusScreen
import com.example.lifequest.ui.screens.QuestInputForm
import com.example.lifequest.ui.screens.QuestListContent
import com.example.lifequest.ui.screens.SettingScreen
import com.example.lifequest.ui.screens.StatisticsScreen
import com.example.lifequest.viewmodel.MainViewModel
import kotlinx.coroutines.delay

enum class ExportType { LOGS, DAILY_QUESTS }
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // 状態の監視
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val breakActivities by viewModel.breakActivities.collectAsState()
    val currentBreakActivity by viewModel.currentBreakActivity.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val dailyProgress by viewModel.dailyProgress.collectAsState()
    val missingPermission by viewModel.missingPermission.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    var exportType by remember { mutableStateOf<ExportType?>(null) }
    // ライフサイクルイベントの監視（設定画面から戻った時の権限再チェック用）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            soundManager.release()
        }
    }

    // レベルアップ演出
    var previousLevel by remember { mutableIntStateOf(status.level) }
    LaunchedEffect(status.level) {
        if (status.level > previousLevel && previousLevel > 0) {
            soundManager.playLevelUpSound()
        }
        previousLevel = status.level
    }

    // クロック更新
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // CSV出力ランチャー
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && exportType != null) {
            when (exportType) {
                ExportType.LOGS -> viewModel.exportLogsToCsv(context, uri)
                ExportType.DAILY_QUESTS -> viewModel.exportDailyQuestsToCsv(context, uri)
                else -> {}
            }
            exportType = null // リセット
        }
    }

    var editingQuestData by remember { mutableStateOf<QuestWithSubtasks?>(null) }

    Scaffold(
        bottomBar = {
            // FOCUSとSETTINGS表示時はボトムバーを隠す
            if (currentScreen != Screen.FOCUS && currentScreen != Screen.SETTINGS) {
                NavigationBar {
                    Screen.entries.filter { it != Screen.FOCUS && it != Screen.SETTINGS }.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
            when (currentScreen) {
                Screen.HOME -> {
                    Column {
                        // 権限不足時の警告通知
                        if (missingPermission) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("「就寝クエスト」機能には設定が必要です", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("使用状況へのアクセスを許可してください", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer)
                                    ) {
                                        Text("設定", color = MaterialTheme.colorScheme.errorContainer)
                                    }
                                }
                            }
                        }

                        HomeScreen(
                            status = status,
                            urgentQuestData = quests.firstOrNull(),
                            timerState = timerState,
                            currentTime = currentTime,
                            onOpenSettings = { currentScreen = Screen.SETTINGS },
                            onEdit = { editingQuestData = it },
                            onToggleTimer = { quest ->
                                if (!timerState.isRunning) {
                                    viewModel.toggleTimer(quest, soundManager)
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
                }
                Screen.LIST -> {
                    QuestListContent(
                        quests = quests,
                        dailyProgress = dailyProgress, //
                        currentTime = currentTime,
                        onEdit = { editingQuestData = it },
                        onToggleTimer = { quest ->
                            viewModel.toggleTimer(quest, soundManager)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("新規クエスト受注", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                        QuestInputForm(
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
                            }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                Screen.FOCUS -> {
                    val activeQuest = quests.firstOrNull()
                    if (activeQuest != null) {
                        FocusScreen(
                            questWithSubtasks = activeQuest,
                            timerState = timerState,
                            currentBreakActivity = currentBreakActivity, //
                            currentTime = currentTime,
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
                                if (timerState.isRunning) {
                                    viewModel.toggleTimer(activeQuest.quest, soundManager)
                                }
                                currentScreen = Screen.HOME
                            },
                            onRerollBreakActivity = { viewModel.shuffleBreakActivity() },
                            onCompleteBreakActivity = { viewModel.completeBreakActivity(soundManager) }
                        )
                    } else {
                        currentScreen = Screen.HOME
                    }
                }
                Screen.STATISTICS -> {
                    StatisticsScreen(statistics = statistics) //
                }
                Screen.SETTINGS -> {
                    SettingScreen(
                        activities = breakActivities,
                        userStatus = status, //
                        onAddActivity = { title, desc -> viewModel.addBreakActivity(title, desc) },
                        onDeleteActivity = { viewModel.deleteBreakActivity(it) },
                        onUpdateTargetTimes = { wh, wm, bh, bm ->
                            viewModel.updateTargetTimes(wh, wm, bh, bm) //
                        },
                        onExportQuestLogs = {
                            exportType = ExportType.LOGS
                            exportLauncher.launch("quest_logs_backup.csv")
                        },
                        onExportDailyQuests = {
                            exportType = ExportType.DAILY_QUESTS
                            exportLauncher.launch("daily_quests_backup.csv")
                        },
                        onBack = { currentScreen = Screen.HOME }
                    )
                    BackHandler {
                        currentScreen = Screen.HOME
                    }
                }
            }
        }
    }

    // 編集ダイアログ管理
    if (editingQuestData != null) {
        QuestEditDialog(
            questWithSubtasks = editingQuestData!!,
            onDismiss = { editingQuestData = null },
            onConfirm = { updatedQuest ->
                viewModel.updateQuest(updatedQuest)
                editingQuestData = null
            },
            onAddSubtask = { title ->
                viewModel.addSubtask(editingQuestData!!.quest.id, title)
            },
            onDeleteSubtask = { subtask ->
                viewModel.deleteSubtask(subtask)
            }
        )
    }
}