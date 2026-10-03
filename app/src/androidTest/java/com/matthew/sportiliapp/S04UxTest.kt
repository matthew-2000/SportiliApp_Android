package com.matthew.sportiliapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S04UxTest {
    @get:Rule val compose = createAndroidComposeRule<S04PreviewActivity>()

    private fun open(id: String) {
        val name = s04Workout().giorni.getValue("day-z").gruppiMuscolari.getValue("group-a").esercizi.getValue(id).name
        if (id == "single") compose.onNode(hasText(name).and(hasText("3 × 40 secondi"))).performClick()
        else {
            val title = "Superset · ${if (id == "double") 2 else 3} parti"
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
        }
        compose.waitForIdle()
    }
    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test fun singlePrescriptionPrecedesSeparateImageErrorAndTimerOpens() {
        open("single")
        val prescription = compose.onNodeWithText("3 × 40 secondi").fetchSemanticsNode().boundsInRoot
        val image = compose.onNodeWithContentDescription("Immagine di Plank non disponibile").fetchSemanticsNode().boundsInRoot
        assertTrue(prescription.bottom < image.top)
        compose.onAllNodesWithText("Plank").assertCountEquals(1)
        compose.onNodeWithText("Immagine non disponibile").assertIsDisplayed()
        compose.onNodeWithText("Apri timer di recupero").performClick()
        compose.onNodeWithText("Timer recupero").assertExists()
    }

    @Test fun tripleSelectionUsesDistinctNotesAndLegacyKeyAndBackReturnsToDay() {
        open("triple")
        compose.onNodeWithText("Parte 1 di 3 · Attiva").assertExists()
        scrollTo("Nota della parte 1")
        compose.onNodeWithText("Nota della parte 1").assertIsDisplayed()
        scrollTo(s04Names[1]); compose.onNodeWithText(s04Names[1]).performClick()
        compose.onNodeWithText("Parte 2 di 3 · Attiva").assertExists()
        scrollTo("Nota della parte 2")
        compose.onNodeWithText("Nota della parte 2").assertIsDisplayed()
        compose.onNodeWithText("Nota della parte 1").assertDoesNotExist()
        scrollTo(s04Names[2]); compose.onNodeWithText(s04Names[2]).performClick()
        compose.onNodeWithText("Parte 3 di 3 · Attiva").assertExists()
        scrollTo("Nota della parte 3 · chiave legacy")
        compose.onNodeWithText("Nota della parte 3 · chiave legacy").assertIsDisplayed()
        compose.runOnUiThread { assertEquals("lite", compose.activity.fixtureModel.exerciseKeyFromName("Élite")) }
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Giorno A · Spinta").assertExists()
        assertTrue(compose.activity.requestedImages.none { it.substringBefore(".png").contains("+") })
    }

    @Test fun doubleIsCombinationAndKeepsEntireLongName() {
        open("double")
        compose.onNodeWithText("Superset · 2 parti").assertIsDisplayed()
        compose.onNodeWithText("Esegui tutte le parti in combinazione.").assertIsDisplayed()
        compose.onNodeWithText(s04Names[1]).assertExists()
        compose.onNodeWithText("Variazioni").assertDoesNotExist()
    }

    @Test fun successfulImageOpensAndClosesFullscreen() {
        compose.activityRule.scenario.onActivity { it.intent.putExtra("image", "success") }
        compose.activityRule.scenario.recreate()
        open("single")
        compose.waitUntil(5000) {
            compose.onAllNodesWithContentDescription("Immagine di Plank. Apri a schermo intero").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Immagine di Plank. Apri a schermo intero").performClick()
        compose.onNodeWithContentDescription("Chiudi").assertExists().performClick()
        compose.onNodeWithText("3 × 40 secondi").assertExists()
    }

    @Test fun loadingKeepsPrescriptionAndHasNoErrorOverlay() {
        compose.activityRule.scenario.onActivity { it.intent.putExtra("image", "loading") }
        compose.activityRule.scenario.recreate()
        open("single")
        compose.onNodeWithContentDescription("Caricamento immagine di Plank").assertExists()
        compose.onNodeWithText("3 × 40 secondi").assertIsDisplayed()
        compose.onNodeWithText("Immagine non disponibile").assertDoesNotExist()
    }
}
