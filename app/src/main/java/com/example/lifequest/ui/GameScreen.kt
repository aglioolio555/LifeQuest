package com.example.lifequest.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.*
import com.example.lifequest.ui.components.*
import com.example.lifequest.ui.dialogs.QuestEditDialog
import kotlinx.coroutines.delay

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()
    val timerState by viewModel.timerState.collectAsState() // ポモドーロ状態を監視

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    // レベルアップ演出
    var previousLevel by remember { mutableIntStateOf(status.level) }
    LaunchedEffect(status.level) {
        if (status.level > previousLevel && previousLevel > 0) {
            soundManager.playLevelUpSound()
        }
        previousLevel = status.level
    }

    // UI更新用のタイマー（1秒ごとに再描画をトリガー）
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
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().imePadding()) {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        status = status,
                        urgentQuestData = quests.firstOrNull(), // リストの先頭を「現在の目的」とする
                        timerState = timerState,
                        currentTime = currentTime,
                        onExportCsv = { exportLauncher.launch("quest_logs_backup.csv") },
                        onEdit = { editingQuestData = it },
                        onToggleTimer = { quest -> viewModel.toggleTimer(quest, soundManager) },
                        onComplete = { quest ->
                            soundManager.playCoinSound()
                            viewModel.completeQuest(quest)
                        },
                        onSubtaskToggle = { viewModel.toggleSubtask(it) },
                        onModeToggle = { viewModel.toggleTimerMode() }
                    )
                }
                Screen.LIST -> {
                    QuestListContent(
                        quests = quests,
                        currentTime = currentTime,
                        onEdit = { editingQuestData = it },
                        onToggleTimer = { viewModel.toggleTimer(it, soundManager) },
                        onComplete = {
                            soundManager.playCoinSound()
                            viewModel.completeQuest(it)
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

@Composable
fun HomeScreen(
    status: UserStatus,
    urgentQuestData: QuestWithSubtasks?,
    timerState: TimerState,
    currentTime: Long,
    onExportCsv: () -> Unit,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    onModeToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusCard(status)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onExportCsv) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("CSV出力")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("⚠️ CURRENT OBJECTIVE ⚠️", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (urgentQuestData != null) {
            UrgentQuestCard(
                questWithSubtasks = urgentQuestData,
                timerState = timerState,
                currentTime = currentTime,
                onToggleTimer = { onToggleTimer(urgentQuestData.quest) },
                onComplete = { onComplete(urgentQuestData.quest) },
                onEdit = { onEdit(urgentQuestData) },
                onSubtaskToggle = onSubtaskToggle,
                onModeToggle = onModeToggle
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if(timerState.isBreak) "しっかりと休息をとりましょう" else "今すぐ取り掛かりましょう！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("全てのクエストを完了しました！")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestInputForm(
    onAddQuest: (String, String, Long?, Int, Int, Long, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var category by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var selectedTimeMillis by remember { mutableLongStateOf(15 * 60 * 1000L) }
    var isOtherTimeSelected by remember { mutableStateOf(false) }
    var otherHours by remember { mutableStateOf("") }
    var otherMinutes by remember { mutableStateOf("") }

    var subtasks by remember { mutableStateOf(listOf("")) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            Spacer(modifier = Modifier.height(16.dp))
            Text("目安時間 (報酬EXPと初期タイマーに影響)", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(15L, 30L, 60L).forEach { mins ->
                    FilterChip(
                        selected = !isOtherTimeSelected && selectedTimeMillis == mins * 60 * 1000L,
                        onClick = {
                            selectedTimeMillis = mins * 60 * 1000L
                            isOtherTimeSelected = false
                        },
                        label = { Text("${mins}分") },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = isOtherTimeSelected,
                    onClick = { isOtherTimeSelected = true },
                    label = { Text("その他") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isOtherTimeSelected) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(value = otherHours, onValueChange = { otherHours = it }, label = { Text("時") }, modifier = Modifier.weight(1f))
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = otherMinutes, onValueChange = { otherMinutes = it }, label = { Text("分") }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(if (dueDate != null) com.example.lifequest.utils.formatDate(dueDate!!) else "期限なし") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
            }
            CategorySelector(selectedCategory = category, onCategorySelected = { category = it })

            Spacer(modifier = Modifier.height(8.dp))
            Text("サブタスク", style = MaterialTheme.typography.labelMedium)
            subtasks.forEachIndexed { index, subtask ->
                OutlinedTextField(
                    value = subtask,
                    onValueChange = { newValue ->
                        val newList = subtasks.toMutableList()
                        newList[index] = newValue
                        subtasks = newList
                    },
                    placeholder = { Text("サブタスク ${index + 1}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val newList = subtasks.toMutableList()
                            newList.removeAt(index)
                            if (newList.isEmpty()) newList.add("")
                            subtasks = newList
                        }) { Icon(Icons.Default.Delete, contentDescription = null) }
                    }
                )
            }
            TextButton(onClick = { subtasks = subtasks + "" }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("サブタスクを追加")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val finalTime = if (isOtherTimeSelected) {
                        (otherHours.toLongOrNull() ?: 0L) * 3600000L + (otherMinutes.toLongOrNull() ?: 0L) * 60000L
                    } else {
                        selectedTimeMillis
                    }

                    onAddQuest(title, note, dueDate, repeatMode, category, finalTime, subtasks)

                    // リセット
                    title = ""; note = ""; dueDate = null; repeatMode = 0; category = 0; subtasks = listOf(""); otherHours = ""; otherMinutes = ""
                    isOtherTimeSelected = false; selectedTimeMillis = 15 * 60 * 1000L
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("クエストを受注する")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun QuestListContent(
    quests: List<QuestWithSubtasks>,
    currentTime: Long,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onDelete: (Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("現在アクティブなクエストはありません", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = quests, key = { it.quest.id }) { item ->
            val quest = item.quest
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(quest)
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                    }
                },
                content = {
                    QuestItem(
                        questWithSubtasks = item,
                        currentTime = currentTime,
                        onClick = { onEdit(item) },
                        onToggleTimer = { onToggleTimer(quest) },
                        onComplete = { onComplete(quest) },
                        onSubtaskToggle = onSubtaskToggle
                    )
                }
            )
        }
    }
}