package com.prstyadev.wibufy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prstyadev.wibufy.data.HomeData
import com.prstyadev.wibufy.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import java.io.IOException
import java.net.UnknownHostException

data class HomeUiState(
    val isLoading: Boolean = true,
    val homeData: HomeData? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchHomeData()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = RetrofitClient.apiService.getHome()
                _uiState.update { it.copy(isLoading = false, homeData = response.data) }
            } catch (e: UnknownHostException) {
                _uiState.update { it.copy(isLoading = false, error = "Unable to reach server. Please check your internet connection.") }
            } catch (e: IOException) {
                _uiState.update { it.copy(isLoading = false, error = "Network error: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
