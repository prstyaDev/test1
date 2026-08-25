package com.prstyadev.wibufy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_cache")
data class ScheduleCacheEntity(
    @PrimaryKey val dayName: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)
