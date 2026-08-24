package com.prstyadev.wibufy.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeDetailData
import com.prstyadev.wibufy.data.AppDatabase
import com.prstyadev.wibufy.data.BookmarkEntity
import com.prstyadev.wibufy.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.IOException
import java.net.UnknownHostException

data class DetailUiState(
    val isLoading: Boolean = true,
    val detailData: AnimeDetailData? = null,
    val error: String? = null,
    val isBookmarked: Boolean = false
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()

    fun loadAnimeDetail(animeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getAnimeDetail(animeId)
                _uiState.update { it.copy(isLoading = false, detailData = response.data) }
                checkBookmarkStatus(animeId)
            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to reach server. Please check your internet connection.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun checkBookmarkStatus(animeId: String) {
        viewModelScope.launch {
            bookmarkDao.getBookmarkById(animeId).collect { bookmark ->
                _uiState.update { it.copy(isBookmarked = bookmark != null) }
            }
        }
    }

    fun toggleBookmark(animeId: String) {
        val currentData = _uiState.value.detailData?.anime ?: return
        viewModelScope.launch {
            if (_uiState.value.isBookmarked) {
                bookmarkDao.deleteBookmarkById(animeId)
            } else {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        animeId = animeId,
                        title = currentData.title ?: "Unknown",
                        poster = currentData.poster
                    )
                )
            }
        }
    }
}
