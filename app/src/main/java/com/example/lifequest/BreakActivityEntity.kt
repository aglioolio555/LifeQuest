package com.example.lifequest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "break_activities")
data class BreakActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isDefault: Boolean = false
)