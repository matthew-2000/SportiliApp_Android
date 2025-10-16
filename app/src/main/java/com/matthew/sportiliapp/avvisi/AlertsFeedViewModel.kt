package com.matthew.sportiliapp.avvisi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AlertsFeedUiState {
    object Loading : AlertsFeedUiState
    data class Success(val alerts: List<Avviso>) : AlertsFeedUiState
    data class Error(val throwable: Throwable) : AlertsFeedUiState
}

class AlertsFeedViewModel(
    private val getAlertsUseCase: GetAlertsUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<AlertsFeedUiState> =
        MutableStateFlow(AlertsFeedUiState.Loading)
    val uiState: StateFlow<AlertsFeedUiState> = _uiState

    init {
        observeAlerts()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            getAlertsUseCase()
                .catch { throwable -> _uiState.value = AlertsFeedUiState.Error(throwable) }
                .collectLatest { alerts ->
                    _uiState.value = AlertsFeedUiState.Success(alerts)
                }
        }
    }
}
