package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.scheda.SchedaOverviewContent
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Qa01PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        val useDarkTheme = intent.getBooleanExtra("dark", false)

        setContent {
            SportiliAppTheme(isDarkTheme = useDarkTheme) {
                Surface {
                    SchedaOverviewContent(
                        userName = "Matteo",
                        scheda = previewWorkout(
                            expired = screen != "active",
                            requested = screen == "requested"
                        ),
                        isLoading = false,
                        isOffline = false,
                        errorMessage = null,
                        userCode = "AB12CD",
                        isCodeVisible = false,
                        isRequesting = false,
                        requestError = null,
                        onToggleCodeVisibility = {},
                        onRetry = {},
                        onOpenDay = {},
                        onRequestNewWorkout = {}
                    )
                }
            }
        }
    }

    private fun previewWorkout(expired: Boolean, requested: Boolean): Scheda {
        val day = Giorno(
            name = "Giorno A · Spinta",
            gruppiMuscolari = linkedMapOf(
                "0" to GruppoMuscolare("Petto"),
                "1" to GruppoMuscolare("Spalle"),
                "2" to GruppoMuscolare("Tricipiti")
            )
        )
        return Scheda(
            dataInizio = if (expired) {
                "2020-01-01T08:00:00+0100"
            } else {
                val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).format(start.time)
            },
            durata = 6,
            giorni = linkedMapOf(
                "giorno-a" to day,
                "giorno-b" to day.copy(name = "Giorno B · Trazione")
            ),
            cambioRichiesto = requested
        )
    }
}
