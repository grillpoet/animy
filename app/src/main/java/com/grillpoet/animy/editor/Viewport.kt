package com.grillpoet.animy.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

/**
 * 视口变换：画布坐标 ↔ 屏幕坐标。
 *
 * 所有手势输入必须经过此变换才进入领域层，避免缩放/平移后坐标错位。
 */
data class Viewport(
    val canvasSize: IntSize,
    val viewSize: IntSize,
    val pan: Offset = Offset.Zero,
    val zoom: Float = 1f,
) {
    /** 适配屏幕的基准缩放（包含用户缩放） */
    val scale: Float
        get() {
            val cw = canvasSize.width.coerceAtLeast(1).toFloat()
            val ch = canvasSize.height.coerceAtLeast(1).toFloat()
            val vw = viewSize.width.toFloat()
            val vh = viewSize.height.toFloat()
            return min(vw / cw, vh / ch) * zoom
        }

    /** 画布左上角在屏幕上的位置 */
    val origin: Offset
        get() {
            val cw = canvasSize.width.coerceAtLeast(1).toFloat()
            val ch = canvasSize.height.coerceAtLeast(1).toFloat()
            val s = scale
            val vw = viewSize.width.toFloat()
            val vh = viewSize.height.toFloat()
            return Offset(
                (vw - cw * s) / 2f + pan.x,
                (vh - ch * s) / 2f + pan.y,
            )
        }

    fun toCanvas(screen: Offset): Offset {
        val s = scale
        val o = origin
        return Offset((screen.x - o.x) / s, (screen.y - o.y) / s)
    }

    fun toScreen(canvas: Offset): Offset {
        val s = scale
        val o = origin
        return Offset(o.x + canvas.x * s, o.y + canvas.y * s)
    }

    /** 屏幕位移 → 画布位移（用于拖动元素） */
    fun deltaToCanvas(screenDelta: Offset): Offset {
        val s = scale
        return Offset(screenDelta.x / s, screenDelta.y / s)
    }

    /** 缩放画布：围绕 focusScreen 缩放 */
    fun zoomAround(focusScreen: Offset, zoomDelta: Float): Viewport {
        val newZoom = (zoom * zoomDelta).coerceIn(0.25f, 8f)
        if (newZoom == zoom) return this
        // 保持 focusScreen 对应的画布点不动
        val canvasFocus = toCanvas(focusScreen)
        val next = copy(zoom = newZoom)
        val newScreenFocus = next.toScreen(canvasFocus)
        val newPan = pan + (focusScreen - newScreenFocus)
        return next.copy(pan = newPan)
    }

    fun panBy(screenDelta: Offset): Viewport = copy(pan = pan + screenDelta)

    fun reset(): Viewport = copy(pan = Offset.Zero, zoom = 1f)
}
