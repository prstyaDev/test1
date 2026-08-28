package com.prstyadev.wibufy.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class WatchHistoryRepository(context: Context) {
    private val watchHistoryDao = AppDatabase.getDatabase(context).watchHistoryDao()

    val allHistory: Flow<List<WatchHistoryEntity>> = watchHistoryDao.getAllHistory()

    suspend fun saveProgress(
        episodeSlug: String,
        animeTitle: String?,
        episodeName: String?,
        posterUrl: String?,
        lastPositionMs: Long,
        totalDurationMs: Long
    ) {
        val existing = watchHistoryDao.getHistoryBySlug(episodeSlug)
        val entity = WatchHistoryEntity(
            episodeSlug = episodeSlug,
            animeTitle = animeTitle ?: existing?.animeTitle,
            episodeName = episodeName ?: existing?.episodeName,
            posterUrl = posterUrl ?: existing?.posterUrl,
            lastPositionMs = lastPositionMs,
            totalDurationMs = if (totalDurationMs > 0) totalDurationMs else (existing?.totalDurationMs ?: 0L),
            timestamp = System.currentTimeMillis()
        )
        watchHistoryDao.insertOrUpdate(entity)
    }

    suspend fun getHistory(episodeSlug: String): WatchHistoryEntity? {
        return watchHistoryDao.getHistoryBySlug(episodeSlug)
    }

    suspend fun delete(history: WatchHistoryEntity) {
        watchHistoryDao.delete(history)
    }

    suspend fun deleteBySlugs(slugs: List<String>) {
        watchHistoryDao.deleteBySlugs(slugs)
    }

    suspend fun clearAll() {
        watchHistoryDao.clearAll()
    }
}
