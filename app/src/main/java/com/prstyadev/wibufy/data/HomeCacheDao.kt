package com.prstyadev.wibufy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HomeCacheDao {
    @Query("SELECT * FROM home_cache WHERE sectionKey = :sectionKey LIMIT 1")
    suspend fun getHomeCache(sectionKey: String): HomeCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeCache(entity: HomeCacheEntity)

    @Query("DELETE FROM home_cache WHERE sectionKey = :sectionKey")
    suspend fun deleteHomeCache(sectionKey: String)

    @Query("DELETE FROM home_cache")
    suspend fun clearHomeCache()
}
