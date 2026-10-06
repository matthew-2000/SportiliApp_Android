package com.matthew.sportiliapp

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Regression: maximum text must leave room for the form and every bottom action. */
class S11AdminAccessibilityTest {
    @get:Rule val compose = createAndroidComposeRule<S08PreviewActivity>()
    @Before fun configureTheme() {
        if (InstrumentationRegistry.getArguments().getString("captureMode") == "dark") {
            compose.activityRule.scenario.onActivity { it.intent.putExtra("dark", true) }
            compose.activityRule.scenario.recreate()
        }
    }
    @Test fun longDayContextLeavesFormAndKeyboardValidationReachable() {
        compose.activityRule.scenario.onActivity { it.intent.putExtra("screen", "day") }
        compose.activityRule.scenario.recreate()
        val form = compose.onNode(hasScrollToNodeAction())
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("Long context must leave a form viewport", form.fetchSemanticsNode().boundsInRoot.height / density >= 64)
        compose.onNodeWithText("Modifica giorno").assertIsDisplayed()
        compose.onNodeWithContentDescription("Indietro").assertIsDisplayed()
        capture("admin-day-form")
        compose.onNodeWithText("Nome del giorno").performScrollTo().performClick().performTextReplacement("")
        capture("admin-day-keyboard")
        compose.onNodeWithText("Salva").performScrollTo().performClick()
        // Wait for the native IME viewport resize before positioning the validation text.
        compose.waitForIdle()
        Thread.sleep(1000)
        val error = compose.onNodeWithText("Inserisci il nome del giorno", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.waitForIdle()
        Thread.sleep(500)
        val viewport = form.fetchSemanticsNode().boundsInRoot
        val errorBounds = error.fetchSemanticsNode().boundsInRoot
        assertTrue("Validation message must fit entirely in the form", errorBounds.top >= viewport.top && errorBounds.bottom <= viewport.bottom)
        capture("admin-day-validation")
        compose.onNodeWithContentDescription("Indietro").assertIsDisplayed().assertIsEnabled()
    }
    @Test fun formAndCompletionActionsStayReachable() {
        val form = compose.onNode(hasScrollToNodeAction())
        val bounds = form.fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("Bottom actions must not consume the form viewport", bounds.height / density >= 64)
        form.performScrollToNode(hasText("Durata (settimane)"))
        compose.onNodeWithText("Durata (settimane)").performScrollTo().assertIsDisplayed()
        capture("admin-card-form")
        compose.onNodeWithText("Salva").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Annulla").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Salva e segna il cambio come completato").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(7000) { compose.onAllNodesWithText("Richiesta cambio: completata").fetchSemanticsNodes().isNotEmpty() }
        capture("admin-card-completed")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(400)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val mode = InstrumentationRegistry.getArguments().getString("captureMode") ?: "normal"
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "S11/$mode").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
