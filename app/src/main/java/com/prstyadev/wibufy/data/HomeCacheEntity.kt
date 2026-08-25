package com.prstyadev.wibufy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_cache")
data class HomeCacheEntity(
    @PrimaryKey val sectionKey: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)
