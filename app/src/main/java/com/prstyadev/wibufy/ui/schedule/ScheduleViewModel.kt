package com.prstyadev.wibufy.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.AnimeItem
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
    val episodeDisplay: String, // e.g. "Episode 9"
    val isAlreadyAired: Boolean,
    val latestEpisodeSlug: String? = null
)

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val days: List<DayInfo> = emptyList(),
    val selectedDayIndex: Int = 0,
    val scheduleByDay: Map<Int, List<ScheduleAnimeItem>> = emptyMap(),
    val ongoingAnimeList: List<AnimeItem> = emptyList(),
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
            // 1. Cek cache lokal terlebih dahulu (Schedule + Ongoing)
            val cachedMap = try {
                scheduleRepository.getCachedSchedule()
            } catch (e: Exception) {
                emptyMap()
            }
            val cachedOngoing = try {
                scheduleRepository.getCachedOngoingAnime()
            } catch (e: Exception) {
                emptyList()
            }

            if (cachedMap.isNotEmpty()) {
                _uiState.update { current ->
                    val currentItems = mapToUiModels(
                        animeList = cachedMap[current.selectedDayIndex] ?: emptyList(),
                        selectedDayIndex = current.selectedDayIndex,
                        ongoingList = cachedOngoing
                    )
                    current.copy(
                        isLoading = false,
                        scheduleByDay = cachedMap,
                        ongoingAnimeList = cachedOngoing,
                        currentDayAnimeList = currentItems,
                        error = null
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // 2. Jalankan network call ke API di background untuk memperbarui data
            try {
                val fetchResult = scheduleRepository.fetchAndCacheSchedule()
                val freshMap = fetchResult.scheduleMap
                val freshOngoing = fetchResult.ongoingList

                _uiState.update { current ->
                    val activeMap = if (freshMap.isNotEmpty()) freshMap else current.scheduleByDay
                    val activeOngoing = if (freshOngoing.isNotEmpty()) freshOngoing else current.ongoingAnimeList
                    val currentItems = mapToUiModels(
                        animeList = activeMap[current.selectedDayIndex] ?: emptyList(),
                        selectedDayIndex = current.selectedDayIndex,
                        ongoingList = activeOngoing
                    )
                    current.copy(
                        isLoading = false,
                        scheduleByDay = activeMap,
                        ongoingAnimeList = activeOngoing,
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
        selectedDayIndex: Int,
        ongoingList: List<AnimeItem>
    ): List<ScheduleItemUiModel> {
        val todayIndex = LocalDate.now().dayOfWeek.value % 7
        val now = LocalTime.now()

        return animeList.mapIndexed { idx, anime ->
            val airTime = when {
                !anime.time.isNullOrBlank() -> anime.time.trim()
                !anime.estimation.isNullOrBlank() -> parseAirTimeFromEstimation(anime.estimation)
                    ?: generateFallbackTime(anime, idx)
                else -> generateFallbackTime(anime, idx)
            }

            // Cross-match with ongoing anime to get real episode number and recent release info
            val matchedOngoing = findMatchingOngoing(anime, ongoingList)

            // Resolve Episode Text Display
            val episodeDisplay = when {
                matchedOngoing != null && !matchedOngoing.episodes.isNullOrBlank() -> {
                    val rawEp = matchedOngoing.episodes.trim()
                    if (rawEp.startsWith("episode", ignoreCase = true) || rawEp.startsWith("eps", ignoreCase = true)) {
                        rawEp
                    } else {
                        "Episode $rawEp"
                    }
                }
                !anime.episodes.isNullOrBlank() -> {
                    val rawEp = anime.episodes.trim()
                    if (rawEp.startsWith("episode", ignoreCase = true) || rawEp.startsWith("eps", ignoreCase = true)) {
                        rawEp
                    } else {
                        "Episode $rawEp"
                    }
                }
                else -> "Episode Baru"
            }

            val isAlreadyAired = when {
                selectedDayIndex < todayIndex -> true
                selectedDayIndex > todayIndex -> false
                else -> {
                    // Today:
                    // 1. If ongoing update was released today / recently
                    if (matchedOngoing != null) {
                        val rel = matchedOngoing.releasedOn?.lowercase()?.trim() ?: ""
                        val isRecentToday = rel.contains("second") || rel.contains("detik") ||
                                rel.contains("minute") || rel.contains("menit") ||
                                rel.contains("hour") || rel.contains("jam") ||
                                rel.contains("hari ini") || rel.contains("today") ||
                                rel.contains("sabtu") // Indonesian current day name if applicable
                        if (isRecentToday) {
                            true
                        } else {
                            checkAirTimePassed(airTime, anime.estimation, now, idx)
                        }
                    } else {
                        checkAirTimePassed(airTime, anime.estimation, now, idx)
                    }
                }
            }

            ScheduleItemUiModel(
                anime = anime,
                airTime = airTime,
                episodeDisplay = episodeDisplay,
                isAlreadyAired = isAlreadyAired,
                latestEpisodeSlug = matchedOngoing?.animeId ?: anime.animeId
            )
        }
    }

    private fun checkAirTimePassed(
        airTime: String,
        estimation: String?,
        now: LocalTime,
        idx: Int
    ): Boolean {
        val est = estimation?.lowercase()?.trim()
        if (!est.isNullOrBlank()) {
            val hasDays = Regex("(\\d+)\\s*d").containsMatchIn(est)
            if (hasDays) {
                return false
            } else {
                return try {
                    val parsedTime = LocalTime.parse(airTime, DateTimeFormatter.ofPattern("HH:mm"))
                    !parsedTime.isAfter(now)
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            return try {
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
                    idx < 2
                }
            }
        }
    }

    private fun findMatchingOngoing(
        scheduleAnime: ScheduleAnimeItem,
        ongoingList: List<AnimeItem>
    ): AnimeItem? {
        if (ongoingList.isEmpty()) return null

        val sId = scheduleAnime.animeId?.lowercase()?.trim() ?: ""
        val sTitle = scheduleAnime.title?.lowercase()?.trim() ?: ""

        // 1. Direct ID / slug matching
        if (sId.isNotBlank()) {
            val directIdMatch = ongoingList.firstOrNull { ongoing ->
                val oId = ongoing.animeId?.lowercase()?.trim() ?: ""
                oId.isNotBlank() && (
                    oId == sId ||
                    oId.replace("-sub-indo", "") == sId.replace("-sub-indo", "") ||
                    oId.contains(sId) || sId.contains(oId)
                )
            }
            if (directIdMatch != null) return directIdMatch
        }

        // 2. Normalized Title matching
        fun normalize(text: String): String {
            return text.lowercase()
                .replace(Regex("-sub-indo|-indo"), " ")
                .replace(Regex("[^a-z0-9\\s]"), " ")
                .replace(Regex("\\b(sub|indo|season|s\\d+|episode|eps|tv|part|cour|the|animation)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        val normSchedule = normalize(sTitle)
        if (normSchedule.length >= 3) {
            val titleMatch = ongoingList.firstOrNull { ongoing ->
                val normOngoing = normalize(ongoing.title ?: "")
                if (normOngoing.length < 3) return@firstOrNull false

                if (normOngoing == normSchedule ||
                    normOngoing.contains(normSchedule) ||
                    normSchedule.contains(normOngoing)) {
                    return@firstOrNull true
                }

                // Check meaningful word overlap
                val scheduleTokens = normSchedule.split(" ").filter { it.length >= 3 }
                val ongoingTokens = normOngoing.split(" ").filter { it.length >= 3 }
                if (scheduleTokens.isNotEmpty() && ongoingTokens.isNotEmpty()) {
                    val overlap = scheduleTokens.intersect(ongoingTokens.toSet())
                    val minThreshold = if (scheduleTokens.size <= 2) scheduleTokens.size else 2
                    overlap.size >= minThreshold
                } else {
                    false
                }
            }
            if (titleMatch != null) return titleMatch
        }

        return null
    }

    private fun parseAirTimeFromEstimation(estimation: String): String? {
        val lower = estimation.trim().lowercase()

        var days = 0L
        var hours = 0L
        var minutes = 0L

        val dayMatch = Regex("(\\d+)\\s*d").find(lower)
        if (dayMatch != null) days = dayMatch.groupValues[1].toLongOrNull() ?: 0L

        val hourMatch = Regex("(\\d+)\\s*h").find(lower)
        if (hourMatch != null) hours = hourMatch.groupValues[1].toLongOrNull() ?: 0L

        val minMatch = Regex("(\\d+)\\s*m").find(lower)
        if (minMatch != null) minutes = minMatch.groupValues[1].toLongOrNull() ?: 0L

        if (days == 0L && hours == 0L && minutes == 0L) {
            return null
        }

        val targetDateTime = java.time.LocalDateTime.now()
            .plusDays(days)
            .plusHours(hours)
            .plusMinutes(minutes)

        return targetDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun generateFallbackTime(anime: ScheduleAnimeItem, idx: Int): String {
        val seed = kotlin.math.abs((anime.animeId ?: anime.title ?: "$idx").hashCode())
        val hour = (seed % 14) + 10 // 10:00 - 23:00
        val minute = (seed % 4) * 15 // 00, 15, 30, 45
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
    }

    fun selectDay(dayIndex: Int) {
        if (dayIndex !in 0..6) return
        _uiState.update { current ->
            val items = mapToUiModels(
                animeList = current.scheduleByDay[dayIndex] ?: emptyList(),
                selectedDayIndex = dayIndex,
                ongoingList = current.ongoingAnimeList
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
