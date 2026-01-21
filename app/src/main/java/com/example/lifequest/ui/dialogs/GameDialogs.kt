package com.example.lifequest.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.Quest
import com.example.lifequest.ui.components.DifficultySelector
import com.example.lifequest.ui.components.RepeatSelector
import com.example.lifequest.ui.components.TimeInputRow
import com.example.lifequest.utils.formatDate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask

@Composable
fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("LEVEL UP!!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column {
                Text("おめでとうございます！", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("勇者レベルが $level になりました。", style = MaterialTheme.typography.titleLarge)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("最高だ！") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditDialog(
    questWithSubtasks: QuestWithSubtasks, // ★型変更
    onDismiss: () -> Unit,
    onConfirm: (Quest) -> Unit,
    onAddSubtask: (String) -> Unit, // ★追加
    onDeleteSubtask: (Subtask) -> Unit // ★追加
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    var title by remember { mutableStateOf(quest.title) }
    var note by remember { mutableStateOf(quest.note) }
    var dueDate by remember { mutableStateOf(quest.dueDate) }
    var difficulty by remember { mutableIntStateOf(quest.difficulty) }
    var repeatMode by remember { mutableIntStateOf(quest.repeatMode) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    var inputHours by remember {
        mutableStateOf((quest.estimatedTime / (1000 * 60 * 60)).toString().let { if(it=="0") "" else it })
    }
    var inputMinutes by remember {
        mutableStateOf(((quest.estimatedTime / (1000 * 60)) % 60).toString().let { if(it=="0") "" else it })
    }

    // サブタスク追加用ステート
    var newSubtaskTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("クエスト修正") },
        text = {
            // LazyColumnだとダイアログ内でスクロールしやすいが、AlertDialogのtext内で使うと競合する場合があるため、
            // 要素数が少ない前提でColumn + verticalScrollを使うのが無難です。
            // ここでは簡易的にColumnを使います。
            Column(modifier = Modifier.fillMaxWidth()) { // 必要に応じて .verticalScroll(rememberScrollState())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))

                // --- 時間・日付など ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限なし") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
                }
                TimeInputRow(
                    hours = inputHours,
                    onHoursChange = { inputHours = it },
                    minutes = inputMinutes,
                    onMinutesChange = { inputMinutes = it }
                )
                DifficultySelector(selectedDifficulty = difficulty, onDifficultySelected = { difficulty = it })

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // --- サブタスク編集エリア ---
                Text("サブタスク", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("新しいサブタスク") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (newSubtaskTitle.isNotBlank()) {
                            onAddSubtask(newSubtaskTitle)
                            newSubtaskTitle = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "追加")
                    }
                }

                // 既存サブタスクリスト
                subtasks.forEach { sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("・ ${sub.title}", modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteSubtask(sub) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = inputHours.toLongOrNull() ?: 0L
                    val m = inputMinutes.toLongOrNull() ?: 0L
                    val newEstimated = (h * 60 * 60 * 1000) + (m * 60 * 1000)

                    onConfirm(quest.copy(
                        title = title,
                        note = note,
                        dueDate = dueDate,
                        estimatedTime = newEstimated,
                        difficulty = difficulty,
                        repeatMode = repeatMode
                    ))
                },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = datePickerState) }
    }
}