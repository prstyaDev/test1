package com.prstyadev.wibufy.ui.subscribed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AppDatabase
import com.prstyadev.wibufy.data.BookmarkEntity
import com.prstyadev.wibufy.data.SubscribedAnimeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SubscribedSortOption(val displayName: String) {
    NEW_UPDATE("New Update"),
    ALPHABETICAL("A - Z"),
    RATING("Rating"),
    OLDEST("Oldest")
}

data class SubscribedUiState(
    val totalCount: Int = 0,
    val ongoingList: List<SubscribedAnimeEntity> = emptyList(),
    val completedList: List<SubscribedAnimeEntity> = emptyList(),
    val selectedSort: SubscribedSortOption = SubscribedSortOption.NEW_UPDATE,
    val isLoading: Boolean = false
)

class SubscribedViewModel(application: Application) : AndroidViewModel(application) {
    private val bookmarkDao = AppDatabase.getDatabase(application).bookmarkDao()

    private val _uiState = MutableStateFlow(SubscribedUiState())
    val uiState: StateFlow<SubscribedUiState> = _uiState.asStateFlow()

    private var allBookmarksRaw: List<SubscribedAnimeEntity> = emptyList()

    init {
        observeBookmarks()
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            bookmarkDao.getAllBookmarks().collect { list ->
                allBookmarksRaw = list
                applyGroupingAndSorting()
            }
        }
    }

    fun deleteBookmark(animeId: String) {
        if (animeId.isBlank()) return
        viewModelScope.launch {
            bookmarkDao.deleteBookmarkById(animeId)
        }
    }

    fun setSortOption(option: SubscribedSortOption) {
        _uiState.update { it.copy(selectedSort = option) }
        applyGroupingAndSorting()
    }

    private fun applyGroupingAndSorting() {
        val sortedList = when (_uiState.value.selectedSort) {
            SubscribedSortOption.NEW_UPDATE -> allBookmarksRaw.sortedByDescending { it.timestamp }
            SubscribedSortOption.ALPHABETICAL -> allBookmarksRaw.sortedBy { it.title.lowercase() }
            SubscribedSortOption.RATING -> allBookmarksRaw.sortedByDescending { 
                it.score?.toDoubleOrNull() ?: 0.0 
            }
            SubscribedSortOption.OLDEST -> allBookmarksRaw.sortedBy { it.timestamp }
        }

        val ongoing = sortedList.filter { 
            val st = it.status?.trim()?.lowercase() ?: ""
            st.contains("ongoing") || 
            st.contains("airing") || 
            st.contains("tayang") || 
            st.contains("sedang") ||
            st.isEmpty() // Default to ongoing if unknown/empty
        }

        val completed = sortedList.filter { 
            !ongoing.contains(it)
        }

        _uiState.update {
            it.copy(
                totalCount = sortedList.size,
                ongoingList = ongoing,
                completedList = completed
            )
        }
    }
}
