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
    ALL("Semua"),
    ONGOING("Ongoing"),
    COMPLETED("Completed"),
    NEW_UPDATE("Terbaru"),
    ALPHABETICAL("A - Z"),
    ALPHABETICAL_DESC("Z - A"),
    RATING("Rating Tertinggi"),
    OLDEST("Terlama")
}

data class SubscribedUiState(
    val totalCount: Int = 0,
    val ongoingList: List<SubscribedAnimeEntity> = emptyList(),
    val completedList: List<SubscribedAnimeEntity> = emptyList(),
    val selectedSort: SubscribedSortOption = SubscribedSortOption.ALL,
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
        val sortOption = _uiState.value.selectedSort

        // Filter based on option if filtering is selected
        val filteredList = when (sortOption) {
            SubscribedSortOption.ONGOING -> allBookmarksRaw.filter { isOngoingStatus(it.status) }
            SubscribedSortOption.COMPLETED -> allBookmarksRaw.filter { !isOngoingStatus(it.status) }
            else -> allBookmarksRaw
        }

        // Sort the filtered list
        val sortedList = when (sortOption) {
            SubscribedSortOption.ALPHABETICAL -> filteredList.sortedBy { it.title.lowercase() }
            SubscribedSortOption.ALPHABETICAL_DESC -> filteredList.sortedByDescending { it.title.lowercase() }
            SubscribedSortOption.RATING -> filteredList.sortedByDescending {
                it.score?.toDoubleOrNull() ?: 0.0
            }
            SubscribedSortOption.OLDEST -> filteredList.sortedBy { it.timestamp }
            SubscribedSortOption.ALL,
            SubscribedSortOption.ONGOING,
            SubscribedSortOption.COMPLETED,
            SubscribedSortOption.NEW_UPDATE -> filteredList.sortedByDescending { it.timestamp }
        }

        val ongoing: List<SubscribedAnimeEntity>
        val completed: List<SubscribedAnimeEntity>

        when (sortOption) {
            SubscribedSortOption.ONGOING -> {
                ongoing = sortedList
                completed = emptyList()
            }
            SubscribedSortOption.COMPLETED -> {
                ongoing = emptyList()
                completed = sortedList
            }
            else -> {
                ongoing = sortedList.filter { isOngoingStatus(it.status) }
                completed = sortedList.filter { !isOngoingStatus(it.status) }
            }
        }

        _uiState.update {
            it.copy(
                totalCount = filteredList.size,
                ongoingList = ongoing,
                completedList = completed
            )
        }
    }

    private fun isOngoingStatus(status: String?): Boolean {
        val st = status?.trim()?.lowercase() ?: ""
        return st.contains("ongoing") ||
                st.contains("airing") ||
                st.contains("tayang") ||
                st.contains("sedang") ||
                st.isEmpty()
    }
}
