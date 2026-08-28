package com.prstyadev.wibufy.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.WatchHistoryEntity
import com.prstyadev.wibufy.data.WatchHistoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HistoryGroup(
    val dateHeader: String,
    val items: List<WatchHistoryEntity>
)

data class HistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val rawList: List<WatchHistoryEntity> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedSlugs: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WatchHistoryRepository(application)

    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedSlugs = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allHistory,
        _isSelectionMode,
        _selectedSlugs
    ) { historyList, isSelection, selected ->
        val groups = groupHistoryByDate(historyList)
        HistoryUiState(
            groups = groups,
            rawList = historyList,
            isSelectionMode = isSelection,
            selectedSlugs = selected,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(isLoading = true)
    )

    private fun groupHistoryByDate(list: List<WatchHistoryEntity>): List<HistoryGroup> {
        val groupedMap = LinkedHashMap<String, MutableList<WatchHistoryEntity>>()
        for (item in list) {
            val header = getHistoryDateHeader(item.timestamp)
            val groupList = groupedMap.getOrPut(header) { mutableListOf() }
            groupList.add(item)
        }
        return groupedMap.map { (header, items) ->
            HistoryGroup(dateHeader = header, items = items)
        }
    }

    private fun getHistoryDateHeader(timestamp: Long): String {
        val calNow = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { timeInMillis = timestamp }

        val isSameYear = calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR)
        if (isSameYear && calNow.get(Calendar.DAY_OF_YEAR) == calItem.get(Calendar.DAY_OF_YEAR)) {
            return "Hari ini"
        }
        if (isSameYear && calNow.get(Calendar.DAY_OF_YEAR) - calItem.get(Calendar.DAY_OF_YEAR) == 1) {
            return "Kemarin"
        }

        val sdf = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    fun startSelectionMode(initialSlug: String? = null) {
        _isSelectionMode.value = true
        if (initialSlug != null) {
            _selectedSlugs.value = setOf(initialSlug)
        }
    }

    fun toggleSelection(slug: String) {
        val current = _selectedSlugs.value.toMutableSet()
        if (current.contains(slug)) {
            current.remove(slug)
            if (current.isEmpty()) {
                _isSelectionMode.value = false
            }
        } else {
            current.add(slug)
            _isSelectionMode.value = true
        }
        _selectedSlugs.value = current
    }

    fun selectAll() {
        val allSlugs = uiState.value.rawList.map { it.episodeSlug }.toSet()
        _selectedSlugs.value = allSlugs
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedSlugs.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelected() {
        val slugsToDelete = _selectedSlugs.value.toList()
        if (slugsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteBySlugs(slugsToDelete)
            clearSelection()
        }
    }

    fun deleteItem(slug: String) {
        viewModelScope.launch {
            repository.deleteBySlugs(listOf(slug))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            clearSelection()
        }
    }
}
