package com.example.lifequest

import androidx.room.Embedded
import androidx.room.Relation

// クエスト本体と、それに紐づくサブタスクリストをまとめて扱うクラス
data class QuestWithSubtasks(
    @Embedded val quest: Quest,

    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val subtasks: List<Subtask> = emptyList()
)