package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.matthew.sportiliapp.avvisi.AlertsFeedUiState
import com.matthew.sportiliapp.avvisi.AvvisiContent
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

class And04PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra(EXTRA_SCREEN).orEmpty()
        val useDarkTheme = intent.getBooleanExtra(EXTRA_DARK_THEME, false)

        setContent {
            SportiliAppTheme(isDarkTheme = useDarkTheme) {
                when (screen) {
                    SCREEN_SETTINGS -> ImpostazioniContent(
                        versionName = "1.3.5",
                    buildNumber = "31",
                        onOpenLink = { false },
                        onLogout = {}
                    )

                    SCREEN_ALERTS_ERROR -> AvvisiContent(
                        state = AlertsFeedUiState.Error(IllegalStateException("Preview error")),
                        onRetry = {}
                    )

                    else -> AvvisiContent(
                        state = AlertsFeedUiState.Success(previewAlerts()),
                        onRetry = {}
                    )
                }
            }
        }
    }

    private fun previewAlerts(): List<Avviso> = listOf(
        Avviso(
            id = "urgent",
            titolo = "Promemoria check-in",
            descrizione = "Ricorda di registrare i progressi dopo l'allenamento.",
            urgenza = "alta",
            scadenza = System.currentTimeMillis() + 172_800_000L
        ),
        Avviso(
            id = "course",
            titolo = "Aggiornamento scheda",
            descrizione = "La nuova scheda sarà disponibile da lunedì.",
            urgenza = "media",
            scadenza = System.currentTimeMillis() + 432_000_000L
        ),
        Avviso(
            id = "info",
            titolo = "Orari festivi",
            descrizione = "La palestra chiuderà alle 18:00 il 31/12.",
            urgenza = "bassa",
            scadenza = System.currentTimeMillis() + 1_036_800_000L
        ),
        Avviso(
            id = "expired",
            titolo = "Orario festivo",
            descrizione = "Comunicazione non più attiva.",
            urgenza = "bassa",
            scadenza = System.currentTimeMillis() - 86_400_000L
        )
    )

    private companion object {
        const val EXTRA_SCREEN = "screen"
        const val EXTRA_DARK_THEME = "dark"
        const val SCREEN_SETTINGS = "settings"
        const val SCREEN_ALERTS_ERROR = "alerts_error"
    }
}
