package com.matthew.sportiliapp.newadmin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.s08Card
import com.matthew.sportiliapp.S08Fixture
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S08EditorsTest {
    @get:Rule val compose = createComposeRule()
    private val card = s08Card()
    private var saved: Scheda? = null
    private var savedDay: Giorno? = null
    private var cancelled = 0
    private fun scroll(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun card(pending: Boolean = false) {
        compose.setContent { SportiliAppTheme {
            EditWorkoutCardScreen(card, userCode = "SPT-042", isSaving = pending,
                onDaySelected = { _, _, updated -> saved = updated }, onSave = { saved = it }, onCancel = { cancelled++ })
        } }
    }
    private fun day(pending: Boolean = false) {
        compose.setContent { SportiliAppTheme {
            EditDayScreen("giorno1", card.giorni.getValue("giorno1"), userCode = "SPT-042", isSaving = pending,
                onSave = { savedDay = it }, onCancel = { cancelled++ }, onMuscleGroupSelected = { _, _, day -> savedDay = day })
        } }
    }
    @Test fun reorderDaysPreservesOriginsAndLimits() {
        card()
        val first = card.giorni.values.first().name
        scroll(first)
        compose.onNodeWithContentDescription("Sposta $first prima").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Sposta $first dopo").performScrollTo().performClick()
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Giorno B · Gambe e glutei", saved!!.giorni.values.first().name)
            assertEquals("giorno1", saved!!.dayOrigins!!["giorno2"])
            assertTrue(saved!!.cambioRichiesto)
        }
        scroll("Giorno C · Trazione")
        compose.onNodeWithContentDescription("Sposta Giorno C · Trazione dopo").assertIsNotEnabled()
    }
    @Test fun removalIsConfirmedAndCanBeDiscarded() {
        card()
        val name = card.giorni.values.first().name
        scroll(name)
        compose.onNodeWithContentDescription("Rimuovi $name dalla bozza").performScrollTo().performClick()
        compose.onNode(hasText("Annulla") and hasAnyAncestor(isDialog())).performClick()
        compose.onNodeWithText(name).assertExists()
        compose.onNodeWithContentDescription("Rimuovi $name dalla bozza").performScrollTo().performClick()
        compose.onNodeWithText("Rimuovi dalla bozza").performClick()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Scarta").performClick()
        compose.runOnIdle { assertEquals(1, cancelled); assertNull(saved); assertEquals(3, card.giorni.size) }
    }
    @Test fun addDayValidationAndNewDaySave() {
        card()
        scroll("Aggiungi giorno")
        compose.onNodeWithText("Aggiungi giorno", substring = false).performClick()
        compose.onNodeWithText("Aggiungi", substring = false).performClick()
        compose.onNodeWithText("Inserisci un nome per il giorno").assertExists()
        compose.onNodeWithText("Nome del giorno").performTextInput("  Nuovo giorno  ")
        compose.onNodeWithText("Aggiungi", substring = false).performClick()
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Nuovo giorno", saved!!.giorni["giorno4"]!!.name) }
    }
    @Test fun pendingConsumesBack() {
        card(true)
        Espresso.pressBack()
        compose.onNodeWithContentDescription("Indietro").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, cancelled); assertNull(saved) }
    }
    @Test fun dayNameValidationReturnsToFormFromExitDialog() {
        day()
        compose.onNodeWithText("Nome del giorno").performScrollTo().performTextReplacement("")
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNode(hasText("Salva") and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Modifiche non salvate") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Inserisci il nome del giorno").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nome del giorno").performTextReplacement(" Giorno modificato ")
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Giorno modificato", savedDay!!.name); assertEquals(3, savedDay!!.gruppiMuscolari.size) }
    }
    @Test fun groupReorderAndRemovalHaveCorrectKeys() {
        day()
        val first = card.giorni.values.first().gruppiMuscolari.values.first().nome
        scroll(first)
        compose.onNodeWithContentDescription("Sposta $first prima").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Sposta $first dopo").performScrollTo().performClick()
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Spalle", savedDay!!.gruppiMuscolari["gruppo1"]!!.nome) }
        scroll("Tricipiti")
        compose.onNodeWithContentDescription("Sposta Tricipiti dopo").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Rimuovi Tricipiti dalla bozza").performScrollTo().performClick()
        compose.onNodeWithText("Rimuovi dalla bozza").performClick()
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("gruppo1", "gruppo2"), savedDay!!.gruppiMuscolari.keys.toList()) }
    }
    @Test fun addSeveralGroupsKeepsEverySelectedGroup() {
        day()
        scroll("Aggiungi gruppi")
        compose.onNodeWithText("Aggiungi gruppi", substring = false).performClick()
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.onNodeWithText("Aggiungi", substring = false).performClick()
        compose.onNodeWithText("Salva", substring = false).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(5, savedDay!!.gruppiMuscolari.size); assertEquals(5, savedDay!!.gruppiMuscolari.keys.toSet().size) }
    }
    @Test fun dayPendingConsumesBack() {
        day(true)
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(0, cancelled); assertNull(savedDay) }
    }
    @Test fun longDayNameHasNoVisualOverflow() {
        card()
        val name = card.giorni.values.first().name
        scroll(name)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(name).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty()); assertFalse(layouts.any { it.hasVisualOverflow })
    }
    @Test fun emptyCardCanStillBeSavedForFirstCreation() {
        compose.setContent { SportiliAppTheme {
            EditWorkoutCardScreen(card.copy(giorni = linkedMapOf()), onSave = { saved = it }, onCancel = {}, onDaySelected = { _, _, _ -> })
        } }
        compose.onNodeWithText("Salva", substring = false).performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(saved!!.giorni.isEmpty()) }
    }
    @Test fun completionRetryPreservesIntentAndDraft() {
        val error = mutableStateOf<String?>(null)
        var calls = 0
        compose.setContent { SportiliAppTheme {
            EditWorkoutCardScreen(card, errorMessage = error.value,
                onDaySelected = { _, _, _ -> }, onCancel = {},
                onSave = { saved = it; calls++; error.value = "Errore locale" })
        } }
        compose.onNodeWithText("Durata (settimane)").performScrollTo().performTextReplacement("10")
        compose.onNodeWithText("Salva e segna il cambio come completato").performScrollTo().performClick()
        scroll("Errore locale")
        compose.onNodeWithText("Riprova").performClick()
        compose.runOnIdle { assertEquals(2, calls); assertFalse(saved!!.cambioRichiesto); assertEquals(10, saved!!.durata) }
    }
    @Test fun loadingErrorKeepsContextBackAndRetry() {
        var retries = 0
        compose.setContent { SportiliAppTheme {
            AdminEditorLoadState("Scheda", "Utente SPT-042 · Scheda", false,
                onBack = { cancelled++ }, onRetry = { retries++ })
        } }
        compose.onNodeWithText("Utente SPT-042 · Scheda").assertIsDisplayed()
        compose.onNodeWithText("Impossibile caricare la scheda").assertIsDisplayed()
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(1, cancelled) }
    }
    @Test fun navigationKeepsCardDayAndGroupContext() {
        compose.setContent { SportiliAppTheme { S08Fixture() } }
        val name = card.giorni.values.first().name
        scroll(name)
        compose.onNodeWithText(name).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Modifica giorno").fetchSemanticsNodes().isNotEmpty() }
        val group = card.giorni.values.first().gruppiMuscolari.values.first().nome
        scroll(group)
        compose.onNodeWithText(group).performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Utente SPT-042 · Scheda · giorno1 · Gruppo").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Modifica giorno").assertIsDisplayed()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Modifica scheda").assertIsDisplayed()
    }

    @Test fun failedDayOpeningRetriesSameCallbackAndKey() {
        val error = mutableStateOf<String?>(null)
        var opens = 0
        var key: String? = null
        compose.setContent { SportiliAppTheme {
            EditWorkoutCardScreen(card, errorMessage = error.value,
                onDaySelected = { selectedKey, _, updated -> opens++; key = selectedKey; saved = updated; error.value = "Apertura fallita" },
                onSave = { fail("Retry must preserve the day-opening callback") }, onCancel = {})
        } }
        val name = card.giorni.values.first().name
        scroll(name)
        compose.onNodeWithText(name).performClick()
        scroll("Apertura fallita")
        compose.onNodeWithText("Riprova").performClick()
        compose.runOnIdle { assertEquals(2, opens); assertEquals("giorno1", key); assertTrue(saved!!.cambioRichiesto) }
    }

}
