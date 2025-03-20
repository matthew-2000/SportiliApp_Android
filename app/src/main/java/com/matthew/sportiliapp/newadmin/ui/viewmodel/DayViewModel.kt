// newadmin/ui/viewmodels/DayViewModel.kt
package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.newadmin.domain.GetDayUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateDayUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class DayUiState {
    object Idle : DayUiState()
    object Loading : DayUiState()
    data class Success(val day: Giorno) : DayUiState()
    data class Error(val exception: Throwable) : DayUiState()
}

class DayViewModel(
    private val getDayUseCase: GetDayUseCase,
    private val updateDayUseCase: UpdateDayUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<DayUiState>(DayUiState.Idle)
    val state: StateFlow<DayUiState> = _state

    fun loadDay(userCode: String, dayKey: String) {
        viewModelScope.launch {
            _state.value = DayUiState.Loading

            // Ora il getDayUseCase restituisce: Result<Giorno>
            val result = getDayUseCase(userCode, dayKey)

            result.fold(
                onSuccess = { day ->
                    day.sortAll()  // Se hai una funzione di ordinamento
                    _state.value = DayUiState.Success(day)
                },
                onFailure = { e ->
                    _state.value = DayUiState.Error(e)
                }
            )
        }
    }

    fun updateDay(userCode: String, dayKey: String, day: Giorno) {
        viewModelScope.launch {
            _state.value = DayUiState.Loading
            val result = updateDayUseCase(userCode, dayKey, day)
            result.fold(
                onSuccess = {
                    // Se l'update va bene, aggiorna lo stato con i nuovi dati
                    _state.value = DayUiState.Success(day)
                },
                onFailure = { e ->
                    _state.value = DayUiState.Error(e)
                }
            )
        }
    }
}