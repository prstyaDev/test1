package com.prstyadev.wibufy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anime_detail_cache")
data class AnimeDetailEntity(
    @PrimaryKey val animeId: String,
    val title: String?,
    val poster: String?,
    val synopsis: String?,
    val rating: String?,
    val genresJson: String?,
    val episodesJson: String?,
    val status: String? = null,
    val type: String? = null,
    val duration: String? = null,
    val japanese: String? = null,
    val synonyms: String? = null,
    val english: String? = null,
    val season: String? = null,
    val studios: String? = null,
    val producers: String? = null,
    val aired: String? = null,
    val trailer: String? = null,
    val rawDetailJson: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
