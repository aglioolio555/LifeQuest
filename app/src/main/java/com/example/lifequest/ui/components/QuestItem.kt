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
import androidx.compose.ui.graphics.compositeOver // ★追加
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
    // 難易度に応じた色とラベル
    val (difficultyColor, difficultyText) = when (quest.difficulty) {
        0 -> MaterialTheme.colorScheme.primary to "EASY"
        2 -> MaterialTheme.colorScheme.error to "HARD"
        else -> MaterialTheme.colorScheme.secondary to "NORMAL"
    }

    // タイマー計算: 実行中なら (現在時刻 - 開始時刻) を加算して表示
    val isRunning = quest.lastStartTime != null
    val displayTime = if (isRunning) {
        quest.accumulatedTime + (currentTime - quest.lastStartTime!!)
    } else {
        quest.accumulatedTime
    }

    // ★修正ポイント: タイマー実行中の背景色を「不透明」にする
    // 単に alpha=0.3f だと透けてしまうため、compositeOver を使って
    // 「背景色(Surface)の上に薄い色を重ねた色」を計算して設定します。
    val containerColor = if (isRunning) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            .compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // タップで編集
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRunning) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側: 再生/停止ボタン
            IconButton(
                onClick = onToggleTimer,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isRunning) {
                    Text("||", fontWeight = FontWeight.Bold) // 簡易停止アイコン
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "開始")
                }
            }

            // 中央: 情報エリア
            Column(modifier = Modifier.weight(1f)) {
                // タイトル行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = quest.title, style = MaterialTheme.typography.titleMedium)
                    // リピートアイコン
                    if (quest.repeatMode > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "繰り返し",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 時間表示行 (現在時間 / 目標時間)
                Row(verticalAlignment = Alignment.Bottom) {
                    // 経過時間 (タイマー中は色を変える)
                    Text(
                        text = formatDuration(displayTime),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    // 目安時間 (設定されている場合のみ表示)
                    if (quest.estimatedTime > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "/ 目標 ${formatDuration(quest.estimatedTime)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 詳細行 (難易度・期限)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 難易度
                    Text(
                        text = "[$difficultyText]",
                        style = MaterialTheme.typography.labelSmall,
                        color = difficultyColor,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    // 期限
                    if (quest.dueDate != null) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = " ${formatDate(quest.dueDate!!)} ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // 右側: 完了ボタン
            IconButton(onClick = onComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "完了",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}