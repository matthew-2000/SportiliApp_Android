package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.di.ManualInjection

class MuscleGroupViewModelFactory(
    private val userCode: String,
    private val dayKey: String,
    private val groupKey: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MuscleGroupViewModel::class.java)) {
            return MuscleGroupViewModel(
                getMuscleGroupUseCase = ManualInjection.getMuscleGroupUseCase,
                addMuscleGroupUseCase = ManualInjection.addMuscleGroupUseCase,
                updateMuscleGroupUseCase = ManualInjection.updateMuscleGroupUseCase,
                removeMuscleGroupUseCase = ManualInjection.removeMuscleGroupUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
