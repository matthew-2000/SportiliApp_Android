package com.matthew.sportiliapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.MutableLiveData
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.intercept.Interceptor
import coil.request.ErrorResult
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.avvisi.AlertsFeedUiState
import com.matthew.sportiliapp.avvisi.AvvisiContent
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.scheda.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay

/** DEBUG only. Real shell/day/detail; isolated offline SDK, fake login and local write callbacks. */
class S11PreviewActivity : ComponentActivity() {
    internal lateinit var fixtureModel: SchedaViewModel
    internal val writeKeys = mutableListOf<String>()
    private var demoApp: FirebaseApp? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = FirebaseOptions.Builder().setApplicationId("1:123456789:android:s11")
            .setApiKey("fake-local-key").setProjectId("demo-sportili-s11")
            .setDatabaseUrl("https://demo-sportili-s11.firebaseio.com").build()
        val app = FirebaseApp.initializeApp(this, options, "s11-${System.nanoTime()}")
        demoApp = app
        val db = FirebaseDatabase.getInstance(app).apply { goOffline() }
        val prefs = getSharedPreferences("s11-local-fixture", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        fixtureModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SchedaViewModel(this@S11PreviewActivity, db, prefs) as T
            }
        })[SchedaViewModel::class.java]
        val screen = intent.getStringExtra("screen") ?: "login"
        (fixtureModel.scheda as MutableLiveData).value = s04Workout().copy(dataInizio = s03Workout(screen)!!.dataInizio,
            cambioRichiesto = screen == "requested")
        (fixtureModel.userExerciseData as MutableLiveData).value = s04Data()
        fixtureModel.isLoading.value = false
        val writes = ExerciseDetailActions(
            addWeight = { key, value, success, _ ->
                writeKeys += key
                val entry = WeightLogEntry(value, 1_790_000_000_000L)
                change(key) { it.copy(weightLogs = it.weightLogs.orEmpty() + ("local-new" to entry)) }
                success("local-new", entry)
            }, updateWeight = { key, id, value, success, _ ->
                writeKeys += key
                val entry = WeightLogEntry(value, 1_790_000_000_000L)
                change(key) { it.copy(weightLogs = it.weightLogs.orEmpty() + (id to entry)) }; success(entry)
            }, updateNote = { key, note, success, _ ->
                writeKeys += key; change(key) { it.copy(noteUtente = note) }; success()
            })
        val images = ImageLoader.Builder(this).components {
            add(Interceptor { chain -> ErrorResult(null, chain.request, IllegalStateException("Local fixture")) })
        }.build()
        setContent {
            CompositionLocalProvider(LocalImageLoader provides images) {
                SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                    var loggedIn by rememberSaveable { mutableStateOf(screen != "login") }
                    if (!loggedIn) S11Login { loggedIn = true }
                    else ContentNavigation(fixtureModel,
                        overview = { nav ->
                            var showCode by remember { mutableStateOf(false) }
                            Scaffold(topBar = { S11HomeBar() }) { padding ->
                                SchedaOverviewContent(modifier = Modifier.padding(padding), userName = "Matteo",
                                    scheda = fixtureModel.scheda.value, isLoading = false, isOffline = false,
                                    errorMessage = null, userCode = "AB12CD", isCodeVisible = showCode,
                                    isRequesting = false, requestError = null,
                                    onToggleCodeVisibility = { showCode = !showCode }, onRetry = {},
                                    onOpenDay = { nav.navigate("giorno/$it") }, onRequestNewWorkout = {})
                            }
                        }, alerts = { AvvisiContent(AlertsFeedUiState.Success(s06Alerts()), onRetry = {}) },
                        settings = { ImpostazioniContent("1.3.5", "31", onOpenLink = { true }, onLogout = { loggedIn = false }) },
                        exerciseActions = writes)
                }
            }
        }
    }
    private fun change(key: String, transform: (UserExerciseData) -> UserExerciseData) {
        val data = fixtureModel.userExerciseData.value.orEmpty().toMutableMap()
        data[key] = transform(data[key] ?: UserExerciseData())
        (fixtureModel.userExerciseData as MutableLiveData).value = data
    }
    override fun onDestroy() { super.onDestroy(); demoApp?.delete() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun S11HomeBar() { TopAppBar(title = { Text("Scheda") }) }

@Composable private fun S11Login(onSuccess: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var help by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(pending) {
        if (pending) {
            delay(700)
            pending = false
            when (code) {
                "AB12CD" -> onSuccess()
                "RETE" -> error = "Non riusciamo a verificare il codice. Controlla la connessione e riprova."
                else -> error = "Il codice non è valido. Controllalo e riprova."
            }
        }
    }
    LoginContent(code, error, pending, onCodeChange = { code = it; error = null },
        onSubmit = { if (code.isBlank()) error = "Inserisci il codice di accesso." else { focus.clearFocus(); keyboard?.hide(); pending = true } },
        onShowCodeInfo = { help = true })
    if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text("Codice di accesso") },
        text = { Text("Il codice viene fornito dal tuo trainer. Contattalo se non lo hai ancora ricevuto.") },
        confirmButton = { TextButton(onClick = { help = false }) { Text("Ho capito") } })
}
