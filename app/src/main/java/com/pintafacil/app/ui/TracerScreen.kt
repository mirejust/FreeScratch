package com.pintafacil.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pintafacil.app.detect.SurfaceDetector
import com.pintafacil.app.overlay.OverlayRenderer
import com.pintafacil.app.overlay.OverlayState
import com.pintafacil.app.overlay.SurfaceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pantalla 2: cámara en vivo + imagen de referencia superpuesta.
 *
 * Distribución (estilo AR Drawing):
 *  - barra superior: volver · título · "Hecho";
 *  - marco de selección siempre visible sobre la imagen;
 *  - flotantes: ojo (mantener para ocultar) arriba a la derecha,
 *    deshacer/rehacer abajo a la izquierda, presets de tamaño a la derecha;
 *  - hoja inferior con pestañas: Opacidad · Escalar · Trazar · Cámara · Flash.
 */
@Composable
fun TracerScreen(
    reference: ImageBitmap,
    onExit: () -> Unit,
    onNewImage: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val renderer = remember { OverlayRenderer() }
    var overlayState by remember {
        mutableStateOf(OverlayState(image = reference.asAndroidBitmap()))
    }
    var viewSize by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var showGuides by remember { mutableStateOf(false) }
    var mirrored by remember { mutableStateOf(false) }
    var flipToken by remember { mutableIntStateOf(0) }
    var detectMsg by remember { mutableStateOf<String?>(null) }

    // --- dock (hoja inferior) ---
    var dockTab by remember { mutableStateOf(DockTab.OPACITY) }
    var dockCollapsed by remember { mutableStateOf(false) }

    // --- estado nuevo: flash, congelar, ocultar ---
    var torchOn by remember { mutableStateOf(false) }
    var frozenFrame by remember { mutableStateOf<Bitmap?>(null) }
    var overlayHidden by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    // --- historial deshacer/rehacer ---
    var history by remember { mutableStateOf(listOf<OverlayState>()) }
    var future by remember { mutableStateOf(listOf<OverlayState>()) }
    var pendingGesture by remember { mutableStateOf<OverlayState?>(null) }
    var sliderPushed by remember { mutableStateOf(false) }

    val pushHistory: (OverlayState) -> Unit = { snapshot ->
        history = (history + snapshot).takeLast(40)
        future = emptyList()
    }
    val pushCurrent: () -> Unit = { pushHistory(overlayState) }
    val undo: () -> Unit = {
        history.lastOrNull()?.let { prev ->
            future = future + overlayState
            history = history.dropLast(1)
            overlayState = prev
            pendingGesture = null
        }
    }
    val redo: () -> Unit = {
        future.lastOrNull()?.let { next ->
            history = history + overlayState
            future = future.dropLast(1)
            overlayState = next
            pendingGesture = null
        }
    }
    // Primer cambio de un slider => guarda el estado previo para poder deshacer.
    val sliderStart: () -> Unit = {
        if (!sliderPushed) {
            pushCurrent()
            sliderPushed = true
        }
    }
    val sliderFinished: () -> Unit = { sliderPushed = false }
    // Cambio discreto (chip, botón) => guarda antes de aplicar.
    val applyState: (OverlayState) -> Unit = { s ->
        if (!sliderPushed) pushCurrent()
        overlayState = s
    }

    LaunchedEffect(reference) {
        val cx = viewSize?.first?.div(2f)
        val cy = viewSize?.second?.div(2f)
        // Si aún no sabemos el tamaño, se queda en (0,0): el renderer la centra.
        overlayState = if (cx != null && cy != null) {
            OverlayState(image = reference.asAndroidBitmap(), center = PointF(cx, cy))
        } else {
            OverlayState(image = reference.asAndroidBitmap())
        }
        history = emptyList()
        future = emptyList()
    }

    val detectRequests = remember { AtomicBoolean(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val scope = rememberCoroutineScope()
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    com.pintafacil.app.util.ImagePicker.load(context, uri)
                }
                if (bmp != null) onNewImage(bmp.asImageBitmap())
            }
        }
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // --- cámara ---
    DisposableEffect(hasPermission, flipToken, lifecycleOwner) {
        if (!hasPermission) return@DisposableEffect onDispose { }
        val futureProvider = ProcessCameraProvider.getInstance(context)
        var bound: ProcessCameraProvider? = null
        futureProvider.addListener({
            val provider = futureProvider.get()
            bound = provider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(analysisExecutor) { proxy ->
                        handleFrame(proxy, detectRequests) { detectMsg = it }
                    }
                }
            val selector = if (flipToken % 2 == 0) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            mirrored = flipToken % 2 != 0
            try {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                boundCamera = camera
                if (torchOn) {
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(true)
                    } else {
                        torchOn = false
                    }
                }
            } catch (e: Exception) {
                detectMsg = "No se pudo abrir la cámara"
            }
        }, context.mainExecutor)

        onDispose {
            boundCamera = null
            analysisExecutor.execute {
                try { bound?.unbindAll() } catch (_: Exception) {}
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    val toggleTorch = {
        val camera = boundCamera
        when {
            camera == null -> detectMsg = "La cámara aún no está lista"
            !camera.cameraInfo.hasFlashUnit() -> detectMsg = "Este teléfono no tiene flash"
            else -> {
                torchOn = !torchOn
                camera.cameraControl.enableTorch(torchOn)
                detectMsg = if (torchOn) "Flash encendido" else "Flash apagado"
            }
        }
    }

    val toggleFreeze = {
        if (frozenFrame != null) {
            frozenFrame = null
            detectMsg = "Cámara reanudada"
        } else {
            val snapshot = previewView.bitmap
            if (snapshot != null) {
                frozenFrame = snapshot
                showGuides = false
                detectMsg = "Vista congelada: la imagen queda anclada"
            } else {
                detectMsg = "La cámara aún no está lista"
            }
        }
    }

    val recenter = {
        val size = viewSize
        if (size != null) {
            pushCurrent()
            val cx = size.first / 2f
            val cy = size.second / 2f
            val dx = cx - overlayState.center.x
            val dy = cy - overlayState.center.y
            overlayState = overlayState.copy(
                center = PointF(cx, cy),
                corners = overlayState.corners?.map { PointF(it.x + dx, it.y + dy) },
            )
            detectMsg = "Imagen centrada"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Fotograma congelado: la imagen de referencia queda "anclada" al papel.
        frozenFrame?.let { snapshot ->
            Image(
                bitmap = snapshot.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (!overlayHidden) {
            OverlaySurface(
                state = overlayState,
                renderer = renderer,
                showGuides = showGuides,
                mirrored = mirrored,
                onSize = { w, h ->
                    if (viewSize == null || viewSize!!.first == 0f) {
                        viewSize = w to h
                        if (overlayState.center.x == 0f) {
                            overlayState = overlayState.copy(center = PointF(w / 2f, h / 2f))
                        }
                    }
                },
                onStateChanged = { s ->
                    overlayState = s
                    // Primer movimiento real del gesto: guarda el estado previo.
                    pendingGesture?.let { prev ->
                        pendingGesture = null
                        pushHistory(prev)
                    }
                },
                onDragStart = { pendingGesture = overlayState },
                onDragEnd = { showGuides = false },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // --- barra superior ---
        TracerTopBar(
            onBack = onExit,
            onDone = onExit,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        // --- mensajes de estado ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp, start = 70.dp, end = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            detectMsg?.let { msg ->
                Text(
                    msg,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (overlayHidden) {
                Text(
                    "Imagen oculta · mantén presionado el ojo para verla",
                    color = Color(0xFFFFD23F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .background(Color(0x99000000), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // --- ojo: mantener para ocultar (arriba a la derecha) ---
        HoldCircleButton(
            holding = overlayHidden,
            onHoldStart = { overlayHidden = true },
            onHoldEnd = { overlayHidden = false },
            contentDescription = "Mantén presionado para ocultar la imagen",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 60.dp, end = 12.dp),
        )

        // --- flotantes + hoja inferior ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                UndoRedoRow(
                    canUndo = history.isNotEmpty(),
                    canRedo = future.isNotEmpty(),
                    onUndo = undo,
                    onRedo = redo,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Mover / recentrar (icono de 4 flechas como la referencia)
                    CircleIconButton(
                        icon = Icons.Default.OpenWith,
                        desc = "Recentrar imagen",
                        enabled = true,
                        onClick = { recenter() },
                    )
                    ZoomPills(
                        size = overlayState.size,
                        onPreset = { s ->
                            pushCurrent()
                            overlayState = overlayState.copy(size = s)
                        },
                    )
                }
            }

            TracerBottomDock(
                tab = dockTab,
                onTab = { dockTab = it },
                collapsed = dockCollapsed,
                onToggleCollapse = { dockCollapsed = !dockCollapsed },
                opacity = overlayState.opacity,
                onOpacity = { v ->
                    sliderStart()
                    overlayState = overlayState.copy(opacity = v)
                },
                onOpacityCommit = sliderFinished,
                size = overlayState.size,
                onSize = { v ->
                    sliderStart()
                    overlayState = overlayState.copy(size = v)
                },
                onSizeCommit = sliderFinished,
                state = overlayState,
                onState = applyState,
                onSliderStart = sliderStart,
                onSliderFinished = sliderFinished,
                onDetect = {
                    pushCurrent()
                    detectRequests.set(true)
                    detectMsg = "Buscando el papel…"
                },
                onCorners = {
                    val size = viewSize ?: return@TracerBottomDock
                    val img = overlayState.image ?: return@TracerBottomDock
                    pushCurrent()
                    if (showGuides && overlayState.corners != null) {
                        overlayState = overlayState.copy(corners = null)
                        showGuides = false
                    } else {
                        val q = renderer.quadOf(overlayState, img, size.first, size.second)
                        overlayState = overlayState.copy(
                            corners = listOf(
                                PointF(q[0], q[1]),
                                PointF(q[2], q[3]),
                                PointF(q[4], q[5]),
                                PointF(q[6], q[7]),
                            )
                        )
                        showGuides = true
                    }
                },
                onReset = {
                    pushCurrent()
                    overlayState = overlayState.copy(
                        corners = null, tilt = 0f, turn = 0f, size = 1f,
                        opacity = 0.55f, surface = SurfaceMode.FLAT,
                    )
                    showGuides = false
                },
                torchOn = torchOn,
                onTorch = toggleTorch,
                frozen = frozenFrame != null,
                onFreeze = toggleFreeze,
                onFlip = { flipToken++ },
            )
        }
    }
}

/** Convierte el fotograma a grises y lanza la detección si se pidió. */
private fun handleFrame(
    proxy: ImageProxy,
    request: AtomicBoolean,
    publish: (String) -> Unit,
) {
    try {
        if (request.getAndSet(false)) {
            val plane = proxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val gray = SurfaceDetector.yPlaneToGray2(
                buffer, rowStride, proxy.width, proxy.height
            )
            val quad = SurfaceDetector.detect(gray, proxy.width, proxy.height)
            publish(
                if (quad != null) {
                    "Papel encontrado. Activa Esquinas para ajustarlo"
                } else {
                    "No vi un papel claro: más luz o toca Esquinas"
                }
            )
        }
    } catch (e: Exception) {
        publish("No se pudo analizar la imagen")
    } finally {
        proxy.close()
    }
}
