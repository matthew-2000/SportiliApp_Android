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
            getMuscleGroupUseCase(userCode, dayKey, groupKey)
                .distinctUntilChanged()
                .catch { e -> _state.value = MuscleGroupUiState.Error(e) }
                .collect { group ->
                    group.sortAll()
                    _state.value = MuscleGroupUiState.Success(group)
                }
        }
    }

    fun addMuscleGroup(userCode: String, dayKey: String, groupKey: String, group: GruppoMuscolare) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = addMuscleGroupUseCase(userCode, dayKey, groupKey, group)
            result.fold(
                onSuccess = { _state.value = MuscleGroupUiState.Success(group) },
                onFailure = { _state.value = MuscleGroupUiState.Error(it) }
            )
        }
    }

    fun updateMuscleGroup(userCode: String, dayKey: String, groupKey: String, group: GruppoMuscolare) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = updateMuscleGroupUseCase(userCode, dayKey, groupKey, group)
            result.fold(
                onSuccess = { _state.value = MuscleGroupUiState.Success(group) },
                onFailure = { _state.value = MuscleGroupUiState.Error(it) }
            )
        }
    }

    fun removeMuscleGroup(userCode: String, dayKey: String, groupKey: String) {
        viewModelScope.launch {
            _state.value = MuscleGroupUiState.Loading
            val result = removeMuscleGroupUseCase(userCode, dayKey, groupKey)
            result.fold(
                onSuccess = { _state.value = MuscleGroupUiState.Idle },
                onFailure = { _state.value = MuscleGroupUiState.Error(it) }
            )
        }
    }
}
