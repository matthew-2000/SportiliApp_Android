package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.domain.GetWorkoutIssueReportsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveWorkoutIssueReportUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateWorkoutIssueReportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface WorkoutReportsUiState {
    object Loading : WorkoutReportsUiState
    data class Success(val reports: List<WorkoutIssueReport>) : WorkoutReportsUiState
    data class Error(val throwable: Throwable) : WorkoutReportsUiState
}

class WorkoutReportsViewModel(
    private val getReportsUseCase: GetWorkoutIssueReportsUseCase,
    private val updateReportUseCase: UpdateWorkoutIssueReportUseCase,
    private val removeReportUseCase: RemoveWorkoutIssueReportUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<WorkoutReportsUiState> =
        MutableStateFlow(WorkoutReportsUiState.Loading)
    val uiState: StateFlow<WorkoutReportsUiState> = _uiState
    private val _actionState = MutableStateFlow<AdminActionState>(AdminActionState.Idle)
    val actionState: StateFlow<AdminActionState> = _actionState

    private var observation: Job? = null

    init {
        observeReports()
    }

    fun retryLoading() {
        if (_uiState.value !is WorkoutReportsUiState.Error) return
        _uiState.value = WorkoutReportsUiState.Loading
        observeReports()
    }

    private fun observeReports() {
        observation?.cancel()
        observation = viewModelScope.launch {
            getReportsUseCase()
                .catch { throwable -> _uiState.value = WorkoutReportsUiState.Error(throwable) }
                .collectLatest { reports ->
                    _uiState.value = WorkoutReportsUiState.Success(reports)
                }
        }
    }

    fun toggleResolved(report: WorkoutIssueReport, onResult: (Result<Unit>) -> Unit = {}) {
        if (_actionState.value is AdminActionState.InProgress) return
        _actionState.value = AdminActionState.InProgress
        viewModelScope.launch {
            val updated = report.copy(resolved = !report.resolved)
            val result = updateReportUseCase(updated)
            _actionState.value = result.toActionState("Errore durante l'aggiornamento della segnalazione")
            onResult(result)
        }
    }

    fun removeReport(reportId: String, onResult: (Result<Unit>) -> Unit = {}) {
        if (_actionState.value is AdminActionState.InProgress) return
        _actionState.value = AdminActionState.InProgress
        viewModelScope.launch {
            val result = removeReportUseCase(reportId)
            _actionState.value = result.toActionState("Errore durante l'eliminazione della segnalazione")
            onResult(result)
        }
    }

    fun clearActionError() {
        if (_actionState.value is AdminActionState.Error) {
            _actionState.value = AdminActionState.Idle
        }
    }
}

private fun Result<Unit>.toActionState(defaultErrorMessage: String): AdminActionState =
    fold(
        onSuccess = { AdminActionState.Idle },
        onFailure = {
            AdminActionState.Error(defaultErrorMessage + ". Riprova.")
        }
    )

class WorkoutReportsViewModelFactory(
    private val getReportsUseCase: GetWorkoutIssueReportsUseCase,
    private val updateReportUseCase: UpdateWorkoutIssueReportUseCase,
    private val removeReportUseCase: RemoveWorkoutIssueReportUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutReportsViewModel::class.java)) {
            return WorkoutReportsViewModel(getReportsUseCase, updateReportUseCase, removeReportUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
