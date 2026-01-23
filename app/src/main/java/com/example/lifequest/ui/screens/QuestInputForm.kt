package com.example.lifequest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications // 時間アイコン用
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.ui.components.CategorySelector
import com.example.lifequest.ui.dialogs.GameTimePickerDialog // ★追加
import com.example.lifequest.ui.components.RepeatSelector
import com.example.lifequest.utils.combineDateAndTime
import com.example.lifequest.utils.extractTime
import com.example.lifequest.utils.formatDate
import com.example.lifequest.utils.formatTime

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
    var showTimePicker by remember { mutableStateOf(false) } // ★追加
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

            // ... (目安時間のコードはそのまま) ...
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

            // ★期限設定UIの修正
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 日付選択ボタン
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限なし") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )

                    // 時間選択ボタン（日付が設定されている場合のみ表示）
                    if (dueDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = { Text(formatTime(dueDate!!)) },
                            leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                        )
                    }
                }

                RepeatSelector(currentMode = repeatMode, onModeSelected = { repeatMode = it })
            }

            CategorySelector(selectedCategory = category, onCategorySelected = { category = it })

            Spacer(modifier = Modifier.height(8.dp))
            // ... (サブタスクのコードはそのまま) ...
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
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // シアン
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = MaterialTheme.shapes.medium,
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

                    // リセット
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
            confirmButton = {
                TextButton(onClick = {
                    // 日付選択時、時間はデフォルトで23:59にするか、現在の時間を維持するか
                    // ここでは一旦 23:59 に設定する例 (または 00:00)
                    val date = datePickerState.selectedDateMillis
                    if (date != null) {
                        dueDate = combineDateAndTime(date, 23, 59)
                    }
                    showDatePicker = false
                    // 日付決定後に自動で時間選択を出すならここで showTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") } }
        ) { DatePicker(state = datePickerState) }
    }

    // ★時間選択ダイアログ
    if (showTimePicker && dueDate != null) {
        val (h, m) = extractTime(dueDate!!)
        GameTimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                dueDate = combineDateAndTime(dueDate!!, hour, minute)
                showTimePicker = false
            }
        )
    }
}