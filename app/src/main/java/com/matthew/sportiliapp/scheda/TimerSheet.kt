package com.matthew.sportiliapp.scheda

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TimerSheet(riposo: String) {
    var timeRemaining by remember { mutableIntStateOf(parseRiposo(riposo)) }
    val totalTime = parseRiposo(riposo)  // Usa il valore iniziale come tempo totale
    var timerIsActive by remember { mutableStateOf(false) }
    var timerPaused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Aggiorna il timer in tempo reale quando è attivo
    LaunchedEffect(timerIsActive, timeRemaining) {
        if (timerIsActive && timeRemaining > 0) {
            while (timeRemaining > 0) {
                delay(1000L)
                timeRemaining -= 1
            }
            if (timeRemaining == 0) {
                playSound(context)
                triggerVibration(context)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center  // Centra verticalmente il contenuto
    ) {
        Text("Tempo di Recupero", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Animazione circolare che mostra il progresso
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularProgressIndicator(
                progress = (totalTime - timeRemaining) / totalTime.toFloat(),
                strokeWidth = 10.dp,
                modifier = Modifier.size(200.dp)
            )
            Text(formatTime(timeRemaining), style = MaterialTheme.typography.headlineLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pulsanti per avviare, fermare o riprendere il timer
        if (timerIsActive) {
            Button(onClick = { timerIsActive = false; timerPaused = true }) {
                Text("Stop")
            }
        } else if (timerPaused) {
            Button(onClick = { timerIsActive = true; timerPaused = false }) {
                Text("Riprendi")
            }
        } else {
            Button(onClick = { timerIsActive = true }) {
                Text("Inizia")
            }
        }
    }
}

// Funzione per parsare il tempo di riposo
fun parseRiposo(riposo: String): Int {
    val parts = riposo.split("'")
    val minutes = parts[0].toIntOrNull() ?: 0
    val seconds = parts.getOrNull(1)?.replace("\"", "")?.toIntOrNull() ?: 0
    return minutes * 60 + seconds
}

// Funzione per formattare il tempo in mm:ss
fun formatTime(time: Int): String {
    val minutes = time / 60
    val seconds = time % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// Funzione per attivare la vibrazione
fun triggerVibration(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(500)
    }
}

// Funzione per riprodurre il suono
fun playSound(context: Context) {
    val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val ringtone = RingtoneManager.getRingtone(context, sound)
    ringtone.play()
}
