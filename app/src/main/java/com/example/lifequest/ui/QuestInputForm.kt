package com.example.lifequest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.ui.components.CategorySelector
import com.example.lifequest.ui.components.RepeatSelector

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
            Text("目安時間", style = MaterialTheme.typography.labelLarge)
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