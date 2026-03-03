package com.matthew.sportiliapp.newadmin.ui.viewmodel

import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.FakeFirebaseRepository
import com.matthew.sportiliapp.newadmin.TestMainDispatcherRule
import com.matthew.sportiliapp.newadmin.domain.GetWorkoutIssueReportsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveWorkoutIssueReportUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateWorkoutIssueReportUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule()

    @Test
    fun `observe reports exposes repository values`() = runTest {
        val repository = FakeFirebaseRepository().apply {
            reportsFlow.value = listOf(WorkoutIssueReport(id = "r1", userCode = "abc123"))
        }
        val viewModel = WorkoutReportsViewModel(
            getReportsUseCase = GetWorkoutIssueReportsUseCase(repository),
            updateReportUseCase = UpdateWorkoutIssueReportUseCase(repository),
            removeReportUseCase = RemoveWorkoutIssueReportUseCase(repository)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkoutReportsUiState.Success
        assertEquals(1, state.reports.size)
        assertEquals("r1", state.reports.first().id)
    }

    @Test
    fun `remove report failure updates action state`() = runTest {
        val repository = FakeFirebaseRepository().apply {
            removeReportResult = Result.failure(IllegalStateException("delete failed"))
        }
        val viewModel = WorkoutReportsViewModel(
            getReportsUseCase = GetWorkoutIssueReportsUseCase(repository),
            updateReportUseCase = UpdateWorkoutIssueReportUseCase(repository),
            removeReportUseCase = RemoveWorkoutIssueReportUseCase(repository)
        )

        viewModel.removeReport("r1")
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is AdminActionState.Error)
        assertEquals("delete failed", (actionState as AdminActionState.Error).message)
    }
}
