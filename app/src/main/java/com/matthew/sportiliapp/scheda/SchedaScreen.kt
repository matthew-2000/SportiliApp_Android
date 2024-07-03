package com.matthew.sportiliapp.scheda
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedaScreen(navController: NavHostController) {
    var scheda by remember { mutableStateOf<Scheda?>(null) }
    val context = LocalContext.current
    var nomeUtente by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("code", "") ?: ""
        val database = FirebaseDatabase.getInstance()
        val schedaRef = database.reference.child("users").child(savedCode).child("scheda")

        schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(Scheda::class.java)
                scheda = data
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        nomeUtente = FirebaseAuth.getInstance().currentUser?.displayName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getTitle(nomeUtente)) }
            )
        },
        content = { padding ->
            if (scheda == null) {
                Text(
                    "No",
                    modifier = Modifier.fillMaxSize(),
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                "Inizio: ${convertDateTime(scheda!!.dataInizio) }",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "x${scheda!!.durata} sett.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(scheda!!.giorni.entries.toList()) { (key, giorno) ->
                        GiornoItem(giorno) {
                            //navController.navigate("giorno")
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun GiornoItem(giorno: Giorno, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 8.dp)) {
        Text("Giorno: ${giorno.name}", style = MaterialTheme.typography.titleSmall)
        Text(getGruppiString(giorno), style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    }
}

fun getTitle(nomeUtente: String?): String {
    return if (nomeUtente != null) {
        "Ciao $nomeUtente"
    } else {
        "Home"
    }
}

fun convertDateTime(inputDateTime: String): String {
    // Formato di input della data e dell'ora
    val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
    // Formato di output desiderato per la data
    val formatterOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)

    // Parsing della stringa di input nella classe LocalDateTime
    val localDateTime = LocalDateTime.parse(inputDateTime, formatterInput)

    // Formattazione della data secondo il formato desiderato
    return localDateTime.format(formatterOutput)
}

fun getGruppiString(giorno: Giorno): String {
    var s = ""

    for (gruppo in giorno.gruppiMuscolari) {
        s += gruppo.value.nome + ", "
    }

    s.removeSuffix(", ")

    return s
}

@Preview(showBackground = true)
@Composable
fun PreviewSchedaScreen() {
    SchedaScreen(navController = rememberNavController())
}
