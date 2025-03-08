package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.matthew.sportiliapp.newadmin.domain.*

class GymAdminViewModelFactory(
    private val getUsersUseCase: GetUsersUseCase,
    private val addUserUseCase: AddUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val removeUserUseCase: RemoveUserUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GymAdminViewModel::class.java)) {
            return GymAdminViewModel(
                getUsersUseCase = getUsersUseCase,
                addUserUseCase = addUserUseCase,
                updateUserUseCase = updateUserUseCase,
                removeUserUseCase = removeUserUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
