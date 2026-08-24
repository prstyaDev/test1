package com.prstyadev.wibufy.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.data.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.IOException
import java.net.UnknownHostException

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<AnimeItem> = emptyList(),
    val error: String? = null
)

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, error = null) }
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isSearching = true, error = null) }
            try {
                val response = RetrofitClient.apiService.searchAnime(newQuery)
                _uiState.update { 
                    it.copy(
                        isSearching = false, 
                        searchResults = response.data?.animeList ?: emptyList()
                    ) 
                }
            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(isSearching = false, error = "Unable to reach server. Please check your internet connection.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(isSearching = false, error = "Network error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }
}
