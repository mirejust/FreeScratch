package com.pintafacil.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.pintafacil.app.R
import com.pintafacil.app.util.ImagePicker
import com.pintafacil.app.util.PixabayApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BgTop = Color(0xFF241447)
private val BgBottom = Color(0xFF150C2B)
private val CardBg = Color(0xFF251748)
private val Accent = Color(0xFFE8452F)
private val Yellow = Color(0xFFFFD23F)
private val TextDim = Color(0xFFB9B2D9)
private val Gradient = listOf(Color(0xFFFF8A3D), Color(0xFFFFDE59))

/** De dónde sale la imagen de referencia. */
private enum class Source { PHONE, PIXABAY }

/**
 * Pantalla 1 (inicio): identidad de "Free Scratch" y elección de la
 * imagen de referencia, desde el teléfono o desde plantillas vectoriales
 * de Pixabay.
 */
@Composable
fun HomeScreen(
    reference: ImageBitmap?,
    onReference: (ImageBitmap) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var notice by remember { mutableStateOf<String?>(null) }   // avisos (verde/amarillo)
    var error by remember { mutableStateOf<String?>(null) }    // errores (rojo)

    // --- fuente de la imagen ---
    var source by remember { mutableStateOf(Source.PHONE) }

    // --- Pixabay ---
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PixabayApi.Hit>>(emptyList()) }
    var selectedHit by remember { mutableStateOf<PixabayApi.Hit?>(null) }
    var searching by remember { mutableStateOf(false) }
    var usingVector by remember { mutableStateOf(false) }

    // --- Paginación ---
    var page by remember { mutableStateOf(1) }
    var totalHits by remember { mutableStateOf(0) }
    var loadingMore by remember { mutableStateOf(false) }
    var exhausted by remember { mutableStateOf(false) }  // no quedan páginas
    val gridState = rememberLazyGridState()

    /** ¿Quedan más resultados por cargar? */
    val hasMore = results.size < totalHits && !exhausted

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val bmp = ImagePicker.load(context, uri)
            if (bmp != null) {
                onReference(bmp.asImageBitmap())
                notice = "Imagen lista para trazar ✓"
                error = null
            } else {
                error = "No se pudo abrir esa imagen"
            }
        }
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStart() else error = "Necesito la cámara para trazar sobre tu papel"
    }

    /**
     * Carga la página [page] de la búsqueda actual y la añade a los
     * resultados (deduplicando por id).
     */
    suspend fun fetchPage(q: String, pageToLoad: Int) {
        val res = withContext(Dispatchers.IO) {
            try {
                PixabayApi.search(q, page = pageToLoad, perPage = 60)
            } catch (e: Exception) {
                null
            }
        }
        if (res == null) {
            if (pageToLoad == 1) error = "No se pudo buscar: revisa tu conexión"
            else error = "Sin conexión: no se pudo cargar más"
            return
        }
        totalHits = minOf(res.totalHits, PixabayApi.MAX_RESULTS)
        if (pageToLoad == 1) {
            if (res.hits.isEmpty()) {
                error = "Sin resultados para \"$q\""
                results = emptyList()
            } else {
                results = res.hits
                gridState.scrollToItem(0)
            }
        } else {
            val known = results.map { it.id }.toHashSet()
            val fresh = res.hits.filter { it.id !in known }
            // Si la API devolvió menos de una página completa o todo era
            // repetido, no hay más páginas que pedir.
            if (res.hits.size < 60 || fresh.isEmpty()) exhausted = true
            results = results + fresh
        }
        page = pageToLoad
    }

    val doSearch: () -> Unit = {
        val q = query.trim()
        when {
            q.isEmpty() -> error = "Escribe qué quieres buscar"
            !PixabayApi.isConfigured ->
                error = "Falta la clave de Pixabay: copia apikeys.properties.example a apikeys.properties y compila de nuevo"
            else -> {
                error = null
                notice = null
                selectedHit = null
                results = emptyList()
                totalHits = 0
                page = 1
                loadingMore = false
                exhausted = false
                searching = true
                scope.launch {
                    fetchPage(q, 1)
                    searching = false
                }
            }
        }
    }

    val loadMore: () -> Unit = {
        val q = query.trim()
        if (!searching && !loadingMore && hasMore && q.isNotEmpty()) {
            loadingMore = true
            scope.launch {
                fetchPage(q, page + 1)
                loadingMore = false
            }
        }
    }

    // Carga automática: cuando se aproxima al final de la cuadrícula
    // (queda ~6 ítems), pide la siguiente página.
    LaunchedEffect(gridState, results.size, totalHits) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 6) loadMore()
        }
    }

    val useVector: () -> Unit = {
        val hit = selectedHit
        if (hit != null && !usingVector) {
            usingVector = true
            error = null
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ImagePicker.loadFromUrl(hit.fullUrl)
                }
                usingVector = false
                if (bmp != null) {
                    onReference(bmp.asImageBitmap())
                    notice = "Vector listo para trazar ✓"
                } else {
                    error = "No se pudo descargar el vector"
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom))),
    ) {
        // Destellos decorativos de fondo
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-70).dp)
                .size(280.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x55FF8A3D), Color(0x00FF8A3D))),
                    CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 70.dp)
                .size(260.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x44FFD23F), Color(0x00FFD23F))),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))

            // Icono de la app
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(Gradient)),
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Free Scratch",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Traza cualquier imagen con la cámara de tu teléfono.\nGratis, sin cuenta y sin que nada salga de tu dispositivo.",
                color = TextDim,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(26.dp))

            // ---------- Tarjeta de la imagen ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        "Tu imagen de referencia",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )

                    // Selector de fuente: un solo modo activo a la vez
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SourcePill(
                            label = "Del teléfono",
                            icon = Icons.Default.PhotoLibrary,
                            selected = source == Source.PHONE,
                            modifier = Modifier.weight(1f),
                        ) {
                            source = Source.PHONE
                            error = null
                            notice = null
                        }
                        SourcePill(
                            label = "Templates Pixabay",
                            icon = Icons.Default.AutoAwesome,
                            selected = source == Source.PIXABAY,
                            modifier = Modifier.weight(1f),
                        ) {
                            source = Source.PIXABAY
                            error = null
                            notice = null
                        }
                    }

                    // Vista previa (común a los dos modos)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .drawBehind {
                                val stroke = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(16f, 12f),
                                    ),
                                )
                                drawRoundRect(
                                    color = Color(0xFF7C3AED),
                                    style = stroke,
                                    cornerRadius = CornerRadius(18.dp.toPx()),
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (reference != null) {
                            Image(
                                bitmap = reference,
                                contentDescription = "Imagen de referencia",
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    if (source == Source.PHONE) Icons.Default.PhotoLibrary
                                    else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF8B82AD),
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    if (source == Source.PHONE) "Toca \"Elegir imagen\" para empezar"
                                    else "Busca un dibujo en Pixabay",
                                    color = Color(0xFF8B82AD),
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    // ---- Controles según la fuente elegida ----
                    when (source) {
                        Source.PHONE -> GradientButton(
                            text = if (reference != null) "Cambiar imagen" else "Elegir imagen",
                            icon = Icons.Default.PhotoLibrary,
                            onClick = { gallery.launch(arrayOf("image/*")) },
                        )

                        Source.PIXABAY -> Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Buscador
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    placeholder = {
                                        Text("Ej: dinosaurio, gato…", color = Color(0xFF8B82AD), fontSize = 14.sp)
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Yellow,
                                        unfocusedBorderColor = Color(0x44FFFFFF),
                                        focusedContainerColor = Color(0x14FFFFFF),
                                        unfocusedContainerColor = Color(0x14FFFFFF),
                                        cursorColor = Yellow,
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.horizontalGradient(Gradient))
                                        .clickable(enabled = !searching, onClick = { doSearch() }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (searching) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF1B1035),
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Buscar en Pixabay",
                                            tint = Color(0xFF1B1035),
                                        )
                                    }
                                }
                            }

                            // Resultados (cuadrícula con carga automática)
                            if (results.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 96.dp),
                                    state = gridState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(results, key = { it.id }) { hit ->
                                        VectorCell(
                                            hit = hit,
                                            selected = selectedHit?.id == hit.id,
                                        ) {
                                            selectedHit =
                                                if (selectedHit?.id == hit.id) null else hit
                                        }
                                    }
                                    // Pie: indicador de carga / aviso de límite
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        when {
                                            loadingMore -> Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                CircularProgressIndicator(
                                                    color = Yellow,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "Cargando más…",
                                                    color = TextDim,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                            hasMore -> Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0x14FFFFFF))
                                                    .clickable(onClick = { loadMore() })
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "Cargar más resultados (${results.size} de $totalHits)",
                                                    color = Yellow,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                            else -> Text(
                                                "Mostrando los ${results.size} primeros resultados",
                                                color = Color(0xFF8B82AD),
                                                fontSize = 11.5.sp,
                                                textAlign = TextAlign.Center,
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "Vectores cortesía de Pixabay",
                                    color = Color(0xFF8B82AD),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                GradientButton(
                                    text = if (usingVector) "Descargando…" else "Usar este vector",
                                    icon = Icons.Default.Check,
                                    enabled = selectedHit != null && !usingVector,
                                    loading = usingVector,
                                    onClick = { useVector() },
                                )
                            } else if (!searching) {
                                Text(
                                    "Escribe una palabra y toca el botón de buscar\npara ver vectores para trazar.",
                                    color = Color(0xFF8B82AD),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // CTA principal
            GradientButton(
                text = "Abrir cámara",
                icon = Icons.Default.PhotoCamera,
                big = true,
                onClick = {
                    if (reference == null) {
                        error = "Primero elige o busca una imagen"
                    } else {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) onStart() else permission.launch(Manifest.permission.CAMERA)
                    }
                },
            )

            Spacer(Modifier.height(20.dp))

            // Chips de confianza
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("Sin cuenta")
                InfoChip("100% local")
                InfoChip("Gratis")
            }

            notice?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = Yellow, fontSize = 13.5.sp, textAlign = TextAlign.Center)
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF8A7A), fontSize = 13.5.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Píldora de selección de fuente (teléfono / Pixabay). */
@Composable
private fun SourcePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                brush = if (selected) {
                    Brush.horizontalGradient(Gradient)
                } else {
                    Brush.horizontalGradient(listOf(Color(0x14FFFFFF), Color(0x14FFFFFF)))
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF1B1035) else TextDim,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) Color(0xFF1B1035) else TextDim,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** Celda de un resultado de Pixabay con marco de selección. */
@Composable
private fun VectorCell(
    hit: PixabayApi.Hit,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Yellow else Color(0x33FFFFFF),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = hit.previewUrl,
            contentDescription = hit.tags,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Yellow,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun GradientButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    big: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (big) 58.dp else 50.dp)
            .clip(RoundedCornerShape(if (big) 18.dp else 15.dp))
            .background(Brush.horizontalGradient(Gradient))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color(0xFF1B1035),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(icon, contentDescription = null, tint = Color(0xFF1B1035), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text,
                color = Color(0xFF1B1035),
                fontSize = if (big) 17.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x1FFFFFFF))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, color = TextDim, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    }
}
