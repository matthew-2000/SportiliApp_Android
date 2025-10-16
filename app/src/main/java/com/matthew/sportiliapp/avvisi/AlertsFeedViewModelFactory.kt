package com.matthew.sportiliapp.avvisi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase

class AlertsFeedViewModelFactory(
    private val getAlertsUseCase: GetAlertsUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsFeedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertsFeedViewModel(getAlertsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
