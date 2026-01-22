package com.example.lifequest.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.Quest
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.ui.components.RepeatSelector
import com.example.lifequest.ui.components.TimeInputRow
import com.example.lifequest.utils.formatDate

@Composable
fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎉 LEVEL UP! 🎉") },
        text = { Text("レベルが $level になりました！") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestEditDialog(
    questWithSubtasks: QuestWithSubtasks,
    onDismiss: () -> Unit,
    onConfirm: (Quest) -> Unit,
    onAddSubtask: (String) -> Unit,
    onDeleteSubtask: (Subtask) -> Unit
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    var title by remember { mutableStateOf(quest.title) }
    var note by remember { mutableStateOf(quest.note) }
    var dueDate by remember { mutableStateOf(quest.dueDate) }
    var repeatMode by remember { mutableIntStateOf(quest.repeatMode) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    var inputHours by remember {
        mutableStateOf((quest.estimatedTime / (1000 * 60 * 60)).toString().let { if(it=="0") "" else it })
    }
    var inputMinutes by remember {
        mutableStateOf(((quest.estimatedTime / (1000 * 60)) % 60).toString().let { if(it=="0") "" else it })
    }

    var newSubtaskTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("クエスト修正") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("クエスト名") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("メモ") }, maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))

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

                Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                        repeatMode = repeatMode
                        // difficulty は削除
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