package com.prstyadev.wibufy.ui.player

import androidx.lifecycle.ViewModel
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
    val currentQualityUrl: String? = null,
    val error: String? = null
)

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun loadStream(episodeSlug: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getStreamEngine(episodeSlug)
                val data = response.data
                val defaultQuality = data?.defaultQuality
                
                val bestQuality = data?.qualities?.find { it.quality == "720p" }
                    ?: data?.qualities?.find { it.quality == defaultQuality }
                    ?: data?.qualities?.firstOrNull()

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        streamData = data,
                        currentQualityUrl = bestQuality?.url
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

    fun changeQuality(url: String) {
        _uiState.update { it.copy(currentQualityUrl = url) }
    }
}
