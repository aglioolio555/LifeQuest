package com.example.lifequest.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("ホーム", Icons.Default.Home),
    LIST("クエスト", Icons.Default.List),
    ADD("受注", Icons.Default.Add),
    FOCUS("集中", Icons.Default.PlayArrow) // 追加
}