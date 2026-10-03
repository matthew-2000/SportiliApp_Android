package com.matthew.sportiliapp

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavType
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.scheda.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.awaitCancellation

/** Real day/detail screens and routes, in-memory data, offline demo SDK and intercepted images. */
class S04PreviewActivity : ComponentActivity() {
    internal lateinit var fixtureModel: SchedaViewModel
    internal val requestedImages = mutableListOf<String>()
    private var demoApp: FirebaseApp? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "day"
        val imageState = intent.getStringExtra("image") ?: "error"
        val options = FirebaseOptions.Builder().setApplicationId("1:123456789:android:s04")
            .setApiKey("fake-local-key").setProjectId("demo-sportili-s04")
            .setDatabaseUrl("https://demo-sportili-s04.firebaseio.com").build()
        val app = FirebaseApp.initializeApp(this, options, "s04-${System.nanoTime()}")
        demoApp = app
        val db = FirebaseDatabase.getInstance(app).apply { goOffline() }
        val prefs = getSharedPreferences("s04-local-fixture", Context.MODE_PRIVATE)
        prefs.edit().clear().commit() // Isolated; no signed-in code, subscriptions or write targets.
        fixtureModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SchedaViewModel(this@S04PreviewActivity, db, prefs) as T
            }
        })[SchedaViewModel::class.java]
        (fixtureModel.scheda as MutableLiveData).value = s04Workout()
        (fixtureModel.userExerciseData as MutableLiveData).value = s04Data()
        fixtureModel.isLoading.value = false
        val images = ImageLoader.Builder(this).components {
            add(Interceptor { chain ->
                val url = chain.request.data.toString()
                synchronized(requestedImages) { requestedImages += url }
                when (imageState) {
                    "loading" -> awaitCancellation()
                    "success" -> SuccessResult(s04Image(this@S04PreviewActivity, url), chain.request, DataSource.MEMORY)
                    else -> ErrorResult(null, chain.request, IllegalStateException("Local missing image"))
                }
            })
        }.build()
        setContent {
            CompositionLocalProvider(LocalImageLoader provides images) {
                SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = if (screen == "day") "giorno" else "preview") {
                        composable("giorno") { GiornoScreen(nav, "day-z", fixtureModel) }
                        composable("preview") { EsercizioScreen(nav, "day-z", "group-a", screen, fixtureModel) }
                        composable("esercizio/{giornoId}/{gruppoId}/{esercizioId}", arguments = listOf("giornoId", "gruppoId", "esercizioId").map { name -> navArgument(name) { type = NavType.StringType } }) { entry ->
                            EsercizioScreen(nav, entry.arguments!!.getString("giornoId")!!,
                                entry.arguments!!.getString("gruppoId")!!, entry.arguments!!.getString("esercizioId")!!, fixtureModel)
                        }
                    }
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        demoApp?.delete()
    }
}

internal val s04Names = listOf("Plank", "Distensioni su panca inclinata con manubri e controllo della fase eccentrica", "Élite")
internal fun s04Workout(): Scheda {
    val exercises = linkedMapOf(
        "single" to Esercizio(s04Names[0], "3 × 40 secondi", riposo = "1:30", notePT = "Mantieni il bacino stabile e respira durante tutta la serie."),
        "double" to Esercizio(s04Names.take(2).joinToString(" + "), "3 × 40 secondi + 4 × 8–10", riposo = "1:30", notePT = "Esegui entrambe le parti, poi recupera."),
        "triple" to Esercizio(s04Names.joinToString(" + "), "3 × 40 secondi + 4 × 8–10 + 3 × 12", riposo = "1:30", notePT = "Esegui tutte e tre le parti, poi recupera."),
        "long" to Esercizio(s04Names[1], "4 × 8–10", riposo = "1:30", notePT = "Controlla la fase eccentrica.")
    )
    val groups = linkedMapOf("group-a" to GruppoMuscolare("Petto e stabilità", exercises))
    return Scheda("2026-10-01T08:00:00+0200", 6,
        giorni = linkedMapOf("day-z" to Giorno("Giorno A · Spinta", groups)))
}

internal fun s04Data() = mapOf(
    "plank" to UserExerciseData("Nota della parte 1", mapOf("p1" to WeightLogEntry(10.0, 1_700_000_000_000))),
    ExerciseDataKey.canonical(s04Names[1]) to UserExerciseData("Nota della parte 2", mapOf("p2" to WeightLogEntry(20.0, 1_700_000_000_000))),
    "lite" to UserExerciseData("Nota della parte 3 · chiave legacy", mapOf("p3" to WeightLogEntry(30.0, 1_700_000_000_000)))
)

private fun s04Image(context: Context, name: String): BitmapDrawable {
    val bitmap = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).apply {
        drawColor(Color.rgb(242, 229, 218))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(155, 74, 0); strokeWidth = 36f }
        val offset = if (name.contains("Plank")) 0f else 80f
        drawLine(240f + offset, 250f, 480f + offset, 250f, paint)
        drawRect(210f + offset, 165f, 250f + offset, 335f, paint)
        drawRect(470f + offset, 165f, 510f + offset, 335f, paint)
    }
    return BitmapDrawable(context.resources, bitmap)
}
