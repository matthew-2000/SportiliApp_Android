package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.domain.AddAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateAlertUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AlertsAdminUiState {
    object Loading : AlertsAdminUiState
    data class Success(val alerts: List<Avviso>) : AlertsAdminUiState
    data class Error(val throwable: Throwable) : AlertsAdminUiState
}

class AlertsAdminViewModel(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val addAlertUseCase: AddAlertUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val removeAlertUseCase: RemoveAlertUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<AlertsAdminUiState> =
        MutableStateFlow(AlertsAdminUiState.Loading)
    val uiState: StateFlow<AlertsAdminUiState> = _uiState
    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    init {
        observeAlerts()
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            getAlertsUseCase()
                .catch { throwable -> _uiState.value = AlertsAdminUiState.Error(throwable) }
                .collectLatest { alerts ->
                    _uiState.value = AlertsAdminUiState.Success(alerts)
                }
        }
    }

    fun addAlert(avviso: Avviso, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _actionState.value = AdminActionState.InProgress
            val result = addAlertUseCase(avviso)
            _actionState.value = result.toActionState("Errore durante il salvataggio dell'avviso")
            onResult(result)
        }
    }

    fun updateAlert(avviso: Avviso, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _actionState.value = AdminActionState.InProgress
            val result = updateAlertUseCase(avviso)
            _actionState.value = result.toActionState("Errore durante l'aggiornamento dell'avviso")
            onResult(result)
        }
    }

    fun removeAlert(alertId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _actionState.value = AdminActionState.InProgress
            val result = removeAlertUseCase(alertId)
            _actionState.value = result.toActionState("Errore durante l'eliminazione dell'avviso")
            onResult(result)
        }
    }

    fun clearActionError() {
        if (_actionState.value is AdminActionState.Error) {
            _actionState.value = AdminActionState.Idle
        }
    }
}

private fun Result<Unit>.toActionState(defaultErrorMessage: String): AdminActionState =
    fold(
        onSuccess = { AdminActionState.Idle },
        onFailure = { error ->
            AdminActionState.Error(error.localizedMessage ?: defaultErrorMessage)
        }
    )
