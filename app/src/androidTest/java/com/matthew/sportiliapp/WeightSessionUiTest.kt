package com.matthew.sportiliapp

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.scheda.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeightSessionUiTest {
    @get:Rule val compose = createAndroidComposeRule<WeightSessionTestActivity>()
    private val reps = listOf(12, 10, 8, 6)
    private val previous = WorkoutWeightSession("previous", 1790000000000,
        reps.mapIndexed { index, rep -> WorkoutWeightSet(rep, 30.0 + index * 5) })
    private val writes = mutableListOf<WorkoutWeightSession>()
    private var success: (() -> Unit)? = null
    private var failure: ((String) -> Unit)? = null

    private fun editor(session: WorkoutWeightSession? = null, sessions: List<WorkoutWeightSession> = listOf(previous)) {
        compose.setContent {
            SportiliAppTheme {
                var open by remember { mutableStateOf(true) }
                if (open) WeightSessionEditor(
                    remember { WeightSessionEditorContext("panca", "Panca", reps, session) }, sessions,
                    onDismiss = { open = false },
                    onSave = { value, ok, error -> writes += value; success = ok; failure = error }
                ) else Text("Editor chiuso")
            }
        }
    }
    private fun reuse() { compose.onNodeWithText("Usa gli ultimi pesi").performScrollTo().performClick() }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.cacheDir, "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun reuseDoesNotSaveAndFailureRetryPreserveDraftIdentityAndDate() {
        editor(sessions = listOf(previous, previous.copy(id = "different", timestamp = previous.timestamp + 1,
            sets = listOf(WorkoutWeightSet(10, 99.0)))))
        reuse()
        assertTrue(writes.isEmpty())
        screenshot("weight-series-editor")
        compose.onNodeWithContentDescription("Peso (kg) serie 1").assertTextContains("30.0")
        compose.onNodeWithContentDescription("Peso (kg) serie 4").performScrollTo().assertTextContains("45.0")
        compose.onNodeWithText("Salva allenamento").performClick()
        compose.onNodeWithText("Salva allenamento").assertIsNotEnabled()
        compose.onNodeWithText("Annulla").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, writes.size); failure!!("Errore locale: riprova") }
        compose.onNodeWithText("Errore locale: riprova").assertExists()
        compose.onNodeWithText("Salva allenamento").performClick()
        compose.runOnIdle {
            assertEquals(2, writes.size)
            assertEquals(writes[0], writes[1])
            assertEquals(previous.sets, writes.last().sets)
            success!!()
        }
        compose.onNodeWithText("Editor chiuso").assertExists()
    }

    @Test fun invalidInputAndDiscardDoNotWrite() {
        editor()
        compose.onNodeWithText("Salva allenamento").performClick()
        compose.onNodeWithText("Controlla ripetizioni e pesi di tutte le serie.").assertExists()
        reuse()
        compose.onNodeWithContentDescription("Peso (kg) serie 1").performScrollTo().performTextReplacement("NaN")
        compose.onNodeWithText("Salva allenamento").performClick()
        assertTrue(writes.isEmpty())
        compose.onNodeWithText("Annulla").performClick()
        compose.onNodeWithText("Scartare i pesi inseriti?").assertExists()
        compose.onNodeWithText("Continua a modificare").performClick()
        compose.onNodeWithText("Annulla").performClick()
        compose.onNodeWithText("Scarta modifiche").performClick()
        compose.onNodeWithText("Editor chiuso").assertExists()
        assertTrue(writes.isEmpty())
    }

    @Test fun editingKeepsSessionIdAndOriginalTimestamp() {
        editor(session = previous)
        compose.onNodeWithContentDescription("Peso (kg) serie 1").performScrollTo().performTextReplacement("32,5")
        compose.onNodeWithText("Salva allenamento").performClick()
        compose.runOnIdle {
            assertEquals(previous.id, writes.single().id)
            assertEquals(previous.timestamp, writes.single().timestamp)
            assertEquals(32.5, writes.single().sets.first().weight, 0.0)
            assertEquals(previous.sets.drop(1), writes.single().sets.drop(1))
        }
    }

    @Test fun noAutomaticReuseWhenRepetitionsDiffer() {
        editor(sessions = listOf(previous.copy(sets = listOf(WorkoutWeightSet(10, 80.0)))))
        compose.onNodeWithText("Usa gli ultimi pesi").assertDoesNotExist()
        compose.onNodeWithText("Salva allenamento").performClick()
        assertTrue(writes.isEmpty())
    }

    @Test fun historyDeletionRequiresConfirmationAndSupportsRetry() {
        var calls = 0
        compose.setContent {
            SportiliAppTheme {
                WeightSessionHistoryCard(previous, null, onEdit = {}, onDelete = { _, fail ->
                    calls++; fail("Eliminazione non riuscita")
                })
            }
        }
        compose.onNodeWithText("Dettagli serie").performClick()
        screenshot("weight-series-history")
        compose.onNodeWithText("Elimina allenamento").performClick()
        assertEquals(0, calls)
        compose.onNodeWithText("Annulla").performClick()
        assertEquals(0, calls)
        compose.onNodeWithText("Elimina allenamento").performClick()
        compose.onAllNodesWithText("Elimina allenamento")[1].performClick()
        compose.onNodeWithText("Eliminazione non riuscita").assertExists()
        assertEquals(1, calls)
    }
}
