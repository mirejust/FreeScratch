package com.pintafacil.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Raíz de navegación: pantalla de preparación -> trazador.
 */
@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    var reference by remember { mutableStateOf<ImageBitmap?>(null) }
    var tracing by remember { mutableStateOf(false) }

    if (tracing && reference != null) {
        TracerScreen(
            reference = reference!!,
            onExit = { tracing = false },
            onNewImage = { reference = it },
            modifier = modifier,
        )
    } else {
        HomeScreen(
            reference = reference,
            onReference = { reference = it },
            onStart = { if (reference != null) tracing = true },
            modifier = modifier,
        )
    }
}
