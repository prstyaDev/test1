package com.prstyadev.wibufy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: WatchHistoryEntity)

    @Delete
    suspend fun delete(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE episodeSlug IN (:slugs)")
    suspend fun deleteBySlugs(slugs: List<String>)

    @Query("DELETE FROM watch_history WHERE episodeSlug = :slug")
    suspend fun deleteBySlug(slug: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()

    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE episodeSlug = :slug LIMIT 1")
    suspend fun getHistoryBySlug(slug: String): WatchHistoryEntity?
}
