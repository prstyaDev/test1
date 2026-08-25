package com.prstyadev.wibufy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.data.RecentData
import com.prstyadev.wibufy.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

object HomeCache {
    var recentData: RecentData? = null
    var page1Items: List<AnimeItem> = emptyList()
    var page2Items: List<AnimeItem> = emptyList()
    var lastFetchTime: Long = 0L

    fun hasData(): Boolean = page1Items.isNotEmpty()

    fun updatePage1(data: RecentData?, items: List<AnimeItem>) {
        recentData = data
        page1Items = items
        lastFetchTime = System.currentTimeMillis()
    }

    fun updatePage2(items: List<AnimeItem>) {
        page2Items = items
    }
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val recentData: RecentData? = null,
    val page1Items: List<AnimeItem> = emptyList(),
    val page2Items: List<AnimeItem> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        if (HomeCache.hasData()) {
            HomeUiState(
                isLoading = false,
                recentData = HomeCache.recentData,
                page1Items = HomeCache.page1Items,
                page2Items = HomeCache.page2Items
            )
        } else {
            HomeUiState(isLoading = true)
        }
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // If cache is available, do silent background update without showing fullscreen spinner
        fetchHomeData(isSilent = HomeCache.hasData())
    }

    fun refreshHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getRecentAnime(page = 1)
                val items = response.data?.animeList ?: emptyList()
                HomeCache.updatePage1(response.data, items)

                val updatedPage2 = if (_uiState.value.page2Items.isNotEmpty()) {
                    try {
                        val p2Response = RetrofitClient.apiService.getRecentAnime(page = 2)
                        val p2Items = p2Response.data?.animeList ?: emptyList()
                        HomeCache.updatePage2(p2Items)
                        p2Items
                    } catch (e: Exception) {
                        _uiState.value.page2Items
                    }
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        recentData = response.data,
                        page1Items = items,
                        page2Items = updatedPage2,
                        error = null
                    )
                }
            } catch (e: UnknownHostException) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (!HomeCache.hasData()) "Unable to reach server. Please check your internet connection." else null
                    ) 
                }
            } catch (e: IOException) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (!HomeCache.hasData()) "Network error: ${e.message}" else null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false, 
                        error = if (!HomeCache.hasData()) e.message else null
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
                val response = RetrofitClient.apiService.getRecentAnime(page = 1)
                val originalList = response.data?.animeList ?: emptyList()
                HomeCache.updatePage1(response.data, originalList)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recentData = response.data,
                        page1Items = originalList,
                        error = null
                    )
                }
            } catch (e: UnknownHostException) {
                if (!HomeCache.hasData()) {
                    _uiState.update { it.copy(isLoading = false, error = "Unable to reach server. Please check your internet connection.") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: IOException) {
                if (!HomeCache.hasData()) {
                    _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                if (!HomeCache.hasData()) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
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
                val response = RetrofitClient.apiService.getRecentAnime(page = 2)
                val newItems = response.data?.animeList ?: emptyList()
                HomeCache.updatePage2(newItems)

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
