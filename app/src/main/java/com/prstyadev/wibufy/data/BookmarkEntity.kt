package com.prstyadev.wibufy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val animeId: String,
    val title: String,
    val poster: String?,
    val episodes: String? = null,
    val score: String? = null,
    val views: String? = null,
    val status: String? = "Ongoing",
    val timestamp: Long = System.currentTimeMillis()
)

typealias SubscribedAnimeEntity = BookmarkEntity
