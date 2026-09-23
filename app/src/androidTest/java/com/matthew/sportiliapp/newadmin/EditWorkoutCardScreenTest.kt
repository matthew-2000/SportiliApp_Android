package com.matthew.sportiliapp.newadmin

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.newadmin.ui.screens.EditWorkoutCardScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises production UI callbacks using in-memory data; never connects to Firebase. */
@RunWith(AndroidJUnit4::class)
class EditWorkoutCardScreenTest {
    @get:Rule val compose = createComposeRule()
    private val completeLabel = "Salva e segna il cambio come completato"
    private val original = Scheda(
        "2026-09-01T00:00:00+0000", 4,
        linkedMapOf("giorno3" to Giorno("Giorno di test")), cambioRichiesto = true
    )
    private var saved: Scheda? = null
    private var selected: Triple<String, Giorno, Scheda>? = null

    private fun show(scheda: Scheda = original, isSaving: Boolean = false, errorMessage: String? = null) {
        compose.setContent {
            MaterialTheme {
                EditWorkoutCardScreen(
                    scheda = scheda, isSaving = isSaving, errorMessage = errorMessage,
                    onSave = { saved = it }, onCancel = {},
                    onDaySelected = { key, day, card -> selected = Triple(key, day, card) }
                )
            }
        }
    }

    @Test fun conflictIsVisibleAndEditorCanBeClosed() {
        val message = "La scheda è cambiata durante la modifica. Riaprila e applica nuovamente le modifiche. Nessun dato è stato sovrascritto."
        show(errorMessage = message)
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Annulla", substring = false).assertIsEnabled()
        compose.runOnIdle { assertNull(saved) }
    }

    @Test fun ordinarySavePreservesPendingRequestAndDayKeys() {
        show()
        compose.onNodeWithText("Durata (settimane)").performTextReplacement("6")
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.runOnIdle {
            assertEquals(6, saved!!.durata)
            assertTrue(saved!!.cambioRichiesto)
            assertEquals(setOf("giorno3"), saved!!.giorni.keys)
            assertTrue(original.cambioRichiesto)
        }
    }

    @Test fun explicitCompletionClearsOnlyTheSavedRequest() {
        show()
        compose.onNodeWithText(completeLabel).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertFalse(saved!!.cambioRichiesto)
            assertEquals(original.giorni, saved!!.giorni)
            assertTrue(original.cambioRichiesto)
        }
    }

    @Test fun openingADayKeepsRequestAndOriginalKey() {
        show()
        compose.onNodeWithText("Giorno di test").performClick()
        compose.runOnIdle {
            assertEquals("giorno3", selected!!.first)
            assertTrue(selected!!.third.cambioRichiesto)
            assertNull(saved)
        }
    }

    @Test fun invalidDurationCannotCompleteRequest() {
        show()
        compose.onNodeWithText("Durata (settimane)").performTextReplacement("0")
        compose.onNodeWithText(completeLabel).performClick()
        compose.onNodeWithText("La durata deve essere tra 1 e 52 settimane").assertIsDisplayed()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("Durata (settimane)").performTextReplacement("4")
        compose.onNodeWithText(completeLabel).performClick()
        compose.runOnIdle { assertFalse(saved!!.cambioRichiesto) }
    }

    @Test fun savingDisablesBothSaveActions() {
        show(isSaving = true)
        compose.onNodeWithText("Salva", substring = false).assertIsNotEnabled()
        compose.onNodeWithText(completeLabel).assertIsNotEnabled()
    }

    @Test fun noPendingRequestMeansNoCompletionAction() {
        show(scheda = original.copy(cambioRichiesto = false))
        compose.onNodeWithText(completeLabel).assertDoesNotExist()
        compose.onNodeWithText("Salva", substring = false).performClick()
        compose.runOnIdle { assertFalse(saved!!.cambioRichiesto) }
    }
}
