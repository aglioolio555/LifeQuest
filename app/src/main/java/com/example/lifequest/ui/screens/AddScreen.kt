package com.example.lifequest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.logic.SoundManager
import com.example.lifequest.ui.components.QuestInputForm

// QuestInputFormがこのパッケージにある、もしくはインポートされている前提です

@Composable
fun AddQuestScreen(
    // QuestInputFormのコールバックに合わせて型を調整してください
    // 例: dateがLong型、repeatがString型の場合
    onAddQuest: (String, String, Long?, Int, Int, Long, List<String>) -> Unit,
    soundManager: SoundManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "NEW QUEST_ENTRY",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        QuestInputForm(
            onAddQuest = onAddQuest
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}