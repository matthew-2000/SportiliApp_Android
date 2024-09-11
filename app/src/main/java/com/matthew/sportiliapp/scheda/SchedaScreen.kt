package com.matthew.sportiliapp.scheda
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.SchedaViewModelFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


fun NavController.navigate(
    route: String,
    args: Bundle,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null
) {
    val nodeId = graph.findNode(route = route)?.id
    if (nodeId != null) {
        navigate(nodeId, args, navOptions, navigatorExtras)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedaScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SchedaViewModel = viewModel(factory = SchedaViewModelFactory(context))
    val scheda by viewModel.scheda.observeAsState()
    val nomeUtente by viewModel.name.observeAsState()

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
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .padding(all = 16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                "Inizio: ${convertDateTime(scheda!!.dataInizio)} ",
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
                        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                    }

                    if (!scheda!!.isSchedaValida()) {
                        item {
                            Text(
                                "Scheda scaduta!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(scheda!!.giorni.entries.toList()) { (key, giorno) ->
                        GiornoItem(giorno) {
                            val bundle = Bundle().apply {
                                putParcelable("giorno", giorno)
                            }
                            navController.navigate(
                                route = "giorno",
                                args = bundle
                            )
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
        .padding(vertical = 16.dp)) {
        Text(giorno.name, style = MaterialTheme.typography.titleMedium)
        Text(getGruppiString(giorno), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Divider(color = Color.LightGray, thickness = 1.dp)
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

    var isFirst = true
    for ((key, gruppo) in giorno.gruppiMuscolari) {
        if (!isFirst) {
            s += ", "
        }
        s += gruppo.nome
        isFirst = false
    }

    return s
}



@Preview(showBackground = true)
@Composable
fun PreviewSchedaScreen() {
    SchedaScreen(navController = rememberNavController())
}
