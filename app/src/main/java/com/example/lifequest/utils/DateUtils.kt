package com.example.lifequest.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

// 日付フォーマット (yyyy/MM/dd)
fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return formatter.format(Date(millis))
}
// ★追加: 日付と時間を表示
fun formatDateTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    return formatter.format(Date(timestamp))
}
// ★追加: 時間のみ表示
fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.JAPAN)
    return formatter.format(Date(timestamp))
}
// ★追加: 既存の日付に時間を合成する
fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
// ★追加: 日付から時間を抽出する
fun extractTime(dateMillis: Long): Pair<Int, Int> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = dateMillis
    return Pair(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
}
// 時間経過フォーマット (00:00:00)
fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}