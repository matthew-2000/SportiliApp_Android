package com.matthew.sportiliapp.newadmin

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.WeightLogEntry
import com.matthew.sportiliapp.newadmin.ui.screens.EsercizioDialog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editingTrainerFieldsPreservesUserNoteHistoryPriorityAndOrder() {
        val original = Esercizio("Panca", "3 x 10", priorita = 7, riposo = "1'00\"",
            notePT = "Controllo", noteUtente = "Non cancellare",
            weightLogs = mapOf("entry" to WeightLogEntry(42.5, 1790000000000L)), ordine = 5)
        var saved: Esercizio? = null
        compose.setContent {
            MaterialTheme { EsercizioDialog(original, {}, { saved = it }, emptyList()) }
        }
        compose.onNodeWithText("Nome Esercizio").performTextReplacement("Panca inclinata")
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle {
            assertNotNull(saved)
            assertEquals("Panca inclinata", saved!!.name)
            assertEquals(original.noteUtente, saved!!.noteUtente)
            assertEquals(original.weightLogs, saved!!.weightLogs)
            assertEquals(original.priorita, saved!!.priorita)
            assertEquals(original.ordine, saved!!.ordine)
            assertEquals(original.notePT, saved!!.notePT)
            assertEquals("Panca", original.name)
        }
    }
}
