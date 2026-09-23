package com.matthew.sportiliapp.avvisi

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.domain.FirebaseRepository
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlertsScreenTest {
    @get:Rule val compose = createComposeRule()

    @After fun resetRepository() = ManualInjection.resetOverrides()

    @Test fun permissionErrorIsVisibleAndRetryLoadsAlertsWithoutRestarting() {
        val attempts = AtomicInteger()
        // Fail loudly on unexpected calls instead of falling back to the live backend.
        val repository = Proxy.newProxyInstance(
            FirebaseRepository::class.java.classLoader,
            arrayOf(FirebaseRepository::class.java)
        ) { _, method, _ ->
            check(method.name == "getAlerts") { "Unexpected repository call: ${method.name}" }
            flow {
                if (attempts.incrementAndGet() == 1) error("Permission denied (test)")
                emit(listOf(Avviso(id = "local-test", titolo = "Avviso locale", descrizione = "Recuperato")))
            }
        } as FirebaseRepository
        ManualInjection.overrideRepositoryForTesting(repository)

        compose.setContent { MaterialTheme { AvvisiScreen() } }
        compose.onNodeWithText("Errore durante il caricamento degli avvisi").assertIsDisplayed()
        compose.onNodeWithText("Permission denied (test)").assertIsDisplayed()
        compose.onNodeWithText("Riprova").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Avviso locale").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Avviso locale").assertIsDisplayed()
        compose.onNodeWithText("Riprova").assertDoesNotExist()
        assertEquals(2, attempts.get())
    }
}
