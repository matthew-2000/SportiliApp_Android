package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.matthew.sportiliapp.avvisi.AlertsFeedUiState
import com.matthew.sportiliapp.avvisi.AvvisiContent
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

/** Local-only S06 UI. No Auth, repository or session preferences are accessed. */
class S06PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        val dark = intent.getBooleanExtra("dark", false)
        setContent {
            SportiliAppTheme(isDarkTheme = dark) {
                var loggedOut by remember { mutableStateOf(false) }
                if (loggedOut) {
                    Text("Sessione locale chiusa")
                } else if (screen.startsWith("settings")) {
                    ImpostazioniContent(versionName = "1.3.5", buildNumber = "31",
                        onOpenLink = { !screen.contains("error") }, onLogout = { loggedOut = true })
                } else {
                    var state by remember {
                        mutableStateOf<AlertsFeedUiState>(when (screen) {
                            "alerts-empty" -> AlertsFeedUiState.Success(emptyList())
                            "alerts-error" -> AlertsFeedUiState.Error(IllegalStateException("Local fixture"))
                            "alerts-loading" -> AlertsFeedUiState.Loading
                            else -> AlertsFeedUiState.Success(s06Alerts(screen == "alerts-long"))
                        })
                    }
                    AvvisiContent(state = state, onRetry = { state = AlertsFeedUiState.Success(s06Alerts()) })
                }
            }
        }
    }
}

internal fun s06Alerts(long: Boolean = false): List<Avviso> {
    val future = System.currentTimeMillis() + 5 * 86_400_000L
    if (long) return listOf(Avviso(id = "long", urgenza = "alta", scadenza = future,
        titolo = "Chiusura straordinaria della sala pesi e aggiornamento degli orari dei corsi serali durante la manutenzione",
        descrizione = "Durante la manutenzione della sala pesi, gli allenamenti e i corsi serali si svolgeranno nella sala al primo piano. Chiedi al trainer come adattare la tua scheda e controlla gli orari prima di raggiungere la palestra. Grazie per la collaborazione."))
    return listOf("alta", "media", "bassa", "nessuna").map { urgency ->
        Avviso(id = urgency, titolo = "Aggiornamento $urgency",
            descrizione = "Controlla gli orari prima di raggiungere la palestra.",
            urgenza = urgency, scadenza = if (urgency == "nessuna") null else future)
    } + Avviso(id = "expired", titolo = "Orario festivo", descrizione = "Comunicazione non più attiva.",
        urgenza = "bassa", scadenza = System.currentTimeMillis() - 86_400_000L)
}
