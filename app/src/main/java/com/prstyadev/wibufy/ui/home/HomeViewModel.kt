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

data class HomeUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val recentData: RecentData? = null,
    val page1Items: List<AnimeItem> = emptyList(),
    val page2Items: List<AnimeItem> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchHomeData()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getRecentAnime(page = 1)
                val originalList = response.data?.animeList?.take(12) ?: emptyList()
                
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        recentData = response.data,
                        page1Items = originalList
                    ) 
                }
            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to reach server. Please check your internet connection.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadPage2() {
        if (_uiState.value.page2Items.isNotEmpty() || _uiState.value.isLoadingMore) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getRecentAnime(page = 2)
                val newItems = response.data?.animeList?.take(12) ?: emptyList()
                
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
