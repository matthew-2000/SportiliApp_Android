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
import org.junit.Assert.assertEquals
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
                    versionName = "1.3.5",
                    buildNumber = "31",
                    onOpenLink = { false },
                    onLogout = {}
                )
            }
        }

        compose.onNodeWithText("Sito web").performScrollTo().performClick()
        compose.onNodeWithText("Impossibile aprire Sito web. Riprova più tardi.")
            .assertIsDisplayed()
    }

    @Test
    fun logoutRequiresExplicitConfirmation() {
        val loggedOut = AtomicBoolean(false)
        compose.setContent {
            MaterialTheme {
                ImpostazioniContent(
                    versionName = "1.3.5",
                    buildNumber = "31",
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
    @Test
    fun eachExternalRowDeliversTheExistingDestination() {
        val opened = mutableListOf<String>()
        compose.setContent {
            com.matthew.sportiliapp.ui.theme.SportiliAppTheme {
                ImpostazioniContent(versionName = "1.3.5", buildNumber = "31",
                    onOpenLink = { opened.add(it); true }, onLogout = {})
            }
        }
        listOf("Instagram", "Facebook", "TikTok", "Sito web").forEach { label ->
            compose.onNodeWithText(label).performScrollTo().performClick()
        }
        assertEquals(listOf(
            "https://www.instagram.com/sportiliacentrofitness",
            "https://www.facebook.com/centrofitness.sportilia",
            "https://www.tiktok.com/@palestrasportilia",
            "https://www.palestrasportilia.it"
        ), opened)
        compose.onNodeWithText("Versione").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1.3.5").assertIsDisplayed()
        compose.onNodeWithText("Build").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("31").assertIsDisplayed()
    }

    @Test
    fun cancelLogoutKeepsTheSessionAndSettingsReachable() {
        val loggedOut = AtomicBoolean(false)
        compose.setContent {
            com.matthew.sportiliapp.ui.theme.SportiliAppTheme {
                ImpostazioniContent(versionName = "1.3.5", buildNumber = "31",
                    onOpenLink = { true }, onLogout = { loggedOut.set(true) })
            }
        }
        compose.onNodeWithText("Esci").performScrollTo().performClick()
        compose.onNodeWithText("Annulla").performClick()
        assertFalse(loggedOut.get())
        compose.onNodeWithText("Esci").assertIsDisplayed()
    }

}
