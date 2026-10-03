package com.matthew.sportiliapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.newadmin.domain.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminViewModel
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S02UxTest {
    @get:Rule val compose = createComposeRule()

    @Test fun schedaRemainsSelectedInDetailsAndTabRestoreAndBackWork() {
        compose.setContent { SportiliAppTheme { S02NavigationFixture() } }
        fun tab(text: String) = compose.onNode(hasText(text) and hasClickAction() and isSelectable())
        tab("Scheda").assertIsSelected()
        compose.onNodeWithText("Giorno A · Spinta").performClick()
        tab("Scheda").assertIsSelected()
        compose.onNodeWithText("Panca piana").performClick()
        tab("Scheda").assertIsSelected()
        tab("Avvisi").performClick().assertIsSelected()
        tab("Scheda").performClick().assertIsSelected()
        compose.onNodeWithText("Panca piana").assertExists()
        compose.onNodeWithText("Indietro").performClick()
        compose.onNodeWithText("Giorno A · Spinta").assertExists()
        tab("Scheda").assertIsSelected()
        compose.onNodeWithText("Indietro").performClick()
        compose.onNodeWithText("La tua scheda").assertExists()
        tab("Impostazioni").performClick().assertIsSelected()
    }

    @Test fun circuitSearchUpdatesVisibleGroupsAndCanBeCleared() {
        compose.setContent {
            SportiliAppTheme {
                EditMuscleGroupContent("FIXTURE", "a", GruppoMuscolare(nome = "Circuito"),
                    s02Catalog, onSave = {}, onCancel = {})
            }
        }
        val search = compose.onNodeWithText("Cerca esercizio...")
        compose.onNodeWithText("Addominali").assertExists()
        search.performTextReplacement("pAnC")
        compose.onNodeWithText("Addominali").assertDoesNotExist()
        compose.onNodeWithText("Pettorali").assertExists()
        compose.onNodeWithText("Panca piana").assertExists()
        compose.onNodeWithText("Panca inclinata").assertExists()
        compose.onNodeWithText("Croci ai cavi").assertDoesNotExist()
        search.performTextReplacement("inesistente")
        compose.onNodeWithText("Nessun esercizio trovato").assertIsDisplayed()
        compose.onNodeWithText("Pettorali").assertDoesNotExist()
        search.performTextReplacement("")
        compose.onNodeWithText("Addominali").assertExists()
    }

    @Test fun newAlertWaitsRetainsDraftOnFailureThenClosesOnSuccessfulRetry() = checkAlertEditor(false)
    @Test fun editedAlertWaitsRetainsDraftOnFailureThenClosesOnSuccessfulRetry() = checkAlertEditor(true)

    private fun checkAlertEditor(editing: Boolean) {
        var gate = CompletableDeferred<Result<Unit>>()
        val repository = S02FixtureRepository().apply {
            if (editing) alertsFlow.value = listOf(s02Alert)
            saveAlert = { gate.await() }
        }
        compose.setContent {
            SportiliAppTheme {
                val vm = remember { AlertsAdminViewModel(GetAlertsUseCase(repository),
                    AddAlertUseCase(repository), UpdateAlertUseCase(repository), RemoveAlertUseCase(repository)) }
                AdminAlertsScreen(onBack = {}, viewModel = vm)
            }
        }
        compose.onNodeWithContentDescription(if (editing) "Modifica avviso" else "Nuovo avviso").performClick()
        compose.onNodeWithText("Titolo").performTextReplacement("Bozza conservata")
        compose.onNodeWithText("Descrizione").performTextReplacement("Descrizione conservata")
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.onNodeWithText("Salva").assertIsNotEnabled()
        compose.onNodeWithText("Annulla").assertIsNotEnabled()
        compose.onNodeWithText("Salvataggio in corso…").assertIsDisplayed()
        Espresso.pressBack() // Hide keyboard or attempt to dismiss the sheet.
        Espresso.pressBack() // Pending writes must keep the sheet open.
        compose.onNodeWithText("Salva").assertExists().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, repository.alertWrites) }
        compose.runOnIdle { gate.complete(Result.failure(IllegalStateException("Errore fixture, riprova"))) }
        compose.onNodeWithText("Errore fixture, riprova").performScrollTo()
        capture("alert-${if (editing) "edit" else "new"}-error")
        compose.onNodeWithText("Errore fixture, riprova").assertIsDisplayed()
        compose.onNodeWithText("Bozza conservata").assertExists()
        compose.onNodeWithText("Descrizione conservata").assertExists()
        compose.onNodeWithText("Salva").assertIsEnabled()
        compose.runOnIdle { gate = CompletableDeferred() }
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.onNodeWithText("Salva").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(2, repository.alertWrites); gate.complete(Result.success(Unit)) }
        compose.onNodeWithText("Salva").assertDoesNotExist()
        compose.onNodeWithText("Bozza conservata").assertExists()
        compose.runOnIdle {
            val saved = repository.alertsFlow.value.single()
            assertEquals("Descrizione conservata", saved.descrizione)
            if (editing) {
                assertEquals(s02Alert.id, saved.id)
                assertEquals(s02Alert.scadenza, saved.scadenza)
                assertEquals(s02Alert.urgenza, saved.urgenza)
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(400) // Allow the native dialog window animation to settle for the screenshot.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "S02")
        directory.mkdirs()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "$name.png").outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
    }

    @Test fun reportActionsRemainVisibleAtLargeFontAndNarrowWidth() {
        var toggles = 0
        var deletes = 0
        compose.setContent {
            SportiliAppTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    Box(Modifier.width(320.dp)) {
                        ReportCard(s02Report, onToggleResolved = { toggles++ }, onRemove = { deletes++ })
                    }
                }
            }
        }
        val resolve = compose.onNodeWithText("Segna come risolta")
        val delete = compose.onNodeWithText("Elimina")
        resolve.assertIsDisplayed().performClick()
        delete.assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, toggles)
            assertEquals(1, deletes)
        }
        val resolveBounds = resolve.fetchSemanticsNode().boundsInRoot
        val deleteBounds = delete.fetchSemanticsNode().boundsInRoot
        assertTrue("Delete wraps below resolve", deleteBounds.top >= resolveBounds.bottom)
        assertTrue("Delete label is not a tall column", deleteBounds.width > deleteBounds.height)
    }
}
