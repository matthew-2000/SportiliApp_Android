package com.matthew.sportiliapp

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.matthew.sportiliapp.nativefeatures.RestTimer
import com.matthew.sportiliapp.nativefeatures.WorkoutSurface
import com.matthew.sportiliapp.nativefeatures.WorkoutWidget
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.scheda.TimerSheet
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

/** Debug-only local screen; never instantiates a workout repository. */
class NativeSurfacePreviewActivity : ComponentActivity() {
    private var previousCode: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("shared", MODE_PRIVATE)
        previousCode = prefs.getString("code", null)?.takeUnless { it == "LOCAL_NATIVE_PREVIEW" }
        prefs.edit().putString("code", "LOCAL_NATIVE_PREVIEW").commit()
        WorkoutSurface.save(this, "local-day", Giorno(name = "Giorno A", gruppiMuscolari = mapOf(
            "local-group" to GruppoMuscolare("Spinta", (1..8).associate { "local-$it" to Esercizio("Esercizio $it", "3 x 10") })
        )))
        setContent {
            SportiliAppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    var widgets by rememberSaveable { mutableStateOf(intent.getBooleanExtra("widgets", false)) }
                    Column {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { widgets = !widgets }) { Text(if (widgets) "Timer" else "Widget") }
                            OutlinedButton(onClick = { RestTimer.start(this@NativeSurfacePreviewActivity, 5, exerciseName = "Panca piana") }) { Text("Avviso tra 5 sec") }
                        }
                        if (widgets) {
                            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(false, true).forEach { dark ->
                                    Text(if (dark) "Widget scuro" else "Widget chiaro")
                                    WidgetPreview(dark, compact = false)
                                }
                                Text("Formato compatto")
                                WidgetPreview(dark = false, compact = true)
                                Button(onClick = {
                                    val manager = AppWidgetManager.getInstance(this@NativeSurfacePreviewActivity)
                                    if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(
                                        ComponentName(this@NativeSurfacePreviewActivity, WorkoutWidget::class.java), null, null)
                                }) { Text("Aggiungi alla Home") }
                            }
                        } else TimerSheet("90", exerciseName = "Panca piana")
                    }
                }
            }
        }
    }

    @Composable private fun WidgetPreview(dark: Boolean, compact: Boolean) {
        AndroidView(factory = { context ->
            val config = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            }
            val themed = context.createConfigurationContext(config)
            WorkoutWidget().createViews(themed, if (compact) 9002 else 9001, compact).apply(themed, null)
        }, modifier = Modifier.width(300.dp).height(if (compact) 76.dp else 168.dp))
    }

    override fun onDestroy() {
        WorkoutSurface.clear(this)
        getSharedPreferences("shared", MODE_PRIVATE).edit().apply {
            previousCode?.let { putString("code", it) } ?: remove("code")
        }.commit()
        super.onDestroy()
    }
}
