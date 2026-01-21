package com.example.lifequest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// 難易度選択
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultySelector(selectedDifficulty: Int, onDifficultySelected: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        FilterChip(
            selected = selectedDifficulty == 0, onClick = { onDifficultySelected(0) },
            label = { Text("EASY") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
        )
        FilterChip(
            selected = selectedDifficulty == 1, onClick = { onDifficultySelected(1) },
            label = { Text("NORMAL") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer)
        )
        FilterChip(
            selected = selectedDifficulty == 2, onClick = { onDifficultySelected(2) },
            label = { Text("HARD") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
        )
    }
}

// リピート選択
@Composable
fun RepeatSelector(currentMode: Int, onModeSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf("繰り返し: なし", "毎日", "毎週", "毎月")

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(modes[currentMode]) },
            leadingIcon = if (currentMode != 0) { { Icon(Icons.Default.Refresh, contentDescription = null) } } else null
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onModeSelected(index); expanded = false },
                    leadingIcon = if (index != 0) { { Icon(Icons.Default.Refresh, contentDescription = null) } } else null
                )
            }
        }
    }
}