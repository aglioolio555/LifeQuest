package com.example.lifequest.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifequest.*
import com.example.lifequest.ui.dialogs.QuestEditDialog
import kotlinx.coroutines.delay

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()
    val timerState by viewModel.timerState.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    var previousLevel by remember { mutableIntStateOf(status.level) }
    LaunchedEffect(status.level) {
        if (status.level > previousLevel && previousLevel > 0) {
            soundManager.playLevelUpSound()
        }
        previousLevel = status.level
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportLogsToCsv(context, uri)
        }
    }

    var editingQuestData by remember { mutableStateOf<QuestWithSubtasks?>(null) }

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.FOCUS) {
                NavigationBar {
                    Screen.entries.filter { it != Screen.FOCUS }.forEach { screen ->
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
                    HomeScreen(
                        status = status,
                        urgentQuestData = quests.firstOrNull(),
                        timerState = timerState,
                        currentTime = currentTime,
                        onExportCsv = { exportLauncher.launch("quest_logs_backup.csv") },
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
                        onSubtaskToggle = { viewModel.toggleSubtask(it) }
                    )
                }
                Screen.LIST -> {
                    QuestListContent(
                        quests = quests,
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
                                viewModel.addQuest(title, note, date, repeat, category, time, subtasks)
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
                            currentTime = currentTime,
                            onToggleTimer = { viewModel.toggleTimer(activeQuest.quest, soundManager) },
                            onModeToggle = { viewModel.toggleTimerMode() },
                            onComplete = {
                                soundManager.playCoinSound()
                                viewModel.completeQuest(activeQuest.quest)
                                currentScreen = Screen.HOME
                            },
                            onSubtaskToggle = { viewModel.toggleSubtask(it) },
                            onExit = { currentScreen = Screen.HOME }
                        )
                    } else {
                        currentScreen = Screen.HOME
                    }
                }
            }
        }
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
                viewModel.addSubtask(editingQuestData!!.quest.id, title)
            },
            onDeleteSubtask = { subtask ->
                viewModel.deleteSubtask(subtask)
            }
        )
    }
}