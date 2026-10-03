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
import org.junit.Assert.assertFalse
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.matthew.sportiliapp.s06Alerts
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
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
        compose.onNodeWithText("Controlla la connessione e riprova.").assertIsDisplayed()
        compose.onNodeWithText("Permission denied (test)").assertDoesNotExist()
        compose.onNodeWithText("Riprova").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Avviso locale").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Avviso locale").assertIsDisplayed()
        compose.onNodeWithText("Attivi (1)").assertIsDisplayed()
        compose.onNodeWithText("Da leggere").assertDoesNotExist()
        compose.onNodeWithText("Riprova").assertDoesNotExist()
        assertEquals(2, attempts.get())
    }
    @Test fun allPrioritiesAndExpiredDateRemainReadableWithoutDuplicateChips() {
        compose.setContent { SportiliAppTheme { AvvisiContent(AlertsFeedUiState.Success(s06Alerts()), {}) } }
        listOf("Priorità alta", "Priorità media", "Priorità bassa", "Senza priorità").forEach {
            compose.onAllNodesWithText(it).onFirst().performScrollTo().assertIsDisplayed()
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Scaduti (1)"))
        compose.onNodeWithText("Scaduti (1)").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Orario festivo"))
        compose.onNodeWithText("Orario festivo").assertIsDisplayed()
        compose.onNodeWithText("Scaduto il", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Scaduto", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Priorità: Alta").assertDoesNotExist()
    }

    @Test fun longTitleDescriptionAndDateWrapWithoutOverflow() {
        val alert = s06Alerts(long = true).single()
        compose.setContent { SportiliAppTheme { AvvisiContent(AlertsFeedUiState.Success(listOf(alert)), {}) } }
        listOf(alert.titolo, alert.descrizione).forEach { text ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("Text must wrap instead of clipping: $text", layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithText("Scade il", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun emptyFeedShowsTheExistingEmptyState() {
        compose.setContent { SportiliAppTheme { AvvisiContent(AlertsFeedUiState.Success(emptyList()), {}) } }
        compose.onNodeWithText("Nessun avviso disponibile").assertIsDisplayed()
        compose.onNodeWithText("Controlla più tardi per nuovi aggiornamenti.").assertIsDisplayed()
    }

}
