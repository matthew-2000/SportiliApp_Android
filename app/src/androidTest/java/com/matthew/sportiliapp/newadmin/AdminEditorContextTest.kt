package com.matthew.sportiliapp.newadmin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.matthew.sportiliapp.S11AdminPreviewActivity
import com.matthew.sportiliapp.s08Card
import org.junit.Rule
import org.junit.Test

/** Real admin navigation with in-memory data; no production writes. */
class AdminEditorContextTest {
    @get:Rule val compose = createAndroidComposeRule<S11AdminPreviewActivity>()

    private fun scrollTo(text: String) {
        if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        } else {
            compose.onNodeWithText(text).performScrollTo()
        }
    }

    private fun assertUserContext(inDialog: Boolean = false) {
        val name = hasText("Giulia Rossi", substring = true) and
            hasAnyAncestor(hasTestTag("admin-editor-context"))
        compose.onNode(if (inDialog) name and hasAnyAncestor(isDialog()) else name).assertIsDisplayed()
        compose.onNodeWithText("SPT-042", substring = true).assertDoesNotExist()
    }

    @Test fun nameFollowsEditorsAndSurvivesActivityRecreation() {
        val day = s08Card().giorni.values.first()
        scrollTo("Giulia Rossi")
        compose.onNodeWithText("Giulia Rossi").performClick()
        scrollTo("Modifica scheda")
        compose.onNodeWithText("Modifica scheda").performClick()
        assertUserContext()
        scrollTo(day.name)
        compose.onNodeWithText(day.name).performClick()
        assertUserContext()
        val group = day.gruppiMuscolari.values.first().nome
        scrollTo(group)
        compose.onNodeWithText(group).performClick()
        assertUserContext()
        compose.activityRule.scenario.recreate()
        assertUserContext()
        compose.onNodeWithText("Nuovo esercizio").performClick()
        assertUserContext(inDialog = true)
        compose.onNodeWithText("Conferma esercizio").assertIsDisplayed()
    }
}
