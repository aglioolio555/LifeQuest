package com.example.lifequest.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifequest.GameViewModel
import com.example.lifequest.Quest
import com.example.lifequest.SoundManager
import com.example.lifequest.ui.components.*
import com.example.lifequest.ui.dialogs.*
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.delay

@Composable
fun GameScreen(viewModel: GameViewModel) {
    // 1. データと状態の監視
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()

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

    // --- 画面構成 ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ステータスカード
        StatusCard(status)

        // CSVボタン
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = { exportLauncher.launch("quest_logs_backup.csv") }) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CSV出力")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 入力フォーム (切り出し済み)
        QuestInputForm(
            onAddQuest = { title, note, date, diff, repeat, time ->
                viewModel.addQuest(title, note, date, diff, repeat, time)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // リスト表示 (切り出し済み)
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

    // 編集ダイアログ
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

// --- 以下、切り出したサブコンポーネント ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestInputForm(
    onAddQuest: (String, String, Long?, Int, Int, Long) -> Unit
) {
    // 入力状態はこのフォーム内だけで管理する
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
            // タイトル
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("クエスト名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 期限とリピート
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

            // 時間入力
            TimeInputRow(
                hours = hours, onHoursChange = { hours = it },
                minutes = minutes, onMinutesChange = { minutes = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 難易度
            DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it })
            Spacer(modifier = Modifier.height(12.dp))

            // 追加ボタン
            Button(
                onClick = {
                    val h = hours.toLongOrNull() ?: 0L
                    val m = minutes.toLongOrNull() ?: 0L
                    val estimatedMillis = (h * 60 * 60 * 1000) + (m * 60 * 1000)

                    onAddQuest(title, note, dueDate, difficulty, repeatMode, estimatedMillis)

                    // フォームリセット
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
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" クエスト受注")
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

@Composable
fun QuestListContent(
    quests: List<Quest>,
    currentTime: Long,
    onEdit: (Quest) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onDelete: (Quest) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
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

            // ★ タイマー実行中はスワイプ削除を禁止するロジック
            val isTimerRunning = quest.lastStartTime != null

            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                // ★ ここでジェスチャーを制御
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