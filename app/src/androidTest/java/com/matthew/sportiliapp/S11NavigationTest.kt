package com.matthew.sportiliapp

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** End-to-end local UI smoke, using the production ContentNavigation shell. No remote writes. */
class S11NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<S11PreviewActivity>()
    @Before fun configureTheme() {
        if (InstrumentationRegistry.getArguments().getString("captureMode") == "dark") {
            compose.activityRule.scenario.onActivity { it.intent.putExtra("dark", true) }
            compose.activityRule.scenario.recreate()
        }
    }
    private fun tab(text: String) = compose.onNode(hasText(text) and hasClickAction() and isSelectable())
    private fun scrollTo(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun click(text: String) {
        if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) scrollTo(text)
        compose.onNodeWithText(text).performScrollTo().performClick()
    }
    private fun back() { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle(); Thread.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val mode = InstrumentationRegistry.getArguments().getString("captureMode") ?: "normal"
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "S11/$mode").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    private fun login() {
        compose.onNodeWithText("Codice di accesso").performTextReplacement("AB12CD")
        click("Accedi")
        compose.waitUntil(5000) { compose.onAllNodes(isSelectable()).fetchSemanticsNodes().size == 3 }
        tab("Scheda").assertIsSelected()
    }
    @Test fun completeNavigationRetainsDetailAcrossTabsAndReturnsToLogin() {
        capture("login"); login(); capture("home")
        click("Giorno A · Spinta"); tab("Scheda").assertIsSelected(); capture("day")
        scrollTo("Plank"); compose.onAllNodesWithText("Plank")[0].performClick()
        tab("Scheda").assertIsSelected(); capture("detail")
        tab("Avvisi").performClick().assertIsSelected(); capture("alerts")
        tab("Scheda").performClick().assertIsSelected()
        compose.onNodeWithText("Apri timer di recupero").assertExists()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Petto e stabilità").assertExists()
        compose.onNodeWithContentDescription("Indietro").performClick()
        compose.onNodeWithText("Giorno A · Spinta").assertExists()
        tab("Impostazioni").performClick().assertIsSelected(); capture("settings")
        scrollTo("Versione"); capture("settings-version")
        click("Esci"); capture("logout-confirmation")
        compose.onNodeWithText("Annulla").performClick()
        tab("Impostazioni").assertIsSelected()
        click("Esci"); compose.onNodeWithContentDescription("Conferma uscita").performClick()
        compose.onNodeWithText("Codice di accesso").assertExists()
        capture("logout-login")
    }
    @Test fun localWeightNotesAndTimerRemainReachableInRealShell() {
        login(); click("Giorno A · Spinta"); scrollTo("Plank"); compose.onAllNodesWithText("Plank")[0].performClick()
        compose.onNodeWithText("Apri timer di recupero").performScrollTo().performClick(); capture("timer")
        back(); scrollTo("Registra peso"); compose.onNodeWithText("Registra peso").performClick()
        compose.onNodeWithText("Peso (kg)").performTextReplacement("47,5"); capture("weight-keyboard")
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        scrollTo("Ultimo peso: 47.5 kg"); compose.onAllNodesWithText("Ultimo peso: 47.5 kg")[0].assertExists(); capture("weight-saved")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("Note personali. Apri editor"))
        compose.onNodeWithContentDescription("Note personali. Apri editor").performScrollTo().performClick()
        compose.onNodeWithText("Nota personale").performTextReplacement("Nota locale S11"); capture("notes-keyboard")
        compose.onNodeWithText("Chiudi").performScrollTo().performClick()
        compose.onNodeWithText("Continua a modificare").performClick()
        compose.onNodeWithText("Nota locale S11").assertExists()
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        compose.runOnUiThread { assertEquals(listOf("plank", "plank"), compose.activity.writeKeys) }
        capture("notes-saved")
    }
    @Test fun loginEmptyInvalidNetworkHelpAndRotation() {
        click("Accedi"); compose.onNodeWithText("Inserisci il codice di accesso.").assertExists()
        compose.onNodeWithText("Codice di accesso").performTextReplacement("ERRATO"); click("Accedi")
        compose.waitUntil(5000) { compose.onAllNodesWithText("Il codice non è valido. Controllalo e riprova.").fetchSemanticsNodes().isNotEmpty() }
        capture("login-invalid")
        compose.onNodeWithText("Codice di accesso").performTextReplacement("RETE"); click("Accedi")
        compose.waitUntil(5000) { compose.onAllNodesWithText("Non riusciamo a verificare il codice. Controlla la connessione e riprova.").fetchSemanticsNodes().isNotEmpty() }
        capture("login-network")
        click("Non hai il codice?"); compose.onNodeWithText("Ho capito").assertIsDisplayed().performClick()
        login(); click("Giorno A · Spinta"); scrollTo("Plank"); compose.onAllNodesWithText("Plank")[0].performClick()
        compose.activityRule.scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitForIdle(); Thread.sleep(1500)
        tab("Scheda").assertIsSelected(); compose.onNodeWithText("Apri timer di recupero").assertExists(); capture("detail-landscape")
        compose.activityRule.scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }
}
