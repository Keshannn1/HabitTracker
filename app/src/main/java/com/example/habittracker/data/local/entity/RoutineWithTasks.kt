package com.example.habittracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithTasks(
    @Embedded val routine: RoutineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val tasks: List<TaskEntity>
)
