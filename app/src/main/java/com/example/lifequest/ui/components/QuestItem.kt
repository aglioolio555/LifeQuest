package com.example.lifequest.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.Quest
import com.example.lifequest.utils.formatDate
import com.example.lifequest.utils.formatDuration

@Composable
fun QuestItem(
    quest: Quest,
    currentTime: Long,
    onClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit
) {
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

    val containerColor = if (isRunning)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleTimer,
                modifier = Modifier.size(40.dp).padding(end = 8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isRunning) {
                    Text("||", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "開始")
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = quest.title, style = MaterialTheme.typography.titleMedium)
                    if (quest.repeatMode > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = formatDuration(displayTime),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "[$difficultyText]", style = MaterialTheme.typography.labelSmall, color = difficultyColor, modifier = Modifier.padding(end = 8.dp))
                    if (quest.dueDate != null) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Text(text = " ${formatDate(quest.dueDate!!)} ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            IconButton(onClick = onComplete) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "完了", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}