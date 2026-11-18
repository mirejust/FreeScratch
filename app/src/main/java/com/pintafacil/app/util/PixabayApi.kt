package com.pintafacil.app.util

import com.pintafacil.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo de la API de Pixabay (https://pixabay.com/api/docs/).
 * Busca solo vectores/dibujos (image_type=vector) para trazar.
 *
 * La clave se inyecta desde `apikeys.properties` vía BuildConfig
 * (ese archivo no se sube a GitHub; ver apikeys.properties.example).
 *
 * Todas las funciones son bloqueantes: llamar en un hilo IO.
 */
object PixabayApi {

    private const val ENDPOINT = "https://pixabay.com/api/"

    /** Máximo de resultados que devuelve Pixabay por consulta (500). */
    const val MAX_RESULTS = 500

    /** Un resultado de búsqueda. */
    data class Hit(
        val id: Int,
        val tags: String,
        /** Imagen de 640 px para la cuadrícula (uso temporal, válida 24 h). */
        val previewUrl: String,
        /** Imagen de hasta 1280 px para trazar. */
        val fullUrl: String,
    )

    /** Una página de resultados. */
    data class Page(
        val hits: List<Hit>,
        /** Total de resultados accesibles para la consulta (máx. 500). */
        val totalHits: Int,
    )

    /** false si falta la clave (archivo apikeys.properties ausente o en blanco). */
    val isConfigured: Boolean
        get() {
            val key = BuildConfig.PIXABAY_API_KEY
            return key.isNotBlank() && !key.contains("Aqui deberas", ignoreCase = true)
        }

    /**
     * Busca vectores que coincidan con [query] en la página [page].
     * Lanza IOException si no hay red o la API falla.
     */
    fun search(query: String, page: Int = 1, perPage: Int = 60): Page {
        val key = BuildConfig.PIXABAY_API_KEY
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = buildString {
            append(ENDPOINT)
            append("?key=").append(key)
            append("&q=").append(q)
            append("&image_type=vector")
            append("&safesearch=true")
            append("&order=popular")
            append("&page=").append(page.coerceAtLeast(1))
            append("&per_page=").append(perPage.coerceIn(3, 200))
        }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 12_000
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val total = json.optInt("totalHits", 0)
            val hits = json.getJSONArray("hits")
            val out = ArrayList<Hit>(hits.length())
            for (i in 0 until hits.length()) {
                val h = hits.getJSONObject(i)
                val preview = h.optString("webformatURL")
                val full = h.optString("largeImageURL")
                if (preview.isNotEmpty() && full.isNotEmpty()) {
                    out.add(
                        Hit(
                            id = h.optInt("id"),
                            tags = h.optString("tags"),
                            previewUrl = preview,
                            fullUrl = full,
                        )
                    )
                }
            }
            return Page(out, total)
        } finally {
            conn.disconnect()
        }
    }
}
