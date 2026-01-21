package com.example.lifequest.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState // 追加
import androidx.compose.foundation.verticalScroll // 追加
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (currentScreen) {
                Screen.HOME -> {
                    var currentUrgentId by remember { mutableStateOf<Int?>(null) }

                    val urgentQuestData = remember(quests) {
                        if (quests.isEmpty()) return@remember null
                        val topData = quests.first()
                        val candidates = quests.takeWhile { it.quest.isSamePriorityAs(topData.quest) }

                        val current = candidates.find { it.quest.id == currentUrgentId }
                        if (current != null) {
                            current
                        } else {
                            val newData = candidates.random()
                            currentUrgentId = newData.quest.id
                            newData
                        }
                    }

                    HomeScreen(
                        status = status,
                        urgentQuestData = urgentQuestData,
                        currentTime = currentTime,
                        onExportCsv = { exportLauncher.launch("quest_logs_backup.csv") },
                        onEdit = { editingQuestData = it },
                        onToggleTimer = { viewModel.toggleTimer(it) },
                        onComplete = {
                            soundManager.playCoinSound()
                            viewModel.completeQuest(it)
                            currentUrgentId = null
                        },
                        onSubtaskToggle = { viewModel.toggleSubtask(it) }
                    )
                }
                Screen.LIST -> {
                    QuestListContent(
                        quests = quests,
                        currentTime = currentTime,
                        onEdit = { editingQuestData = it },
                        onToggleTimer = { viewModel.toggleTimer(it) },
                        onComplete = {
                            soundManager.playCoinSound()
                            viewModel.completeQuest(it)
                        },
                        onDelete = { viewModel.deleteQuest(it) },
                        onSubtaskToggle = { viewModel.toggleSubtask(it) }
                    )
                }
                Screen.ADD -> {
                    // ★スクロール機能を追加
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("新規クエスト受注", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                        QuestInputForm(
                            onAddQuest = { title, note, date, diff, repeat, category, time, subtasks ->
                                viewModel.addQuest(title, note, date, diff, repeat, category, time, subtasks)
                                currentScreen = Screen.LIST
                            }
                        )
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

private fun Quest.isSamePriorityAs(other: Quest): Boolean {
    val thisHasDate = this.dueDate != null
    val otherHasDate = other.dueDate != null
    if (thisHasDate != otherHasDate) return false
    if (thisHasDate && this.dueDate != other.dueDate) return false
    val thisRecurring = this.repeatMode != 0
    val otherRecurring = other.repeatMode != 0
    if (thisRecurring != otherRecurring) return false
    return true
}

@Composable
fun HomeScreen(
    status: UserStatus,
    urgentQuestData: QuestWithSubtasks?,
    currentTime: Long,
    onExportCsv: () -> Unit,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
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
            QuestItem(
                questWithSubtasks = urgentQuestData,
                currentTime = currentTime,
                onClick = { onEdit(urgentQuestData) },
                onToggleTimer = { onToggleTimer(urgentQuestData.quest) },
                onComplete = { onComplete(urgentQuestData.quest) },
                onSubtaskToggle = onSubtaskToggle,
                isLarge = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("今すぐ取り掛かりましょう！", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
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
    onAddQuest: (String, String, Long?, Int, Int, Int, Long, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var difficulty by remember { mutableIntStateOf(1) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var category by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var subtasks by remember { mutableStateOf(listOf("")) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(if (dueDate != null) com.example.lifequest.utils.formatDate(dueDate!!) else "期限なし") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
            }
            TimeInputRow(hours = hours, onHoursChange = { hours = it }, minutes = minutes, onMinutesChange = { minutes = it })
            DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it })
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
                        if (subtasks.size > 1 || subtask.isNotEmpty()) {
                            IconButton(onClick = {
                                val newList = subtasks.toMutableList()
                                newList.removeAt(index)
                                if (newList.isEmpty()) newList.add("")
                                subtasks = newList
                            }) { Icon(Icons.Default.Delete, contentDescription = null) }
                        }
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
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val estimatedMillis = (h * 60 * 60 * 1000) + (m * 60 * 1000)

                    onAddQuest(title, note, dueDate, difficulty, repeatMode, category, estimatedMillis, subtasks)

                    title = ""; note = ""; dueDate = null; hours = ""; minutes = ""; difficulty = 1; repeatMode = 0; category = 0; subtasks = listOf("")
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