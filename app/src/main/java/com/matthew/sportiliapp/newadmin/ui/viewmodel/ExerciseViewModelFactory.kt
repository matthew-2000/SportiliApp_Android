package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.di.ManualInjection

class ExerciseViewModelFactory(
    private val userCode: String,
    private val dayKey: String,
    private val muscleGroupKey: String,
    private val exerciseKey: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseViewModel::class.java)) {
            return ExerciseViewModel(
                addExerciseUseCase = ManualInjection.addExerciseUseCase,
                updateExerciseUseCase = ManualInjection.updateExerciseUseCase,
                removeExerciseUseCase = ManualInjection.removeExerciseUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
