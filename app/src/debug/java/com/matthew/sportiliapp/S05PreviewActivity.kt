package com.matthew.sportiliapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.intercept.Interceptor
import coil.request.ErrorResult
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.scheda.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

/** Real detail and sheets with controlled local write callbacks. Never connects to production. */
class S05PreviewActivity : ComponentActivity() {
    internal lateinit var fixtureModel: SchedaViewModel
    internal var writeOutcome = "success"
    internal val writeKeys = mutableListOf<String>()
    internal var pending: (() -> Unit)? = null
    internal var failedPending: (() -> Unit)? = null
    private var demoApp: FirebaseApp? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        writeOutcome = intent.getStringExtra("outcome") ?: "success"
        val options = FirebaseOptions.Builder().setApplicationId("1:123456789:android:s05")
            .setApiKey("fake-local-key").setProjectId("demo-sportili-s05")
            .setDatabaseUrl("https://demo-sportili-s05.firebaseio.com").build()
        val app = FirebaseApp.initializeApp(this, options, "s05-${System.nanoTime()}")
        demoApp = app
        val db = FirebaseDatabase.getInstance(app).apply { goOffline() }
        val prefs = getSharedPreferences("s05-local-fixture", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        fixtureModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SchedaViewModel(this@S05PreviewActivity, db, prefs) as T
            }
        })[SchedaViewModel::class.java]
        val workout = s04Workout()
        if (intent.getBooleanExtra("noRest", false)) {
            workout.giorni["day-z"]!!.gruppiMuscolari["group-a"]!!.esercizi["single"]!!.riposo = null
        }
        (fixtureModel.scheda as MutableLiveData).value = workout
        val count = intent.getIntExtra("count", 12)
        val data = s04Data().toMutableMap()
        data["plank"] = UserExerciseData(if (intent.getBooleanExtra("emptyNote", false)) null else s05LongNote,
            (0 until count).associate { index -> "log-$index" to WeightLogEntry(20.0 + index * 2.5,
                1_700_000_000_000L + index.toLong() * index * 86_400_000L) })
        (fixtureModel.userExerciseData as MutableLiveData).value = data
        fixtureModel.isLoading.value = false
        val writes = ExerciseDetailActions(
            addWeight = { key, weight, success, failure ->
                completeWrite(key, {
                    val entry = WeightLogEntry(weight, 1_790_000_000_000L)
                    changeData(key) { old -> old.copy(weightLogs = old.weightLogs.orEmpty() + ("new-log" to entry)) }
                    success("new-log", entry)
                }, failure)
            },
            updateWeight = { key, id, weight, success, failure ->
                completeWrite(key, {
                    val entry = WeightLogEntry(weight, 1_790_000_000_000L)
                    changeData(key) { old -> old.copy(weightLogs = old.weightLogs.orEmpty() + (id to entry)) }
                    success(entry)
                }, failure)
            },
            updateNote = { key, note, success, failure -> completeWrite(key, {
                changeData(key) { it.copy(noteUtente = note) }; success()
            }, failure) }
        )
        val images = ImageLoader.Builder(this).components {
            add(Interceptor { chain -> ErrorResult(null, chain.request, IllegalStateException("Local image fixture")) })
        }.build()
        setContent {
            CompositionLocalProvider(LocalImageLoader provides images) {
                SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                    EsercizioScreen(rememberNavController(), "day-z", "group-a",
                        intent.getStringExtra("exercise") ?: "single", fixtureModel, actions = writes)
                }
            }
        }
    }
    private fun changeData(key: String, transform: (UserExerciseData) -> UserExerciseData) {
        val data = fixtureModel.userExerciseData.value.orEmpty().toMutableMap()
        data[key] = transform(data[key] ?: UserExerciseData())
        (fixtureModel.userExerciseData as MutableLiveData).value = data
    }
    private fun completeWrite(key: String, success: () -> Unit, failure: (String) -> Unit) {
        writeKeys += key
        when (writeOutcome) {
            "failure" -> failure("Salvataggio non riuscito. Riprova.")
            "pending" -> { pending = success; failedPending = { failure("Salvataggio non riuscito. Riprova.") } }
            else -> success()
        }
    }
    override fun onDestroy() { super.onDestroy(); demoApp?.delete() }
}

internal val s05LongNote = (1..8).joinToString("\n") { "Nota $it: mantieni il controllo del movimento e registra le sensazioni della serie." }
