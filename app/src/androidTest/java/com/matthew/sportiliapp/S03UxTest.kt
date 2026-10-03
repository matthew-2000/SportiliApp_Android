package com.matthew.sportiliapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class S03UxTest {
    @get:Rule val compose = createComposeRule()

    @Test fun namedActionOpensFirstDayInExistingOrder() {
        compose.setContent { SportiliAppTheme { S03HomeFixture("active") } }
        compose.onNodeWithText("Continua dal prossimo allenamento.").assertDoesNotExist()
        compose.onNodeWithText("Apri Giorno A · Spinta").performScrollTo().performClick()
        compose.onNodeWithText("Dettaglio locale · first-z").assertIsDisplayed()
    }

    @Test fun expiredWorkoutRemainsNavigableAfterRequest() {
        compose.setContent { SportiliAppTheme { S03HomeFixture("expired") } }
        compose.onNodeWithText("Richiedi nuova scheda").performScrollTo().performClick()
        compose.onNodeWithText("Richiesta inviata").assertExists()
        compose.onNodeWithText("Richiedi nuova scheda").assertDoesNotExist()
        compose.onNodeWithText("Giorno A · Spinta").performScrollTo().performClick()
        compose.onNodeWithText("Dettaglio locale · first-z").assertIsDisplayed()
    }

    @Test fun cachedErrorRetainsDaysAndRetryClearsBanner() {
        compose.setContent { SportiliAppTheme { S03HomeFixture("cached-error") } }
        compose.onNodeWithText("Nessuna scheda disponibile").assertDoesNotExist()
        compose.onNodeWithText("Aggiornamento non riuscito. Mostriamo gli ultimi dati disponibili.").assertIsDisplayed()
        compose.onNodeWithText("Riprova").performClick()
        compose.onNodeWithText("Aggiornamento non riuscito. Mostriamo gli ultimi dati disponibili.").assertDoesNotExist()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Giorno B · Trazione e stabilità del tronco"))
        compose.onNodeWithText("Giorno B · Trazione e stabilità del tronco").assertIsDisplayed()
    }

    @Test fun errorWithEmptyWorkoutIsNotAbsence() {
        compose.setContent { SportiliAppTheme { S03HomeFixture("empty-error") } }
        compose.onNodeWithText("Non riusciamo a caricare la scheda").assertIsDisplayed()
        compose.onNodeWithText("Nessuna scheda disponibile").assertDoesNotExist()
    }

    @Test fun longCodeErrorEditingAndHelpRemainUsable() {
        compose.setContent { SportiliAppTheme { S03LoginFixture() } }
        compose.onNodeWithText("Accedi").performScrollTo().performClick()
        compose.onNodeWithText("Inserisci il codice di accesso.").assertIsDisplayed()
        val field = compose.onNode(hasSetTextAction())
        field.performClick().assertIsFocused()
        field.performTextReplacement("SPORTILI2026CODICEMOLTOLUNGO1234567890")
        compose.onNodeWithText("Inserisci il codice di accesso.").assertDoesNotExist()
        compose.onNodeWithText("Accedi").performScrollTo().performClick()
        compose.onNodeWithText("Il codice non è valido. Controllalo e riprova.").assertExists()
        compose.onNodeWithText("Non hai il codice?").performScrollTo().performClick()
        compose.onNodeWithText("Il codice viene fornito dal tuo trainer. Contattalo se non lo hai ancora ricevuto.").assertIsDisplayed()
    }

    @Test fun cachedErrorAndDayActionRemainReachableAtLargeFontAndNarrowWidth() {
        compose.setContent {
            SportiliAppTheme(isDarkTheme = true) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    Box(Modifier.width(320.dp)) { S03HomeFixture("cached-error") }
                }
            }
        }
        compose.onNodeWithText("Riprova").performScrollTo().assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Giorno B · Trazione e stabilità del tronco"))
        compose.onNodeWithText("Giorno B · Trazione e stabilità del tronco").performClick()
        compose.onNodeWithText("Dettaglio locale · second-a").assertIsDisplayed()
    }
}
