package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.di.ManualInjection

class DayViewModelFactory(
    private val userCode: String,
    private val dayKey: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayViewModel::class.java)) {
            return DayViewModel(
                getDayUseCase = ManualInjection.getDayUseCase,
                updateDayUseCase = ManualInjection.updateDayUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
