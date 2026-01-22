package com.example.lifequest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.QuestWithSubtasks
import com.example.lifequest.Subtask
import com.example.lifequest.TimerState
import com.example.lifequest.UserStatus
import com.example.lifequest.ui.components.StatusCard
import com.example.lifequest.ui.components.UrgentQuestCard

@Composable
fun HomeScreen(
    status: UserStatus,
    urgentQuestData: QuestWithSubtasks?,
    timerState: TimerState,
    currentTime: Long,
    onExportCsv: () -> Unit,
    onEdit: (QuestWithSubtasks) -> Unit,
    onToggleTimer: (com.example.lifequest.Quest) -> Unit,
    onComplete: (com.example.lifequest.Quest) -> Unit,
    onSubtaskToggle: (Subtask) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusCard(status)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onExportCsv) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("CSV出力")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("⚠️ CURRENT OBJECTIVE ⚠️", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (urgentQuestData != null) {
            UrgentQuestCard(
                questWithSubtasks = urgentQuestData,
                currentTime = currentTime,
                onToggleTimer = { onToggleTimer(urgentQuestData.quest) },
                onComplete = { onComplete(urgentQuestData.quest) },
                onEdit = { onEdit(urgentQuestData) },
                onSubtaskToggle = onSubtaskToggle
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if(timerState.isBreak) "しっかりと休息をとりましょう" else "集中画面へ移行して開始しましょう",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("全てのクエストを完了しました！")
                }
            }
        }
    }
}