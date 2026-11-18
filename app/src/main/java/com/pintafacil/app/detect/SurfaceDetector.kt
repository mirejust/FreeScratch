package com.pintafacil.app.detect

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Detector de superficie: encuentra el papel/lienzo en un fotograma
 * en escala de grises y devuelve sus 4 esquinas.
 *
 * 1. Reduce la imagen y aplica umbral Otsu.
 * 2. Busca el mayor componente conexo claro.
 * 3. Calcula su casco convexo y lo simplifica a 4 puntos.
 *
 * Kotlin puro: sin OpenCV, sin modelos, sin conexión.
 */
object SurfaceDetector {

    fun detect(gray: IntArray, width: Int, height: Int): List<PointF>? {
        if (width < 16 || height < 16) return null

        // 1. Reducir a un ancho manejable para que el análisis sea rápido.
        val scale = max(1f, width / 240f)
        val rw = max(16, (width / scale).toInt())
        val rh = max(16, (height / scale).toInt())
        val small = IntArray(rw * rh)
        for (y in 0 until rh) {
            val sy = min(height - 1, (y * scale).toInt())
            for (x in 0 until rw) {
                val sx = min(width - 1, (x * scale).toInt())
                small[y * rw + x] = gray[sy * width + sx]
            }
        }

        // 2. Umbral Otsu.
        val threshold = otsu(small)

        // 3. Componentes conexos (4 vecinos) del lado claro.
        val visited = BooleanArray(rw * rh)
        var best: List<PointF>? = null
        var bestArea = 0
        val stack = IntArray(rw * rh)
        for (start in small.indices) {
            if (visited[start] || small[start] < threshold) continue
            var sp = 0
            stack[sp++] = start
            visited[start] = true
            val pixels = IntArray(1024)
            var count = 0
            var minX = rw
            var maxX = 0
            var minY = rh
            var maxY = 0
            var sum = 0
            while (sp > 0) {
                val idx = stack[--sp]
                val px = idx % rw
                val py = idx / rw
                if (count == pixels.size) pixels[count] = 0
                if (count < pixels.size) { pixels[count] = idx; count++ }
                sum += small[idx]
                if (px < minX) minX = px
                if (px > maxX) maxX = px
                if (py < minY) minY = py
                if (py > maxY) maxY = py
                if (px > 0 && !visited[idx - 1] && small[idx - 1] >= threshold) {
                    visited[idx - 1] = true; stack[sp++] = idx - 1
                }
                if (px < rw - 1 && !visited[idx + 1] && small[idx + 1] >= threshold) {
                    visited[idx + 1] = true; stack[sp++] = idx + 1
                }
                if (py > 0 && !visited[idx - rw] && small[idx - rw] >= threshold) {
                    visited[idx - rw] = true; stack[sp++] = idx - rw
                }
                if (py < rh - 1 && !visited[idx + rw] && small[idx + rw] >= threshold) {
                    visited[idx + rw] = true; stack[sp++] = idx + rw
                }
            }

            val area = (maxX - minX + 1) * (maxY - minY + 1)
            val density = count.toFloat() / area
            // Un papel: mancha grande y bastante rellena, sin tocar todo el fotograma.
            val touchesEdge = minX <= 1 || minY <= 1 || maxX >= rw - 2 || maxY >= rh - 2
            if (count >= 400 && density > 0.55f && area < rw * rh * 0.92f && !touchesEdge && area > bestArea) {
                bestArea = area
                best = quadFromComponent(small, visited, threshold, rw, minX, minY, maxX, maxY, pixels, count, scale)
            }
        }
        return best?.map { PointF(it.x * scale, it.y * scale) }
    }

    /** Casco convexo de los píxeles del componente + simplificación a 4 puntos. */
    private fun quadFromComponent(
        small: IntArray, visited: BooleanArray, threshold: Int,
        rw: Int, minX: Int, minY: Int, maxX: Int, maxY: Int,
        pixels: IntArray, count: Int, scale: Float,
    ): List<PointF>? {
        // Borde del componente: píxeles claros con al menos un vecino no claro.
        val hull = ArrayList<PointF>()
        for (i in 0 until count) {
            val idx = pixels[i]
            if (idx == 0 && i != 0) continue
            val px = idx % rw
            val py = idx / rw
            val clear = px > 0 && small[idx - 1] >= threshold
            val clear2 = px < rw - 1 && small[idx + 1] >= threshold
            val clear3 = py > 0 && small[idx - rw] >= threshold
            val clear4 = py < rw - 1 && small[idx + rw] >= threshold
            val edge = !clear || !clear2 || !clear3 || !clear4
            if (edge) hull.add(PointF(px.toFloat(), py.toFloat()))
        }
        if (hull.size < 4) return null
        val convex = convexHull(hull)
        if (convex.size < 4) return null
        val four = simplifyTo4(convex) ?: return null
        // Sanity check: área mínima razonable.
        val area = quadArea(four)
        if (area < 1500f) return null
        return four
    }

