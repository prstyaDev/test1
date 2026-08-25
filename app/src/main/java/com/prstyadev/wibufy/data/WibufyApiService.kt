package com.prstyadev.wibufy.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WibufyApiService {
    @GET("api/samehadaku/home")
    suspend fun getHome(@Query("page") page: Int = 1): BaseResponse<HomeData>

    @GET("api/samehadaku/recent")
    suspend fun getRecentAnime(@Query("page") page: Int = 1): BaseResponse<RecentData>

    @GET("api/samehadaku/anime/{animeId}")
    suspend fun getAnimeDetail(@Path("animeId") animeId: String): BaseResponse<AnimeDetailData>

    @GET("api/samehadaku/search")
    suspend fun searchAnime(@Query("q") query: String): BaseResponse<SearchData>

    @GET("api/stream/{episodeSlug}")
    suspend fun getStreamEngine(@Path("episodeSlug") episodeSlug: String): BaseResponse<StreamData>
}
