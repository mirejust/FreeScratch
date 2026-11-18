package com.pintafacil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pintafacil.app.i18n.T
import com.pintafacil.app.ui.AppRoot
import com.pintafacil.app.ui.HomeScreen
import com.pintafacil.app.ui.TracerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        T.load(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFFFD23F),
                secondary = Color(0xFFE8452F),
                background = Color(0xFF1B1035),
                surface = Color(0xFF251748),
            )) {
                PintaFacilApp()
            }
        }
    }
}

@Composable
private fun PintaFacilApp() {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        AppRoot(Modifier.padding(padding))
    }
}
