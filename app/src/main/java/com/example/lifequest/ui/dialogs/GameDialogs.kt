package com.example.lifequest.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications // 追加
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.ui.components.RepeatSelector
import com.example.lifequest.ui.components.TimeInputRow
import com.example.lifequest.utils.combineDateAndTime // 追加
import com.example.lifequest.utils.extractTime // 追加
import com.example.lifequest.utils.formatDate
import com.example.lifequest.utils.formatTime // 追加
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.lifequest.DailyQuestType
import com.example.lifequest.logic.LocalSoundManager
import com.example.lifequest.ui.components.SoundButton
import com.example.lifequest.ui.components.SoundTextButton
import com.example.lifequest.ui.components.soundClickable


// ... (LevelUpDialog, QuestDetailsDialog, GiveUpConfirmDialog は変更なし) ...
// ★追加: 画面固定（ピン留め）の提案ダイアログ
//@Composable
//fun PinningConfirmDialog(
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("不退転の集中モード") },
//        text = {
//            Text(
//                "集中を最大化するために、画面固定機能（ピン留め）を使用しますか？\n\n" +
//                        "※固定中はホームボタンや他のアプリへの切り替えが制限され、物理的な「聖域」が作られます。"
//            )
//        },
//        confirmButton = {
//            Button(onClick = onConfirm) {
//                Text("固定して開始")
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text("通常モードで開始")
//            }
//        }
//    )
//}

// ★追加: 中断からの復帰（おかえり）ダイアログ
@Composable
fun WelcomeBackDialog(
    onResume: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {}, // 外側タップで閉じさせない
        title = { Text("⚠️ 集中が中断されました") },
        text = {
            Column {
                Text(
                    "先程はクエストから離脱してしまいました。\n" +
                            "しかし、まだ挽回できます。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "「一度始めたことは最後までやり遂げる。」",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("クエストを再開しますか？", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            SoundButton(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("再開する！")
            }
        }
    )
}

// ★修正: 既存のGiveUpConfirmDialogの文言と色を強化
//@Composable
//fun GiveUpConfirmDialog(
//    onDismiss: () -> Unit,
//    onConfirm: () -> Unit
//) {
//    AlertDialog(
//        containerColor = MaterialTheme.colorScheme.errorContainer, // 警告色背景
//        titleContentColor = MaterialTheme.colorScheme.onErrorContainer,
//        textContentColor = MaterialTheme.colorScheme.onErrorContainer,
//        onDismissRequest = onDismiss,
//        title = { Text("クエストを放棄しますか？") },
//        text = {
//            Text(
//                "今中断すると、ここまでの積み重ねが途切れてしまいます。\n\n" +
//                        "「あと少し」の踏ん張りが、自分を変える一歩になります。\n" +
//                        "本当に冒険を諦めますか？"
//            )
//        },
//        confirmButton = {
//            TextButton(
//                onClick = onConfirm,
//                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
//            ) {
//                Text("断念する")
//            }
//        },
//        dismissButton = {
//            SoundButton(
//                onClick = onDismiss,
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//            ) {
//                Text("集中を続ける！")
//            }
//        }
//    )
//}
//@Composable
//fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("🎉 LEVEL UP! 🎉") },
//        text = { Text("レベルが $level になりました！") },
//        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
//    )
//}

@Composable
fun QuestDetailsDialog(
    quest: Quest,
    subtasks: List<Subtask>,
    onDismiss: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
) {
    val soundManager = LocalSoundManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = quest.title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (quest.note.isNotBlank()) {
                    Text("メモ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(quest.note, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text("サブタスク", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (subtasks.isEmpty()) {
                    Text("サブタスクはありません", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                } else {
                    subtasks.forEach { sub ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .soundClickable { onSubtaskToggle(sub) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sub.isCompleted,
                                onCheckedChange = {
                                    soundManager.playClick()
                                    onSubtaskToggle(sub)
                                                  },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null,
                                color = if (sub.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { SoundTextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

@Composable
fun GiveUpConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("集中を中断しますか？") },
        text = { Text("今中断すると、このセッションのフローが途切れてしまいます。\n\n本当に終了しますか？") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("中断する")
            }
        },
        dismissButton = {
            SoundButton(onClick = onDismiss) {
                Text("続ける！")
            }
        }
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
    var showTimePicker by remember { mutableStateOf(false) } // ★追加
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { showDatePicker = true },
                            label = { Text(if (dueDate != null) formatDate(dueDate!!) else "期限なし") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )
                        // ★時間選択ボタンを追加
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
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis
                    if (date != null) {
                        // 時間情報は保持したいが、初期設定の場合は23:59などにする
                        val (currentH, currentM) = if (dueDate != null) extractTime(dueDate!!) else Pair(23, 59)
                        dueDate = combineDateAndTime(date, currentH, currentM)
                    }
                    showDatePicker = false
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
// デイリークエスト達成の特別ポップアップ
@Composable
fun DailyQuestCompletionDialog(
    type: DailyQuestType,
    expEarned: Int,
    onDismiss: () -> Unit
) {
    // ポップアップアニメーション
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    AlertDialog(
        onDismissRequest = {}, // タップで閉じない（ボタン必須）
        containerColor = Color.White, // 特別感を出すため白ベース（またはテーマ色）
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false), // 幅広に
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale.value)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. アイコンと光の演出
                    Box(contentAlignment = Alignment.Center) {
                        // 後ろの光（簡易）
                        Icon(
                            imageVector = Icons.Default.Star, // 光の代わり
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = type.color.copy(alpha = 0.2f)
                        )
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = type.color
                        )
                    }

                    // 2. タイトル
                    Text(
                        text = "DAILY QUEST CLEARED!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = type.color
                    )

                    Divider(color = type.color.copy(alpha = 0.5f), thickness = 2.dp, modifier = Modifier.width(60.dp))

                    // 3. 詳細テキスト
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = type.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = type.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4. 報酬表示
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GET REWARD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("+ $expEarned EXP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            SoundButton(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = type.color),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("素晴らしい！", fontWeight = FontWeight.Bold)
            }
        }
    )
}