package com.example.lifequest.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifequest.QuestCategory
import com.example.lifequest.model.CategoryStats
import com.example.lifequest.model.DailyStats
import com.example.lifequest.model.StatisticsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    statistics: StatisticsData
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("歩みの軌跡", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 総合サマリーカード
            SummaryCards(statistics)

            // 2. カテゴリー別割合 (円グラフ)
            if (statistics.totalTime > 0) {
                CategoryPieChartCard(statistics.categoryBreakdown)
            }

            // 3. 週間活動グラフ (積み上げ棒グラフ)
            if (statistics.weeklyActivity.any { it.totalTime > 0 }) {
                WeeklyActivityChartCard(statistics.weeklyActivity)
            }
        }
    }
}

@Composable
fun SummaryCards(statistics: StatisticsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 完了クエスト数
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("完了クエスト", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "${statistics.totalQuests}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 総集中時間
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("総集中時間", style = MaterialTheme.typography.labelMedium)
                val totalHours = statistics.totalTime / (1000 * 60 * 60)
                val totalMinutes = (statistics.totalTime / (1000 * 60)) % 60
                Text(
                    text = "${totalHours}h ${totalMinutes}m",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryPieChartCard(breakdown: List<CategoryStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("属性分布 (カテゴリー別)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 円グラフ描画
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(breakdown)
                }

                Spacer(modifier = Modifier.width(24.dp))

                // 凡例
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    breakdown.forEach { stat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(stat.category.color, shape = RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${stat.category.label} (${(stat.percentage * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(data: List<CategoryStats>) {
    // アニメーション用
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "pieChart"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f // 12時の方向から開始
        val strokeWidth = 30.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth / 2

        data.forEach { stat ->
            val sweepAngle = 360f * stat.percentage * progress

            drawArc(
                color = stat.category.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun WeeklyActivityChartCard(weeklyData: List<DailyStats>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("週間アクティビティ (直近7日間)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // 最大値を求めて高さを正規化
            val maxTime = weeklyData.maxOfOrNull { it.totalTime }?.coerceAtLeast(1L) ?: 1L

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 積み上げ棒グラフ本体
                        StackedBar(
                            day = day,
                            maxTime = maxTime,
                            modifier = Modifier
                                .width(24.dp)
                                .weight(1f) // 親の高さに合わせて伸縮
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(day.dayOfWeek, style = MaterialTheme.typography.labelSmall)
                        Text(day.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun StackedBar(day: DailyStats, maxTime: Long, modifier: Modifier) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "barChart"
    )

    Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
        // 全体の高さに対する割合
        val heightFraction = (day.totalTime.toFloat() / maxTime.toFloat()) * progress

        Column(
            modifier = Modifier
                .fillMaxHeight(heightFraction)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        ) {
            // カテゴリごとに積み上げる
            // 順番を固定するためにQuestCategory.entriesを使用
            QuestCategory.entries.forEach { category ->
                val time = day.categoryTimes[category] ?: 0L
                if (time > 0) {
                    val weight = time.toFloat() / day.totalTime.toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(weight)
                            .background(category.color)
                    )
                }
            }
        }
    }
}