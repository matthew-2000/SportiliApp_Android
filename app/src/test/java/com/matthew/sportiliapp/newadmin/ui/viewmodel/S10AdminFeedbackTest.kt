package com.matthew.sportiliapp.newadmin.ui.viewmodel

import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.*
import com.matthew.sportiliapp.newadmin.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class S10AdminFeedbackTest {
    @get:Rule val main = TestMainDispatcherRule()

    @Test fun alertLoadFailureRetriesSameObserverWithoutDuplicateSubscriptions() = runTest {
        var attempts = 0
        var active = 0
        var maximum = 0
        val repository = FakeFirebaseRepository().apply {
            alertsFlow.value = listOf(Avviso(id = "same-id"))
            observeAlerts = { flow {
                attempts++; active++; maximum = maxOf(maximum, active)
                try {
                    if (attempts == 1) error("technical permission details")
                    emitAll(alertsFlow)
                } finally { active-- }
            } }
        }
        val vm = AlertsAdminViewModel(GetAlertsUseCase(repository), AddAlertUseCase(repository),
            UpdateAlertUseCase(repository), RemoveAlertUseCase(repository))
        runCurrent()
        assertTrue(vm.uiState.value is AlertsAdminUiState.Error)
        vm.retryLoading(); vm.retryLoading()
        assertTrue(vm.uiState.value is AlertsAdminUiState.Loading)
        runCurrent()
        assertEquals("same-id", (vm.uiState.value as AlertsAdminUiState.Success).alerts.single().id)
        vm.retryLoading(); runCurrent()
        assertEquals(2, attempts)
        assertEquals(1, maximum)
    }

    @Test fun reportLoadFailureRetriesAndStillReceivesLiveUpdates() = runTest {
        var attempts = 0
        val repository = FakeFirebaseRepository().apply {
            observeReports = { flow {
                if (++attempts == 1) error("technical database details")
                emitAll(reportsFlow)
            } }
        }
        val vm = WorkoutReportsViewModel(GetWorkoutIssueReportsUseCase(repository),
            UpdateWorkoutIssueReportUseCase(repository), RemoveWorkoutIssueReportUseCase(repository))
        runCurrent()
        assertTrue(vm.uiState.value is WorkoutReportsUiState.Error)
        vm.retryLoading(); vm.retryLoading(); runCurrent()
        assertEquals(2, attempts)
        assertTrue((vm.uiState.value as WorkoutReportsUiState.Success).reports.isEmpty())
        repository.reportsFlow.value = listOf(WorkoutIssueReport(id = "r", resolved = true))
        runCurrent()
        assertTrue((vm.uiState.value as WorkoutReportsUiState.Success).reports.single().resolved)
    }

    @Test fun reportActionsRejectOverlappingWritesAndRetryOriginalIdentityAndData() = runTest {
        for (removing in listOf(false, true)) {
            var gate = CompletableDeferred<Unit>()
            val records = mutableListOf<WorkoutIssueReport>()
            val removals = mutableListOf<String>()
            val repository = FakeFirebaseRepository().apply {
                saveReport = { records.add(it); gate.await(); Result.success(Unit) }
                deleteReport = { removals.add(it); gate.await(); Result.success(Unit) }
                updateReportResult = Result.failure(IllegalStateException("raw exception"))
                removeReportResult = Result.failure(IllegalStateException("raw exception"))
            }
            val vm = WorkoutReportsViewModel(GetWorkoutIssueReportsUseCase(repository),
                UpdateWorkoutIssueReportUseCase(repository), RemoveWorkoutIssueReportUseCase(repository))
            val report = WorkoutIssueReport(id = "r", userCode = "CODE", message = "Keep this message",
                createdAt = 123, resolutionNote = "Keep this note")
            val results = mutableListOf<Result<Unit>>()
            fun submit() {
                if (removing) vm.removeReport(report.id) { results.add(it) }
                else vm.toggleResolved(report) { results.add(it) }
            }
            submit(); submit()
            vm.toggleResolved(report) { error("Overlapping toggle") }
            vm.removeReport(report.id) { error("Overlapping delete") }
            assertTrue(vm.actionState.value is AdminActionState.InProgress)
            runCurrent()
            assertEquals(1, repository.reportWrites + repository.reportRemovals)
            assertTrue(results.isEmpty())
            gate.complete(Unit); runCurrent()
            assertTrue(results.single().isFailure)
            assertFalse((vm.actionState.value as AdminActionState.Error).message.contains("raw exception"))
            gate = CompletableDeferred()
            repository.updateReportResult = Result.success(Unit)
            repository.removeReportResult = Result.success(Unit)
            submit(); runCurrent()
            gate.complete(Unit); runCurrent()
            assertTrue(vm.actionState.value is AdminActionState.Idle)
            assertTrue(results.last().isSuccess)
            if (removing) assertEquals(listOf("r", "r"), removals)
            else assertEquals(listOf(report.copy(resolved = true), report.copy(resolved = true)), records)
        }
    }
}
