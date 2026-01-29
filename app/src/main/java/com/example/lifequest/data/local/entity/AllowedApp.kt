package com.example.lifequest.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowed_apps")
data class AllowedApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val iconUri: String? = null // 必要であればアイコンURIも保存
)