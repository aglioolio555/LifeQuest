package com.example.lifequest.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 日付フォーマット (yyyy/MM/dd)
fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return formatter.format(Date(millis))
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