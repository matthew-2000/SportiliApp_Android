package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.domain.AddAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateAlertUseCase

class AlertsAdminViewModelFactory(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val addAlertUseCase: AddAlertUseCase,
    private val updateAlertUseCase: UpdateAlertUseCase,
    private val removeAlertUseCase: RemoveAlertUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertsAdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertsAdminViewModel(
                getAlertsUseCase,
                addAlertUseCase,
                updateAlertUseCase,
                removeAlertUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
