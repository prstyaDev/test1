package com.prstyadev.wibufy.data

import android.content.Context

class ScheduleRepository(context: Context) {
    private val scheduleCacheDao = AppDatabase.getDatabase(context).scheduleCacheDao()

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

    suspend fun fetchAndCacheSchedule(): Map<Int, List<ScheduleAnimeItem>> {
        val response = RetrofitClient.apiService.getSchedule()
        val scheduleList = response.data?.scheduleList ?: emptyList()
        val scheduleMap = parseScheduleList(scheduleList)

        // Save to cache
        val entities = scheduleMap.map { (dayIndex, animeList) ->
            val json = JsonUtils.scheduleAnimeListAdapter.toJson(animeList)
            ScheduleCacheEntity(
                dayName = dayIndex.toString(),
                jsonContent = json,
                updatedAt = System.currentTimeMillis()
            )
        }
        if (entities.isNotEmpty()) {
            scheduleCacheDao.insertAll(entities)
        }

        return scheduleMap
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
