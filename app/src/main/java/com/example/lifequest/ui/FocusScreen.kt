package com.example.lifequest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifequest.FocusMode
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.TimerState
import com.example.lifequest.ui.dialogs.GiveUpConfirmDialog // ★インポート
import com.example.lifequest.ui.dialogs.QuestDetailsDialog // ★インポート
import com.example.lifequest.utils.formatDuration

@Composable
fun FocusScreen(
    questWithSubtasks: QuestWithSubtasks,
    timerState: TimerState,
    currentTime: Long,
    onToggleTimer: () -> Unit,
    onModeToggle: () -> Unit,
    onComplete: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    onExit: () -> Unit
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    // ダイアログの状態管理
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }

    // 戻るボタンをフックして中断確認ダイアログを表示
    BackHandler {
        showGiveUpDialog = true
    }

    val accumulatedTime = if (quest.lastStartTime != null) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    val targetColor = if (timerState.isBreak) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    val animatedColor by animateColorAsState(targetColor, label = "focusColor")
    val backgroundColor = animatedColor.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左上: 詳細・サブタスク確認ボタン
        IconButton(
            onClick = { showDetailsDialog = true },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Info, contentDescription = "詳細", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // 右上: 閉じるボタン（中断）
        IconButton(
            onClick = { showGiveUpDialog = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Close, contentDescription = "閉じる", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (timerState.isBreak) "休憩タイム" else quest.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(48.dp))

            // タイマー表示エリア
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                val progress = if (timerState.mode == FocusMode.COUNT_UP) {
                    if (quest.estimatedTime > 0) accumulatedTime.toFloat() / quest.estimatedTime.toFloat() else 0f
                } else {
                    if (timerState.initialSeconds > 0) timerState.remainingSeconds.toFloat() / timerState.initialSeconds.toFloat() else 0f
                }

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = animatedColor,
                    strokeWidth = 24.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        onClick = onModeToggle,
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                        enabled = !timerState.isRunning
                    ) {
                        Text(
                            text = timerState.mode.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    val timeText = if (timerState.mode == FocusMode.COUNT_UP) {
                        formatDuration(accumulatedTime)
                    } else {
                        val m = timerState.remainingSeconds / 60
                        val s = timerState.remainingSeconds % 60
                        "%02d:%02d".format(m, s)
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                        fontWeight = FontWeight.Black,
                        color = animatedColor,
                        modifier = Modifier.clickable { onToggleTimer() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // 操作ボタン群
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier.size(96.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = animatedColor)
                ) {
                    Icon(
                        imageVector = if (timerState.isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (timerState.isRunning) "停止" else "開始",
                        modifier = Modifier.size(48.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onComplete,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完了",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    // ★分離したコンポーザブルを呼び出し
    if (showDetailsDialog) {
        QuestDetailsDialog(
            quest = quest,
            subtasks = subtasks,
            onDismiss = { showDetailsDialog = false },
            onSubtaskToggle = onSubtaskToggle
        )
    }

    if (showGiveUpDialog) {
        GiveUpConfirmDialog(
            onDismiss = { showGiveUpDialog = false },
            onConfirm = {
                showGiveUpDialog = false
                onExit()
            }
        )
    }
}