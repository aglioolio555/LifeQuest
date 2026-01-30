package com.example.lifequest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.data.local.entity.UserStatus
import com.example.lifequest.ui.dialogs.GameTimePickerDialog
import com.example.lifequest.data.local.entity.ExtraQuest
import com.example.lifequest.ui.components.SoundIconButton
import com.example.lifequest.ui.components.soundClickable
import com.example.lifequest.ui.components.CategorySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    activities: List<BreakActivity>,
    userStatus: UserStatus,
    onAddActivity: (String, String) -> Unit,
    onDeleteActivity: (BreakActivity) -> Unit,
    onUpdateTargetTimes: (Int, Int, Int, Int) -> Unit,
    onExportQuestLogs: () -> Unit,
    onExportDailyQuests: () -> Unit,
    extraQuests: List<ExtraQuest> = emptyList(),
    onAddExtraQuest: (String, String, Int,Int) -> Unit = {_,_,_,_ ->},
    onDeleteExtraQuest: (ExtraQuest) -> Unit = {},
    onNavigateToWhitelist: () -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showExtraAddDialog by remember { mutableStateOf(false) }
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
        }
    ) { innerPadding ->
        // ★変更点1: Column ではなく LazyColumn を使用
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp), // 左右の余白を設定
            verticalArrangement = Arrangement.spacedBy(8.dp), // 全体の要素間隔を統一
            contentPadding = PaddingValues(vertical = 16.dp)  // 上下の余白を設定
        ) {

            // ★変更点2: 単一の要素は item { ... } で囲む

            // --- デイリークエスト設定 ---
            item {
                Text("デイリークエスト設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundClickable { showWakeUpPicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("目標起床時刻", style = MaterialTheme.typography.bodyLarge)
                            Text("%02d:%02d".format(userStatus.targetWakeUpHour, userStatus.targetWakeUpMinute), style = MaterialTheme.typography.titleLarge)
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundClickable { showBedTimePicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("目標就寝時刻", style = MaterialTheme.typography.bodyLarge)
                            Text("%02d:%02d".format(userStatus.targetBedTimeHour, userStatus.targetBedTimeMinute), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- データ管理セクション ---
            item {
                Text("データ管理", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundClickable { onExportQuestLogs() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("クエスト履歴出力 (CSV)", style = MaterialTheme.typography.bodyLarge)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundClickable { onExportDailyQuests() }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("デイリー記録出力 (CSV)", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- エキストラクエスト設定 ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("エキストラクエスト設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("全タスク完了後にランダム出現", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    SoundIconButton(onClick = { showExtraAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "追加", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ★変更点3: リストデータを直接 items(...) で展開 (入れ子のLazyColumnを削除)
            items(extraQuests) { extra ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(extra.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${extra.estimatedTime / 60000}分 / +${extra.expReward}EXP", style = MaterialTheme.typography.bodySmall)
                        }
                        SoundIconButton(onClick = { onDeleteExtraQuest(extra) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
//            if (extraQuests.isEmpty()) {
//                item { Text("登録なし", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) }
//            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // --- 回復アクティビティ設定 ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("回復アクティビティ設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("休憩時間に提案される行動リストです。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    SoundIconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "追加", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ★変更点4: 2つ目のリストもそのまま続けて記述可能
            items(activities) { activity ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activity.title, style = MaterialTheme.typography.titleMedium)
                            Text(activity.description, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDeleteActivity(activity) },modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            //AllowedAppSetting
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .soundClickable { onNavigateToWhitelist() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("アプリホワイトリスト設定", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("集中モード中に使用を許可するアプリ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }


            // 下部の余白確保
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showWakeUpPicker) {
        GameTimePickerDialog(
            initialHour = userStatus.targetWakeUpHour,
            initialMinute = userStatus.targetWakeUpMinute,
            onDismissRequest = { showWakeUpPicker = false },
            onConfirm = { h, m -> onUpdateTargetTimes(h, m, userStatus.targetBedTimeHour, userStatus.targetBedTimeMinute); showWakeUpPicker = false }
        )
    }
    if (showBedTimePicker) {
        GameTimePickerDialog(
            initialHour = userStatus.targetBedTimeHour,
            initialMinute = userStatus.targetBedTimeMinute,
            onDismissRequest = { showBedTimePicker = false },
            onConfirm = { h, m -> onUpdateTargetTimes(userStatus.targetWakeUpHour, userStatus.targetWakeUpMinute, h, m); showBedTimePicker = false }
        )
    }
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("新しいアクティビティ") },
            text = { Column { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("タイトル") }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("詳細") }) } },
            confirmButton = { TextButton(onClick = { if (title.isNotBlank()) { onAddActivity(title, description); showAddDialog = false } }) { Text("追加") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("キャンセル") } }
        )
    }
    if (showExtraAddDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var minutes by remember { mutableStateOf("15") }
        var category by remember { mutableIntStateOf(2) }

        AlertDialog(
            onDismissRequest = { showExtraAddDialog = false },
            title = { Text("エキストラクエスト追加") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("タイトル (例: 読書)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("詳細 (任意)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = minutes, onValueChange = { if(it.all{c->c.isDigit()}) minutes = it }, label = { Text("目安時間 (分)") })
                    Spacer(modifier = Modifier.height(8.dp))
                    CategorySelector(selectedCategory = category, onCategorySelected = { category = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onAddExtraQuest(title, desc, minutes.toIntOrNull() ?: 15,category)
                        showExtraAddDialog = false
                    }
                }) { Text("追加") }
            },
            dismissButton = { TextButton(onClick = { showExtraAddDialog = false }) { Text("キャンセル") } }
        )
    }
}
