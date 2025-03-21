package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.newadmin.domain.GetWorkoutCardUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateWorkoutCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

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

    fun loadWorkoutCard(userCode: String) {
        viewModelScope.launch {
            _state.value = WorkoutCardUiState.Loading
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

    fun updateWorkoutCard(userCode: String, scheda: Scheda) {
        viewModelScope.launch {
            _state.value = WorkoutCardUiState.Loading
            val result = updateWorkoutCardUseCase(userCode, scheda)
            result.fold(
                onSuccess = {
                    // Aggiorni lo stato locale con la scheda modificata
                    _state.value = WorkoutCardUiState.Success(scheda)
                },
                onFailure = { exception ->
                    _state.value = WorkoutCardUiState.Error(exception)
                }
            )
        }
    }
}
