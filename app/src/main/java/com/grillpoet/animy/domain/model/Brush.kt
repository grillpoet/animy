package com.grillpoet.animy.domain.model

import androidx.compose.ui.graphics.Color

enum class BrushType(val displayName: String) {
    Pen("画笔"),
    Marker("马克笔"),
    Highlighter("荧光笔"),
    Airbrush("喷枪"),
    Eraser("橡皮"),
}

/**
 * 笔刷样式。与笔画数据分离，为后续"笔刷预设库"预留。
 */
data class BrushStyle(
    val type: BrushType = BrushType.Pen,
    val color: Color = Color.Black,
    val width: Float = 8f,
    val opacity: Float = 1f,
) {
    init {
        require(width > 0f) { "笔刷宽度必须为正" }
        require(opacity in 0f..1f) { "不透明度必须在 0..1" }
    }

    val isEraser: Boolean get() = type == BrushType.Eraser
    fun withColor(c: Color) = copy(color = c)
    fun withWidth(w: Float) = copy(width = w.coerceIn(0.5f, 200f))
}

/**
 * 笔画采样点。pressure / tilt 预留给压感笔；普通触摸填默认值。
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val tilt: Float = 0f,
    val timestamp: Long = 0L,
)
