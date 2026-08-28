package com.prstyadev.wibufy.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.QualityItem
import com.prstyadev.wibufy.data.RetrofitClient
import com.prstyadev.wibufy.data.StreamData
import com.prstyadev.wibufy.data.WatchHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException

data class PlayerUiState(
    val isLoading: Boolean = true,
    val streamData: StreamData? = null,
    val currentQuality: String? = null,
    val currentQualityUrl: String? = null,
    val initialPositionMs: Long = 0L,
    val error: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("wibufy_player_prefs", Context.MODE_PRIVATE)
    private val historyRepository = WatchHistoryRepository(application)

    companion object {
        const val PREF_SELECTED_QUALITY = "PREF_SELECTED_QUALITY"
    }

    // In-memory stream cache
    private val streamCache = mutableMapOf<String, StreamData>()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private fun getSavedQuality(): String? {
        return prefs.getString(PREF_SELECTED_QUALITY, null)
    }

    private fun saveQualityPreference(quality: String) {
        prefs.edit().putString(PREF_SELECTED_QUALITY, quality).apply()
    }

    private fun selectBestQuality(data: StreamData?): Pair<String?, String?> {
        val savedQuality = getSavedQuality()
        val defaultQuality = data?.defaultQuality ?: "480p"

        val selectedQualityItem = if (!savedQuality.isNullOrEmpty()) {
            data?.qualities?.find { it.quality.equals(savedQuality, ignoreCase = true) }
                ?: data?.qualities?.find { it.quality.equals(defaultQuality, ignoreCase = true) }
                ?: data?.qualities?.find { it.quality.equals("480p", ignoreCase = true) }
                ?: data?.qualities?.firstOrNull()
        } else {
            data?.qualities?.find { it.quality.equals(defaultQuality, ignoreCase = true) }
                ?: data?.qualities?.find { it.quality.equals("480p", ignoreCase = true) }
                ?: data?.qualities?.firstOrNull()
        }
        return Pair(selectedQualityItem?.quality, selectedQualityItem?.url)
    }

    fun loadStream(episodeSlug: String) {
        viewModelScope.launch {
            val savedHistory = historyRepository.getHistory(episodeSlug)
            val initialPos = savedHistory?.lastPositionMs ?: 0L

            // Check cache first
            val cachedStream = streamCache[episodeSlug]
            if (cachedStream != null) {
                val (qualityName, qualityUrl) = selectBestQuality(cachedStream)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        streamData = cachedStream,
                        currentQuality = qualityName,
                        currentQualityUrl = qualityUrl,
                        initialPositionMs = initialPos,
                        error = null
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null, initialPositionMs = initialPos) }
            try {
                val response = RetrofitClient.apiService.getStreamEngine(episodeSlug)
                val data = response.data
                if (data != null) {
                    streamCache[episodeSlug] = data
                }

                val (qualityName, qualityUrl) = selectBestQuality(data)

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        streamData = data,
                        currentQuality = qualityName,
                        currentQualityUrl = qualityUrl,
                        initialPositionMs = initialPos
                    )
                }
            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(isLoading = false, error = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Kesalahan jaringan: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Terjadi kesalahan") }
            }
        }
    }

    fun saveProgress(
        episodeSlug: String,
        animeTitle: String?,
        episodeName: String?,
        posterUrl: String?,
        lastPositionMs: Long,
        totalDurationMs: Long
    ) {
        if (lastPositionMs <= 0 && totalDurationMs <= 0) return
        viewModelScope.launch {
            historyRepository.saveProgress(
                episodeSlug = episodeSlug,
                animeTitle = animeTitle,
                episodeName = episodeName,
                posterUrl = posterUrl,
                lastPositionMs = lastPositionMs,
                totalDurationMs = totalDurationMs
            )
        }
    }

    fun changeQuality(qualityItem: QualityItem) {
        val qualityName = qualityItem.quality
        val url = qualityItem.url ?: return
        if (!qualityName.isNullOrEmpty()) {
            saveQualityPreference(qualityName)
        }
        _uiState.update { 
            it.copy(
                currentQuality = qualityName,
                currentQualityUrl = url
            ) 
        }
    }
}
