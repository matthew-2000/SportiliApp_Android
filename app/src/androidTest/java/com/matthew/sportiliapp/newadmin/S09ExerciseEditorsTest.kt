package com.matthew.sportiliapp.newadmin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.s09Catalog
import com.matthew.sportiliapp.s09Exercise
import com.matthew.sportiliapp.s09Group
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S09ExerciseEditorsTest {
    @get:Rule val compose = createComposeRule()
    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))
    private fun edit(original: Esercizio = s09Exercise(), onDismiss: () -> Unit = {}, onSave: (Esercizio) -> Unit) {
        compose.setContent { SportiliAppTheme { EsercizioDialog(original, onDismiss, onSave, s09Catalog().flatMap { it.esercizi }) } }
    }
    @Test fun trainerEditPreservesAllPartsAndPersonalData() {
        val original = s09Exercise(); var saved: Esercizio? = null
        edit(onSave = { saved = it })
        field("Istruzioni del trainer").performScrollTo().performTextReplacement("Nuove istruzioni")
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle {
            assertEquals(original.name, saved!!.name)
            assertEquals(original.serie, saved!!.serie)
            assertEquals(original.riposo, saved!!.riposo)
            assertEquals("Nuove istruzioni", saved!!.notePT)
            assertEquals(original.noteUtente, saved!!.noteUtente)
            assertEquals(original.weightLogs, saved!!.weightLogs)
            assertEquals(original.ordine, saved!!.ordine)
            assertEquals(original.priorita, saved!!.priorita)
        }
    }
    @Test fun nullTrainerAndUnusualPrescriptionAndRecoverySurviveNameEdit() {
        var saved: Esercizio? = null
        val original = Esercizio("Rematore", "AMRAP 45 secondi", riposo = "90 secondi", notePT = null)
        edit(original, onSave = { saved = it })
        field("Nome Esercizio").performTextReplacement("Rematore manubrio")
        compose.onNodeWithText("Salva", false).assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { assertEquals(original.serie, saved!!.serie); assertEquals(original.riposo, saved!!.riposo); assertNull(saved!!.notePT) }
    }
    @Test fun trainerCanBeInsertedAndCleared() {
        var saved: Esercizio? = null
        edit(Esercizio("Panca", "3 x 10", notePT = "Prima"), onSave = { saved = it })
        field("Istruzioni del trainer").performScrollTo().performTextClearance()
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals("", saved!!.notePT) }
    }
    @Test fun newExerciseValidationAndDefaultSyntax() {
        var saved: Esercizio? = null
        edit(Esercizio("", ""), onSave = { saved = it })
        compose.onNodeWithText("Salva", false).performClick()
        compose.onNodeWithText("Inserisci il nome dell'esercizio").assertExists()
        compose.runOnIdle { assertNull(saved) }
        field("Nome Esercizio").performTextReplacement("Affondi")
        field("Istruzioni del trainer").performScrollTo().performTextReplacement("Passo lungo")
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals("3 x 10", saved!!.serie); assertEquals("Passo lungo", saved!!.notePT) }
    }
    @Test fun extraPickerReturnsToSameDraftAndPreservesPrescription() {
        var saved: Esercizio? = null
        edit(onSave = { saved = it })
        compose.onNodeWithText("Scegli parte 2 dal catalogo").performScrollTo().performClick()
        field("Cerca nel catalogo").performTextReplacement("squat")
        compose.onNodeWithText("Squat", false).performClick()
        field("Prescrizione parte 2").performScrollTo().assertTextContains("12")
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals("Panca piana + Squat + Push up con controllo della fase eccentrica", saved!!.name); assertEquals("4x8 + 12 + 30 secondi", saved!!.serie) }
    }
    @Test fun extraAdditionValidatesAndUsesExistingSerialization() {
        var saved: Esercizio? = null
        edit(Esercizio("Panca", "3x10"), onSave = { saved = it })
        compose.onNodeWithText("Aggiungi esercizio extra").performScrollTo().performClick()
        compose.onNodeWithText("Salva", false).performClick()
        field("Nome parte 2").performScrollTo().performTextReplacement("Croci")
        field("Prescrizione parte 2").performTextReplacement("15")
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals("Panca + Croci", saved!!.name); assertEquals("3x10 + 15", saved!!.serie) }
    }
    @Test fun extraRemovalKeepsNamePrescriptionAssociation() {
        var saved: Esercizio? = null
        edit(onSave = { saved = it })
        compose.onNodeWithContentDescription("Rimuovi parte 2").performScrollTo().performClick()
        compose.onNodeWithText("Rimuovi parte", false).performClick()
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals("Panca piana + Push up con controllo della fase eccentrica", saved!!.name); assertEquals("4x8 + 30 secondi", saved!!.serie) }
    }
    @Test fun cancelOffersContinueAndDiscardWithoutConfirming() {
        var closed = 0; var saves = 0
        edit(onDismiss = { closed++ }, onSave = { saves++ })
        field("Nome Esercizio").performTextReplacement("Bozza")
        compose.onNodeWithText("Annulla", false).performClick()
        compose.onNodeWithText("Continua modifica").performClick()
        field("Nome Esercizio").assertTextContains("Bozza")
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Scarta modifiche").performClick()
        compose.runOnIdle { assertEquals(1, closed); assertEquals(0, saves) }
    }
    @Test fun circuitSearchClearAndDuplicatesRemainAvailable() {
        compose.setContent { SportiliAppTheme { EditMuscleGroupContent("SPT", "giorno1", s09Group(), s09Catalog(), onSave = {}, onCancel = {}) } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasSetTextAction() and hasText("Cerca esercizio..."))
        field("Cerca esercizio...").performTextReplacement("inesistente")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Nessun esercizio trovato"))
        compose.onNodeWithText("Nessun esercizio trovato").assertExists()
        compose.onNodeWithText("Cancella", false).performScrollTo().performClick()
        field("Cerca esercizio...").performTextReplacement("panca")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Già presente · Aggiungi ancora"))
        compose.onNodeWithText("Già presente · Aggiungi ancora").assertExists()
        compose.onNodeWithText("Panca piana", false).performScrollTo().performClick()
        field("Nome Esercizio").assertTextContains("Panca piana")
    }
    @Test fun selectedReorderRemovalAndSaveKeepPersonalFieldsAndKeys() {
        var saved: GruppoMuscolare? = null
        compose.setContent { SportiliAppTheme { EditMuscleGroupContent("SPT", "giorno1", s09Group(), s09Catalog(), onSave = { saved = it }, onCancel = {}) } }
        compose.onAllNodesWithText("Modifica", false).onFirst().performClick()
        field("Istruzioni del trainer").performScrollTo().performTextReplacement("Istruzioni modificate")
        compose.onNode(hasText("Salva") and hasAnyAncestor(isDialog())).performClick()
        val name = s09Exercise().name
        compose.onNodeWithContentDescription("Sposta $name prima").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Sposta $name dopo").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Rimuovi Squat dalla bozza").performScrollTo().performClick()
        compose.onNodeWithText("Rimuovi dalla bozza").performClick()
        compose.onNodeWithText("Salva", false).performClick()
        compose.runOnIdle { assertEquals(listOf("esercizio1"), saved!!.esercizi.keys.toList()); assertEquals(0, saved!!.esercizi.values.first().ordine); assertEquals(s09Exercise().weightLogs, saved!!.esercizi.values.first().weightLogs); assertEquals("Istruzioni modificate", saved!!.esercizi.values.first().notePT) }
    }
    @Test fun groupDraftDiscardDoesNotSaveAndPendingConsumesBack() {
        var saves = 0; var cancels = 0
        var pending by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { SportiliAppTheme { EditMuscleGroupContent("SPT", "giorno1", s09Group(), s09Catalog(),
            isSaving = pending, onSave = { saves++ }, onCancel = { cancels++ }) } }
        compose.runOnIdle { pending = true }
        compose.onNodeWithContentDescription("Indietro").assertIsNotEnabled()
        androidx.test.espresso.Espresso.pressBack()
        compose.runOnIdle { assertEquals(0, cancels); pending = false }
        compose.onNodeWithContentDescription("Sposta ${s09Exercise().name} dopo").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Scarta", false).performClick()
        compose.runOnIdle { assertEquals(1, cancels); assertEquals(0, saves) }
    }

}
