package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.newadmin.domain.GetWorkoutCardUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateWorkoutCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WorkoutCardUiState {
    object Idle : WorkoutCardUiState()
    object Loading : WorkoutCardUiState()
    data class Success(val scheda: Scheda) : WorkoutCardUiState()
    data class Error(val error: Throwable) : WorkoutCardUiState()
}

class WorkoutCardViewModel(
    private val getWorkoutCardUseCase: GetWorkoutCardUseCase,
    private val updateWorkoutCardUseCase: UpdateWorkoutCardUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<WorkoutCardUiState>(WorkoutCardUiState.Idle)
    val state: StateFlow<WorkoutCardUiState> = _state
    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    fun loadWorkoutCard(userCode: String) {
        viewModelScope.launch {
            _state.value = WorkoutCardUiState.Loading
            _actionState.value = AdminActionState.Idle
            // Ora getWorkoutCardUseCase restituisce Result<Scheda>
            val result = getWorkoutCardUseCase(userCode)

            result.fold(
                onSuccess = { scheda ->
                    // Se la lettura va a buon fine
                    scheda.sortAll() // se hai un metodo per ordinare
                    _state.value = WorkoutCardUiState.Success(scheda)
                },
                onFailure = { exception ->
                    // In caso di errore
                    _state.value = WorkoutCardUiState.Error(exception)
                }
            )
        }
    }

    fun updateWorkoutCard(userCode: String, scheda: Scheda, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _actionState.value = AdminActionState.InProgress
            val result = updateWorkoutCardUseCase(userCode, scheda)
            result.fold(
                onSuccess = {
                    scheda.sortAll()
                    loadWorkoutCard(userCode)
                    _actionState.value = AdminActionState.Idle
                    onSuccess()
                },
                onFailure = { exception ->
                    _actionState.value = AdminActionState.Error(
                        exception.localizedMessage ?: "Errore durante il salvataggio della scheda"
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
