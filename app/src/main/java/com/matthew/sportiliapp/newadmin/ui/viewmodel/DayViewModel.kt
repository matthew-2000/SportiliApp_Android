// newadmin/ui/viewmodels/DayViewModel.kt
package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.newadmin.domain.GetDayUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateDayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DayUiState {
    object Idle : DayUiState()
    object Loading : DayUiState()
    data class Success(val day: Giorno) : DayUiState()
    data class Error(val exception: Throwable) : DayUiState()
}

class DayViewModel(
    private val getDayUseCase: GetDayUseCase,
    private val updateDayUseCase: UpdateDayUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<DayUiState>(DayUiState.Idle)
    val state: StateFlow<DayUiState> = _state
    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    fun loadDay(userCode: String, dayKey: String) {
        viewModelScope.launch {
            _state.value = DayUiState.Loading
            _actionState.value = AdminActionState.Idle

            val result = getDayUseCase(userCode, dayKey)

            result.fold(
                onSuccess = { day ->
                    day.sortAll()  // Se hai una funzione di ordinamento
                    _state.value = DayUiState.Success(day)
                },
                onFailure = { e ->
                    _state.value = DayUiState.Error(e)
                }
            )
        }
    }

    fun updateDay(userCode: String, dayKey: String, day: Giorno, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _actionState.value = AdminActionState.InProgress
            val result = updateDayUseCase(userCode, dayKey, day)
            result.fold(
                onSuccess = {
                    day.sortAll()
                    _state.value = DayUiState.Success(day)
                    _actionState.value = AdminActionState.Idle
                    onSuccess()
                },
                onFailure = { e ->
                    _actionState.value = AdminActionState.Error(
                        e.localizedMessage ?: "Errore durante il salvataggio del giorno"
                    )
                }
            )
        }
    }

    fun clearActionError() {
        if (_actionState.value is AdminActionState.Error) {
            _actionState.value = AdminActionState.Idle
        }
    }
}
