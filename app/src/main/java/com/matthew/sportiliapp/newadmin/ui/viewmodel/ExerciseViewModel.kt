package com.matthew.sportiliapp.newadmin.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.newadmin.domain.AddExerciseUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveExerciseUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateExerciseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ExerciseUiState {
    object Idle : ExerciseUiState()
    object Loading : ExerciseUiState()
    data class Success(val exercise: Esercizio) : ExerciseUiState()
    data class Error(val exception: Throwable) : ExerciseUiState()
}

class ExerciseViewModel(
    private val addExerciseUseCase: AddExerciseUseCase,
    private val updateExerciseUseCase: UpdateExerciseUseCase,
    private val removeExerciseUseCase: RemoveExerciseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ExerciseUiState>(ExerciseUiState.Idle)
    val state: StateFlow<ExerciseUiState> = _state

    fun addExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        exercise: Esercizio
    ) {
        viewModelScope.launch {
            _state.value = ExerciseUiState.Loading
            val result = addExerciseUseCase(userCode, dayKey, muscleGroupKey, exerciseKey, exercise)
            result.fold(
                onSuccess = { _state.value = ExerciseUiState.Success(exercise) },
                onFailure = { _state.value = ExerciseUiState.Error(it) }
            )
        }
    }

    fun updateExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        exercise: Esercizio
    ) {
        viewModelScope.launch {
            _state.value = ExerciseUiState.Loading
            val result = updateExerciseUseCase(userCode, dayKey, muscleGroupKey, exerciseKey, exercise)
            result.fold(
                onSuccess = { _state.value = ExerciseUiState.Success(exercise) },
                onFailure = { _state.value = ExerciseUiState.Error(it) }
            )
        }
    }

    fun removeExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String
    ) {
        viewModelScope.launch {
            _state.value = ExerciseUiState.Loading
            val result = removeExerciseUseCase(userCode, dayKey, muscleGroupKey, exerciseKey)
            result.fold(
                onSuccess = { _state.value = ExerciseUiState.Idle },
                onFailure = { _state.value = ExerciseUiState.Error(it) }
            )
        }
    }
}
