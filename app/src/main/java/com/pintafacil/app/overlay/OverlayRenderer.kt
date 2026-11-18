package com.pintafacil.app.overlay

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class SurfaceMode { FLAT, CUP, ROLLER, BALL }

/**
 * Estado completo de la imagen superpuesta: dónde está, cómo se deforma
 * y cuánto pesa cada ajuste del panel.
 */
data class OverlayState(
    val image: Bitmap? = null,
    val opacity: Float = 0.55f,      // 0..1
    val size: Float = 1f,            // escala relativa
    val tilt: Float = 0f,            // grados (-60..60)
    val turn: Float = 0f,            // grados (-60..60)
    val surface: SurfaceMode = SurfaceMode.FLAT,
    val center: PointF = PointF(0f, 0f),
    val corners: List<PointF>? = null, // 4 puntos en coordenadas de vista, null = sin ajuste manual
)

/**
 * Dibuja la imagen de referencia sobre la cámara aplicando:
 *  - opacidad
 *  - deformación en perspectiva (esquinas / tilt / turn)
 *  - envoltura curva según la superficie elegida
 *
 * Todo ocurre en un Bitmap intermedio: sin OpenGL, sin dependencias.
 */
class OverlayRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    /** Renderiza el overlay completo en [canvas] del tamaño de la vista. */
    fun draw(canvas: Canvas, state: OverlayState, viewW: Float, viewH: Float, showGuides: Boolean) {
        val src = state.image ?: return
        if (viewW <= 0f || viewH <= 0f) return

        // Si la imagen aún no tiene posición, se dibuja centrada en la vista
        // en lugar de descartarla (evita que "aparezca" solo al arrastrar).
        val st = if (state.center.x == 0f && state.center.y == 0f) {
            state.copy(center = PointF(viewW / 2f, viewH / 2f))
        } else state

        val quad = quadOf(st, src, viewW, viewH)
        val warped = warp(src, state.surface, state.tilt, state.turn) ?: src

        canvas.save()
        val matrix = Matrix()
        val srcPts = floatArrayOf(
            0f, 0f,
            warped.width.toFloat(), 0f,
            warped.width.toFloat(), warped.height.toFloat(),
            0f, warped.height.toFloat(),
        )
        matrix.setPolyToPoly(srcPts, 0, quad, 0, 4)
        paint.alpha = (state.opacity.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(warped, matrix, paint)
        canvas.restore()

        // Marco de selección siempre visible alrededor de la imagen (estilo AR Drawing).
        val border = Path().apply {
            moveTo(quad[0], quad[1])
            for (i in 1 until 4) lineTo(quad[i * 2], quad[i * 2 + 1])
            close()
        }
        canvas.drawPath(border, borderPaint)

        if (showGuides && st.corners != null) {
            canvas.drawPath(border, wirePaint)
            for (i in 0 until 4) {
                canvas.drawCircle(quad[i * 2], quad[i * 2 + 1], 18f, wirePaint)
            }
        }
    }

    /**
     * Los 4 puntos de destino en coordenadas de vista.
     * Si el usuario arrastró esquinas se usan esas; si no, se calcula
     * un rectángulo girado con tilt/turn simulados en perspectiva.
     */
    fun quadOf(state: OverlayState, src: Bitmap, viewW: Float, viewH: Float): FloatArray {
        state.corners?.let { c ->
            if (c.size == 4) return floatArrayOf(
                c[0].x, c[0].y, c[1].x, c[1].y, c[2].x, c[2].y, c[3].x, c[3].y,
            )
        }
        val fit = min(viewW / src.width, viewH / src.height) * 0.82f
        val halfW = src.width * fit * state.size / 2f
        val halfH = src.height * fit * state.size / 2f

        // Rotación en el plano por "turn" (giro tipo letrero)
        val rad = Math.toRadians(state.turn.toDouble())
        val cosT = cos(rad).toFloat()
        val sinT = sin(rad).toFloat()

        // "tilt" comprime el eje vertical (la lámina se inclina)
        val tiltFactor = cos(Math.toRadians(state.tilt.toDouble())).toFloat().coerceIn(0.15f, 1f)

        val base = arrayOf(
            floatArrayOf(-halfW, -halfH),
            floatArrayOf(halfW, -halfH),
            floatArrayOf(halfW, halfH),
            floatArrayOf(-halfW, halfH),
        )
        val out = FloatArray(8)
        for (i in 0..3) {
            val x = base[i][0]
            val y = base[i][1] * tiltFactor
            out[i * 2] = state.center.x + x * cosT - y * sinT
            out[i * 2 + 1] = state.center.y + x * sinT + y * cosT
        }
        return out
    }

    /**
     * Deformación previa según la superficie:
     *  FLAT   -> sin cambios
     *  CUP    -> comprime laterales (el dibujo abraza un cilindro vertical)
     *  ROLLER -> comprime arriba/abajo (cilindro horizontal)
     *  BALL   -> comprime ambos ejes con caída hacia los bordes
     */
    private fun warp(src: Bitmap, mode: SurfaceMode, tilt: Float, turn: Float): Bitmap? {
        if (mode == SurfaceMode.FLAT) return src
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cols = 48
        val rows = 48
        val strength = 0.35f

        when (mode) {
            SurfaceMode.CUP -> {
                for (c in 0 until cols) {
                    val u0 = c / cols.toFloat()
                    val u1 = (c + 1) / cols.toFloat()
                    val s0 = bend(u0, strength)
                    val s1 = bend(u1, strength)
                    val srcRect = android.graphics.Rect(
                        (s0 * w).toInt(), 0, (s1 * w).toInt(), h
                    )
                    val dstRect = android.graphics.RectF(u0 * w, 0f, u1 * w, h.toFloat())
                    canvas.drawBitmap(src, srcRect, dstRect, p)
                }
            }
            SurfaceMode.ROLLER -> {
                for (r in 0 until rows) {
                    val v0 = r / rows.toFloat()
                    val v1 = (r + 1) / rows.toFloat()
                    val s0 = bend(v0, strength)
                    val s1 = bend(v1, strength)
                    val srcRect = android.graphics.Rect(
                        0, (s0 * h).toInt(), w, (s1 * h).toInt()
                    )
                    val dstRect = android.graphics.RectF(0f, v0 * h, w.toFloat(), v1 * h)
                    canvas.drawBitmap(src, srcRect, dstRect, p)
                }
            }
            SurfaceMode.BALL -> {
                for (c in 0 until cols) {
                    for (r in 0 until rows) {
                        val u0 = c / cols.toFloat()
                        val u1 = (c + 1) / cols.toFloat()
                        val v0 = r / rows.toFloat()
                        val v1 = (r + 1) / rows.toFloat()
                        val srcRect = android.graphics.Rect(
                            (bend(u0, strength) * w).toInt(), (bend(v0, strength) * h).toInt(),
                            (bend(u1, strength) * w).toInt(), (bend(v1, strength) * h).toInt(),
                        )
                        val dstRect = android.graphics.RectF(u0 * w, v0 * h, u1 * w, v1 * h)
                        canvas.drawBitmap(src, srcRect, dstRect, p)
                    }
                }
            }
            else -> return src
        }
        return out
    }

    /** Mapea una coordenada normalizada comprimiendo los extremos. */
    private fun bend(u: Float, strength: Float): Float {
        val centered = u * 2f - 1f
        val bent = centered - strength * centered * centered * centered
        return (bent + 1f) / 2f
    }

    companion object {
        /** Distancia mínima (px) para que el dedo agarre una esquina. */
        fun hitCorner(state: OverlayState, x: Float, y: Float, radius: Float): Int? {
            val c = state.corners ?: return null
            for (i in c.indices) {
                val dx = c[i].x - x
                val dy = c[i].y - y
                if (dx * dx + dy * dy <= radius * radius) return i
            }
            return null
        }
    }
}
