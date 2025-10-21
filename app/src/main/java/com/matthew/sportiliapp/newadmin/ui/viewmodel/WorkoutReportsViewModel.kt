package com.matthew.sportiliapp.newadmin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.domain.GetWorkoutIssueReportsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveWorkoutIssueReportUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateWorkoutIssueReportUseCase
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

    init {
        observeReports()
    }

    private fun observeReports() {
        viewModelScope.launch {
            getReportsUseCase()
                .catch { throwable -> _uiState.value = WorkoutReportsUiState.Error(throwable) }
                .collectLatest { reports ->
                    _uiState.value = WorkoutReportsUiState.Success(reports)
                }
        }
    }

    fun toggleResolved(report: WorkoutIssueReport) {
        viewModelScope.launch {
            val updated = report.copy(resolved = !report.resolved)
            updateReportUseCase(updated)
        }
    }

    fun removeReport(reportId: String) {
        viewModelScope.launch {
            removeReportUseCase(reportId)
        }
    }
}

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
