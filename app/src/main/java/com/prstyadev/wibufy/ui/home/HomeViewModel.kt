package com.prstyadev.wibufy.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.data.AppDatabase
import com.prstyadev.wibufy.data.GenreItem
import com.prstyadev.wibufy.data.HomeRepository
import com.prstyadev.wibufy.data.RecentData
import com.prstyadev.wibufy.data.WatchHistoryEntity
import com.prstyadev.wibufy.data.WatchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val recentData: RecentData? = null,
    val page1Items: List<AnimeItem> = emptyList(),
    val page2Items: List<AnimeItem> = emptyList(),
    val completedAnime: List<AnimeItem> = emptyList(),
    val genres: List<GenreItem> = emptyList(),
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val subscribedAnimeIds: Set<String> = emptySet(),
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val homeRepository = HomeRepository(application)
    private val watchHistoryRepository = WatchHistoryRepository(application)
    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeWatchHistory()
        observeSubscribedAnime()
        loadInitialHomeData()
    }

    private fun observeWatchHistory() {
        viewModelScope.launch {
            watchHistoryRepository.allHistory.collect { historyList ->
                val distinctLatestPerAnime = historyList
                    .sortedByDescending { it.timestamp }
                    .distinctBy { item ->
                        extractAnimeBaseKey(item)
                    }
                    .take(15)
                _uiState.update { it.copy(watchHistory = distinctLatestPerAnime) }
            }
        }
    }

    private fun extractAnimeBaseKey(item: WatchHistoryEntity): String {
        val title = item.animeTitle?.trim()?.lowercase().orEmpty()
        val cleanTitle = title
            .replace(Regex("(?i)\\s*[-–:]?\\s*(?:episode|eps|ep)\\s*\\d+.*$"), "")
            .replace(Regex("(?i)\\s*(?:subtitle\\s+indonesia|sub\\s+indo|batch).*$"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

        val slug = item.episodeSlug.trim().lowercase()
        val cleanSlug = slug
            .replace(Regex("(?i)-(?:episode|eps|ep)[-_]?\\d+.*$"), "")
            .replace(Regex("(?i)-(?:sub-indo|subtitle-indonesia|end|batch).*$"), "")
            .trim('-')

        return when {
            cleanTitle.isNotEmpty() && cleanTitle != "anime" -> cleanTitle
            cleanSlug.isNotEmpty() -> cleanSlug
            else -> item.episodeSlug
        }
    }

    private fun observeSubscribedAnime() {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { bookmarkList ->
                val ids = bookmarkList.map { it.animeId }.toSet()
                _uiState.update { it.copy(subscribedAnimeIds = ids) }
            }
        }
    }

    private fun loadInitialHomeData() {
        viewModelScope.launch {
            // Step 1 (0 ms): BACA LANGSUNG data dari Room DB (home_cache)
            val cachedPage1 = try {
                homeRepository.getCachedRecentAnime()
            } catch (e: Exception) {
                null
            }

            val cachedPage2 = try {
                homeRepository.getCachedPage2Anime()
            } catch (e: Exception) {
                null
            }

            val cachedGenres = try {
                homeRepository.getCachedGenres()
            } catch (e: Exception) {
                null
            }

            val cachedCompleted = try {
                homeRepository.getCachedCompletedAnime()
            } catch (e: Exception) {
                null
            }

            val initialGenres = cachedGenres?.takeIf { it.isNotEmpty() } ?: homeRepository.getDefaultGenres()
            val initialCompleted = cachedCompleted?.takeIf { it.isNotEmpty() } ?: homeRepository.getDefaultCompletedAnime()

            val hasCache = !cachedPage1.isNullOrEmpty()
            if (hasCache) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        page1Items = cachedPage1 ?: emptyList(),
                        page2Items = cachedPage2 ?: emptyList(),
                        completedAnime = initialCompleted,
                        genres = initialGenres,
                        error = null
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = true, 
                        completedAnime = initialCompleted,
                        genres = initialGenres,
                        error = null
                    ) 
                }
            }

            // Step 2 & 3: Background sync fetch ke API Recent Anime & Genres dan simpan ke Room DB
            fetchHomeData(isSilent = hasCache)
        }
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val (recentData, page1List) = homeRepository.fetchAndCacheRecentAnime(page = 1)
                val updatedGenres = try {
                    homeRepository.fetchAndCacheGenres()
                } catch (e: Exception) {
                    _uiState.value.genres
                }
                val updatedCompleted = try {
                    homeRepository.fetchAndCacheCompletedAnime()
                } catch (e: Exception) {
                    _uiState.value.completedAnime
                }

                val updatedPage2 = if (_uiState.value.page2Items.isNotEmpty()) {
                    try {
                        val (_, p2List) = homeRepository.fetchAndCacheRecentAnime(page = 2)
                        p2List
                    } catch (e: Exception) {
                        _uiState.value.page2Items
                    }
                } else {
                    _uiState.value.page2Items
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        recentData = recentData,
                        page1Items = page1List,
                        page2Items = updatedPage2,
                        completedAnime = if (updatedCompleted.isNotEmpty()) updatedCompleted else it.completedAnime,
                        genres = updatedGenres,
                        error = null
                    )
                }
            } catch (e: UnknownHostException) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (_uiState.value.page1Items.isEmpty()) "Unable to reach server. Please check your internet connection." else null
                    ) 
                }
            } catch (e: IOException) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (_uiState.value.page1Items.isEmpty()) "Network error: ${e.message}" else null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (_uiState.value.page1Items.isEmpty()) (e.message ?: "Failed to load anime") else null
                    ) 
                }
            }
        }
    }

    private fun fetchHomeData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }
            try {
                val (recentData, page1List) = homeRepository.fetchAndCacheRecentAnime(page = 1)
                val updatedGenres = try {
                    homeRepository.fetchAndCacheGenres()
                } catch (e: Exception) {
                    _uiState.value.genres
                }
                val updatedCompleted = try {
                    homeRepository.fetchAndCacheCompletedAnime()
                } catch (e: Exception) {
                    _uiState.value.completedAnime
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentData = recentData,
                        page1Items = page1List,
                        completedAnime = if (updatedCompleted.isNotEmpty()) updatedCompleted else it.completedAnime,
                        genres = updatedGenres,
                        error = null
                    )
                }
            } catch (e: UnknownHostException) {
                if (_uiState.value.page1Items.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "Unable to reach server. Please check your internet connection.") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: IOException) {
                if (_uiState.value.page1Items.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                if (_uiState.value.page1Items.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load anime") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun loadPage2() {
        if (_uiState.value.page2Items.isNotEmpty() || _uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            try {
                val (_, newItems) = homeRepository.fetchAndCacheRecentAnime(page = 2)

                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        page2Items = newItems
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }
}
