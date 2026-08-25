package com.prstyadev.wibufy.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.ScheduleAnimeItem
import com.prstyadev.wibufy.data.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DayInfo(
    val dayIndex: Int, // 0 = Min, 1 = Sen, 2 = Sel, 3 = Rab, 4 = Kam, 5 = Jum, 6 = Sab
    val shortName: String,
    val fullName: String,
    val englishName: String,
    val dayOfMonth: Int,
    val isToday: Boolean
)

data class ScheduleItemUiModel(
    val anime: ScheduleAnimeItem,
    val airTime: String, // format HH:mm
    val isAlreadyAired: Boolean
)

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val days: List<DayInfo> = emptyList(),
    val selectedDayIndex: Int = 0,
    val scheduleByDay: Map<Int, List<ScheduleAnimeItem>> = emptyMap(),
    val currentDayAnimeList: List<ScheduleItemUiModel> = emptyList(),
    val error: String? = null
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private val scheduleRepository = ScheduleRepository(application)

    init {
        setupWeekDays()
        loadSchedule()
    }

    private fun setupWeekDays() {
        val today = LocalDate.now()
        // In Java DayOfWeek: MONDAY is 1 .. SUNDAY is 7.
        // Convert to 0 = Min (Sunday), 1 = Sen, 2 = Sel, 3 = Rab, 4 = Kam, 5 = Jum, 6 = Sab
        val todayIndex = today.dayOfWeek.value % 7
        val startOfWeek = today.minusDays(todayIndex.toLong())

        val dayNames = listOf(
            Triple("Min", "Minggu", "Sunday"),
            Triple("Sen", "Senin", "Monday"),
            Triple("Sel", "Selasa", "Tuesday"),
            Triple("Rab", "Rabu", "Wednesday"),
            Triple("Kam", "Kamis", "Thursday"),
            Triple("Jum", "Jumat", "Friday"),
            Triple("Sab", "Sabtu", "Saturday")
        )

        val days = (0..6).map { i ->
            val date = startOfWeek.plusDays(i.toLong())
            val (shortName, fullName, englishName) = dayNames[i]
            DayInfo(
                dayIndex = i,
                shortName = shortName,
                fullName = fullName,
                englishName = englishName,
                dayOfMonth = date.dayOfMonth,
                isToday = (i == todayIndex)
            )
        }

        _uiState.update {
            it.copy(
                days = days,
                selectedDayIndex = todayIndex
            )
        }
    }

    fun loadSchedule() {
        viewModelScope.launch {
            // 1. Cek cache lokal terlebih dahulu. Jika ada data cache, LANGSUNG tampilkan ke UI secara instan (0 ms)
            val cachedMap = try {
                scheduleRepository.getCachedSchedule()
            } catch (e: Exception) {
                emptyMap()
            }

            if (cachedMap.isNotEmpty()) {
                _uiState.update { current ->
                    val currentItems = mapToUiModels(
                        animeList = cachedMap[current.selectedDayIndex] ?: emptyList(),
                        selectedDayIndex = current.selectedDayIndex
                    )
                    current.copy(
                        isLoading = false,
                        scheduleByDay = cachedMap,
                        currentDayAnimeList = currentItems,
                        error = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // 2. Jalankan network call ke API di background untuk memperbarui data
            try {
                val freshMap = scheduleRepository.fetchAndCacheSchedule()
                _uiState.update { current ->
                    val currentItems = mapToUiModels(
                        animeList = freshMap[current.selectedDayIndex] ?: emptyList(),
                        selectedDayIndex = current.selectedDayIndex
                    )
                    current.copy(
                        isLoading = false,
                        scheduleByDay = freshMap,
                        currentDayAnimeList = currentItems,
                        error = null
                    )
                }
            } catch (e: UnknownHostException) {
                if (_uiState.value.scheduleByDay.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                        )
                    }
                }
            } catch (e: IOException) {
                if (_uiState.value.scheduleByDay.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Terjadi kesalahan jaringan: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                if (_uiState.value.scheduleByDay.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Gagal memuat jadwal"
                        )
                    }
                }
            }
        }
    }

    fun fetchSchedule() {
        loadSchedule()
    }

    private fun mapToUiModels(
        animeList: List<ScheduleAnimeItem>,
        selectedDayIndex: Int
    ): List<ScheduleItemUiModel> {
        val todayIndex = LocalDate.now().dayOfWeek.value % 7
        val now = LocalTime.now()

        // Mockup consistent times
        val sampleTimes = listOf(
            "00:39", "00:25", "20:59", "20:06", "18:55", "17:30", "15:00", "12:30", "10:00", "08:15"
        )

        return animeList.mapIndexed { idx, anime ->
            val airTime = if (!anime.time.isNullOrBlank()) {
                anime.time
            } else {
                sampleTimes[idx % sampleTimes.size]
            }

            val isAlreadyAired = when {
                selectedDayIndex < todayIndex -> true
                selectedDayIndex > todayIndex -> false
                else -> {
                    // Today: compare airTime with current LocalTime
                    try {
                        val parsedTime = LocalTime.parse(airTime, DateTimeFormatter.ofPattern("HH:mm"))
                        !parsedTime.isAfter(now)
                    } catch (e: Exception) {
                        try {
                            val parts = airTime.split(":")
                            val hour = parts[0].trim().toInt()
                            val min = parts.getOrNull(1)?.trim()?.toInt() ?: 0
                            val t = LocalTime.of(hour, min)
                            !t.isAfter(now)
                        } catch (e2: Exception) {
                            idx < 2 // First couple aired
                        }
                    }
                }
            }

            ScheduleItemUiModel(
                anime = anime,
                airTime = airTime,
                isAlreadyAired = isAlreadyAired
            )
        }
    }

    fun selectDay(dayIndex: Int) {
        if (dayIndex !in 0..6) return
        _uiState.update { current ->
            val items = mapToUiModels(
                animeList = current.scheduleByDay[dayIndex] ?: emptyList(),
                selectedDayIndex = dayIndex
            )
            current.copy(
                selectedDayIndex = dayIndex,
                currentDayAnimeList = items
            )
        }
    }

    fun selectPreviousDay() {
        val current = _uiState.value.selectedDayIndex
        val prev = if (current == 0) 6 else current - 1
        selectDay(prev)
    }

    fun selectNextDay() {
        val current = _uiState.value.selectedDayIndex
        val next = if (current == 6) 0 else current + 1
        selectDay(next)
    }
}
