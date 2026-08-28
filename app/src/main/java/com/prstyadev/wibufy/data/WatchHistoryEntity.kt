package com.prstyadev.wibufy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val episodeSlug: String,
    val animeTitle: String? = null,
    val episodeName: String? = null,
    val posterUrl: String? = null,
    val lastPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
