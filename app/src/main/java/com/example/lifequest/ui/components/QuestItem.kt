package com.example.lifequest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifequest.QuestCategory
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.utils.formatDate
import com.example.lifequest.utils.formatDuration

@Composable
fun QuestItem(
    questWithSubtasks: QuestWithSubtasks, // ★型変更
    currentTime: Long,
    onClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit,
    onSubtaskToggle: (Subtask) -> Unit, // ★追加
    isLarge: Boolean = false
) {
    val quest = questWithSubtasks.quest
    val subtasks = questWithSubtasks.subtasks

    val (difficultyColor, difficultyText) = when (quest.difficulty) {
        0 -> MaterialTheme.colorScheme.primary to "EASY"
        2 -> MaterialTheme.colorScheme.error to "HARD"
        else -> MaterialTheme.colorScheme.secondary to "NORMAL"
    }

    val isRunning = quest.lastStartTime != null
    val displayTime = if (isRunning) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    val categoryEnum = QuestCategory.fromInt(quest.category)

    val containerColor = if (isRunning) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            .compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardPadding = if (isLarge) 24.dp else 16.dp
    val titleStyle = if (isLarge) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
    val timeStyle = if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
    val iconSize = if (isLarge) 48.dp else 40.dp
    val completeIconSize = if (isLarge) 36.dp else 28.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 上段：メイン情報
            Row(
                modifier = Modifier
                    .padding(top = cardPadding, start = cardPadding, end = cardPadding, bottom = if(subtasks.isNotEmpty()) 8.dp else cardPadding)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左側: 再生/停止ボタン
                IconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .size(iconSize)
                        .padding(end = 8.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (isRunning) {
                        Text("||", fontWeight = FontWeight.Bold, style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "開始", modifier = Modifier.size(if (isLarge) 32.dp else 24.dp))
                    }
                }

                // 中央: 情報エリア
                Column(modifier = Modifier.weight(1f)) {
                    // タイトル行
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = categoryEnum.icon,
                            contentDescription = categoryEnum.label,
                            modifier = Modifier.size(if (isLarge) 24.dp else 20.dp),
                            tint = categoryEnum.color
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = quest.title, style = titleStyle)

                        if (quest.repeatMode > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "繰り返し",
                                modifier = Modifier.size(if (isLarge) 20.dp else 16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                    // 時間表示行
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatDuration(displayTime),
                            style = timeStyle,
                            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (quest.estimatedTime > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ 目標 ${formatDuration(quest.estimatedTime)}",
                                style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                    // 詳細行
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "[$difficultyText]",
                            style = if (isLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                            color = difficultyColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        if (quest.dueDate != null) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(if (isLarge) 18.dp else 14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = " ${formatDate(quest.dueDate!!)} ",
                                style = if (isLarge) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // 右側: 完了ボタン
                IconButton(onClick = onComplete, modifier = Modifier.size(completeIconSize)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "完了",
                        modifier = Modifier.size(completeIconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 下段：サブタスクリスト
            if (subtasks.isNotEmpty()) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(start = cardPadding + 8.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)) {
                    subtasks.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSubtaskToggle(subtask) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { onSubtaskToggle(subtask) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
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