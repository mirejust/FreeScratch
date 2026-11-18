package com.pintafacil.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pintafacil.app.R
import com.pintafacil.app.i18n.T
import com.pintafacil.app.overlay.OverlayState

/** Colores del dock (parte inferior clara, estilo AR Drawing). */
object Ui {
    val Ink = Color(0xFF1B1035)
    val InkDim = Color(0xFF6B6480)
    val Sheet = Color.White
    val Accent = Color(0xFFE8452F)
    val AccentSoft = Color(0xFFF1F1F5)
    val Dark = Color(0xB3140B26)
    val Yellow = Color(0xFFFFD23F)
}

/** Pestañas del dock; las etiquetas siguen el idioma activo (T.lang). */
enum class DockTab {
    OPACITY,
    SCALING,
    TOOLS,
    CAMERA,
    FLASH;

    fun label(): String = when (this) {
        OPACITY -> T.tabOpacity
        SCALING -> T.tabScaling
        TOOLS -> T.tabTools
        CAMERA -> T.tabCamera
        FLASH -> T.tabFlash
    }
}

/**
 * Barra superior de la cámara: solo "volver" y "Hecho", ambos en modo
 * desvanecido para no distraer al trazar (sin nombre de la app).
 */
@Composable
fun TracerTopBar(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Ui.Dark)
            .statusBarsPadding()
            .height(52.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = T.exit,
                tint = Color.White.copy(alpha = 0.45f),
            )
        }

        TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
            Text(
                T.done,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
    }
}

/** Botón circular que solo actúa mientras se mantiene presionado (ojo). */
@Composable
fun HoldCircleButton(
    holding: Boolean,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(if (holding) Ui.Yellow else Ui.Dark, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHoldStart()
                        try {
                            awaitRelease()
                        } finally {
                            onHoldEnd()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Visibility,
            contentDescription = contentDescription,
            tint = if (holding) Ui.Ink else Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Deshacer / rehacer (abajo a la izquierda, sobre la hoja). */
@Composable
fun UndoRedoRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Ui.Dark, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CircleIconButton(Icons.Default.Undo, T.undo, canUndo, onUndo)
        CircleIconButton(Icons.Default.Redo, T.redo, canRedo, onRedo)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    desc: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Presets de tamaño a la derecha, sobre la hoja (0.5x / 1.0x / 1.5x). */
@Composable
fun ZoomPills(
    size: Float,
    onPreset: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Ui.Dark, RoundedCornerShape(20.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ZoomPill("0.5x", kotlin.math.abs(size - 0.5f) < 0.01f) { onPreset(0.5f) }
        ZoomPill("1.0x", kotlin.math.abs(size - 1f) < 0.01f) { onPreset(1f) }
        ZoomPill("1.5x", kotlin.math.abs(size - 1.5f) < 0.01f) { onPreset(1.5f) }
    }
}

@Composable
private fun ZoomPill(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Ui.Accent else Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (active) Color.White else Ui.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Hoja inferior con pestañas + barra de pestañas:
 * Opacidad · Escalar · Trazar · Cámara · Flash.
 */
@Composable
fun TracerBottomDock(
    tab: DockTab,
    onTab: (DockTab) -> Unit,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    opacity: Float,
    onOpacity: (Float) -> Unit,
    onOpacityCommit: () -> Unit,
    size: Float,
    onSize: (Float) -> Unit,
    onSizeCommit: () -> Unit,
    state: OverlayState,
    onState: (OverlayState) -> Unit,
    onSliderStart: () -> Unit,
    onSliderFinished: () -> Unit,
    onDetect: () -> Unit,
    onCorners: () -> Unit,
    onReset: () -> Unit,
    torchOn: Boolean,
    onTorch: () -> Unit,
    frozen: Boolean,
    onFreeze: () -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hoja blanca
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ui.Sheet, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IconButton(onClick = onToggleCollapse) {
                    Icon(
                        if (collapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (collapsed) T.showSettings else T.hideSettings,
                        tint = Ui.Ink,
                    )
                }
            }

            if (!collapsed) {
                when (tab) {
                    DockTab.OPACITY -> {
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            Text(
                                T.tabOpacity,
                                color = Ui.Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = opacity,
                                    onValueChange = onOpacity,
                                    onValueChangeFinished = onOpacityCommit,
                                    valueRange = 0.05f..1f,
                                    colors = sliderColors(),
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.Default.Opacity, contentDescription = null, tint = Ui.Ink)
                            }
                        }
                    }

                    DockTab.SCALING -> {
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            Text(
                                T.sizeTitle,
                                color = Ui.Ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = size,
                                    onValueChange = onSize,
                                    onValueChangeFinished = onSizeCommit,
                                    valueRange = 0.15f..3f,
                                    colors = sliderColors(),
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Ui.Ink)
                            }
                        }
                    }

                    DockTab.TOOLS -> ToolsTab(
                        state = state,
                        onState = onState,
                        onSliderStart = onSliderStart,
                        onSliderFinished = onSliderFinished,
                        onDetect = onDetect,
                        onCorners = onCorners,
                        onReset = onReset,
                    )

                    DockTab.CAMERA -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DockAction(T.flip, Icons.Default.FlipCameraAndroid, Modifier.weight(1f), onFlip)
                            DockAction(
                                if (frozen) T.resume else T.freeze,
                                if (frozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                                Modifier.weight(1f),
                                onFreeze,
                            )
                        }
                    }

                    DockTab.FLASH -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DockAction(
                                if (torchOn) T.flashTurnOff else T.flashTurnOn,
                                if (torchOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                                Modifier.weight(1f),
                                onTorch,
                            )
                            Text(
                                if (torchOn) T.stateOn else T.stateOff,
                                color = Ui.InkDim,
                                fontSize = 13.sp,
                                modifier = Modifier.width(72.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // Barra de pestañas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ui.Sheet)
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
        ) {
            DockTab.entries.forEach { t ->
                TabItem(
                    tab = t,
                    selected = t == tab,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (collapsed) onToggleCollapse()
                        onTab(t)
                    },
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: DockTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (selected) Ui.Accent else Ui.InkDim
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(tab.icon(), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            tab.label(),
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

private fun DockTab.icon(): ImageVector = when (this) {
    DockTab.OPACITY -> Icons.Default.Opacity
    DockTab.SCALING -> Icons.Default.AspectRatio
    DockTab.TOOLS -> Icons.Default.Brush
    DockTab.CAMERA -> Icons.Default.PhotoCamera
    DockTab.FLASH -> Icons.Default.FlashOn
}

@Composable
private fun DockAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ui.AccentSoft)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Ui.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Ui.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun sliderColors() = SliderDefaults.colors(
    thumbColor = Ui.Accent,
    activeTrackColor = Ui.Accent,
    inactiveTrackColor = Color(0xFFE4E2EC),
)