    fun convexHull(points: List<PointF>): List<PointF> {
        if (points.size < 3) return points
        val sorted = points.sortedWith(compareBy({ it.x }, { it.y }))
        fun cross(o: PointF, a: PointF, b: PointF) =
            (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
        val lower = ArrayList<PointF>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0f) {
                lower.removeAt(lower.size - 1)
            }
            lower.add(p)
        }
        val upper = ArrayList<PointF>()
        for (i in sorted.indices.reversed()) {
            val p = sorted[i]
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0f) {
                upper.removeAt(upper.size - 1)
            }
            upper.add(p)
        }
        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)
        lower.addAll(upper)
        return lower
    }

    /**
     * Elige 4 puntos del casco que maximizan el área: primero divide el casco
     * en 4 cuadrantes por extremales, luego refina moviendo cada punto.
     */
    private fun simplifyTo4(hull: List<PointF>): List<PointF>? {
        if (hull.size == 4) return hull
        var left = hull.minBy { it.x }
        var right = hull.maxBy { it.x }
        var top = hull.minBy { it.y }
        var bottom = hull.maxBy { it.y }
        // Asegurar orden TL, TR, BR, BL.
        var quad = order4(left, top, right, bottom)
        // Refinamiento: para cada esquina, probar todos los puntos del casco.
        for (pass in 0 until 3) {
            var improved = false
            val current = quad
            for (corner in 0..3) {
                var bestQ = current
                var bestA = quadArea(current)
                for (p in hull) {
                    val candidate = current.toMutableList()
                    candidate[corner] = p
                    val a = quadArea(candidate)
                    if (a > bestA) {
                        bestA = a
                        bestQ = candidate
                    }
                }
                if (bestQ !== current) {
                    improved = true
                    quad = bestQ
                }
            }
            if (!improved) break
        }
        return quad
    }

    /** Ordena 4 puntos arbitrarios en TL, TR, BR, BL. */
    fun order4(a: PointF, b: PointF, c: PointF, d: PointF): List<PointF> {
        val pts = listOf(a, b, c, d)
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()
        // Por ángulo desde el centro (sentido horario desde arriba-izquierda).
        return pts.sortedBy { p ->
            val ang = Math.atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())
            // -PI/4..7PI/4 para que el TL quede primero
            val shifted = ang + Math.PI * 0.25
            if (shifted < 0) shifted + Math.PI * 2 else shifted
        }
    }

    fun quadArea(q: List<PointF>): Float {
        var area = 0f
        for (i in q.indices) {
            val j = (i + 1) % q.size
            area += q[i].x * q[j].y - q[j].x * q[i].y
        }
        return abs(area) / 2f
    }

    /** Umbral de Otsu sobre el histograma. */
    fun otsu(values: IntArray): Int {
        val hist = IntArray(256)
        for (v in values) hist[v.coerceIn(0, 255)]++
        val total = values.size
        var sum = 0.0
        for (t in 0..255) sum += t * hist[t].toDouble()
        var sumB = 0.0
        var wB = 0
        var best = 0.0
        var threshold = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t * hist[t].toDouble()
            val mB = sumB / wB
            val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) {
                best = between
                threshold = t
            }
        }
        return threshold
    }

    /** Convierte un ByteBuffer del plano Y a grises respetando el rowStride. */
    fun yPlaneToGray2(buffer: java.nio.ByteBuffer, rowStride: Int, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        val bytes = ByteArray(rowStride * height)
        buffer.rewind()
        val len = minOf(bytes.size, buffer.remaining())
        buffer.get(bytes, 0, len)
        for (row in 0 until height) {
            val offset = row * rowStride
            val base = row * width
            if (offset + width > len) break
            for (col in 0 until width) {
                out[base + col] = bytes[offset + col].toInt() and 0xFF
            }
        }
        return out
    }

    /** Convierte un buffer YUV (solo plano Y) a grises. */
    fun yPlaneToGray(y: ByteArray, rowStride: Int, width: Int, height: Int): IntArray {
        val out = IntArray(width * height)
        for (row in 0 until height) {
            val offset = row * rowStride
            val base = row * width
            for (col in 0 until width) {
                out[base + col] = y[offset + col].toInt() and 0xFF
            }
        }
        return out
    }
}
