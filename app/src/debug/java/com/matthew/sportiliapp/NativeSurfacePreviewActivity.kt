package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.matthew.sportiliapp.nativefeatures.WorkoutSurface
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.scheda.TimerSheet
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

/** Debug-only local screen; never instantiates a workout repository. */
class NativeSurfacePreviewActivity : ComponentActivity() {
    private var previousCode: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("shared", MODE_PRIVATE)
        previousCode = prefs.getString("code", null)
        prefs.edit().putString("code", "LOCAL_NATIVE_PREVIEW").commit()
        setContent {
            SportiliAppTheme {
                Column {
                    Button(onClick = { WorkoutSurface.save(this@NativeSurfacePreviewActivity, "local-day", Giorno(name = "Giorno A")) }) { Text("Scegli Giorno A per il widget") }
                    TimerSheet("90")
                }
            }
        }
    }
    override fun onDestroy() {
        WorkoutSurface.clear(this)
        getSharedPreferences("shared", MODE_PRIVATE).edit().apply {
            previousCode?.let { putString("code", it) } ?: remove("code")
        }.commit()
        super.onDestroy()
    }
}
