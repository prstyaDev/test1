package com.prstyadev.wibufy.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WibufyApiService {
    @GET("api/samehadaku/home")
    suspend fun getHome(@Query("page") page: Int = 1): BaseResponse<HomeData>

    @GET("api/samehadaku/recent")
    suspend fun getRecentAnime(@Query("page") page: Int = 1): BaseResponse<RecentData>

    @GET("api/samehadaku/complete")
    suspend fun getCompleteAnime(@Query("page") page: Int = 1): BaseResponse<RecentData>

    @GET("api/samehadaku/completed")
    suspend fun getCompletedAnime(@Query("page") page: Int = 1): BaseResponse<RecentData>

    @GET("api/samehadaku/anime/{animeId}")
    suspend fun getAnimeDetail(@Path("animeId") animeId: String): BaseResponse<AnimeDetailData>

    @GET("api/samehadaku/search")
    suspend fun searchAnime(@Query("q") query: String): BaseResponse<SearchData>

    @GET("api/samehadaku/genres")
    suspend fun getGenres(): BaseResponse<GenreListData>

    @GET("api/samehadaku/genre/{genreId}")
    suspend fun getAnimeByGenre(
        @Path("genreId") genreId: String,
        @Query("page") page: Int = 1
    ): BaseResponse<RecentData>

    @GET("api/samehadaku/movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("order") order: String = "update"
    ): BaseResponse<RecentData>

    @GET("api/stream/{episodeSlug}")
    suspend fun getStreamEngine(@Path("episodeSlug") episodeSlug: String): BaseResponse<StreamData>

    @GET("api/schedule")
    suspend fun getSchedule(): BaseResponse<ScheduleData>
}
