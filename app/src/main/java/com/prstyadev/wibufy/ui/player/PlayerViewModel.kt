package com.prstyadev.wibufy.ui.player

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.QualityItem
import com.prstyadev.wibufy.data.RetrofitClient
import com.prstyadev.wibufy.data.StreamData
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
    val error: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("wibufy_player_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_SELECTED_QUALITY = "PREF_SELECTED_QUALITY"
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private fun getSavedQuality(): String? {
        return prefs.getString(PREF_SELECTED_QUALITY, null)
    }

    private fun saveQualityPreference(quality: String) {
        prefs.edit().putString(PREF_SELECTED_QUALITY, quality).apply()
    }

    fun loadStream(episodeSlug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getStreamEngine(episodeSlug)
                val data = response.data
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

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        streamData = data,
                        currentQuality = selectedQualityItem?.quality,
                        currentQualityUrl = selectedQualityItem?.url
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
