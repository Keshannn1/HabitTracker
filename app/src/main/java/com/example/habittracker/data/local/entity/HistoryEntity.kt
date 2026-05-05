package com.example.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val routineName: String,
    val completionDate: Long,
    val totalTimeSpent: Long
)