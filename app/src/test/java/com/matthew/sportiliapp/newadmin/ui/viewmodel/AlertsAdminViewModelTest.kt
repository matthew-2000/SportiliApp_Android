package com.matthew.sportiliapp.newadmin.ui.viewmodel

import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.FakeFirebaseRepository
import com.matthew.sportiliapp.newadmin.TestMainDispatcherRule
import com.matthew.sportiliapp.newadmin.domain.AddAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import com.matthew.sportiliapp.newadmin.domain.RemoveAlertUseCase
import com.matthew.sportiliapp.newadmin.domain.UpdateAlertUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
}
