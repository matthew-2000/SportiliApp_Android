package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

class GymAdminViewModel(
    private val getUsersUseCase: GetUsersUseCase,
    private val addUserUseCase: AddUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val removeUserUseCase: RemoveUserUseCase
) : ViewModel() {

    private val _usersState = MutableStateFlow<UiState<List<Utente>>>(UiState.Loading)
    val usersState: StateFlow<UiState<List<Utente>>> = _usersState

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            getUsersUseCase()
                .distinctUntilChanged()
                .catch { e -> _usersState.value = UiState.Error(e) }
                .collect { users ->
                    for (user in users) {
                        user.scheda?.sortAll()
                    }
                    _usersState.value = UiState.Success(users)
                }
        }
    }

    fun addUser(user: Utente, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = addUserUseCase(user)
            onResult(result)
            // Ricarica la lista
            loadUsers()
        }
    }

    fun updateUser(user: Utente, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = updateUserUseCase(user)
            onResult(result)
            loadUsers()
        }
    }

    fun removeUser(userCode: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = removeUserUseCase(userCode)
            onResult(result)
            loadUsers()
        }
    }
}
