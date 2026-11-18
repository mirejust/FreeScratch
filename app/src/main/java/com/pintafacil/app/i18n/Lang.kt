package com.pintafacil.app.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Idioma de la interfaz. Inglés por defecto. */
enum class AppLang { EN, ES }

/**
 * Cadenas de la app en inglés/español.
 *
 * [T.lang] es estado de Compose: al cambiarlo, todo texto que lea una
 * cadena de aquí se recomposa automáticamente con el nuevo idioma.
 * La elección se guarda en SharedPreferences y arranca en inglés.
 */
object T {

    private const val PREFS = "settings"
    private const val KEY_LANG = "lang"

    /** Idioma actual (observable). */
    var lang: AppLang by mutableStateOf(AppLang.EN)
        private set

    private val es get() = lang == AppLang.ES

    /** Carga el idioma guardado (inglés por defecto). Llamar al arrancar. */
    fun load(context: Context) {
        val saved = context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, null)
        lang = if (saved == "es") AppLang.ES else AppLang.EN
    }

    /** Cambia el idioma y lo guarda. */
    fun set(context: Context, newLang: AppLang) {
        lang = newLang
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, newLang.name.lowercase())
            .apply()
    }

    // ---------------- Inicio ----------------

    val tagline: String
        get() = if (es) {
            "Traza cualquier imagen con la cámara de tu teléfono.\n" +
                "Gratis, sin cuenta y sin que nada salga de tu dispositivo."
        } else {
            "Trace any image with your phone camera.\n" +
                "Free, no account, and nothing leaves your device."
        }

    val cardTitle: String
        get() = if (es) "Tu imagen de referencia" else "Your reference image"

    val pillPhone: String
        get() = if (es) "Del teléfono" else "From phone"

    val pillPixabay: String
        get() = if (es) "Templates Pixabay" else "Pixabay templates"

    val refImageDesc: String
        get() = if (es) "Imagen de referencia" else "Reference image"

    val emptyPhone: String
        get() = if (es) "Toca \"Elegir imagen\" para empezar" else "Tap \"Choose image\" to start"

    val emptyPixabay: String
        get() = if (es) "Busca un dibujo en Pixabay" else "Search for a drawing on Pixabay"

    val pickImage: String
        get() = if (es) "Elegir imagen" else "Choose image"

    val changeImage: String
        get() = if (es) "Cambiar imagen" else "Change image"

    val searchHint: String
        get() = if (es) "Ej: dinosaurio, gato…" else "E.g. dinosaur, cat…"

    val searchDesc: String
        get() = if (es) "Buscar en Pixabay" else "Search Pixabay"

    val loadingMore: String
        get() = if (es) "Cargando más…" else "Loading more…"

    fun loadMore(n: Int, total: Int): String =
        if (es) "Cargar más resultados ($n de $total)" else "Load more results ($n of $total)"

    fun showingFirst(n: Int): String =
        if (es) "Mostrando los $n primeros resultados" else "Showing the first $n results"

    val credit: String
        get() = if (es) "Vectores cortesía de Pixabay" else "Vectors courtesy of Pixabay"

    val downloading: String
        get() = if (es) "Descargando…" else "Downloading…"

    val useVector: String
        get() = if (es) "Usar este vector" else "Use this vector"

    val searchEmptyHint: String
        get() = if (es) {
            "Escribe una palabra y toca el botón de buscar\npara ver vectores para trazar."
        } else {
            "Type a word and tap the search button\nto see vectors to trace."
        }

    val openCamera: String
        get() = if (es) "Abrir cámara" else "Open camera"

    val chipNoAccount: String
        get() = if (es) "Sin cuenta" else "No account"

    val chipLocal: String
        get() = if (es) "100% local" else "100% local"

    val chipFree: String
        get() = if (es) "Gratis" else "Free"

    // -------- Avisos y errores del inicio --------

    val imgReady: String
        get() = if (es) "Imagen lista para trazar ✓" else "Image ready to trace ✓"

    val errOpenImage: String
        get() = if (es) "No se pudo abrir esa imagen" else "Couldn't open that image"

    val errNeedCamera: String
        get() = if (es) {
            "Necesito la cámara para trazar sobre tu papel"
        } else {
            "I need the camera to trace on your paper"
        }

    val errSearchFail: String
        get() = if (es) "No se pudo buscar: revisa tu conexión" else "Search failed: check your connection"

    val errLoadMore: String
        get() = if (es) "Sin conexión: no se pudo cargar más" else "Offline: couldn't load more"

    fun noResults(q: String): String =
        if (es) "Sin resultados para \"$q\"" else "No results for \"$q\""

    val errEmptyQuery: String
        get() = if (es) "Escribe qué quieres buscar" else "Type what you want to search"

    val errNoKey: String
        get() = if (es) {
            "Falta la clave de Pixabay: copia apikeys.properties.example a apikeys.properties y compila de nuevo"
        } else {
            "Pixabay API key missing: copy apikeys.properties.example to apikeys.properties and rebuild"
        }

    val vectorReady: String
        get() = if (es) "Vector listo para trazar ✓" else "Vector ready to trace ✓"

    val errDownload: String
        get() = if (es) "No se pudo descargar el vector" else "Couldn't download the vector"

    val errNoRef: String
        get() = if (es) "Primero elige o busca una imagen" else "Choose or search for an image first"

    // ---------------- Trazador ----------------

    val errOpenCamera: String
        get() = if (es) "No se pudo abrir la cámara" else "Couldn't open the camera"

    val camNotReady: String
        get() = if (es) "La cámara aún no está lista" else "Camera isn't ready yet"

    val noFlash: String
        get() = if (es) "Este teléfono no tiene flash" else "This phone has no flash"

    val flashOn: String
        get() = if (es) "Flash encendido" else "Flash on"

    val flashOff: String
        get() = if (es) "Flash apagado" else "Flash off"

    val camResumed: String
        get() = if (es) "Cámara reanudada" else "Camera resumed"

    val viewFrozen: String
        get() = if (es) {
            "Vista congelada: la imagen queda anclada"
        } else {
            "View frozen: the image stays anchored"
        }

    val imageCentered: String
        get() = if (es) "Imagen centrada" else "Image centered"

    val lookingPaper: String
        get() = if (es) "Buscando el papel…" else "Looking for paper…"

    val paperFound: String
        get() = if (es) {
            "Papel encontrado. Activa Esquinas para ajustarlo"
        } else {
            "Paper found. Tap Corners to adjust it"
        }

    val noPaper: String
        get() = if (es) {
            "No vi un papel claro: más luz o toca Esquinas"
        } else {
            "No clear paper found: add more light or tap Corners"
        }

    val errAnalyze: String
        get() = if (es) "No se pudo analizar la imagen" else "Couldn't analyze the image"

    val imageHidden: String
        get() = if (es) {
            "Imagen oculta · mantén presionado el ojo para verla"
        } else {
            "Image hidden · press and hold the eye to show it"
        }

    val eyeDesc: String
        get() = if (es) {
            "Mantén presionado para ocultar la imagen"
        } else {
            "Press and hold to hide the image"
        }

    val recenterDesc: String
        get() = if (es) "Recentrar imagen" else "Recenter image"

    // ---------------- Barra superior y dock ----------------

    val exit: String
        get() = if (es) "Salir" else "Exit"

    val done: String
        get() = if (es) "Hecho" else "Done"

    val undo: String
        get() = if (es) "Deshacer" else "Undo"

    val redo: String
        get() = if (es) "Rehacer" else "Redo"

    val showSettings: String
        get() = if (es) "Mostrar ajustes" else "Show settings"

    val hideSettings: String
        get() = if (es) "Ocultar ajustes" else "Hide settings"

    val tabOpacity: String
        get() = if (es) "Opacidad" else "Opacity"

    val tabScaling: String
        get() = if (es) "Escalar" else "Scale"

    val tabTools: String
        get() = if (es) "Trazar" else "Trace"

    val tabCamera: String
        get() = if (es) "Cámara" else "Camera"

    val tabFlash: String
        get() = if (es) "Flash" else "Flash"

    val sizeTitle: String
        get() = if (es) "Tamaño" else "Size"

    val flip: String
        get() = if (es) "Voltear" else "Flip"

    val freeze: String
        get() = if (es) "Congelar" else "Freeze"

    val resume: String
        get() = if (es) "Reanudar" else "Resume"

    val flashTurnOn: String
        get() = if (es) "Encender flash" else "Turn on flash"

    val flashTurnOff: String
        get() = if (es) "Apagar flash" else "Turn off flash"

    val stateOn: String
        get() = if (es) "Encendido" else "On"

    val stateOff: String
        get() = if (es) "Apagado" else "Off"

    // ---------------- Pestaña Trazar ----------------

    val surface: String
        get() = if (es) "Superficie" else "Surface"

    val surfaceFlat: String
        get() = if (es) "Plano" else "Flat"

    val surfaceCup: String
        get() = if (es) "Vaso" else "Cup"

    val surfaceRoller: String
        get() = if (es) "Rodillo" else "Roller"

    val surfaceBall: String
        get() = if (es) "Pelota" else "Ball"

    val tilt: String
        get() = if (es) "Inclinación" else "Tilt"

    val rotate: String
        get() = if (es) "Giro" else "Rotate"

    val detect: String
        get() = if (es) "Detectar" else "Detect"

    val corners: String
        get() = if (es) "Esquinas" else "Corners"

    val resetAll: String
        get() = if (es) "Reiniciar todo" else "Reset all"

    // ---------------- Selector de idioma ----------------

    val langEn: String
        get() = "🇬🇧 EN"

    val langEs: String
        get() = "🇪🇸 ES"

    val langDesc: String
        get() = if (es) "Cambiar idioma" else "Change language"
}
