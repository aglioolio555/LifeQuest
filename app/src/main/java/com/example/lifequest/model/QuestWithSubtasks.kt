package com.example.lifequest.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.lifequest.data.local.entity.Quest
import com.example.lifequest.data.local.entity.Subtask

// クエスト本体と、それに紐づくサブタスクリストをまとめて扱うクラス
data class QuestWithSubtasks(
    @Embedded val quest: Quest,

    @Relation(
        parentColumn = "id",
        entityColumn = "questId"
    )
    val subtasks: List<Subtask> = emptyList()
)