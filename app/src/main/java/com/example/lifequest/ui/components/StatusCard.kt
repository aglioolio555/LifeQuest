package com.example.lifequest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifequest.UserStatus

@Composable
fun StatusCard(status: UserStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "Lv.", style = MaterialTheme.typography.titleMedium)
                Text(text = "${status.level}", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(start = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (status.maxExp > 0) status.currentExp.toFloat() / status.maxExp.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "EXP: ${status.currentExp} / ${status.maxExp}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${status.gold} G", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}