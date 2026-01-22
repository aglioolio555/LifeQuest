package com.example.lifequest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifequest.FocusMode
import com.example.lifequest.QuestCategory
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.TimerState
import com.example.lifequest.utils.formatDate
import com.example.lifequest.utils.formatDuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun UrgentQuestCard(
    questWithSubtasks: QuestWithSubtasks,
    timerState: TimerState, // ViewModelから渡される状態
    currentTime: Long,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    onModeToggle: () -> Unit // モード切り替え用コールバック
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    // カウントアップ用の時間計算（従来ロジック）
    val accumulatedTime = if (quest.lastStartTime != null) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    val categoryEnum = QuestCategory.fromInt(quest.category)

    // ★色設定: 休憩中は緑、集中時はプライマリ色
    val targetColor = if (timerState.isBreak) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val animatedColor by animateColorAsState(targetColor, label = "color")

    val containerColor = if (timerState.isRunning) {
        animatedColor.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
            .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            // ヘッダー
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = categoryEnum.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = categoryEnum.color
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if(timerState.isBreak) "休憩中: 次の集中へ備えよう" else quest.title,
                    fontSize = 40.sp,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ★円形プログレスバーとタイマー
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                // 進捗率の計算
                val progress = if (timerState.mode == FocusMode.COUNT_UP) {
                    if (quest.estimatedTime > 0) accumulatedTime.toFloat() / quest.estimatedTime.toFloat() else 0f
                } else {
                    if (timerState.initialSeconds > 0) timerState.remainingSeconds.toFloat() / timerState.initialSeconds.toFloat() else 0f
                }

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = animatedColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 時間表示
                    val timeText = if (timerState.mode == FocusMode.COUNT_UP) {
                        formatDuration(accumulatedTime)
                    } else {
                        // 残り秒数を mm:ss 形式に
                        val m = timerState.remainingSeconds / 60
                        val s = timerState.remainingSeconds % 60
                        "%02d:%02d".format(m, s)
                    }

                    // モード変更可能なテキストボタン
                    TextButton(onClick = onModeToggle, enabled = !timerState.isRunning) {
                        Text(
                            text = timerState.mode.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                        fontWeight = FontWeight.Black,
                        color = animatedColor,
                        modifier = Modifier.clickable { onToggleTimer() } // タイマータップでも開始停止
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 操作ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = animatedColor)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "停止" else "開始",
                        modifier = Modifier.size(40.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完了",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // フッター情報
            if (quest.dueDate != null || subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (subtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    subtasks.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubtaskToggle(subtask) }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { onSubtaskToggle(subtask) },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                                color = if (subtask.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}