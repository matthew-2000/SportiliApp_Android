package com.matthew.sportiliapp

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ContentScreen() {
    val context = LocalContext.current
    val code = remember { mutableStateOf("") }

    // Recupera il codice dalle SharedPreferences quando la Composable si avvia
    LaunchedEffect(key1 = Unit) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("code", "") ?: ""
        code.value = savedCode
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Il codice salvato nelle SharedPreferences è:",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = code.value,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
