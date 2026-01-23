package com.example.lifequest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.data.local.entity.DailyQuestProgress
import com.example.lifequest.QuestCategory
import com.example.lifequest.model.QuestWithSubtasks
import com.example.lifequest.data.local.entity.Subtask
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.ui.components.QuestItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestListContent(
    quests: List<QuestWithSubtasks>,
    futureQuests: List<QuestWithSubtasks>, // ★追加: 未来のクエストリスト
    dailyProgress: DailyQuestProgress,
    currentTime: Long,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (Quest) -> Unit,
    onComplete: (Quest) -> Unit,
    onDelete: (Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- デイリークエストセクション ---
        item {
            DailyQuestSection(dailyProgress)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "アクティブクエスト",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- 通常のクエストリスト (今日やるべきもの) ---
        if (quests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("現在アクティブなクエストはありません", color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            items(items = quests, key = { it.quest.id }) { item ->
                // ... (既存のSwipeToDismissBox処理) ...
                val quest = item.quest
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(quest)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    content = {
                        QuestItem(
                            questWithSubtasks = item,
                            currentTime = currentTime,
                            onClick = { onEdit(item) },
                            onToggleTimer = { onToggleTimer(quest) },
                            onComplete = { onComplete(quest) },
                            onSubtaskToggle = onSubtaskToggle
                        )
                    }
                )
            }
        }

        // --- ★追加: 今後のリピート予定（折りたたみ） ---
        if (futureQuests.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                FutureQuestsSection(futureQuests = futureQuests, onEdit = onEdit)
            }
        }
    }
}

// ★追加: 未来のクエストを表示するセクション
@Composable
fun FutureQuestsSection(
    futureQuests: List<QuestWithSubtasks>,
    onEdit: (QuestWithSubtasks) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "今後のリピート予定 (${futureQuests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    futureQuests.forEach { item ->
                        // 簡易表示（タイマーなどは不要なので、タイトルと日付のみ）
                        ListItem(
                            headlineContent = { Text(item.quest.title, style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = {
                                val date = item.quest.dueDate
                                if (date != null) {
                                    Text("次回予定: ${com.example.lifequest.utils.formatDate(date)}")
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = com.example.lifequest.QuestCategory.fromInt(item.quest.category).icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier.clickable { onEdit(item) }
                        )
                    }
                }
            }
        }
    }
}

// ... (DailyQuestSection以降はそのまま)
@Composable
fun DailyQuestSection(progress: DailyQuestProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("デイリーミッション", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // 1. 起床 & スマホ断ちミッション
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DailyMissionItem(
                    title = "早起き",
                    isCleared = progress.isWakeUpCleared,
                    icon = Icons.Default.WbSunny,
                    modifier = Modifier.weight(1f)
                )
                DailyMissionItem(
                    title = "早起き",
                    isCleared = progress.isBedTimeCleared,
                    icon = Icons.Default.Bedtime,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 2. 集中リミット (段階的な進捗バー)
            Text("集中リミット", style = MaterialTheme.typography.labelMedium)
            val hours = progress.totalFocusTime / (1000 * 60 * 60)
            val progressPercent = (hours.toFloat() / 10f).coerceIn(0f, 1f) // 最大10時間で計算

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progressPercent },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${hours}h / 10h", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. バランスミッション (カテゴリー別の達成状況)
            Text("バランス", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuestCategory.entries.filter { it != QuestCategory.OTHER }.forEach { cat ->
                    val isCleared = progress.hasCategoryCleared(cat.id)
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = cat.label,
                        tint = if (isCleared) cat.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyMissionItem(title: String, isCleared: Boolean, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (isCleared) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        border = if (!isCleared) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isCleared) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCleared) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}