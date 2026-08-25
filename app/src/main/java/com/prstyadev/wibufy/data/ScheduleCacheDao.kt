package com.prstyadev.wibufy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduleCacheDao {
    @Query("SELECT * FROM schedule_cache")
    suspend fun getAllScheduleCache(): List<ScheduleCacheEntity>

    @Query("SELECT * FROM schedule_cache WHERE dayName = :dayName LIMIT 1")
    suspend fun getScheduleByDay(dayName: String): ScheduleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleCache(entity: ScheduleCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ScheduleCacheEntity>)

    @Query("DELETE FROM schedule_cache")
    suspend fun clearScheduleCache()
}
