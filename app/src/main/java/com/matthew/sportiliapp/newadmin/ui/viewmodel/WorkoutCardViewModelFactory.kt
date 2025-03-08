// newadmin/ui/viewmodels/WorkoutCardViewModelFactory.kt
package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.di.ManualInjection

class WorkoutCardViewModelFactory(
    private val userCode: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutCardViewModel::class.java)) {
            // Recupera il use case tramite l'iniezione manuale
            val updateWorkoutCardUseCase = ManualInjection.updateWorkoutCardUseCase
            val getWorkoutCardUseCase = ManualInjection.getWorkoutCardUseCase
            return WorkoutCardViewModel(getWorkoutCardUseCase, updateWorkoutCardUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
