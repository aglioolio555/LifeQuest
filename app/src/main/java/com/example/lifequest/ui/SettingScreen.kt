package com.example.lifequest.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.BreakActivity
import com.example.lifequest.UserStatus
import com.example.lifequest.ui.dialogs.GameTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    activities: List<BreakActivity>,
    userStatus: UserStatus,
    onAddActivity: (String, String) -> Unit,
    onDeleteActivity: (BreakActivity) -> Unit,
    onUpdateTargetTimes: (Int, Int, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // 時刻設定用ダイアログの表示管理
    var showWakeUpPicker by remember { mutableStateOf(false) }
    var showBedTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "追加")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

            // --- デイリークエスト設定セクション ---
            Text("デイリークエスト設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 起床時刻設定
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWakeUpPicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目標起床時刻", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "%02d:%02d".format(userStatus.targetWakeUpHour, userStatus.targetWakeUpMinute),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    HorizontalDivider()
                    // 就寝時刻設定
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBedTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("目標就寝時刻", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "%02d:%02d".format(userStatus.targetBedTimeHour, userStatus.targetBedTimeMinute),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 回復アクティビティ設定セクション ---
            Text(
                "回復アクティビティ設定",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "休憩時間に提案される行動リストです。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(activities) { activity ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activity.title, style = MaterialTheme.typography.titleMedium)
                                Text(activity.description, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteActivity(activity) }) {
                                Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // 起床時刻選択ダイアログ
    if (showWakeUpPicker) {
        GameTimePickerDialog(
            initialHour = userStatus.targetWakeUpHour,
            initialMinute = userStatus.targetWakeUpMinute,
            onDismissRequest = { showWakeUpPicker = false },
            onConfirm = { h, m ->
                onUpdateTargetTimes(h, m, userStatus.targetBedTimeHour, userStatus.targetBedTimeMinute)
                showWakeUpPicker = false
            }
        )
    }

    // 就寝時刻選択ダイアログ
    if (showBedTimePicker) {
        GameTimePickerDialog(
            initialHour = userStatus.targetBedTimeHour,
            initialMinute = userStatus.targetBedTimeMinute,
            onDismissRequest = { showBedTimePicker = false },
            onConfirm = { h, m ->
                onUpdateTargetTimes(userStatus.targetWakeUpHour, userStatus.targetWakeUpMinute, h, m)
                showBedTimePicker = false
            }
        )
    }

    // 新規アクティビティ追加ダイアログ
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新しいアクティビティ") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("タイトル (例: 深呼吸)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("詳細 (例: 4秒吸って...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddActivity(title, description)
                            showAddDialog = false
                        }
                    }
                ) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("キャンセル") }
            }
        )
    }
}