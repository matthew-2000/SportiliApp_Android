package com.matthew.sportiliapp.newadmin

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.S07AdminFixture
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.UiState
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests production presentation with local callbacks only. */
@RunWith(AndroidJUnit4::class)
class S07AdminUsersTest {
    @get:Rule val compose = createComposeRule()
    private val user = previewAdminUsers().first()
    private var saved: Utente? = null
    private var removed = 0
    private var cancelled = 0

    private fun showEditor(initial: Utente? = user, pending: Boolean = false) {
        compose.setContent {
            SportiliAppTheme {
                EditUserScreen(initialUser = initial, isSaving = pending,
                    onSave = { saved = it }, onRemove = { removed++ },
                    onCancel = { cancelled++ }, onEditWorkoutCard = {})
            }
        }
    }
    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = false))
    }
    private fun editName(name: String) {
        compose.onNodeWithText("Modifica dati utente").performClick()
        scrollTo("Nome")
        compose.onNodeWithText("Nome", substring = false).performTextReplacement(name)
    }

    @Test fun emptyListKeepsFabAndSecondaryAreasAvailable() {
        var added = 0
        var reports = 0
        var alerts = 0
        compose.setContent { SportiliAppTheme {
            AdminUserListScreen(UiState.Success(emptyList()), 3, {}, { added++ }, { alerts++ }, { reports++ }, {})
        } }
        compose.onNodeWithText("Nessun utente presente").assertExists()
        compose.onNodeWithText("Nuovo utente", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Altre aree admin").performClick()
        compose.onNodeWithText("Segnalazioni · 3 aperte").performClick()
        compose.onNodeWithContentDescription("Altre aree admin").performClick()
        compose.onNodeWithText("Avvisi", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, added); assertEquals(1, reports); assertEquals(1, alerts) }
    }

    @Test fun filtersSearchAndResetPreserveOrderAndSelection() {
        var selected: Utente? = null
        compose.setContent { SportiliAppTheme {
            AdminUserListScreen(UiState.Success(previewAdminUsers()), 3, { selected = it }, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Scadute o mancanti 2").performClick().assertIsSelected()
        compose.onNodeWithText("Cerca per nome o codice").performTextReplacement("SPT-211")
        compose.onNodeWithText("1 risultato").assertExists()
        compose.onNodeWithText("Sara Verdi").performClick()
        compose.runOnIdle { assertEquals("SPT-211", selected!!.code) }
        compose.onNodeWithText("Cerca per nome o codice").performTextReplacement("nessun-match")
        compose.onNodeWithText("Nessun utente corrisponde ai filtri").assertExists()
        compose.onAllNodesWithText("Azzera filtri")[0].performClick()
        compose.onNodeWithText("Tutti 4").assertIsSelected()
        compose.onNodeWithText("4 risultati").assertExists()
    }

    @Test fun manyUsersCanReachLastRowAndLongNamesDoNotOverflow() {
        val users = com.matthew.sportiliapp.s07Users()
        compose.setContent { SportiliAppTheme {
            AdminUserListScreen(UiState.Success(users), 3, {}, {}, {}, {}, {})
        } }
        scrollTo("Alessandro Bianchi con un cognome molto lungo")
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("Alessandro Bianchi con un cognome molto lungo", useUnmergedTree = true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse(layouts.any { it.hasVisualOverflow })
        scrollTo("Utente 24 Locale")
        compose.onNodeWithText("Utente 24 Locale").assertIsDisplayed()
        compose.onNodeWithText("Nuovo utente", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun listErrorRetriesToLoadedState() {
        val state = mutableStateOf<UiState<List<Utente>>>(UiState.Error(Exception("fixture")))
        compose.setContent { SportiliAppTheme {
            AdminUserListScreen(state.value, null, {}, {}, {}, {}, { state.value = UiState.Success(previewAdminUsers()) })
        } }
        compose.onNodeWithText("Impossibile caricare gli utenti").assertIsDisplayed()
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Tutti 4").assertIsSelected()
    }

    @Test fun newCreationValidatesAndPreservesCodeAndDefaultWorkoutContract() {
        showEditor(initial = null)
        compose.onNodeWithText("Dati da completare").assertExists()
        compose.onNodeWithText("Crea utente", substring = false).assertIsEnabled().performClick()
        scrollTo("Inserisci il nome")
        compose.onNodeWithText("Inserisci il nome").assertExists()
        compose.onNodeWithText("Nome", substring = false).performTextReplacement("  giulia   maria ")
        scrollTo("Cognome")
        compose.onNodeWithText("Cognome", substring = false).performTextReplacement("rossi")
        compose.onNodeWithText("Crea utente", substring = false).performClick()
        compose.runOnIdle {
            assertEquals("Giulia Maria", saved!!.nome)
            assertEquals("Rossi", saved!!.cognome)
            assertTrue(saved!!.code.matches(Regex("giuros[0-9]{4}")))
            assertEquals(7, saved!!.scheda!!.durata)
            assertEquals(listOf("giorno1", "giorno2", "giorno3"), saved!!.scheda!!.giorni.keys.toList())
        }
    }

    @Test fun cleanAndRevertedNamesDisableSaveButWorkoutNavigationRemainsAvailable() {
        showEditor()
        compose.onNodeWithText("Giulia Rossi").assertExists()
        compose.onNodeWithText("Codice SPT-042 · Utente esistente").assertExists()
        compose.onNodeWithText("Salva", substring = false).assertIsNotEnabled()
        editName("Giulia Maria")
        compose.onNodeWithText("Salva", substring = false).assertIsEnabled()
        compose.onNodeWithText("Nome", substring = false).performTextReplacement("  giulia  ")
        compose.onNodeWithText("Nessuna modifica").assertExists()
        compose.onNodeWithText("Salva", substring = false).assertIsNotEnabled()
        scrollTo("Modifica scheda")
        compose.onNodeWithText("Modifica scheda").assertIsEnabled()
    }

    @Test fun draftBackCanContinueOrDiscard() {
        showEditor()
        editName("Giulia Maria")
        compose.onNodeWithContentDescription("Torna agli utenti").performClick()
        compose.onAllNodesWithText("Modifiche non salvate", substring = false).assertCountEquals(2)
        Espresso.pressBack()
        compose.onNodeWithText("Nome", substring = false).assertTextContains("Giulia Maria")
        compose.onNodeWithContentDescription("Torna agli utenti").performClick()
        compose.onNodeWithText("Scarta", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, cancelled); assertNull(saved) }
    }

    @Test fun pendingConsumesBackAndBlocksAllEditorActions() {
        showEditor(pending = true)
        compose.onNodeWithText("Salvataggio in corso…").assertExists()
        compose.onNodeWithText("Salva", substring = false).assertIsNotEnabled()
        compose.onNodeWithText("Annulla", substring = false).assertIsNotEnabled()
        compose.onNodeWithContentDescription("Torna agli utenti").assertIsNotEnabled()
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(0, cancelled); assertEquals(0, removed); assertNull(saved) }
    }

    @Test fun removalRequiresConfirmationAndRetryNeverSavesUser() {
        val error = mutableStateOf<String?>(null)
        compose.setContent { SportiliAppTheme {
            EditUserScreen(user, errorMessage = error.value,
                onSave = { saved = it }, onRemove = { removed++; error.value = "Rimozione fallita" },
                onCancel = {}, onEditWorkoutCard = {})
        } }
        scrollTo("Rimuovi utente")
        compose.onNodeWithText("Rimuovi utente").performClick()
        compose.onNodeWithText("Conferma rimozione").assertIsDisplayed()
        compose.onAllNodesWithText("Annulla").filter(hasClickAction()).onLast().performClick()
        compose.runOnIdle { assertEquals(0, removed) }
        compose.onNodeWithText("Rimuovi utente").performClick()
        compose.onAllNodesWithText("Rimuovi utente").onLast().performClick()
        scrollTo("Riprova")
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Conferma rimozione").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, removed); assertNull(saved) }
    }

    @Test fun failedSaveRetainsDraftAndRetriesSameIdentityAndWorkout() {
        val error = mutableStateOf<String?>(null)
        var attempts = 0
        compose.setContent { SportiliAppTheme {
            EditUserScreen(user, errorMessage = error.value,
                onSave = { attempts++; saved = it; error.value = "Salvataggio fallito" },
                onCancel = {}, onEditWorkoutCard = {})
        } }
        editName("Giulia Maria")
        compose.onNodeWithText("Salva", substring = false).performClick()
        scrollTo("Riprova")
        compose.onNodeWithText("Riprova").performClick()
        scrollTo("Nome")
        compose.onNodeWithText("Nome", substring = false).assertTextContains("Giulia Maria")
        compose.runOnIdle {
            assertEquals(2, attempts)
            assertEquals(user.code, saved!!.code)
            assertEquals(user.scheda, saved!!.scheda)
        }
    }

    @Test fun navigationAwayAndBackKeepsEditorDraftAndListFilter() {
        compose.setContent { SportiliAppTheme { S07AdminFixture() } }
        compose.onNodeWithText("Richieste 1").performClick()
        compose.onNodeWithText("Giulia Rossi").performClick()
        editName("Giulia Maria")
        scrollTo("Modifica scheda")
        compose.onNodeWithText("Modifica scheda").performClick()
        compose.onNodeWithText("Torna al form").performClick()
        scrollTo("Nome")
        compose.onNodeWithText("Nome", substring = false).assertTextContains("Giulia Maria")
        compose.onNodeWithContentDescription("Torna agli utenti").performClick()
        compose.onNodeWithText("Scarta").performClick()
        compose.onNodeWithText("Richieste 1").assertIsSelected()
    }
}
