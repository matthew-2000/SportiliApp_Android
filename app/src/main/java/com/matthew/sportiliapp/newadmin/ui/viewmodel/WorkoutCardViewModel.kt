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

sealed class WorkoutCardUiState {
    object Idle : WorkoutCardUiState()
    object Loading : WorkoutCardUiState()
    data class Success(val scheda: Scheda) : WorkoutCardUiState()
    data class Error(val error: Throwable) : WorkoutCardUiState()
}

class WorkoutCardViewModel(
    private val getWorkoutCardUseCase: GetWorkoutCardUseCase,
    private val updateWorkoutCardUseCase: UpdateWorkoutCardUseCase
    // Puoi aggiungere altri use case per giorni, gruppi ed esercizi se necessario
) : ViewModel() {

    private val _state = MutableStateFlow<WorkoutCardUiState>(WorkoutCardUiState.Idle)
    val state: StateFlow<WorkoutCardUiState> = _state

    // Carica la scheda in base al userCode
    fun loadWorkoutCard(userCode: String) {
        viewModelScope.launch {
            _state.value = WorkoutCardUiState.Loading
            getWorkoutCardUseCase(userCode)
                .catch { exception ->
                    _state.value = WorkoutCardUiState.Error(exception)
                }
                .collect { scheda ->
                    scheda.sortAll()
                    _state.value = WorkoutCardUiState.Success(scheda)
                }
        }
    }

    // Aggiorna la scheda e aggiorna lo stato in base al risultato
    fun updateWorkoutCard(userCode: String, scheda: Scheda) {
        viewModelScope.launch {
            _state.value = WorkoutCardUiState.Loading
            val result = updateWorkoutCardUseCase(userCode, scheda)
            result.fold(
                onSuccess = { _state.value = WorkoutCardUiState.Success(scheda) },
                onFailure = { _state.value = WorkoutCardUiState.Error(it) }
            )
        }
    }
}
