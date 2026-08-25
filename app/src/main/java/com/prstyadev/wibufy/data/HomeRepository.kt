package com.prstyadev.wibufy.data

import android.content.Context

class HomeRepository(context: Context) {
    private val homeCacheDao = AppDatabase.getDatabase(context).homeCacheDao()

    suspend fun getCachedRecentAnime(sectionKey: String = "recent_anime_page1"): List<AnimeItem>? {
        val entity = homeCacheDao.getHomeCache(sectionKey) ?: return null
        return try {
            JsonUtils.animeItemListAdapter.fromJson(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCachedPage2Anime(sectionKey: String = "recent_anime_page2"): List<AnimeItem>? {
        val entity = homeCacheDao.getHomeCache(sectionKey) ?: return null
        return try {
            JsonUtils.animeItemListAdapter.fromJson(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchAndCacheRecentAnime(page: Int = 1): Pair<RecentData?, List<AnimeItem>> {
        val response = RetrofitClient.apiService.getRecentAnime(page = page)
        val items = response.data?.animeList ?: emptyList()
        if (items.isNotEmpty()) {
            val sectionKey = if (page == 1) "recent_anime_page1" else "recent_anime_page2"
            val json = JsonUtils.animeItemListAdapter.toJson(items)
            homeCacheDao.insertHomeCache(
                HomeCacheEntity(
                    sectionKey = sectionKey,
                    jsonContent = json,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return Pair(response.data, items)
    }
}
