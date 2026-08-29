package com.prstyadev.wibufy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnimeDetailDao {
    @Query("SELECT * FROM anime_detail_cache WHERE animeId = :animeId LIMIT 1")
    suspend fun getAnimeDetail(animeId: String): AnimeDetailEntity?

    @Query("SELECT * FROM anime_detail_cache WHERE animeId IN (:animeIds)")
    suspend fun getAnimeDetails(animeIds: List<String>): List<AnimeDetailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnimeDetail(entity: AnimeDetailEntity)

    @Query("DELETE FROM anime_detail_cache WHERE animeId = :animeId")
    suspend fun deleteAnimeDetail(animeId: String)
}
