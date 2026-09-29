package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.ui.navigation.AdminMasterDetailLayout
import com.matthew.sportiliapp.newadmin.ui.screens.EditDayScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditUserScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditWorkoutCardScreen
import com.matthew.sportiliapp.newadmin.ui.screens.UserListContent
import com.matthew.sportiliapp.newadmin.ui.screens.previewAdminUsers
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

class Adm02PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        val useDarkTheme = intent.getBooleanExtra("dark", false)
        val user = previewUser()

        setContent {
            SportiliAppTheme(isDarkTheme = useDarkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when (screen) {
                        "tablet" -> AdminMasterDetailLayout(
                            master = {
                                UserListContent(
                                    users = previewAdminUsers(),
                                    openReportCount = 3,
                                    onUserSelected = {},
                                    onManageAlerts = {},
                                    onViewReports = {},
                                    showDashboard = false
                                )
                            },
                            detail = { UserEditorPreview(user) }
                        )

                        "workout_error" -> EditWorkoutCardScreen(
                            scheda = user.scheda!!,
                            errorMessage = "Non è stato possibile salvare la scheda.",
                            onDaySelected = { _, _, _ -> },
                            onSave = {},
                            onCancel = {}
                        )

                        "day" -> EditDayScreen(
                            dayKey = "giorno1",
                            day = user.scheda!!.giorni.getValue("giorno1"),
                            onSave = {},
                            onCancel = {},
                            onMuscleGroupSelected = { _, _, _ -> }
                        )

                        else -> UserEditorPreview(user)
                    }
                }
            }
        }
    }

    private fun previewUser(): Utente {
        val group = GruppoMuscolare(
            nome = "Pettorali",
            esercizi = linkedMapOf(
                "esercizio1" to Esercizio("Panca piana", "4 x 8", riposo = "1'30\"")
            )
        )
        val day = Giorno("Giorno A · Spinta", linkedMapOf("gruppo1" to group))
        return Utente(
            code = "SPT-042",
            cognome = "Rossi",
            nome = "Giulia",
            scheda = Scheda(
                dataInizio = "2026-09-01T08:00:00+0200",
                durata = 8,
                giorni = linkedMapOf("giorno1" to day),
                cambioRichiesto = true
            )
        )
    }
}

@androidx.compose.runtime.Composable
private fun UserEditorPreview(user: Utente) {
    EditUserScreen(
        initialUser = user,
        onSave = {},
        onRemove = {},
        onCancel = {},
        onEditWorkoutCard = {}
    )
}
