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
import com.example.lifequest.utils.formatDate

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
    quest: Quest,
    onDismiss: () -> Unit,
    onConfirm: (Quest) -> Unit
) {
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
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限なし") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
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
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = datePickerState) }
    }
}