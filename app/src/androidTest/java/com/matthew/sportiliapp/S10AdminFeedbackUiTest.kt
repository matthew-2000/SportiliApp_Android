package com.matthew.sportiliapp

import android.graphics.Bitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.matthew.sportiliapp.newadmin.domain.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class S10AdminFeedbackUiTest {
    @get:Rule val compose = createComposeRule()
    private val args get() = InstrumentationRegistry.getArguments()

    private fun alerts(repository: S02FixtureRepository, back: () -> Unit = {}) {
        compose.setContent { SportiliAppTheme(isDarkTheme = args.getString("dark") == "true") {
            val vm = remember { AlertsAdminViewModel(GetAlertsUseCase(repository), AddAlertUseCase(repository),
                UpdateAlertUseCase(repository), RemoveAlertUseCase(repository)) }
            AdminAlertsScreen(back, vm)
        } }
    }
    private fun reports(repository: S02FixtureRepository, back: () -> Unit = {}) {
        compose.setContent { SportiliAppTheme(isDarkTheme = args.getString("dark") == "true") {
            val vm = remember { WorkoutReportsViewModel(GetWorkoutIssueReportsUseCase(repository),
                UpdateWorkoutIssueReportUseCase(repository), RemoveWorkoutIssueReportUseCase(repository)) }
            AdminReportsScreen(back, vm)
        } }
    }
    private fun listScroll(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(250)
        val inst = InstrumentationRegistry.getInstrumentation()
        val dir = File(inst.targetContext.getExternalFilesDir(null), "S10/${args.getString("captureMode") ?: "light"}")
        dir.mkdirs()
        val bitmap = inst.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun alertsLoadingHasProgressAndCreationEntry() {
        alerts(S02FixtureRepository().apply { observeAlerts = { flow { awaitCancellation() } } })
        compose.onNodeWithText("Caricamento avvisi…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Nuovo avviso").assertIsDisplayed()
        capture("alerts-loading")
    }
    @Test fun reportsLoadingHasProgress() {
        reports(S02FixtureRepository().apply { observeReports = { flow { awaitCancellation() } } })
        compose.onNodeWithText("Caricamento segnalazioni…").assertIsDisplayed()
        capture("reports-loading")
    }
    @Test fun emptyAlertsCanStartValidDraftWithExplicitNoDeadline() {
        alerts(S02FixtureRepository())
        compose.onNodeWithText("Nessun avviso disponibile").assertIsDisplayed()
        capture("alerts-empty")
        compose.onNodeWithContentDescription("Nuovo avviso").performClick()
        compose.onNodeWithText("Salva").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Nessuna scadenza").performScrollTo().assertIsDisplayed()
        capture("alerts-new-no-deadline")
    }
    @Test fun reportsEmptyExplainsTheList() {
        reports(S02FixtureRepository())
        compose.onNodeWithText("Nessuna segnalazione ricevuta").assertIsDisplayed()
        capture("reports-empty")
    }
    @Test fun alertsLoadErrorHidesTechnicalDetailsAndRetries() {
        var attempts = 0
        alerts(S02FixtureRepository().apply {
            alertsFlow.value = listOf(s02Alert)
            observeAlerts = { flow {
                if (++attempts == 1) error("Raw Firebase details")
                emitAll(alertsFlow)
            } }
        })
        compose.onNodeWithText("Avvisi non disponibili").assertIsDisplayed()
        compose.onNodeWithText("Raw Firebase details").assertDoesNotExist()
        capture("alerts-load-error")
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText(s02Alert.titolo).assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, attempts) }
        capture("alerts-load-retry-success")
    }
    @Test fun reportsLoadErrorRetriesIntoBothTextStatuses() {
        var attempts = 0
        reports(S02FixtureRepository().apply {
            reportsFlow.value = s10Reports
            observeReports = { flow {
                if (++attempts == 1) error("Raw Firebase details")
                emitAll(reportsFlow)
            } }
        })
        compose.onNodeWithText("Segnalazioni non disponibili").assertIsDisplayed()
        capture("reports-load-error")
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Da rivedere").assertIsDisplayed()
        capture("reports-open")
        listScroll("Risolta")
        compose.onNodeWithText("Risolta").assertIsDisplayed()
        capture("reports-resolved")
        compose.runOnIdle { assertEquals(2, attempts) }
    }
    @Test fun allPrioritiesAndActiveExpiredNoDeadlineAreVisible() {
        // Keep the active-state test independent of the audit fixture's October 2026 deadline.
        val date = java.time.LocalDate.now().plusDays(7)
        val deadline = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formatted = date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ITALIAN))
        alerts(S02FixtureRepository().apply {
            alertsFlow.value = s10Alerts.map { if (it.scadenza != null && it.scadenza > 1L) it.copy(scadenza = deadline) else it }
        })
        compose.onNodeWithText("Priorità alta").assertIsDisplayed()
        compose.onAllNodesWithText("Attivo · Scade il $formatted").onFirst().assertExists()
        capture("alerts-priority-high")
        listScroll("Priorità media")
        compose.onNodeWithText("Priorità media").assertIsDisplayed()
        capture("alerts-priority-medium")
        listScroll("Attivo · Nessuna scadenza")
        compose.onNodeWithText("Priorità bassa").assertExists()
        capture("alerts-priority-low-no-deadline")
        val expiredDate = java.time.Instant.ofEpochMilli(1L).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ITALIAN))
        listScroll("Scaduto il $expiredDate")
        compose.onNodeWithText("Senza priorità").assertIsDisplayed()
        capture("alerts-expired-no-priority")
    }
    @Test fun editedAlertPreservesFieldsDeadlineAndPriorityOnFailureAndRetry() {
        var gate = CompletableDeferred<Result<Unit>>()
        val repository = S02FixtureRepository().apply {
            alertsFlow.value = listOf(s02Alert)
            saveAlert = { gate.await() }
        }
        var backs = 0
        alerts(repository) { backs++ }
        compose.onNodeWithContentDescription("Modifica avviso").performClick()
        compose.onNodeWithText("Titolo").performTextReplacement("Titolo lungo conservato durante il salvataggio e il retry")
        compose.onNodeWithText("Descrizione").performTextReplacement("Descrizione conservata nella stessa bozza")
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.onNodeWithText("Salva").assertIsNotEnabled()
        compose.onNodeWithText("Annulla").assertIsNotEnabled()
        compose.onNodeWithText("Salvataggio in corso…").assertIsDisplayed()
        capture("alerts-save-pending")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK); InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("Salva").assertExists().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, repository.alertWrites); assertEquals(0, backs)
            gate.complete(Result.failure(IllegalStateException("Raw write error"))) }
        compose.onNodeWithText("La bozza è conservata. Premi Salva per riprovare.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Raw write error").assertDoesNotExist()
        capture("alerts-save-error-draft")
        compose.runOnIdle { gate = CompletableDeferred() }
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.runOnIdle { gate.complete(Result.success(Unit)) }
        compose.onNodeWithText("Salva").assertDoesNotExist()
        compose.runOnIdle {
            val saved = repository.alertsFlow.value.single()
            assertEquals(s02Alert.id, saved.id)
            assertEquals(s02Alert.scadenza, saved.scadenza)
            assertEquals(s02Alert.urgenza, saved.urgenza)
            assertEquals("Descrizione conservata nella stessa bozza", saved.descrizione)
            assertEquals(2, repository.alertWrites)
        }
        capture("alerts-save-success")
    }
    @Test fun reportToggleIsProtectedAndFailureRetriesSameReport() {
        var gate = CompletableDeferred<Result<Unit>>()
        val repository = S02FixtureRepository().apply {
            reportsFlow.value = listOf(s10Reports.first())
            saveReport = { gate.await() }
        }
        var backs = 0
        reports(repository) { backs++ }
        listScroll("Segna come risolta")
        compose.onNodeWithText("Segna come risolta").performClick().assertIsNotEnabled()
        compose.onNodeWithText("Elimina").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Indietro").assertIsNotEnabled()
        capture("reports-update-pending")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.runOnIdle { assertEquals(0, backs); assertEquals(1, repository.reportWrites)
            gate.complete(Result.failure(IllegalStateException("Raw write error"))) }
        compose.onNodeWithText("Errore durante l'aggiornamento della segnalazione. Riprova.").assertIsDisplayed()
        compose.onNodeWithText("Da rivedere").assertExists()
        capture("reports-update-error")
        compose.runOnIdle { gate = CompletableDeferred() }
        compose.onNodeWithText("Riprova").performClick()
        compose.runOnIdle { gate.complete(Result.success(Unit)) }
        compose.onNodeWithText("Risolta").assertExists()
        compose.runOnIdle { assertEquals(s10Reports.first().copy(resolved = true), repository.reportsFlow.value.single()) }
        capture("reports-update-success")
        listScroll("Segna come da rivedere")
        compose.onNodeWithText("Segna come da rivedere").performClick()
        compose.onNodeWithText("Da rivedere").assertExists()
        compose.runOnIdle { assertEquals(3, repository.reportWrites) }
        capture("reports-reopened")
    }
    @Test fun reportDeletionRetryReopensConfirmationAndCanBeCancelled() {
        var gate = CompletableDeferred<Result<Unit>>()
        val repository = S02FixtureRepository().apply {
            reportsFlow.value = listOf(s02Report)
            deleteReport = { gate.await() }
        }
        reports(repository)
        compose.onNodeWithText("Elimina").performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertEquals(0, repository.reportRemovals) }
        compose.onNodeWithText("Elimina").performClick()
        capture("reports-delete-confirm")
        compose.onAllNodesWithText("Elimina").onLast().performClick()
        compose.runOnIdle { gate.complete(Result.failure(IllegalStateException("Raw delete error"))) }
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Elimina segnalazione").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repository.reportRemovals); gate = CompletableDeferred() }
        compose.onAllNodesWithText("Elimina").onLast().performClick()
        compose.runOnIdle { gate.complete(Result.success(Unit)) }
        compose.onNodeWithText("Nessuna segnalazione ricevuta").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, repository.reportRemovals) }
        capture("reports-delete-success")
    }
    @Test fun alertDeletionProtectsListAndRetriesThroughConfirmation() {
        var gate = CompletableDeferred<Result<Unit>>()
        val repository = S02FixtureRepository().apply {
            alertsFlow.value = listOf(s02Alert)
            deleteAlert = { gate.await() }
        }
        alerts(repository)
        compose.onNodeWithContentDescription("Elimina avviso").performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.runOnIdle { assertEquals(0, repository.alertRemovals) }
        compose.onNodeWithContentDescription("Elimina avviso").performClick()
        compose.onAllNodesWithText("Elimina").onLast().performClick()
        compose.onNodeWithContentDescription("Modifica avviso").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Indietro").assertIsNotEnabled()
        capture("alerts-delete-pending")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(s02Alert.titolo).assertExists()
        compose.runOnIdle { gate.complete(Result.failure(IllegalStateException("Raw delete error"))) }
        capture("alerts-delete-error")
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Elimina avviso").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repository.alertRemovals); gate = CompletableDeferred() }
        compose.onAllNodesWithText("Elimina").onLast().performClick()
        compose.runOnIdle { gate.complete(Result.success(Unit)) }
        compose.onNodeWithText("Nessun avviso disponibile").assertIsDisplayed()
        capture("alerts-delete-success")
    }
    @Test fun editorRemovesDeadlineAndChangesPriorityBeforeSave() {
        val repository = S02FixtureRepository().apply { alertsFlow.value = listOf(s02Alert) }
        alerts(repository)
        compose.onNodeWithContentDescription("Modifica avviso").performClick()
        compose.onNodeWithText("Rimuovi scadenza").performScrollTo().performClick()
        compose.onNodeWithText("Nessuna scadenza").assertIsDisplayed()
        compose.onNodeWithText("Priorità").performScrollTo().performClick()
        compose.onNodeWithText("Alta").performClick()
        capture("alerts-editor-priority-no-deadline")
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.onNodeWithText("Salva").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(repository.alertsFlow.value.single().scadenza)
            assertEquals("alta", repository.alertsFlow.value.single().urgenza)
        }
    }
}
