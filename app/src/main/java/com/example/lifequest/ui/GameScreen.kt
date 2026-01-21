package com.example.lifequest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifequest.GameViewModel
import com.example.lifequest.Quest
import com.example.lifequest.SoundManager
import com.example.lifequest.ui.components.* // StatusCard, QuestItem, Selectors, TimeInputRow
import com.example.lifequest.ui.dialogs.* // QuestEditDialog, LevelUpDialog
import com.example.lifequest.utils.formatDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel) {
    // データ監視
    val status by viewModel.uiState.collectAsState()
    val quests by viewModel.questList.collectAsState()

    // サウンド管理
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

    // タイマー表示用の現在時刻更新 (1秒ごと)
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // --- 入力フォームの状態変数 ---
    var inputTitle by remember { mutableStateOf("") }
    var inputNote by remember { mutableStateOf("") }
    var inputDueDate by remember { mutableStateOf<Long?>(null) }

    // ★目安時間の入力用
    var inputHours by remember { mutableStateOf("") }
    var inputMinutes by remember { mutableStateOf("") }

    var selectedDifficulty by remember { mutableIntStateOf(1) }
    var selectedRepeat by remember { mutableIntStateOf(0) }

    // ダイアログ制御
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var editingQuest by remember { mutableStateOf<Quest?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ステータス表示
        StatusCard(status)

        Spacer(modifier = Modifier.height(16.dp))

        // 入力カード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // タイトル
                OutlinedTextField(
                    value = inputTitle,
                    onValueChange = { inputTitle = it },
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
                    // 期限設定
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
                    // リピート設定
                    RepeatSelector(currentMode = selectedRepeat, onModeSelected = { selectedRepeat = it })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ★目安時間入力 (TimeInputRow)
                TimeInputRow(
                    hours = inputHours,
                    onHoursChange = { inputHours = it },
                    minutes = inputMinutes,
                    onMinutesChange = { inputMinutes = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 難易度
                DifficultySelector(selectedDifficulty = selectedDifficulty, onDifficultySelected = { selectedDifficulty = it })

                Spacer(modifier = Modifier.height(12.dp))

                // 追加ボタン
                Button(
                    onClick = {
                        // 時間・分をミリ秒に変換して保存
                        val h = inputHours.toLongOrNull() ?: 0L
                        val m = inputMinutes.toLongOrNull() ?: 0L
                        val estimatedMillis = (h * 60 * 60 * 1000) + (m * 60 * 1000)

                        viewModel.addQuest(
                            inputTitle,
                            inputNote,
                            inputDueDate,
                            selectedDifficulty,
                            selectedRepeat,
                            estimatedMillis // ★渡す
                        )

                        // フォームリセット
                        inputTitle = ""
                        inputNote = ""
                        inputDueDate = null
                        inputHours = ""   // ★リセット
                        inputMinutes = "" // ★リセット
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

        // クエストリスト
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = quests, key = { it.id }) { quest ->
                // スワイプ削除のロジック
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
                            onClick = { editingQuest = quest }, // タップで編集
                            onToggleTimer = { viewModel.toggleTimer(quest) },
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

    // 日付選択ダイアログ
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    inputDueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) { DatePicker(state = datePickerState) }
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