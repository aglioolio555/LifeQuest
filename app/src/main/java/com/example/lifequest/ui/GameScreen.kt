package com.example.lifequest.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.GameViewModel
import com.example.lifequest.Quest
import com.example.lifequest.SoundManager
import com.example.lifequest.UserStatus
import com.example.lifequest.ui.components.* // UrgentQuestCard, QuestItem, StatusCard等がここに含まれます
import com.example.lifequest.ui.dialogs.*
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.delay

// 画面の定義
enum class Screen(val label: String, val icon: ImageVector) {
    HOME("ホーム", Icons.Default.Home),
    LIST("一覧", Icons.Default.List),
    ADD("受注", Icons.Default.AddCircle)
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    // 1. データと状態の監視
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()

    // 現在の画面 (初期値はホーム)
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // 2. 効果音とタイマー管理
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

    // 3. CSV出力
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportLogsToCsv(context, uri)
    }

    // 4. ダイアログ管理
    var editingQuest by remember { mutableStateOf<Quest?>(null) }

    // --- 画面構成 (Scaffoldでボトムバーを設置) ---
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
        // 画面の切り替え
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {

            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        status = status,
                        // DAOでソート済みなので、先頭が「一番緊急（期限が近い/古い）」なクエスト
                        urgentQuest = quests.firstOrNull(),
                        currentTime = currentTime,
                        onExportCsv = { exportLauncher.launch("quest_logs_backup.csv") },
                        onEdit = { editingQuest = it },
                        onToggleTimer = { viewModel.toggleTimer(it) },
                        onComplete = {
                            soundManager.playCoinSound()
                            viewModel.completeQuest(it)
                        }
                    )
                }
                Screen.LIST -> {
                    QuestListContent(
                        quests = quests,
                        currentTime = currentTime,
                        onEdit = { editingQuest = it },
                        onToggleTimer = { viewModel.toggleTimer(it) },
                        onComplete = {
                            soundManager.playCoinSound()
                            viewModel.completeQuest(it)
                        },
                        onDelete = { viewModel.deleteQuest(it) }
                    )
                }
                Screen.ADD -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "新規クエスト受注",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                        )
                        QuestInputForm(
                            onAddQuest = { title, note, date, diff, repeat, time ->
                                viewModel.addQuest(title, note, date, diff, repeat, time)
                                // 追加したら一覧へ戻る
                                currentScreen = Screen.LIST
                            }
                        )
                    }
                }
            }
        }
    }

    // 編集ダイアログ (全画面共通)
    if (editingQuest != null) {
        QuestEditDialog(
            quest = editingQuest!!,
            onDismiss = { editingQuest = null },
            onConfirm = { updatedQuest ->
                viewModel.updateQuest(updatedQuest)
                editingQuest = null
            }
        )
    }
}

// --- ホーム画面コンポーネント ---
@Composable
fun HomeScreen(
    status: UserStatus,
    urgentQuest: Quest?,
    currentTime: Long,
    onExportCsv: () -> Unit,
    onEdit: (Quest) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. ステータス表示
        StatusCard(status)

        // CSVボタン
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onExportCsv) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV出力")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 最優先クエスト表示エリア
        Text(
            text = "⚠️ CURRENT OBJECTIVE ⚠️",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (urgentQuest != null) {
            // ★専用のリッチなカードコンポーネントを使用
            UrgentQuestCard(
                quest = urgentQuest,
                currentTime = currentTime,
                onToggleTimer = { onToggleTimer(urgentQuest) },
                onComplete = { onComplete(urgentQuest) },
                onEdit = { onEdit(urgentQuest) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "今すぐ取り掛かりましょう！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            // クエストがない場合
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("全てのクエストを完了しました！", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("「受注」タブから新しい目標を追加してください。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// --- 入力フォーム ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestInputForm(
    onAddQuest: (String, String, Long?, Int, Int, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var difficulty by remember { mutableIntStateOf(1) }
    var repeatMode by remember { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("クエスト名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限設定") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    if (dueDate != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { dueDate = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "クリア", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
            }
            Spacer(modifier = Modifier.height(8.dp))

            TimeInputRow(
                hours = hours, onHoursChange = { hours = it },
                minutes = minutes, onMinutesChange = { minutes = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it })
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val estimatedMillis = (h * 60 * 60 * 1000) + (m * 60 * 1000)

                    onAddQuest(title, note, dueDate, difficulty, repeatMode, estimatedMillis)

                    title = ""
                    note = ""
                    dueDate = null
                    hours = ""
                    minutes = ""
                    difficulty = 1
                    repeatMode = 0
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("クエストを受注する")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

// --- リスト表示 ---
@Composable
fun QuestListContent(
    quests: List<Quest>,
    currentTime: Long,
    onEdit: (Quest) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onDelete: (Quest) -> Unit
) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("クエストがありません", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = quests, key = { it.id }) { quest ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(quest)
                        true
                    } else false
                }
            )

            // タイマー実行中はスワイプ操作を禁止する
            val isTimerRunning = quest.lastStartTime != null

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                gesturesEnabled = !isTimerRunning,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer, shape = CardDefaults.shape)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                },
                content = {
                    QuestItem(
                        quest = quest,
                        currentTime = currentTime,
                        onClick = { onEdit(quest) },
                        onToggleTimer = { onToggleTimer(quest) },
                        onComplete = { onComplete(quest) }
                    )
                }
            )
        }
    }
}