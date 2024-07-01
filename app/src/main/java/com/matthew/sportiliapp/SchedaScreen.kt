package com.matthew.sportiliapp
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda

@Composable
fun SchedaScreen() {
    val context = LocalContext.current
    var scheda by remember { mutableStateOf<Scheda?>(null) }

    val database = FirebaseDatabase.getInstance()

    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("code", "") ?: ""
        val userId = savedCode
        val schedaRef = database.reference.child("users").child(userId).child("scheda")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(Scheda::class.java)
                scheda = data
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore in base alle tue esigenze
            }
        }
        schedaRef.addListenerForSingleValueEvent(listener)
    }

    // Se i dati non sono ancora stati recuperati, mostra un progresso
    if (scheda == null) {
       Text(text = "No")
    } else {
        // Mostra i dati recuperati
        SchedaContent(scheda!!)
    }
}

@Composable
fun SchedaContent(scheda: Scheda) {
    Column(modifier = Modifier
        .padding(16.dp)
        .fillMaxSize()) {

        // Visualizza i dettagli della scheda
        Text("Data Inizio: ${scheda.dataInizio}")
        Text("Durata: ${scheda.durata} settimane")

        // Itera sui giorni della scheda
        scheda.giorni.forEach { giorno ->
            GiornoItem(giorno = giorno.value)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GiornoItem(giorno: Giorno) {
    Column {
        Text("Giorno: ${giorno.name}")

        // Itera sui gruppi muscolari del giorno
        giorno.gruppiMuscolari.forEach { gruppo ->
            GruppoMuscolareItem(gruppo = gruppo.value)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun GruppoMuscolareItem(gruppo: GruppoMuscolare) {
    Column {
        Text("Gruppo Muscolare: ${gruppo.nome}")

        // Itera sugli esercizi del gruppo muscolare
        gruppo.esercizi.forEach { esercizio ->
            EsercizioItem(esercizio = esercizio.value)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun EsercizioItem(esercizio: Esercizio) {
    Column {
        Text("Esercizio: ${esercizio.name}")
        Text("Serie: ${esercizio.serie}")
        Text("Riposo: ${esercizio.riposo ?: ""}")
        Text("Note PT: ${esercizio.notePT ?: ""}")
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSchedaScreen() {
    SchedaScreen()
}
