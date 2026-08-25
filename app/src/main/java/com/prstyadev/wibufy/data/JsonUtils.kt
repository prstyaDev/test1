package com.prstyadev.wibufy.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonUtils {
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val scheduleAnimeListAdapter: JsonAdapter<List<ScheduleAnimeItem>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, ScheduleAnimeItem::class.java)
    )

    val genreListAdapter: JsonAdapter<List<GenreItem>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, GenreItem::class.java)
    )

    val episodeListAdapter: JsonAdapter<List<EpisodeItem>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, EpisodeItem::class.java)
    )

    val animeDetailAdapter: JsonAdapter<AnimeDetail> = moshi.adapter(
        AnimeDetail::class.java
    )

    val animeItemListAdapter: JsonAdapter<List<AnimeItem>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, AnimeItem::class.java)
    )
}
