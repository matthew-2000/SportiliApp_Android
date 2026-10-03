package com.matthew.sportiliapp.newadmin.ui.viewmodel

import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.FakeFirebaseRepository
import com.matthew.sportiliapp.newadmin.TestMainDispatcherRule
import com.matthew.sportiliapp.newadmin.domain.AddAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateAlertUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsAdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = TestMainDispatcherRule()

    @Test
    fun `observe alerts exposes repository values`() = runTest {
        val repository = FakeFirebaseRepository().apply {
            alertsFlow.value = listOf(Avviso(id = "a1", titolo = "Promo"))
        }
        val viewModel = AlertsAdminViewModel(
            getAlertsUseCase = GetAlertsUseCase(repository),
            addAlertUseCase = AddAlertUseCase(repository),
            updateAlertUseCase = UpdateAlertUseCase(repository),
            removeAlertUseCase = RemoveAlertUseCase(repository)
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as AlertsAdminUiState.Success
        assertEquals(1, state.alerts.size)
        assertEquals("a1", state.alerts.first().id)
    }

    @Test
    fun `add alert failure updates action state`() = runTest {
        val repository = FakeFirebaseRepository().apply {
            addAlertResult = Result.failure(IllegalStateException("write failed"))
        }
        val viewModel = AlertsAdminViewModel(
            getAlertsUseCase = GetAlertsUseCase(repository),
            addAlertUseCase = AddAlertUseCase(repository),
            updateAlertUseCase = UpdateAlertUseCase(repository),
            removeAlertUseCase = RemoveAlertUseCase(repository)
        )

        viewModel.addAlert(Avviso(titolo = "Alert"))
        advanceUntilIdle()

        val actionState = viewModel.actionState.value
        assertTrue(actionState is AdminActionState.Error)
        assertEquals("write failed", (actionState as AdminActionState.Error).message)
    }
    @Test
    fun `add and update wait for outcome and reject repeated or overlapping writes`() = runTest {
        for (updating in listOf(false, true)) {
            for (succeeds in listOf(false, true)) {
                val gate = CompletableDeferred<Unit>()
                val result = if (succeeds) Result.success(Unit)
                    else Result.failure<Unit>(IllegalStateException("write failed"))
                val repository = FakeFirebaseRepository().apply {
                    awaitAlertWrite = { gate.await() }
                    addAlertResult = result
                    updateAlertResult = result
                }
                val viewModel = AlertsAdminViewModel(
                    GetAlertsUseCase(repository), AddAlertUseCase(repository),
                    UpdateAlertUseCase(repository), RemoveAlertUseCase(repository)
                )
                val outcomes = mutableListOf<Result<Unit>>()
                val alert = Avviso(id = if (updating) "a1" else "", titolo = "Bozza")
                fun submit() {
                    if (updating) viewModel.updateAlert(alert) { outcomes.add(it) }
                    else viewModel.addAlert(alert) { outcomes.add(it) }
                }
                submit()
                submit() // Both calls occur before the dispatcher runs.
                viewModel.removeAlert("a1") { error("An overlapping delete must be rejected") }
                assertTrue(viewModel.actionState.value is AdminActionState.InProgress)
                runCurrent()
                assertEquals(1, repository.addAlertCalls + repository.updateAlertCalls)
                assertTrue(outcomes.isEmpty())
                gate.complete(Unit)
                advanceUntilIdle()
                assertEquals(listOf(result), outcomes)
                if (succeeds) assertTrue(viewModel.actionState.value is AdminActionState.Idle)
                else {
                    assertEquals(AdminActionState.Error("write failed"), viewModel.actionState.value)
                    // Retry is accepted after failure, without creating a new editor/view model.
                    submit()
                    advanceUntilIdle()
                    assertEquals(2, repository.addAlertCalls + repository.updateAlertCalls)
                    assertEquals(2, outcomes.size)
                }
            }
        }
    }

}
