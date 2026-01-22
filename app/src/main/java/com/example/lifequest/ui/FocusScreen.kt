package com.example.lifequest.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.lifequest.BreakActivity // ★追加
import com.example.lifequest.FocusMode
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.TimerState
import com.example.lifequest.ui.dialogs.GiveUpConfirmDialog
import com.example.lifequest.ui.dialogs.QuestDetailsDialog
import com.example.lifequest.utils.formatDuration

@Composable
fun FocusScreen(
    questWithSubtasks: QuestWithSubtasks,
    timerState: TimerState,
    currentBreakActivity: BreakActivity?, // ★追加
    currentTime: Long,
    onToggleTimer: () -> Unit,
    onModeToggle: () -> Unit,
    onComplete: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    onExit: () -> Unit,
    onRerollBreakActivity: () -> Unit, // ★追加
    onCompleteBreakActivity: () -> Unit // ★追加
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showGiveUpDialog by remember { mutableStateOf(false) }

    BackHandler { showGiveUpDialog = true }

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
        // ヘッダーボタン類
        IconButton(
            onClick = { showDetailsDialog = true },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Info, contentDescription = "詳細", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

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

            Spacer(modifier = Modifier.height(32.dp))

            // タイマー表示
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
                val progress = if (timerState.mode == FocusMode.COUNT_UP) {
                    if (quest.estimatedTime > 0) accumulatedTime.toFloat() / quest.estimatedTime.toFloat() else 0f
                } else {
                    if (timerState.initialSeconds > 0) timerState.remainingSeconds.toFloat() / timerState.initialSeconds.toFloat() else 0f
                }

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = animatedColor,
                    strokeWidth = 20.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // モード表示 (休憩中は非表示または固定)
                    if (!timerState.isBreak) {
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
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
                        fontWeight = FontWeight.Black,
                        color = animatedColor,
                        modifier = Modifier.clickable { onToggleTimer() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ★変更: 休憩中でアクティビティがある場合は提案カードを表示
            if (timerState.isBreak && currentBreakActivity != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("回復の儀式", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentBreakActivity.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(currentBreakActivity.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = onRerollBreakActivity) {
                                Text("パス")
                            }
                            Button(onClick = onCompleteBreakActivity) {
                                Text("実行する (+XP)")
                            }
                        }
                    }
                }
            } else {
                // 通常の操作ボタン
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
            }
        }
    }

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