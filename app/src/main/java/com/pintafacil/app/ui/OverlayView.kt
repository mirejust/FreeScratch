package com.pintafacil.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pintafacil.app.overlay.OverlayRenderer
import com.pintafacil.app.overlay.OverlayState
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Vista nativa que dibuja el overlay y gestiona los gestos:
 * arrastre (mover), pellizco (escalar), rotación de dos dedos,
 * arrastre de esquinas y toque para alternar guías.
 */
class OverlayView(context: Context, private val renderer: OverlayRenderer) : View(context) {

    var state: OverlayState = OverlayState()
    var showGuides: Boolean = false
    var mirrored: Boolean = false
    var onStateChanged: ((OverlayState) -> Unit)? = null
    var onDragStart: (() -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null

    private val touchRadius = 110f
    private var draggingCorner = -1
    private var lastX = 0f
    private var lastY = 0f
    private var lastDist = 0f
    private var lastAngle = 0f
    private var moved = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // La vista ya conoce su tamaño: si la imagen no tiene posición, la centramos.
        if (w > 0 && h > 0) {
            val c = state.center
            if (c.x == 0f && c.y == 0f) {
                update(state.copy(center = PointF(w / 2f, h / 2f)))
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        renderer.draw(
            canvas, state, width.toFloat(), height.toFloat(),
            showGuides || draggingCorner >= 0,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Normaliza la posición antes del primer gesto: si la imagen no ha
                // colocado aún, la centra para que no "salte" desde la esquina.
                if (state.center.x == 0f && state.center.y == 0f && width > 0 && height > 0) {
                    update(state.copy(center = PointF(width / 2f, height / 2f)))
                }
                lastX = event.x
                lastY = event.y
                moved = false
                draggingCorner = OverlayRenderer.hitCorner(state, event.x, event.y, touchRadius) ?: -1
                if (draggingCorner >= 0) {
                    showGuides = true
                    invalidate()
                }
                // Avisa antes de modificar nada: permite guardar el estado para deshacer.
                onDragStart?.invoke()
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    lastDist = distance(event)
                    lastAngle = angle(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (draggingCorner >= 0) {
                    val current = state.corners
                    if (current != null) {
                        val pts = current.toMutableList()
                        pts[draggingCorner] = PointF(event.x, event.y)
                        update(state.copy(corners = pts))
                        moved = true
                        invalidate()
                    }
                } else if (event.pointerCount >= 2) {
                    val d = distance(event)
                    val a = angle(event)
                    if (lastDist > 0f) {
                        val scale = (d / lastDist).coerceIn(0.2f, 5f)
                        val rotation = Math.toDegrees((a - lastAngle).toDouble()).toFloat()
                        applyGesture(scale, rotation, event)
                        moved = true
                    }
                    lastDist = d
                    lastAngle = a
                } else {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) + abs(dy) > 1f) {
                        val c = state.center
                        var next = state.copy(center = PointF(c.x + dx, c.y + dy))
                        next.corners?.let { pts ->
                            next = next.copy(corners = pts.map { PointF(it.x + dx, it.y + dy) })
                        }
                        update(next)
                        lastX = event.x
                        lastY = event.y
                        moved = true
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> lastDist = 0f
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (draggingCorner >= 0) {
                    draggingCorner = -1
                    onDragEnd?.invoke()
                } else if (showGuides && !moved) {
                    showGuides = false
                    invalidate()
                } else if (moved) {
                    onDragEnd?.invoke()
                }
                lastDist = 0f
            }
        }
        return true
    }

    /** Escala y gira alrededor del punto medio de los dos dedos. */
    private fun applyGesture(scale: Float, rotationDeg: Float, event: MotionEvent) {
        val cx = (event.getX(0) + event.getX(1)) / 2f
        val cy = (event.getY(0) + event.getY(1)) / 2f
        val newSize = (state.size * scale).coerceIn(0.05f, 8f)
        val rad = Math.toRadians(rotationDeg.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()

        fun rotateAround(p: PointF, s: Float): PointF {
            val dx = p.x - cx
            val dy = p.y - cy
            return PointF(
                cx + (dx * cos - dy * sin) * s,
                cy + (dx * sin + dy * cos) * s,
            )
        }

        val newCorners = state.corners?.map { rotateAround(it, scale) }
        val nc = rotateAround(state.center, scale)

        // Sin esquinas manuales, la rotación de dos dedos suma al giro "turn".
        val turn = if (state.corners == null) {
            (state.turn + rotationDeg).coerceIn(-180f, 180f)
        } else state.turn

        update(state.copy(size = newSize, center = nc, corners = newCorners, turn = turn))
        invalidate()
    }

    private fun update(s: OverlayState) {
        state = s
        onStateChanged?.invoke(s)
    }

    private fun distance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return hypot(dx, dy)
    }

    private fun angle(event: MotionEvent): Float =
        Math.atan2(
            (event.getY(1) - event.getY(0)).toDouble(),
            (event.getX(1) - event.getX(0)).toDouble(),
        ).toFloat()

    fun refresh(newState: OverlayState) {
        val imageChanged = newState.image !== state.image
        state = if (imageChanged && width > 0) {
            newState.copy(corners = null, center = PointF(width / 2f, height / 2f))
        } else newState
        invalidate()
    }
}

/**
 * Envoltorio Compose del OverlayView.
 */
@Composable
fun OverlaySurface(
    state: OverlayState,
    renderer: OverlayRenderer,
    showGuides: Boolean,
    mirrored: Boolean,
    onSize: (Float, Float) -> Unit,
    onStateChanged: (OverlayState) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { ctx -> OverlayView(ctx, renderer) },
        modifier = modifier,
        update = { v ->
            v.onStateChanged = onStateChanged
            v.onDragStart = onDragStart
            v.onDragEnd = onDragEnd
            v.showGuides = showGuides
            v.mirrored = mirrored
            if (v.width > 0) onSize(v.width.toFloat(), v.height.toFloat())
            if (v.state !== state) v.refresh(state)
            v.invalidate()
        },
    )
}
