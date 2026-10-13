package com.grillpoet.animy.data.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.grillpoet.animy.domain.model.BitmapElement
import com.grillpoet.animy.domain.model.BrushType
import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.Frame
import com.grillpoet.animy.domain.model.Layer
import com.grillpoet.animy.domain.model.Project
import com.grillpoet.animy.domain.model.ShapeElement
import com.grillpoet.animy.domain.model.ShapeType
import com.grillpoet.animy.domain.model.StrokePoint
import com.grillpoet.animy.domain.model.VectorStroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object BitmapCompositor {

    // ═══════════ 公开 API ═══════════

    fun renderFrame(
        frame: Frame,
        size: IntSize,
        background: Color = Color.White,
    ): ImageBitmap {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            if (background.alpha > 0f) drawRect(background)
            frame.layers.forEach { l ->
                if (l.visible && l.alpha > 0f) drawLayer(l, 1f)
            }
        }
        return bmp
    }

    fun renderFrameExcept(
        frame: Frame,
        exceptLayerIndex: Int,
        size: IntSize,
        background: Color = Color.White,
    ): ImageBitmap {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            if (background.alpha > 0f) drawRect(background)
            frame.layers.forEachIndexed { i, l ->
                if (i == exceptLayerIndex) return@forEachIndexed
                if (l.visible && l.alpha > 0f) drawLayer(l, 1f)
            }
        }
        return bmp
    }

    fun renderLayer(layer: Layer, size: IntSize): ImageBitmap {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            if (layer.visible && layer.alpha > 0f) drawLayer(layer, 1f)
        }
        return bmp
    }

    /**
     * 渲染图层内容，但**不应用 layer.alpha**。
     *
     * 用途：浮动选区抠图、合并图层 —— 这些场景需要"原始内容"，
     * alpha 应该在合成/预览时单独应用。
     */
    fun renderLayerRaw(layer: Layer, size: IntSize): ImageBitmap {
        val w = size.width.coerceAtLeast(1)
        val h = size.height.coerceAtLeast(1)
        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            if (!layer.visible) return@draw
            // 用 1.0 的 alpha 强制绘制
            drawIntoCanvas { c ->
                if (layer.needsLayerIsolation) {
                    c.saveLayer(
                        androidx.compose.ui.geometry.Rect(0f, 0f, w.toFloat(), h.toFloat()),
                        Paint(),
                    )
                }
                layer.elements.forEach { drawElement(it, 1f) }
                if (layer.needsLayerIsolation) c.restore()
            }
        }
        return bmp
    }

    /** 项目缩略图（首页卡片用） */
    fun renderThumbnail(project: Project, maxDim: Int): ImageBitmap {
        val cw = project.canvasSize.width.coerceAtLeast(1)
        val ch = project.canvasSize.height.coerceAtLeast(1)
        val k = maxDim.toFloat() / maxOf(cw, ch)
        val w = (cw * k).toInt().coerceAtLeast(1)
        val h = (ch * k).toInt().coerceAtLeast(1)

        val img = ImageBitmap(w, h)
        val canvas = Canvas(img)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            drawRect(project.background)
            withTransform({ scale(k, k, pivot = Offset.Zero) }) {
                project.frames.firstOrNull()?.layers?.forEach { l ->
                    if (l.visible && l.alpha > 0f) drawLayer(l, 1f)
                }
            }
        }
        return img
    }

    /**
     * 单帧缩略图（帧时间线用）。
     *
     * 实现：把整个绘制调用包在 DrawScope.scale(k, k) 里 —— 笔画宽度、
     * 位图元素尺寸全部按比例缩放。这样视觉与全尺寸完全一致，
     * 不会出现"缩小后笔画特别粗"的问题。
     *
     * 关键：scale 必须在最外层（drawLayer 之前），
     *       这样内部所有 drawPath/drawCircle 的坐标和宽度都被统一缩放。
     */
    fun renderFrameThumb(
        frame: Frame,
        canvasSize: IntSize,
        background: Color,
        maxDim: Int = 128,
    ): ImageBitmap {
        val cw = canvasSize.width.coerceAtLeast(1)
        val ch = canvasSize.height.coerceAtLeast(1)
        val k = maxDim.toFloat() / maxOf(cw, ch)
        val w = (cw * k).toInt().coerceAtLeast(1)
        val h = (ch * k).toInt().coerceAtLeast(1)

        val bmp = ImageBitmap(w, h)
        val canvas = Canvas(bmp)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            drawRect(background)
            withTransform({ scale(k, k, pivot = Offset.Zero) }) {
                frame.layers.forEach { l ->
                    if (l.visible && l.alpha > 0f) drawLayer(l, 1f)
                }
            }
        }
        return bmp
    }

    // ═══════════ 内部绘制 ═══════════

    private fun DrawScope.drawLayer(layer: Layer, parentAlpha: Float) {
        val a = parentAlpha * layer.alpha
        if (a <= 0f) return

        if (!layer.needsLayerIsolation) {
            layer.elements.forEach { drawElement(it, a) }
            return
        }

        drawIntoCanvas { canvas ->
            canvas.saveLayer(
                androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                Paint(),
            )
            layer.elements.forEach { drawElement(it, a) }
            canvas.restore()
        }
    }

    private fun DrawScope.drawElement(e: DrawElement, alpha: Float) = when (e) {
        is VectorStroke -> drawStroke(e, alpha)
        is BitmapElement -> drawBitmapElement(e, alpha)
        is ShapeElement -> drawShapeElement(e, alpha)
    }

    private fun DrawScope.drawShapeElement(e: ShapeElement, parentAlpha: Float) {
        if (e.isEmpty) return
        val brush = e.brush
        val alpha = (parentAlpha * brush.opacity).coerceIn(0f, 1f)
        if (alpha <= 0f) return

        val b = e.bounds()
        val pivot = Offset(b.left + b.width / 2f, b.top + b.height / 2f)

        withTransform({
            translate(e.transform.offset.x, e.transform.offset.y)
            if (e.transform.scale != 1f || e.transform.rotation != 0f) {
                rotate(e.transform.rotation, pivot = pivot)
                scale(e.transform.scale, e.transform.scale, pivot = pivot)
            }
        }) {
            val color = brush.color
            val strokeStyle = Stroke(
                brush.width,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val style = if (e.filled) Fill else strokeStyle
            when (e.shapeType) {
                ShapeType.Line -> drawLine(
                    color, e.start, e.end, brush.width, StrokeCap.Round, alpha = alpha,
                )
                ShapeType.Arrow -> {
                    drawLine(
                        color, e.start, e.end, brush.width, StrokeCap.Round, alpha = alpha,
                    )
                    val dx = e.end.x - e.start.x
                    val dy = e.end.y - e.start.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (len > 1f) {
                        val ux = dx / len
                        val uy = dy / len
                        val headLen = (brush.width * 4f).coerceAtMost(len * 0.5f)
                        val headHalf = headLen * 0.6f
                        val bx = e.end.x - ux * headLen
                        val by = e.end.y - uy * headLen
                        val px = -uy * headHalf
                        val py = ux * headHalf
                        val path = Path().apply {
                            moveTo(e.end.x, e.end.y)
                            lineTo(bx + px, by + py)
                            lineTo(bx - px, by - py)
                            close()
                        }
                        drawPath(path, color, alpha = alpha)
                    }
                }
                ShapeType.Rect -> {
                    val l = minOf(e.start.x, e.end.x)
                    val t = minOf(e.start.y, e.end.y)
                    val w = kotlin.math.abs(e.end.x - e.start.x)
                    val h = kotlin.math.abs(e.end.y - e.start.y)
                    drawRect(color, Offset(l, t), Size(w, h), style = style, alpha = alpha)
                }
                ShapeType.Ellipse -> {
                    val l = minOf(e.start.x, e.end.x)
                    val t = minOf(e.start.y, e.end.y)
                    val w = kotlin.math.abs(e.end.x - e.start.x)
                    val h = kotlin.math.abs(e.end.y - e.start.y)
                    drawOval(color, Offset(l, t), Size(w, h), style = style, alpha = alpha)
                }
            }
        }
    }

    private fun DrawScope.drawStroke(s: VectorStroke, parentAlpha: Float) {
        if (s.isEmpty) return
        val brush = s.brush
        val alpha = parentAlpha * brush.opacity

        val b = s.bounds()
        val pivot = Offset(b.left + b.width / 2f, b.top + b.height / 2f)

        withTransform({
            translate(s.transform.offset.x, s.transform.offset.y)
            if (s.transform.scale != 1f || s.transform.rotation != 0f) {
                rotate(s.transform.rotation, pivot = pivot)
                scale(s.transform.scale, s.transform.scale, pivot = pivot)
            }
        }) {
            when (brush.type) {
                BrushType.Pen         -> strokePath(s.points, brush, alpha, StrokeCap.Round, 1f)
                BrushType.Marker      -> strokePath(s.points, brush, alpha, StrokeCap.Square, 0.85f)
                BrushType.Highlighter -> strokePath(s.points, brush, alpha, StrokeCap.Round, 0.35f)
                BrushType.Airbrush    -> airbrush(s, alpha)
                BrushType.Eraser      -> strokePath(s.points, brush, alpha, StrokeCap.Round, 1f,
                                                    blendMode = BlendMode.Clear)
            }
        }
    }

    private fun DrawScope.strokePath(
        points: List<StrokePoint>,
        brush: com.grillpoet.animy.domain.model.BrushStyle,
        alpha: Float,
        cap: StrokeCap,
        opacity: Float,
        blendMode: BlendMode = BlendMode.SrcOver,
    ) = drawPath(
        path = points.toSmoothPath(),
        color = brush.color,
        alpha = (alpha * opacity).coerceIn(0f, 1f),
        style = Stroke(brush.width, cap = cap, join = StrokeJoin.Round),
        blendMode = blendMode,
    )

    private fun DrawScope.airbrush(s: VectorStroke, alpha: Float) {
        val brush = s.brush
        val rng = Random(s.id.toInt())
        val spread = brush.width * 1.5f
        val dotR = brush.width * 0.35f
        val count = (brush.width / 2f).toInt().coerceIn(4, 24)
        s.points.forEach { p ->
            repeat(count) {
                val a = rng.nextFloat() * 6.2831855f
                val r = rng.nextFloat() * spread
                drawCircle(
                    color = brush.color,
                    radius = dotR,
                    center = Offset(p.x + cos(a) * r, p.y + sin(a) * r),
                    alpha = (alpha * 0.12f).coerceIn(0f, 1f),
                )
            }
        }
    }

    private fun DrawScope.drawBitmapElement(e: BitmapElement, alpha: Float) {
        val w = e.image.width.toFloat()
        val h = e.image.height.toFloat()
        val pivot = Offset(w / 2f, h / 2f)
        withTransform({
            translate(e.transform.offset.x, e.transform.offset.y)
            if (e.transform.scale != 1f || e.transform.rotation != 0f) {
                // 与 FloatingSelectionLayer 严格一致：都绕图片中心
                rotate(e.transform.rotation, pivot = pivot)
                scale(e.transform.scale, e.transform.scale, pivot = pivot)
            }
        }) {
            drawImage(e.image, alpha = alpha, filterQuality = FilterQuality.High)
        }
    }

    private fun List<StrokePoint>.toSmoothPath(): Path = Path().apply {
        if (isEmpty()) return@apply
        moveTo(this@toSmoothPath[0].x, this@toSmoothPath[0].y)
        if (size == 1) {
            lineTo(this@toSmoothPath[0].x + 0.01f, this@toSmoothPath[0].y)
            return@apply
        }
        for (i in 1 until size) {
            val p = this@toSmoothPath[i - 1]
            val c = this@toSmoothPath[i]
            quadraticBezierTo(p.x, p.y, (p.x + c.x) / 2f, (p.y + c.y) / 2f)
        }
        val last = this@toSmoothPath.last()
        lineTo(last.x, last.y)
    }
}
