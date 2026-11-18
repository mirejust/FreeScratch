package com.pintafacil.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pintafacil.app.overlay.OverlayState
import com.pintafacil.app.overlay.SurfaceMode

/**
 * Pestaña "Trazar": superficie, inclinación, giro y herramientas
 * de detección. Estilo claro dentro de la hoja inferior.
 */
@Composable
fun ToolsTab(
    state: OverlayState,
    onState: (OverlayState) -> Unit,
    onSliderStart: () -> Unit,
    onSliderFinished: () -> Unit,
    onDetect: () -> Unit,
    onCorners: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Superficie",
            color = Ui.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SurfaceMode.entries.forEach { mode ->
                val selected = state.surface == mode
                FilterChip(
                    selected = selected,
                    onClick = { onState(state.copy(surface = mode)) },
                    label = { Text(mode.label(), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Ui.Accent,
                        selectedLabelColor = Color.White,
                        labelColor = Ui.Ink,
                    ),
                )
            }
        }

        LabeledSlider(
            label = "Inclinación",
            value = (state.tilt + 60f) / 120f,
            display = "${Math.round(state.tilt)}°",
            onStart = onSliderStart,
            onFinish = onSliderFinished,
        ) {
            onState(state.copy(tilt = it * 120f - 60f))
        }

        LabeledSlider(
            label = "Giro",
            value = (state.turn + 180f) / 360f,
            display = "${Math.round(state.turn)}°",
            onStart = onSliderStart,
            onFinish = onSliderFinished,
        ) {
            onState(state.copy(turn = it * 360f - 180f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolButton("Detectar", Icons.Default.MyLocation, Modifier.weight(1f), onDetect)
            ToolButton("Esquinas", Icons.Default.Refresh, Modifier.weight(1f), onCorners)
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Reiniciar todo", maxLines = 1, color = Ui.Ink) }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    display: String,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = Ui.InkDim,
            fontSize = 13.sp,
            modifier = Modifier.width(78.dp),
        )
        Slider(
            value = value,
            onValueChange = { onStart(); onChange(it) },
            onValueChangeFinished = onFinish,
            valueRange = 0f..1f,
            colors = sliderColors(),
            modifier = Modifier.weight(1f),
        )
        Text(
            display,
            color = Ui.InkDim,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(44.dp),
        )
    }
}

@Composable
private fun ToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ui.Accent),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.height(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun SurfaceMode.label(): String = when (this) {
    SurfaceMode.FLAT -> "Plano"
    SurfaceMode.CUP -> "Vaso"
    SurfaceMode.ROLLER -> "Rodillo"
    SurfaceMode.BALL -> "Pelota"
}
