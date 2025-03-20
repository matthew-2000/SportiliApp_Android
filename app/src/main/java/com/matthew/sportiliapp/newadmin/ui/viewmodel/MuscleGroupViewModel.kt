package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.newadmin.domain.AddMuscleGroupUseCase
import com.matthew.sportiliapp.newadmin.domain.GetMuscleGroupUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveMuscleGroupUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateMuscleGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class MuscleGroupUiState {
    object Idle : MuscleGroupUiState()
    object Loading : MuscleGroupUiState()
    data class Success(val group: GruppoMuscolare) : MuscleGroupUiState()
    data class Error(val exception: Throwable) : MuscleGroupUiState()
}

class MuscleGroupViewModel(
    private val getMuscleGroupUseCase: GetMuscleGroupUseCase,
    private val addMuscleGroupUseCase: AddMuscleGroupUseCase,
    private val updateMuscleGroupUseCase: UpdateMuscleGroupUseCase,
    private val removeMuscleGroupUseCase: RemoveMuscleGroupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<MuscleGroupUiState>(MuscleGroupUiState.Idle)
    val state: StateFlow<MuscleGroupUiState> = _state

    fun loadGroup(userCode: String, dayKey: String, groupKey: String) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading

            val result = getMuscleGroupUseCase(userCode, dayKey, groupKey)
            result.fold(
                onSuccess = { group ->
                    group.sortAll() // Se esiste un metodo di ordinamento
                    _state.value = MuscleGroupUiState.Success(group)
                },
                onFailure = { e ->
                    _state.value = MuscleGroupUiState.Error(e)
                }
            )
        }
    }

    fun addMuscleGroup(userCode: String, dayKey: String, groupKey: String, group: GruppoMuscolare) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = addMuscleGroupUseCase(userCode, dayKey, groupKey, group)
            result.fold(
                onSuccess = {
                    _state.value = MuscleGroupUiState.Success(group)
                },
                onFailure = { e ->
                    _state.value = MuscleGroupUiState.Error(e)
                }
            )
        }
    }

    fun updateMuscleGroup(userCode: String, dayKey: String, groupKey: String, group: GruppoMuscolare) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = updateMuscleGroupUseCase(userCode, dayKey, groupKey, group)
            result.fold(
                onSuccess = {
                    _state.value = MuscleGroupUiState.Success(group)
                },
                onFailure = { e ->
                    _state.value = MuscleGroupUiState.Error(e)
                }
            )
        }
    }

    fun removeMuscleGroup(userCode: String, dayKey: String, groupKey: String) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = removeMuscleGroupUseCase(userCode, dayKey, groupKey)
            result.fold(
                onSuccess = {
                    // Se vuoi, potresti ritornare alla schermata precedente
                    // o semplicemente segnalare "Idle" per dire che non c'è più un gruppo.
                    _state.value = MuscleGroupUiState.Idle
                },
                onFailure = { e ->
                    _state.value = MuscleGroupUiState.Error(e)
                }
            )
        }
    }
}