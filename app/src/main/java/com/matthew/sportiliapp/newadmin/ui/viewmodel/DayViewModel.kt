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
import kotlinx.coroutines.launch

sealed class DayUiState {
    object Idle : DayUiState()
    object Loading : DayUiState()
    data class Success(val day: Giorno) : DayUiState()
    data class Error(val exception: Throwable) : DayUiState()
}

class DayViewModel(
    private val getDayUseCase: GetDayUseCase,
    private val updateDayUseCase: UpdateDayUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DayUiState>(DayUiState.Idle)
    val state: StateFlow<DayUiState> = _state

    fun loadDay(userCode: String, dayKey: String) {
        viewModelScope.launch {
            _state.value = DayUiState.Loading
            getDayUseCase(userCode, dayKey)
                .catch { e -> _state.value = DayUiState.Error(e) }
                .collect { day ->
                    day.sortAll()
                    _state.value = DayUiState.Success(day)
                }
        }
    }

    fun updateDay(userCode: String, dayKey: String, day: Giorno) {
        viewModelScope.launch {
            _state.value = DayUiState.Loading
            val result = updateDayUseCase(userCode, dayKey, day)
            result.fold(
                onSuccess = { _state.value = DayUiState.Success(day) },
                onFailure = { _state.value = DayUiState.Error(it) }
            )
        }
    }

    // Aggiungi metodi per addDay e removeDay se necessario.
}
