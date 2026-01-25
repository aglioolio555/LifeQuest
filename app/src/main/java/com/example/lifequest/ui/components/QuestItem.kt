package com.example.lifequest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifequest.QuestCategory
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.utils.formatDateTime
import com.example.lifequest.utils.formatDuration
import com.example.lifequest.logic.LocalSoundManager

@Composable
fun QuestItem(
    questWithSubtasks: QuestWithSubtasks,
    currentTime: Long,
    onClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit,
    isLarge: Boolean = false
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks
    val haptic = LocalHapticFeedback.current // Haptic

    val isRunning = quest.lastStartTime != null
    val displayTime = if (isRunning) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    val categoryEnum = QuestCategory.fromInt(quest.category)

    // Tech Noir Style: Translucent Glass
    val containerColor = if (isRunning) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surface // Themeで透過設定済み
    }

    val borderColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderWidth = if (isRunning) 1.dp else 0.5.dp

    val cardPadding = if (isLarge) 24.dp else 16.dp
    val titleStyle = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
    val timeStyle = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
    val iconSize = if (isLarge) 48.dp else 40.dp
    val completeIconSize = if (isLarge) 36.dp else 28.dp

    val soundManager = LocalSoundManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Feedback
                soundManager.playClick()
                onClick()
            }),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // フラット（枠線で表現）
        shape = MaterialTheme.shapes.medium, // 8dp
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(top = cardPadding, start = cardPadding, end = cardPadding, bottom = if(subtasks.isNotEmpty()) 8.dp else cardPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 開始ボタン
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        soundManager.playTimerStart()
                        onToggleTimer()
                    },
                    modifier = Modifier.size(iconSize).padding(end = 8.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isRunning) {
                        Text("||", fontWeight = FontWeight.Bold, style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "開始", modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = categoryEnum.icon,
                            contentDescription = categoryEnum.label,
                            modifier = Modifier.size(if (isLarge) 24.dp else 20.dp),
                            tint = categoryEnum.color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quest.title,
                            style = titleStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (quest.repeatMode > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Refresh, "繰り返し", modifier = Modifier.size(if (isLarge) 20.dp else 16.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatDuration(displayTime),
                            style = timeStyle.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), // Monospace
                            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (quest.estimatedTime > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ ${formatDuration(quest.estimatedTime)}",
                                style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (quest.dueDate != null) {
                            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(if (isLarge) 18.dp else 14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = " ${formatDateTime(quest.dueDate!!)} ",
                                style = if (isLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    soundManager.playQuestComplete()
                    onComplete()
                }, modifier = Modifier.size(completeIconSize)) {
                    Icon(Icons.Default.CheckCircle, "完了", modifier = Modifier.size(completeIconSize), tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (subtasks.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(start = cardPadding + 8.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)) {
                    subtasks.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubtaskToggle(subtask)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSubtaskToggle(subtask)
                                },
                                modifier = Modifier.size(24.dp),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val textColor = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}