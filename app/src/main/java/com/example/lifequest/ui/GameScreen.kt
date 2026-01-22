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
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportLogsToCsv(context, uri)
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
                Screen.HOME -> HomeScreen(
                    status = status,
                    urgentQuestData = quests.firstOrNull(),
                    currentTime = currentTime,
                    onExportCsv = { exportLauncher.launch("quest_logs.csv") },
                    onEdit = { editingQuestData = it },
                    onToggleTimer = { viewModel.toggleTimer(it) },
                    onComplete = { soundManager.playCoinSound(); viewModel.completeQuest(it) },
                    onSubtaskToggle = { viewModel.toggleSubtask(it) }
                )
                Screen.LIST -> QuestListContent(
                    quests = quests,
                    currentTime = currentTime,
                    onEdit = { editingQuestData = it },
                    onToggleTimer = { viewModel.toggleTimer(it) },
                    onComplete = { soundManager.playCoinSound(); viewModel.completeQuest(it) },
                    onDelete = { viewModel.deleteQuest(it) },
                    onSubtaskToggle = { viewModel.toggleSubtask(it) }
                )
                Screen.ADD -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
                ) {
                    Text("新規クエスト受注", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    QuestInputForm(onAddQuest = { t, n, d, r, c, time, s ->
                        viewModel.addQuest(t, n, d, r, c, time, s)
                        currentScreen = Screen.LIST
                    })
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (editingQuestData != null) {
        QuestEditDialog(
            questWithSubtasks = editingQuestData!!,
            onDismiss = { editingQuestData = null },
            onConfirm = { updated -> viewModel.updateQuest(updated); editingQuestData = null },
            onAddSubtask = { title -> viewModel.addSubtask(editingQuestData!!.quest.id, title) },
            onDeleteSubtask = { sub -> viewModel.deleteSubtask(sub) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestInputForm(onAddQuest: (String, String, Long?, Int, Int, Long, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var category by remember { mutableIntStateOf(0) }

    // 目安時間選択用
    var selectedTimeMillis by remember { mutableLongStateOf(15 * 60 * 1000L) }
    var isOtherSelected by remember { mutableStateOf(false) }
    var otherH by remember { mutableStateOf("") }
    var otherM by remember { mutableStateOf("") }

    var subtasks by remember { mutableStateOf(listOf("")) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
            Text("目安時間 (報酬に影響)", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(15, 30, 60).forEach { mins ->
                    val millis = mins * 60 * 1000L
                    FilterChip(
                        selected = !isOtherSelected && selectedTimeMillis == millis,
                        onClick = { selectedTimeMillis = millis; isOtherSelected = false },
                        label = { Text("${mins}分") },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = isOtherSelected,
                    onClick = { isOtherSelected = true },
                    label = { Text("その他") },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isOtherSelected) {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = otherH, onValueChange = { otherH = it }, label = { Text("時") }, modifier = Modifier.weight(1f))
                    Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(value = otherM, onValueChange = { otherM = it }, label = { Text("分") }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AssistChip(onClick = { showDatePicker = true }, label = { Text(if (dueDate != null) com.example.lifequest.utils.formatDate(dueDate!!) else "期限なし") })
                RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
            }
            CategorySelector(selectedCategory = category, onCategorySelected = { category = it })

            Spacer(modifier = Modifier.height(8.dp))
            Text("サブタスク", style = MaterialTheme.typography.labelMedium)
            subtasks.forEachIndexed { index, sub ->
                OutlinedTextField(
                    value = sub,
                    onValueChange = { v -> val l = subtasks.toMutableList(); l[index] = v; subtasks = l },
                    placeholder = { Text("サブタスク ${index+1}") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    trailingIcon = { IconButton(onClick = { val l = subtasks.toMutableList(); l.removeAt(index); if(l.isEmpty()) l.add(""); subtasks = l }) { Icon(Icons.Default.Delete, null) } }
                )
            }
            TextButton(onClick = { subtasks = subtasks + "" }) { Icon(Icons.Default.Add, null); Text("サブタスク追加") }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val finalTime = if (isOtherSelected) (otherH.toLongOrNull() ?: 0)*3600000 + (otherM.toLongOrNull() ?: 0)*60000 else selectedTimeMillis
                    onAddQuest(title, note, dueDate, repeatMode, category, finalTime, subtasks)
                    title = ""; note = ""; subtasks = listOf(""); isOtherSelected = false; selectedTimeMillis = 15*60*1000L; otherH=""; otherM=""
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("クエストを受注する") }
        }
    }
    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }) { DatePicker(datePickerState) }
    }
}

@Composable
fun HomeScreen(status: UserStatus, urgentQuestData: QuestWithSubtasks?, currentTime: Long, onExportCsv: () -> Unit, onEdit: (QuestWithSubtasks) -> Unit, onToggleTimer: (Quest) -> Unit, onComplete: (Quest) -> Unit, onSubtaskToggle: (Subtask) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StatusCard(status)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onExportCsv) { Icon(Icons.Default.Share, null, Modifier.size(16.dp)); Text("CSV出力") }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("⚠️ CURRENT OBJECTIVE ⚠️", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        if (urgentQuestData != null) {
            QuestItem(questWithSubtasks = urgentQuestData, currentTime = currentTime, onClick = { onEdit(urgentQuestData) }, onToggleTimer = { onToggleTimer(urgentQuestData.quest) }, onComplete = { onComplete(urgentQuestData.quest) }, onSubtaskToggle = onSubtaskToggle, isLarge = true)
        } else {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
                    Text("全てのクエストを完了しました！")
                }
            }
        }
    }
}

@Composable
fun QuestListContent(quests: List<QuestWithSubtasks>, currentTime: Long, onEdit: (QuestWithSubtasks) -> Unit, onToggleTimer: (Quest) -> Unit, onComplete: (Quest) -> Unit, onDelete: (Quest) -> Unit, onSubtaskToggle: (Subtask) -> Unit) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("アクティブなクエストはありません", color = MaterialTheme.colorScheme.secondary) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quests, key = { it.quest.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(item.quest); true } else false })
                SwipeToDismissBox(state = dismissState, backgroundContent = { Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, "削除", tint = MaterialTheme.colorScheme.error) } }, content = { QuestItem(item, currentTime, { onEdit(item) }, { onToggleTimer(item.quest) }, { onComplete(item.quest) }, onSubtaskToggle) })
            }
        }
    }
}