package com.example.lifequest.ui.screens

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
import com.example.lifequest.data.local.entity.BreakActivity
import com.example.lifequest.FocusMode
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.logic.TimerState
import com.example.lifequest.ui.dialogs.GiveUpConfirmDialog
import com.example.lifequest.ui.dialogs.QuestDetailsDialog
import com.example.lifequest.utils.formatDuration
import androidx.compose.ui.platform.LocalContext
import com.example.lifequest.MainActivity
import com.example.lifequest.logic.LocalSoundManager
import com.example.lifequest.ui.components.SoundIconButton
// import com.example.lifequest.ui.dialogs.PinningConfirmDialog // 削除
import com.example.lifequest.ui.dialogs.WelcomeBackDialog

@Composable
fun FocusScreen(
    questWithSubtasks: QuestWithSubtasks,
    timerState: TimerState,
    currentBreakActivity: BreakActivity?,
    currentTime: Long,
    isInterrupted: Boolean = false,
    onResumeFromInterruption: () -> Unit = {},
    onToggleTimer: () -> Unit,
    onModeToggle: () -> Unit,
    onComplete: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    onExit: () -> Unit,
    onRerollBreakActivity: () -> Unit,
    onCompleteBreakActivity: () -> Unit
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
    val context = LocalContext.current
    val activity = context as? MainActivity
    val soundManager=LocalSoundManager.current


    //自動固定ロジック
    val handleStartTimer = {
        if (!timerState.isRunning && !timerState.isBreak) {
            // タイマー開始時は問答無用で画面固定を試行
            activity?.startPinning()
            onToggleTimer()
        } else {
            onToggleTimer()
        }
    }

    //完了時は固定解除
    val handleComplete = {
        activity?.stopPinning()
        onComplete()
    }

    //中断時は固定解除
    val handleExit = {
        activity?.stopPinning()
        onExit()
    }

    // 中断からの復帰ダイアログ
    if (isInterrupted) {
        WelcomeBackDialog(onResume = onResumeFromInterruption)
    }

    //PinningConfirmDialogの呼び出しブロック

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // ヘッダーボタン類
        SoundIconButton(
            onClick = { showDetailsDialog = true },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Info, contentDescription = "詳細", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SoundIconButton(
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
                            OutlinedButton(onClick = {
                                soundManager.playClick()
                                onRerollBreakActivity
                            }) {
                                Text("パス")
                            }
                            Button(onClick = {
                                soundManager.playClick()
                                onCompleteBreakActivity
                            }) {
                                Text("実行する (+XP)")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = handleStartTimer,
                        modifier = Modifier.size(80.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = animatedColor)
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (timerState.isRunning) "停止" else "開始",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = handleComplete,
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
                handleExit()
            }
        )
    }
}