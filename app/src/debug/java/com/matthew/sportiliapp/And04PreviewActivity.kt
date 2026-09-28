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
                        versionLabel = "Versione 1.3.5 (31)",
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
            titolo = "Chiusura straordinaria della sala pesi",
            descrizione = "La sala pesi chiude alle 19:00 per manutenzione.",
            urgenza = "alta",
            scadenza = System.currentTimeMillis() + 86_400_000L
        ),
        Avviso(
            id = "course",
            titolo = "Nuovo orario corso mobility",
            descrizione = "Da lunedì il corso inizierà alle 18:30.",
            urgenza = "media",
            scadenza = System.currentTimeMillis() + 604_800_000L
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
