package com.prstyadev.wibufy.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.data.RetrofitClient
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<AnimeItem> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _query
                .debounce(500L)
                .distinctUntilChanged()
                .collectLatest { queryText ->
                    val trimmed = queryText.trim()
                    if (trimmed.isBlank()) {
                        _uiState.update { 
                            it.copy(
                                isSearching = false, 
                                searchResults = emptyList(), 
                                error = null,
                                hasSearched = false
                            ) 
                        }
                    } else {
                        performSearch(trimmed)
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _uiState.update { 
            it.copy(
                query = newQuery,
                isSearching = newQuery.trim().isNotBlank()
            ) 
        }
    }

    fun clearQuery() {
        onQueryChange("")
    }

    private suspend fun performSearch(queryText: String) {
        _uiState.update { it.copy(isSearching = true, error = null, hasSearched = true) }
        try {
            val response = RetrofitClient.apiService.searchAnime(queryText)
            val results = response.data?.animeList ?: emptyList()
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    searchResults = results,
                    error = null
                ) 
            }
        } catch (e: UnknownHostException) {
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    error = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                ) 
            }
        } catch (e: IOException) {
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    error = "Terjadi kesalahan jaringan: ${e.message}"
                ) 
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isSearching = false, 
                    error = e.message ?: "Terjadi kesalahan yang tidak diketahui"
                ) 
            }
        }
    }
}
