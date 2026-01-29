package com.example.lifequest.ui.components

import androidx.compose.foundation.horizontalScroll // ★追加
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // ★追加
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp // ★追加
import com.example.lifequest.QuestCategory // ★追加
import com.example.lifequest.RepeatMode
import com.example.lifequest.logic.LocalSoundManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatSelector(
    currentMode: Int,
    onModeSelected: (Int) -> Unit
) {
    val soundManager = LocalSoundManager.current
    val mode = RepeatMode.fromInt(currentMode)
    var expanded = false // 簡易実装のため、ここではドロップダウンではなく順次切り替え方式にします

    AssistChip(
        onClick = {
            soundManager.playClick()
            // 0->1->2->3->0 の順で切り替え
            val nextMode = (currentMode + 1) % RepeatMode.entries.size
            onModeSelected(nextMode)
        },
        label = {
            val text = when (mode) {
                RepeatMode.NONE -> "繰り返しなし"
                RepeatMode.DAILY -> "毎日"
                RepeatMode.WEEKLY -> "毎週"
                RepeatMode.MONTHLY -> "毎月"
            }
            Text(text)
        },
        leadingIcon = {
            if (mode != RepeatMode.NONE) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (mode != RepeatMode.NONE) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
        )
    )
}

// ★追加したカテゴリセレクター
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedCategory: Int,
    onCategorySelected: (Int) -> Unit
) {
    val soundManager = LocalSoundManager.current
    Column {
        Text("カテゴリ", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()), // 横スクロール対応
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestCategory.entries.forEach { category ->
                FilterChip(
                    selected = category.id == selectedCategory,
                    onClick = {
                        soundManager.playClick()
                        onCategorySelected(category.id)
                              },
                    label = { Text(category.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (category.id == selectedCategory) MaterialTheme.colorScheme.onPrimaryContainer else category.color
                        )
                    }
                )
            }
        }
    }
}