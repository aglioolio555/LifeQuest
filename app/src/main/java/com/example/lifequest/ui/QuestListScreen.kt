package com.example.lifequest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.ui.components.QuestItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestListContent(
    quests: List<QuestWithSubtasks>,
    currentTime: Long,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (com.example.lifequest.Quest) -> Unit,
    onComplete: (com.example.lifequest.Quest) -> Unit,
    onDelete: (com.example.lifequest.Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
) {
    if (quests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("現在アクティブなクエストはありません", color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = quests, key = { it.quest.id }) { item ->
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
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
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
}