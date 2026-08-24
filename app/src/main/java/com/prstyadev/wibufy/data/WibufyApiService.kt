package com.prstyadev.wibufy.data

import retrofit2.http.GET
import retrofit2.http.Path

interface WibufyApiService {
    @GET("api/samehadaku/home")
    suspend fun getHome(): BaseResponse<HomeData>

    @GET("api/samehadaku/anime/{animeId}")
    suspend fun getAnimeDetail(@Path("animeId") animeId: String): BaseResponse<AnimeDetailData>

    @GET("api/samehadaku/search/{query}")
    suspend fun searchAnime(@Path("query") query: String): BaseResponse<SearchData>

    @GET("api/stream/{episodeSlug}")
    suspend fun getStreamEngine(@Path("episodeSlug") episodeSlug: String): BaseResponse<StreamData>
}
