package com.example.lifequest.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(
        entity = Quest::class,
        parentColumns = ["id"],
        childColumns = ["questId"],
        onDelete = ForeignKey.CASCADE // クエストが消えたらサブタスクも消える
    )],
    indices = [Index("questId")]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questId: Int,
    val title: String,
    val isCompleted: Boolean = false
)