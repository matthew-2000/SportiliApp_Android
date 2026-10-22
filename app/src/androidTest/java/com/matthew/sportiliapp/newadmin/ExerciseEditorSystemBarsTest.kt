package com.matthew.sportiliapp.newadmin

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.matthew.sportiliapp.S09PreviewActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Uses the real app theme/window; the generic Compose test host fits system bars. */
class ExerciseEditorSystemBarsTest {
    @get:Rule val compose = createAndroidComposeRule<S09PreviewActivity>()

    @Test fun newExerciseSaveStaysAboveSystemNavigation() {
        compose.activityRule.scenario.onActivity { it.intent.putExtra("screen", "new") }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        val saveBounds = compose.onNodeWithText("Conferma esercizio", false).fetchSemanticsNode().boundsInWindow
        onView(isRoot()).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            // Dialog insets can be zero: compare against the host's visible system bar.
            val insets = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)!!
                .getInsets(WindowInsetsCompat.Type.navigationBars())
            val position = IntArray(2).also { view.getLocationInWindow(it) }
            val navigationTop = position[1] + view.height - insets.bottom
            assertTrue("Portrait test requires a visible navigation bar", insets.bottom > 0)
            assertTrue("Save $saveBounds must fit above navigation at $navigationTop",
                saveBounds.bottom <= navigationTop)
        }
    }
}
