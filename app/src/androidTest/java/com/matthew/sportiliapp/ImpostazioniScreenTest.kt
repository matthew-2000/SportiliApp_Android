package com.matthew.sportiliapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImpostazioniScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun unavailableExternalLinkShowsFeedback() {
        compose.setContent {
            MaterialTheme {
                ImpostazioniContent(
                    versionLabel = "Versione 1.3.5 (31)",
                    onOpenLink = { false },
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Visita il sito web").performScrollTo().performClick()
        compose.onNodeWithText("Impossibile aprire Sito web. Riprova più tardi.")
            .assertIsDisplayed()
    }

    @Test
    fun logoutRequiresExplicitConfirmation() {
        val loggedOut = AtomicBoolean(false)
        compose.setContent {
            MaterialTheme {
                ImpostazioniContent(
                    versionLabel = "Versione 1.3.5 (31)",
                    onOpenLink = { true },
                    onLogout = { loggedOut.set(true) }
                )
            }
        }

        compose.onNodeWithText("Esci").performScrollTo().performClick()
        compose.onNodeWithText("Uscire dall’account?").assertIsDisplayed()
        assertFalse(loggedOut.get())

        compose.onNodeWithContentDescription("Conferma uscita").performClick()
        assertTrue(loggedOut.get())
    }
}
