package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SportiliAppTheme {
                AppNavHost()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    SportiliAppTheme {
        AppNavHost()
    }
}