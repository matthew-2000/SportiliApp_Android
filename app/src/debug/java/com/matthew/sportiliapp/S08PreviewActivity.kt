package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun s08Card() = Scheda("2026-09-01T08:00:00+0200", 8,
    linkedMapOf(
        "giorno1" to Giorno("Giorno A · Spinta e stabilità con un nome molto lungo", linkedMapOf(
            "gruppo1" to GruppoMuscolare("Pettorali e stabilità con un nome molto lungo"),
            "gruppo2" to GruppoMuscolare("Spalle"), "gruppo3" to GruppoMuscolare("Tricipiti"))),
        "giorno2" to Giorno("Giorno B · Gambe e glutei"),
        "giorno3" to Giorno("Giorno C · Trazione")), cambioRichiesto = true)

/** Debug-only host: all writes and navigation results stay in memory. */
class S08PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                S08Fixture(intent.getStringExtra("screen").orEmpty(), intent.getLongExtra("delay", 3000))
            }
        }
    }
}

@Composable
internal fun S08Fixture(screen: String = "card", resultDelayMillis: Long = 3000) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var card by remember { mutableStateOf(if (screen == "empty") s08Card().copy(giorni = linkedMapOf()) else s08Card()) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    fun save(action: () -> Unit) {
        pending = true; error = null
        scope.launch {
            delay(resultDelayMillis)
            pending = false
            if (screen == "failure" && attempts++ == 0) error = "Connessione interrotta. La bozza è ancora disponibile."
            else action()
        }
    }
    LaunchedEffect(Unit) { if (screen == "day") nav.navigate("day/giorno1") }
    NavHost(nav, startDestination = "card") {
        composable("card") {
            var unavailable by remember { mutableStateOf(screen == "loading" || screen == "load_error") }
            if (unavailable) {
                AdminEditorLoadState("Scheda", "Utente SPT-042 · Scheda", screen == "loading",
                    onBack = { nav.navigate("result") }, onRetry = { unavailable = false })
            } else EditWorkoutCardScreen(card, userCode = "SPT-042", isSaving = pending, errorMessage = error,
                onDaySelected = { key, _, updated -> save { card = updated; nav.navigate("day/$key") } },
                onSave = { updated -> save { card = updated; nav.navigate("result") } },
                onCancel = { nav.navigate("result") })
        }
        composable("day/{key}") { entry ->
            val key = entry.arguments!!.getString("key")!!
            val day = card.giorni.getValue(key)
            fun update(updated: Giorno) { card = card.copy(giorni = LinkedHashMap(card.giorni).apply { put(key, updated) }) }
            EditDayScreen(key, day, userCode = "SPT-042", isSaving = pending, errorMessage = error,
                onSave = { updated -> save { update(updated); nav.popBackStack() } },
                onCancel = { nav.popBackStack() },
                onMuscleGroupSelected = { groupKey, _, updated -> save { update(updated); nav.navigate("group/$key/$groupKey") } })
        }
        composable("group/{day}/{group}") { entry ->
            val key = entry.arguments!!.getString("day")!!
            val group = entry.arguments!!.getString("group")!!
            EditMuscleGroupContent("SPT-042", key, card.giorni.getValue(key).gruppiMuscolari.getValue(group),
                predefiniti = emptyList(),
                onSave = { nav.popBackStack() }, onCancel = { nav.popBackStack() })
        }
        composable("result") {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Text("Risultato locale", style = MaterialTheme.typography.titleLarge)
                Text("Richiesta cambio: ${if (card.cambioRichiesto) "aperta" else "completata"}")
                Button(onClick = { nav.popBackStack() }) { Text("Torna alla scheda") }
            }
        }
    }
}
