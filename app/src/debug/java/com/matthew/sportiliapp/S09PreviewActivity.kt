package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun s09Catalog() = listOf(
    GruppoMuscolarePredefinito("petto", "Pettorali", listOf(EsercizioPredefinito("panca", "Panca piana", ""), EsercizioPredefinito("croci", "Croci ai cavi", ""))),
    GruppoMuscolarePredefinito("gambe", "Gambe", listOf(EsercizioPredefinito("squat", "Squat", ""), EsercizioPredefinito("affondi", "Affondi posteriori con manubri", ""))))
internal fun s09Exercise() = Esercizio("Panca piana + Croci ai cavi + Push up con controllo della fase eccentrica", "4x8 + 12 + 30 secondi", priorita = 3,
    riposo = "1'30\"", notePT = "Mantieni le scapole stabili e controlla la discesa. Esegui tutte le parti del superset in sequenza.",
    noteUtente = "Nota personale conservata", weightLogs = mapOf("peso" to WeightLogEntry(42.5, 1790000000000L)), ordine = 0)
internal fun s09Group() = GruppoMuscolare("Circuito", linkedMapOf("esercizio1" to s09Exercise(), "esercizio2" to Esercizio("Squat", "3 x 10", riposo = "2'00\"", ordine = 1)))

/** Every catalog entry and save result is local; no ViewModel or repository is instantiated. */
class S09PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                val mode = intent.getStringExtra("screen").orEmpty()
                val scope = rememberCoroutineScope()
                var saved by remember { mutableStateOf<GruppoMuscolare?>(null) }
                var pending by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                var attempts by remember { mutableIntStateOf(0) }
                var closed by remember { mutableStateOf(false) }
                if (closed || saved != null) Column(Modifier.fillMaxSize().padding(24.dp)) {
                    Text("Risultato locale", style = MaterialTheme.typography.titleLarge)
                    Text(saved?.esercizi?.values?.joinToString("\n") { "${it.name}: ${it.serie}\n${it.notePT.orEmpty()}" } ?: "Bozza scartata")
                } else if (mode == "editor" || mode == "new") EsercizioDialog(
                    if (mode == "new") Esercizio("", "") else s09Exercise(),
                    onDismiss = { closed = true }, onConfirm = { saved = GruppoMuscolare("Circuito", mapOf("esercizio1" to it)) },
                    predefiniti = s09Catalog().flatMap { it.esercizi })
                else EditMuscleGroupContent("SPT-042", "giorno1", if (mode == "empty") s09Group().copy(esercizi = emptyMap()) else s09Group(),
                    s09Catalog(), pending, error, onSave = { updated ->
                        pending = true; error = null
                        scope.launch { delay(intent.getLongExtra("delay", 3000)); pending = false
                            if (mode == "failure" && attempts++ == 0) error = "Connessione interrotta. La bozza è ancora disponibile." else saved = updated }
                    }, onCancel = { closed = true })
            }
        }
    }
}
