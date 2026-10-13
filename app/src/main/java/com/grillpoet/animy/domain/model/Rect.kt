package com.grillpoet.animy.domain.model

import androidx.compose.ui.geometry.Offset

/** 纯数据矩形，避免依赖 Compose Rect 用于 bounds 计算 */
data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isEmpty: Boolean get() = width <= 0f || height <= 0f

    fun contains(p: Offset): Boolean =
        p.x in left..right && p.y in top..bottom

    fun intersects(other: Rect): Boolean =
        left < other.right && right > other.left &&
        top < other.bottom && bottom > other.top

    fun inflate(dx: Float, dy: Float = dx): Rect =
        Rect(left - dx, top - dy, right + dx, bottom + dy)

    companion object {
        val Empty = Rect(0f, 0f, 0f, 0f)
    }
}
