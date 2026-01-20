package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.lifequest.ui.theme.LifeQuestTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "lifequest-db"
        ).fallbackToDestructiveMigration().build()

        val viewModel = GameViewModel(db.userDao())

        setContent {
            LifeQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()

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

    // --- 現在時刻の更新用State (1秒ごとに更新) ---
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L) // 1秒待機
        }
    }

    var inputTitle by remember { mutableStateOf("") }
    var inputNote by remember { mutableStateOf("") }
    var inputDueDate by remember { mutableStateOf<Long?>(null) }
    var selectedDifficulty by remember { mutableIntStateOf(1) }
    var selectedRepeat by remember { mutableIntStateOf(0) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var editingQuest by remember { mutableStateOf<Quest?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusCard(status)
        Spacer(modifier = Modifier.height(16.dp))

        // 入力エリア
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = inputTitle, onValueChange = { inputTitle = it },
                    label = { Text("クエスト名") }, modifier = Modifier.fillMaxWidth(), singleLine = true
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
                            label = { Text(if (inputDueDate != null) formatDate(inputDueDate!!) else "期限設定") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )
                        if (inputDueDate != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { inputDueDate = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "クリア", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    RepeatSelector(currentMode = selectedRepeat, onModeSelected = { selectedRepeat = it })
                }

                Spacer(modifier = Modifier.height(8.dp))
                DifficultySelector(selectedDifficulty = selectedDifficulty, onDifficultySelected = { selectedDifficulty = it })
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.addQuest(inputTitle, inputNote, inputDueDate, selectedDifficulty, selectedRepeat)
                        inputTitle = ""
                        inputNote = ""
                        inputDueDate = null
                        selectedDifficulty = 1
                        selectedRepeat = 0
                    },
                    enabled = inputTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" クエスト受注")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = quests, key = { it.id }) { quest ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.deleteQuest(quest)
                            true
                        } else { false }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, shape = CardDefaults.shape).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) { Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.onErrorContainer) }
                    },
                    content = {
                        QuestItem(
                            quest = quest,
                            currentTime = currentTime, // ★現在時刻を渡す
                            onClick = { editingQuest = quest },
                            onToggleTimer = { viewModel.toggleTimer(quest) }, // ★タイマー切り替え
                            onComplete = {
                                soundManager.playCoinSound()
                                viewModel.completeQuest(quest)
                            }
                        )
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    inputDueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = datePickerState) }
    }

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

// 時間フォーマット関数 (ミリ秒 -> "00:00:00")
fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

// ★タイマー機能付きQuestItem
@Composable
fun QuestItem(
    quest: Quest,
    currentTime: Long,
    onClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit
) {
    val (difficultyColor, difficultyText) = when (quest.difficulty) {
        0 -> MaterialTheme.colorScheme.primary to "EASY"
        2 -> MaterialTheme.colorScheme.error to "HARD"
        else -> MaterialTheme.colorScheme.secondary to "NORMAL"
    }

    // タイマー計算
    val isRunning = quest.lastStartTime != null
    val displayTime = if (isRunning) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    // タイマー動作中はカードの色を少し変える演出
    val containerColor = if (isRunning)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側: 再生ボタン
            IconButton(
                onClick = onToggleTimer,
                modifier = Modifier.size(40.dp).padding(end = 8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                // PlayArrow がない場合は手動で追加するか、Text("▶")等で代用可。ここでは標準アイコンを使用
                // 停止アイコンがないため、PlayArrow と Check(あるいはPauseぽいもの)で代用
                // Androidの標準アイコンにPauseがない場合が多いので、isRunningなら四角(■)を表示する工夫
                if (isRunning) {
                    // 簡易的な停止マーク
                    Text("||", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "開始")
                }
            }

            // 中央: 情報
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = quest.title, style = MaterialTheme.typography.titleMedium)
                    if (quest.repeatMode > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // ★時間表示 (タイマー)
                Text(
                    text = formatDuration(displayTime),
                    style = MaterialTheme.typography.titleLarge, // 時間を目立たせる
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "[$difficultyText]", style = MaterialTheme.typography.labelSmall, color = difficultyColor, modifier = Modifier.padding(end = 8.dp))
                    if (quest.dueDate != null) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(text = " ${formatDate(quest.dueDate!!)} ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            // 右側: 完了ボタン
            IconButton(onClick = onComplete) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "完了", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- 以下、既存のコンポーネント ---

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelector(selectedDifficulty: Int, onDifficultySelected: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilterChip(
            selected = selectedDifficulty == 0, onClick = { onDifficultySelected(0) },
            label = { Text("EASY") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
        )
        FilterChip(
            selected = selectedDifficulty == 1, onClick = { onDifficultySelected(1) },
            label = { Text("NORMAL") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
        )
        FilterChip(
            selected = selectedDifficulty == 2, onClick = { onDifficultySelected(2) },
            label = { Text("HARD") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
        )
    }
}

@Composable
fun RepeatSelector(currentMode: Int, onModeSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("繰り返し: なし", "毎日", "毎週", "毎月")

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(modes[currentMode]) },
            leadingIcon = if (currentMode != 0) { { Icon(Icons.Default.Refresh, contentDescription = null) } } else null
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onModeSelected(index); expanded = false },
                    leadingIcon = if (index != 0) { { Icon(Icons.Default.Refresh, contentDescription = null) } } else null
                )
            }
        }
    }
}

@Composable
fun StatusCard(status: UserStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "Lv.", style = MaterialTheme.typography.titleMedium)
                Text(text = "${status.level}", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (status.maxExp > 0) status.currentExp.toFloat() / status.maxExp.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "EXP: ${status.currentExp} / ${status.maxExp}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${status.gold} G", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditDialog(quest: Quest, onDismiss: () -> Unit, onConfirm: (Quest) -> Unit) {
    var title by remember { mutableStateOf(quest.title) }
    var note by remember { mutableStateOf(quest.note) }
    var dueDate by remember { mutableStateOf(quest.dueDate) }
    var difficulty by remember { mutableIntStateOf(quest.difficulty) }
    var repeatMode by remember { mutableIntStateOf(quest.repeatMode) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("クエスト修正") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = { showDatePicker = true }, label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限なし") }, leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) })
                    RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
                }
                Spacer(modifier = Modifier.height(16.dp))
                DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(quest.copy(title = title, note = note, dueDate = dueDate, difficulty = difficulty, repeatMode = repeatMode)) },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }) { DatePicker(state = datePickerState) }
    }
}