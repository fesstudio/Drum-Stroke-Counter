package com.drummer.speed.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "practice_history")
data class SessionResult(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val strokes: Int,
    val duration: Int,
    val bpm: Int?,
    val timestamp: Long = System.currentTimeMillis(),
)
