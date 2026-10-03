package com.matthew.sportiliapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S05UxTest {
    @get:Rule val compose = createAndroidComposeRule<S05PreviewActivity>()
    private fun configure(count: Int = 12, exercise: String = "single", noRest: Boolean = false, emptyNote: Boolean = false) {
        compose.activityRule.scenario.onActivity {
            it.intent.putExtra("count", count).putExtra("exercise", exercise)
                .putExtra("noRest", noRest).putExtra("emptyNote", emptyNote)
        }
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
    }
    private fun scrollTo(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun weight() { scrollTo("Registra peso"); compose.onNodeWithText("Registra peso").performClick() }
    private fun inputWeight(value: String) { compose.onNode(hasSetTextAction().and(hasText("Peso (kg)"))).performTextReplacement(value) }
    private fun notes() {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Note personali. Apri editor"))
        compose.onNodeWithContentDescription("Note personali. Apri editor").performScrollTo().performClick()
    }
    private fun noteField() = compose.onNode(hasSetTextAction().and(hasText("Nota personale")))
    private fun outcome(value: String) { compose.runOnUiThread { compose.activity.writeOutcome = value } }

    @Test fun weightValidationFailureAndRetryKeepDraftAndAcceptBothSeparators() {
        configure(0); weight(); inputWeight("NaN"); compose.onNodeWithText("Salva").performClick()
        compose.onNodeWithText("Inserisci un peso maggiore di zero.").assertExists()
        compose.runOnUiThread { assertTrue(compose.activity.writeKeys.isEmpty()) }
        inputWeight("47,5"); outcome("failure"); compose.onNodeWithText("Salva").performClick()
        compose.onNodeWithText("Salvataggio non riuscito. Riprova.").assertExists()
        compose.onNodeWithText("47,5").assertExists()
        outcome("success"); compose.onNodeWithText("Salva").performClick()
        compose.waitForIdle()
        scrollTo("Registra peso"); compose.onAllNodesWithText("Ultimo peso: 47.5 kg")[0].assertExists()
        weight(); inputWeight("50.25"); compose.onNodeWithText("Salva").performClick()
        compose.runOnUiThread { assertEquals(50.25, compose.activity.fixtureModel.userExerciseData.value!!["plank"]!!.weightLogs!!["new-log"]!!.weight!!, 0.0) }
    }
    @Test fun pendingWeightBlocksEditingCancelBackAndDuplicateWrites() {
        weight(); inputWeight("47,5"); outcome("pending"); compose.onNodeWithText("Salva").performClick()
        compose.onNodeWithText("Salvataggio peso…").assertExists()
        compose.onNodeWithText("Annulla").assertIsNotEnabled()
        compose.onNodeWithText("Salva").assertIsNotEnabled()
        compose.onNodeWithText("Peso (kg)").assertIsNotEnabled()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK); compose.waitForIdle(); compose.onNodeWithText("Salvataggio peso…").assertExists()
        compose.runOnUiThread { assertEquals(1, compose.activity.writeKeys.size); compose.activity.failedPending!!() }
        compose.onNodeWithText("47,5").assertExists()
        outcome("success"); compose.onNodeWithText("Salva").performClick()
        compose.onNodeWithText("Salvataggio peso…").assertDoesNotExist()
    }
    @Test fun editExistingWeightKeepsItsId() {
        configure(1); scrollTo("Modifica"); compose.onNodeWithText("Modifica").performClick()
        compose.onNodeWithText("Modifica peso").assertExists(); inputWeight("22,5")
        compose.onNodeWithText("Data della registrazione").assertExists()
        compose.onNodeWithText("Salva").performClick()
        compose.runOnUiThread {
            val logs = compose.activity.fixtureModel.userExerciseData.value!!["plank"]!!.weightLogs!!
            assertEquals(setOf("log-0"), logs.keys); assertEquals(22.5, logs["log-0"]!!.weight!!, 0.0)
        }
    }
    @Test fun longNoteExpandsAndCloseRequiresExplicitDiscard() {
        scrollTo("Mostra tutta la nota"); compose.onNodeWithText("Mostra tutta la nota").performClick()
        compose.onNodeWithText("Riduci nota").assertExists(); notes()
        noteField().performTextReplacement("bozza personale")
        compose.onNodeWithText("Chiudi").performClick()
        compose.onNodeWithText("Scartare le modifiche alla nota?").assertExists()
        compose.onNodeWithText("Continua a modificare").performClick()
        compose.onNodeWithText("bozza personale").assertExists()
        compose.onNodeWithText("Chiudi").performClick(); compose.onNodeWithText("Scarta modifiche").performClick()
        compose.onNodeWithText("bozza personale").assertDoesNotExist()
        compose.runOnUiThread { assertEquals(s05LongNote, compose.activity.fixtureModel.userExerciseData.value!!["plank"]!!.noteUtente); assertTrue(compose.activity.writeKeys.isEmpty()) }
    }
    @Test fun cleanNoteAfterSaveAcceptsNextRemoteSnapshot() {
        notes(); noteField().performTextReplacement("nota salvata")
        compose.onNodeWithText("Salva").performClick()
        compose.runOnUiThread {
            val model = compose.activity.fixtureModel
            val data = model.userExerciseData.value!!.toMutableMap()
            data["plank"] = data.getValue("plank").copy(noteUtente = "nota remota successiva")
            (model.userExerciseData as androidx.lifecycle.MutableLiveData).value = data
        }
        notes(); noteField().assertTextContains("nota remota successiva")
        compose.onNodeWithText("Salva").assertIsNotEnabled()
    }
    @Test fun remoteNoteDeliveryKeepsDraftAndBackRequiresDiscard() {
        notes(); noteField().performTextReplacement("bozza Indietro")
        compose.runOnUiThread {
            val model = compose.activity.fixtureModel
            val data = model.userExerciseData.value!!.toMutableMap()
            data["plank"] = data.getValue("plank").copy(noteUtente = "nota remota")
            (model.userExerciseData as androidx.lifecycle.MutableLiveData).value = data
        }
        compose.onNodeWithText("bozza Indietro").assertExists()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Scartare le modifiche alla nota?").fetchSemanticsNodes().isEmpty()) {
            noteField().assertTextContains("bozza Indietro")
            val descriptor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent 4")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Scartare le modifiche alla nota?").assertExists()
        compose.onNodeWithText("Continua a modificare").performClick()
        compose.onNodeWithText("bozza Indietro").assertExists()
        compose.onNodeWithText("Chiudi").performClick(); compose.onNodeWithText("Scarta modifiche").performClick()
    }
    @Test fun notePendingFailureRetryAndEmptyRemovalPreserveHistory() {
        notes(); noteField().performTextReplacement("bozza da salvare"); outcome("pending")
        compose.onNodeWithText("Salva").performClick()
        compose.onNodeWithText("Salvataggio nota…").assertExists()
        compose.onNodeWithText("Chiudi").assertIsNotEnabled(); compose.onNodeWithText("Nota personale").assertIsNotEnabled()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK); compose.waitForIdle(); compose.onNodeWithText("Salvataggio nota…").assertExists()
        compose.runOnUiThread { compose.activity.failedPending!!() }
        compose.onNodeWithText("Salvataggio non riuscito. Riprova.").assertExists()
        compose.onNodeWithText("bozza da salvare").assertExists()
        outcome("success"); compose.onNodeWithText("Salva").performClick(); notes()
        noteField().performTextReplacement("   "); compose.onNodeWithText("Salva").performClick()
        compose.runOnUiThread {
            val data = compose.activity.fixtureModel.userExerciseData.value!!["plank"]!!
            assertNull(data.noteUtente); assertEquals(12, data.weightLogs!!.size)
        }
    }
    @Test fun historyZeroOneTenAndMoreHasSampleAxisAndAccessibleDatedValues() {
        configure(0); scrollTo("Nessun peso registrato"); compose.onNodeWithText("Nessun peso registrato").assertExists()
        for (count in listOf(1, 10, 12)) {
            configure(count); scrollTo("Ultime 10 registrazioni")
            scrollTo("Valori e date dei campioni"); compose.onNodeWithText("Valori e date dei campioni").performClick()
            val expected = if (count > 10) "Campione 1: 25 kg" else "Campione 1: 20 kg"
            scrollTo(expected); compose.onNodeWithText(expected).assertExists()
            val last = "Campione ${minOf(count, 10)}: ${if (count == 1) "20" else if (count == 10) "42.5" else "47.5"} kg"
            scrollTo(last); compose.onNodeWithText(last).assertExists()
        }
    }
    @Test fun timerKeepsConfiguredDurationAndMissingRestPresets() {
        compose.onNodeWithText("Apri timer di recupero").performClick()
        compose.onNodeWithText("Recupero impostato: 01:30").assertExists()
        configure(noRest = true); compose.onNodeWithText("Apri timer di recupero").performClick()
        compose.onNodeWithText("Recupero non configurato per questo esercizio.").assertExists()
        compose.onNodeWithText("60 sec").performClick(); compose.onNodeWithText("01:00").assertExists()
    }
    @Test fun thirdPartWeightAndNoteUseLegacyKeyAndPreserveOtherParts() {
        configure(exercise = "triple"); scrollTo("Élite"); compose.onNodeWithText("Élite").performClick()
        weight(); inputWeight("35,5"); compose.onNodeWithText("Salva").performClick()
        notes(); noteField().performTextReplacement("nuova nota terza parte"); compose.onNodeWithText("Salva").performClick()
        compose.runOnUiThread {
            assertEquals(listOf("lite", "lite"), compose.activity.writeKeys)
            assertEquals(s05LongNote, compose.activity.fixtureModel.userExerciseData.value!!["plank"]!!.noteUtente)
            assertEquals("nuova nota terza parte", compose.activity.fixtureModel.userExerciseData.value!!["lite"]!!.noteUtente)
        }
    }
}
