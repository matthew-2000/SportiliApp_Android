package com.matthew.sportiliapp.avvisi

import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.FakeFirebaseRepository
import com.matthew.sportiliapp.newadmin.TestMainDispatcherRule
import com.matthew.sportiliapp.newadmin.domain.FirebaseRepository
import com.matthew.sportiliapp.newadmin.domain.GetAlertsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsFeedViewModelTest {
    @get:Rule val mainDispatcher = TestMainDispatcherRule()

    @Test
    fun `badge observation contains permission failure and can recover on retry`() = runTest(mainDispatcher.dispatcher) {
        val permissionError = IllegalStateException("Permission denied")
        var denied = true
        val alerts = listOf(Avviso(id = "notice", titolo = "Avviso"))
        val repository = object : FirebaseRepository by FakeFirebaseRepository() {
            override fun getAlerts(): Flow<List<Avviso>> = flow {
                if (denied) throw permissionError
                emit(alerts)
            }
        }
        val viewModel = AlertsFeedViewModel(GetAlertsUseCase(repository))
        advanceUntilIdle()

        assertSame(permissionError, (viewModel.uiState.value as AlertsFeedUiState.Error).throwable)
        denied = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(alerts, (viewModel.uiState.value as AlertsFeedUiState.Success).alerts)
    }

    @Test
    fun `failure after a successful emission clears a stale badge`() = runTest(mainDispatcher.dispatcher) {
        val repository = object : FirebaseRepository by FakeFirebaseRepository() {
            override fun getAlerts(): Flow<List<Avviso>> = flow {
                emit(listOf(Avviso(id = "notice")))
                throw IllegalStateException("Access revoked")
            }
        }
        val viewModel = AlertsFeedViewModel(GetAlertsUseCase(repository))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AlertsFeedUiState.Error)
    }
}
