package com.prstyadev.wibufy.data

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ScheduleFetchResult(
    val scheduleMap: Map<Int, List<ScheduleAnimeItem>>,
    val ongoingList: List<AnimeItem>
)

class ScheduleRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val scheduleCacheDao = database.scheduleCacheDao()
    private val homeCacheDao = database.homeCacheDao()

    suspend fun getCachedSchedule(): Map<Int, List<ScheduleAnimeItem>> {
        val cachedEntities = scheduleCacheDao.getAllScheduleCache()
        if (cachedEntities.isEmpty()) return emptyMap()

        val resultMap = mutableMapOf<Int, List<ScheduleAnimeItem>>()
        for (entity in cachedEntities) {
            val dayIndex = entity.dayName.toIntOrNull() ?: continue
            try {
                val animeList = JsonUtils.scheduleAnimeListAdapter.fromJson(entity.jsonContent)
                if (animeList != null) {
                    resultMap[dayIndex] = animeList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return resultMap
    }

    suspend fun getCachedOngoingAnime(): List<AnimeItem> {
        val list = mutableListOf<AnimeItem>()
        try {
            val page1Entity = homeCacheDao.getHomeCache("recent_anime_page1")
            if (page1Entity != null) {
                val p1 = JsonUtils.animeItemListAdapter.fromJson(page1Entity.jsonContent)
                if (!p1.isNullOrEmpty()) list.addAll(p1)
            }
            val page2Entity = homeCacheDao.getHomeCache("recent_anime_page2")
            if (page2Entity != null) {
                val p2 = JsonUtils.animeItemListAdapter.fromJson(page2Entity.jsonContent)
                if (!p2.isNullOrEmpty()) list.addAll(p2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    suspend fun fetchAndCacheSchedule(): ScheduleFetchResult = coroutineScope {
        val scheduleDeferred = async {
            try {
                RetrofitClient.apiService.getSchedule()
            } catch (e: Exception) {
                null
            }
        }

        val ongoing1Deferred = async {
            try {
                RetrofitClient.apiService.getRecentAnime(page = 1)
            } catch (e: Exception) {
                null
            }
        }

        val ongoing2Deferred = async {
            try {
                RetrofitClient.apiService.getRecentAnime(page = 2)
            } catch (e: Exception) {
                null
            }
        }

        val scheduleResponse = scheduleDeferred.await()
        val ongoing1Response = ongoing1Deferred.await()
        val ongoing2Response = ongoing2Deferred.await()

        val scheduleList = scheduleResponse?.data?.scheduleList ?: emptyList()
        val scheduleMap = parseScheduleList(scheduleList)

        // Cache schedule
        if (scheduleMap.isNotEmpty()) {
            val entities = scheduleMap.map { (dayIndex, animeList) ->
                val json = JsonUtils.scheduleAnimeListAdapter.toJson(animeList)
                ScheduleCacheEntity(
                    dayName = dayIndex.toString(),
                    jsonContent = json,
                    updatedAt = System.currentTimeMillis()
                )
            }
            scheduleCacheDao.insertAll(entities)
        }

        // Cache ongoing
        val ongoingList = mutableListOf<AnimeItem>()
        val p1Items = ongoing1Response?.data?.animeList
        if (!p1Items.isNullOrEmpty()) {
            ongoingList.addAll(p1Items)
            val json = JsonUtils.animeItemListAdapter.toJson(p1Items)
            homeCacheDao.insertHomeCache(
                HomeCacheEntity(
                    sectionKey = "recent_anime_page1",
                    jsonContent = json,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        val p2Items = ongoing2Response?.data?.animeList
        if (!p2Items.isNullOrEmpty()) {
            ongoingList.addAll(p2Items)
            val json = JsonUtils.animeItemListAdapter.toJson(p2Items)
            homeCacheDao.insertHomeCache(
                HomeCacheEntity(
                    sectionKey = "recent_anime_page2",
                    jsonContent = json,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        ScheduleFetchResult(
            scheduleMap = scheduleMap,
            ongoingList = if (ongoingList.isNotEmpty()) ongoingList else getCachedOngoingAnime()
        )
    }

    private fun parseScheduleList(list: List<ScheduleDayItem>): Map<Int, List<ScheduleAnimeItem>> {
        val map = mutableMapOf<Int, List<ScheduleAnimeItem>>()
        list.forEach { dayItem ->
            val dayName = dayItem.day?.trim()?.lowercase() ?: ""
            val index = when {
                dayName.contains("sun") || dayName.contains("minggu") || dayName.contains("min") -> 0
                dayName.contains("mon") || dayName.contains("senin") || dayName.contains("sen") -> 1
                dayName.contains("tue") || dayName.contains("selasa") || dayName.contains("sel") -> 2
                dayName.contains("wed") || dayName.contains("rabu") || dayName.contains("rab") -> 3
                dayName.contains("thu") || dayName.contains("kamis") || dayName.contains("kam") -> 4
                dayName.contains("fri") || dayName.contains("jumat") || dayName.contains("jum") -> 5
                dayName.contains("sat") || dayName.contains("sabtu") || dayName.contains("sab") -> 6
                else -> -1
            }
            if (index in 0..6) {
                map[index] = dayItem.animeList ?: emptyList()
            }
        }
        return map
    }
}
