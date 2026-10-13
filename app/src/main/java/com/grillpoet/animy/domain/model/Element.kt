package com.grillpoet.animy.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap

/** 元素的仿射变换 */
data class ElementTransform(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    val isIdentity: Boolean
        get() = offset == Offset.Zero && scale == 1f && rotation == 0f

    companion object {
        val Identity = ElementTransform()
    }
}

/** 可绘制元素：矢量笔画 / 位图 / 形状 */
sealed interface DrawElement {
    val id: Long
    val transform: ElementTransform
}

enum class ShapeType(val displayName: String) {
    Line("直线"),
    Arrow("箭头"),
    Rect("矩形"),
    Ellipse("椭圆"),
}


/** 矢量笔画。缩放不失真。 */
data class VectorStroke(
    override val id: Long,
    override val transform: ElementTransform = ElementTransform.Identity,
    val points: List<StrokePoint>,
    val brush: BrushStyle,
) : DrawElement {
    val isEmpty: Boolean get() = points.isEmpty()

    /** 包围盒（含笔宽）。用于命中测试和 dirty rect。 */
    fun bounds(): Rect {
        if (points.isEmpty()) return Rect.Empty
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (p in points) {
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }
        val half = brush.width / 2f
        return Rect(minX - half, minY - half, maxX + half, maxY + half)
    }

    /** 点是否命中笔画（沿路径的近似距离）。先做包围盒预筛。 */
    fun hitTest(p: Offset, tolerance: Float = 6f): Boolean {
        val b = bounds()
        if (b.isEmpty) return false
        if (p.x < b.left - tolerance || p.x > b.right + tolerance ||
            p.y < b.top - tolerance || p.y > b.bottom + tolerance) {
            return false
        }

        val r = brush.width / 2f + tolerance
        val r2 = r * r
        var prev = points.firstOrNull() ?: return false
        for (i in 1 until points.size) {
            val curr = points[i]
            if (segmentDistanceSq(p, prev.x, prev.y, curr.x, curr.y) <= r2) return true
            prev = curr
        }
        if (points.size == 1) {
            val dx = p.x - prev.x; val dy = p.y - prev.y
            return dx * dx + dy * dy <= r2
        }
        return false
    }

    private fun segmentDistanceSq(p: Offset, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay
        val apx = p.x - ax; val apy = p.y - ay
        val len2 = abx * abx + aby * aby
        if (len2 < 1e-6f) return apx * apx + apy * apy
        val t = ((apx * abx + apy * aby) / len2).coerceIn(0f, 1f)
        val cx = ax + abx * t; val cy = ay + aby * t
        val dx = p.x - cx; val dy = p.y - cy
        return dx * dx + dy * dy
    }
}

/** 位图元素：导入的图片、选区抠出的内容 */
data class BitmapElement(
    override val id: Long,
    override val transform: ElementTransform = ElementTransform.Identity,
    val image: ImageBitmap,
    val label: String = "",
) : DrawElement {
    val width: Int get() = image.width
    val height: Int get() = image.height

    val center: Offset
        get() = Offset(
            transform.offset.x + width * transform.scale / 2f,
            transform.offset.y + height * transform.scale / 2f,
        )

    fun bounds(): Rect {
        val w = width * transform.scale
        val h = height * transform.scale
        return Rect(transform.offset.x, transform.offset.y,
                    transform.offset.x + w, transform.offset.y + h)
    }

    /** 命中测试。checkAlpha = true 时忽略透明像素 */
    fun hitTest(p: Offset, checkAlpha: Boolean = false): Boolean {
        if (!bounds().contains(p)) return false
        if (!checkAlpha) return true
        val lx = ((p.x - transform.offset.x) / transform.scale).toInt()
        val ly = ((p.y - transform.offset.y) / transform.scale).toInt()
        if (lx !in 0 until width || ly !in 0 until height) return false
        val buffer = IntArray(1)
        image.readPixels(
            buffer = buffer,
            startX = lx, startY = ly,
            width = 1, height = 1,
        )
        return (buffer[0] ushr 24) > 20
    }
}


/** 形状元素：直线 / 箭头 / 矩形 / 椭圆 */
data class ShapeElement(
    override val id: Long,
    override val transform: ElementTransform = ElementTransform.Identity,
    val shapeType: ShapeType,
    val start: Offset,
    val end: Offset,
    val brush: BrushStyle,
    val filled: Boolean = false,
) : DrawElement {
    val isEmpty: Boolean get() = start.x == end.x && start.y == end.y

    fun bounds(): Rect {
        val half = brush.width / 2f + (if (shapeType == ShapeType.Arrow) brush.width * 4f else 0f)
        return Rect(
            minOf(start.x, end.x) - half,
            minOf(start.y, end.y) - half,
            maxOf(start.x, end.x) + half,
            maxOf(start.y, end.y) + half,
        )
    }

    fun hitTest(p: Offset, tolerance: Float = 6f): Boolean {
        val b = bounds()
        if (b.isEmpty) return false
        if (p.x < b.left - tolerance || p.x > b.right + tolerance ||
            p.y < b.top - tolerance || p.y > b.bottom + tolerance) return false

        return when (shapeType) {
            ShapeType.Line, ShapeType.Arrow -> {
                val r = brush.width / 2f + tolerance
                segmentDistanceSq(p, start.x, start.y, end.x, end.y) <= r * r
            }
            ShapeType.Rect -> {
                val l = minOf(start.x, end.x) - brush.width / 2f
                val t = minOf(start.y, end.y) - brush.width / 2f
                val r = maxOf(start.x, end.x) + brush.width / 2f
                val bb = maxOf(start.y, end.y) + brush.width / 2f
                p.x in l..r && p.y in t..bb
            }
            ShapeType.Ellipse -> {
                val cx = (start.x + end.x) / 2f
                val cy = (start.y + end.y) / 2f
                val rx = kotlin.math.abs(end.x - start.x) / 2f + brush.width / 2f + tolerance
                val ry = kotlin.math.abs(end.y - start.y) / 2f + brush.width / 2f + tolerance
                if (rx < 0.01f || ry < 0.01f) false
                else {
                    val dx = (p.x - cx) / rx
                    val dy = (p.y - cy) / ry
                    dx * dx + dy * dy <= 1f
                }
            }
        }
    }

    private fun segmentDistanceSq(p: Offset, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val abx = bx - ax; val aby = by - ay
        val apx = p.x - ax; val apy = p.y - ay
        val len2 = abx * abx + aby * aby
        if (len2 < 1e-6f) return apx * apx + apy * apy
        val t = ((apx * abx + apy * aby) / len2).coerceIn(0f, 1f)
        val cx = ax + abx * t; val cy = ay + aby * t
        val dx = p.x - cx; val dy = p.y - cy
        return dx * dx + dy * dy
    }
}

/** 复制元素时生成新 id，避免跨帧/跨层 id 冲突 */
fun DrawElement.withNewId(): DrawElement = when (this) {
    is VectorStroke -> copy(id = newId())
    is BitmapElement -> copy(id = newId())
    is ShapeElement -> copy(id = newId())
}
