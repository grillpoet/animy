package com.grillpoet.animy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grillpoet.animy.domain.model.BrushType
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle

@Composable fun PenIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawPen(tint) }

@Composable fun MarkerIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawMarker(tint) }

@Composable fun HighlighterIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawHighlighter(tint) }

@Composable fun AirbrushIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawAirbrush(tint) }

@Composable fun EraserIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawEraser(tint) }

@Composable fun EyedropperIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawEyedropper(tint) }

@Composable fun MagicWandIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawMagicWand(tint) }

@Composable fun UndoIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawUndo(tint, mirrored = false) }

@Composable fun RedoIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawUndo(tint, mirrored = true) }

@Composable fun PlayIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawPlay(tint) }

@Composable fun PauseIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawPause(tint) }

@Composable fun PlusIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawPlus(tint) }

@Composable fun SaveIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawSave(tint) }

@Composable fun VideoIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawVideo(tint) }

@Composable fun ImageIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawImageIcon(tint) }

@Composable fun OnionIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawOnion(tint) }

@Composable
private fun IconCanvas(
    modifier: Modifier,
    size: Dp,
    block: DrawScope.() -> Unit,
) {
    Canvas(modifier.size(size)) { block() }
}

private fun DrawScope.sw(): Float = 2.dp.toPx()
private fun DrawScope.stroke(tint: Color, width: Float = sw()): StrokeStyle =
    StrokeStyle(width, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawPen(tint: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.15f, h * 0.85f); lineTo(w * 0.35f, h * 0.55f)
        lineTo(w * 0.75f, h * 0.15f); lineTo(w * 0.85f, h * 0.25f)
        lineTo(w * 0.45f, h * 0.65f); close()
    }
    drawPath(p, tint)
}

private fun DrawScope.drawMarker(tint: Color) {
    val w = size.width; val h = size.height
    drawRoundRect(tint, Offset(w * 0.25f, h * 0.15f), Size(w * 0.5f, h * 0.55f),
        CornerRadius(w * 0.08f))
    drawRect(tint, Offset(w * 0.3f, h * 0.7f), Size(w * 0.4f, h * 0.15f))
}

private fun DrawScope.drawHighlighter(tint: Color) {
    val w = size.width; val h = size.height
    drawRoundRect(tint.copy(alpha = 0.9f), Offset(w * 0.35f, h * 0.1f),
        Size(w * 0.3f, h * 0.5f), CornerRadius(w * 0.06f))
    drawRect(tint.copy(alpha = 0.4f), Offset(w * 0.3f, h * 0.6f), Size(w * 0.4f, h * 0.3f))
}

private fun DrawScope.drawAirbrush(tint: Color) {
    val w = size.width; val h = size.height
    drawCircle(tint, w * 0.12f, Offset(w * 0.5f, h * 0.5f))
    listOf(
        0.5f to 0.15f, 0.85f to 0.5f, 0.5f to 0.85f, 0.15f to 0.5f,
        0.25f to 0.25f, 0.75f to 0.25f, 0.75f to 0.75f, 0.25f to 0.75f,
    ).forEach { (dx, dy) ->
        drawCircle(tint.copy(alpha = 0.55f), w * 0.06f, Offset(w * dx, h * dy))
    }
}

private fun DrawScope.drawEraser(tint: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.2f, h * 0.7f); lineTo(w * 0.55f, h * 0.2f)
        lineTo(w * 0.85f, h * 0.4f); lineTo(w * 0.55f, h * 0.85f); close()
    }
    drawPath(p, tint)
    drawLine(Color.White, Offset(w * 0.38f, h * 0.62f), Offset(w * 0.68f, h * 0.78f), sw())
}

private fun DrawScope.drawEyedropper(tint: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.25f, h * 0.75f); lineTo(w * 0.7f, h * 0.3f)
        lineTo(w * 0.8f, h * 0.4f); lineTo(w * 0.35f, h * 0.85f); close()
    }
    drawPath(p, tint)
    drawCircle(tint, w * 0.1f, Offset(w * 0.78f, h * 0.22f))
    val drop = Path().apply {
        moveTo(w * 0.18f, h * 0.9f); lineTo(w * 0.25f, h * 0.78f); lineTo(w * 0.32f, h * 0.9f); close()
    }
    drawPath(drop, tint.copy(alpha = 0.5f))
}

private fun DrawScope.drawMagicWand(tint: Color) {
    val w = size.width; val h = size.height
    drawLine(tint, Offset(w * 0.22f, h * 0.88f), Offset(w * 0.62f, h * 0.48f),
        3.dp.toPx(), StrokeCap.Round)
    val star = Path().apply {
        moveTo(w * 0.75f, h * 0.12f); lineTo(w * 0.79f, h * 0.26f)
        lineTo(w * 0.93f, h * 0.30f); lineTo(w * 0.79f, h * 0.34f)
        lineTo(w * 0.75f, h * 0.48f); lineTo(w * 0.71f, h * 0.34f)
        lineTo(w * 0.57f, h * 0.30f); lineTo(w * 0.71f, h * 0.26f); close()
    }
    drawPath(star, tint)
    drawCircle(tint.copy(alpha = 0.7f), 1.2.dp.toPx(), Offset(w * 0.5f, h * 0.15f))
    drawCircle(tint.copy(alpha = 0.5f), 1.dp.toPx(), Offset(w * 0.9f, h * 0.6f))
}

private fun DrawScope.drawUndo(tint: Color, mirrored: Boolean) {
    val w = size.width; val h = size.height
    val s = if (mirrored) -1f else 1f
    drawArc(
        color = tint,
        startAngle = if (mirrored) 0f else 180f,
        sweepAngle = if (mirrored) -270f else 270f,
        useCenter = false,
        topLeft = Offset(w * 0.15f, h * 0.25f),
        size = Size(w * 0.7f, h * 0.6f),
        style = stroke(tint),
    )
    val tipX = w * (if (mirrored) 0.85f else 0.15f)
    val arrow = Path().apply {
        moveTo(tipX, h * 0.25f); lineTo(tipX + s * w * 0.15f, h * 0.1f)
        moveTo(tipX, h * 0.25f); lineTo(tipX + s * w * 0.15f, h * 0.4f)
    }
    drawPath(arrow, tint, style = stroke(tint))
}

private fun DrawScope.drawPlay(tint: Color) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w * 0.3f, h * 0.2f); lineTo(w * 0.8f, h * 0.5f); lineTo(w * 0.3f, h * 0.8f); close()
    }
    drawPath(p, tint)
}

private fun DrawScope.drawPause(tint: Color) {
    val w = size.width; val h = size.height
    drawRoundRect(tint, Offset(w * 0.28f, h * 0.22f), Size(w * 0.14f, h * 0.56f), CornerRadius(w * 0.05f))
    drawRoundRect(tint, Offset(w * 0.58f, h * 0.22f), Size(w * 0.14f, h * 0.56f), CornerRadius(w * 0.05f))
}

private fun DrawScope.drawPlus(tint: Color) {
    val w = size.width; val h = size.height
    drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), sw(), StrokeCap.Round)
    drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), sw(), StrokeCap.Round)
}

private fun DrawScope.drawSave(tint: Color) {
    val w = size.width; val h = size.height
    drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.6f), sw(), StrokeCap.Round)
    val arrow = Path().apply {
        moveTo(w * 0.32f, h * 0.45f); lineTo(w * 0.5f, h * 0.62f); lineTo(w * 0.68f, h * 0.45f)
    }
    drawPath(arrow, tint, style = stroke(tint))
    drawLine(tint, Offset(w * 0.2f, h * 0.82f), Offset(w * 0.8f, h * 0.82f), sw(), StrokeCap.Round)
}

private fun DrawScope.drawVideo(tint: Color) {
    val w = size.width; val h = size.height
    // 圆角矩形
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.1f, h * 0.2f),
        size = Size(w * 0.8f, h * 0.6f),
        cornerRadius = CornerRadius(w * 0.08f),
        style = stroke(tint),
    )
    // 播放三角
    val tri = Path().apply {
        moveTo(w * 0.42f, h * 0.36f)
        lineTo(w * 0.68f, h * 0.5f)
        lineTo(w * 0.42f, h * 0.64f)
        close()
    }
    drawPath(tri, tint)
}

private fun DrawScope.drawImageIcon(tint: Color) {
    val w = size.width
    val h = size.height
    // 圆角矩形边框
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.15f, h * 0.2f),
        size = Size(w * 0.7f, h * 0.6f),
        cornerRadius = CornerRadius(w * 0.08f),
        style = stroke(tint),
    )
    // 小太阳
    drawCircle(tint, w * 0.06f, Offset(w * 0.35f, h * 0.4f))
    // 山峰
    val tri = Path().apply {
        moveTo(w * 0.2f, h * 0.75f)
        lineTo(w * 0.42f, h * 0.5f)
        lineTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.68f, h * 0.55f)
        lineTo(w * 0.8f, h * 0.75f)
        close()
    }
    drawPath(tri, tint)
}

private fun DrawScope.drawOnion(tint: Color) {
    val w = size.width; val h = size.height
    drawCircle(tint.copy(alpha = 0.35f), w * 0.22f, Offset(w * 0.38f, h * 0.5f))
    drawCircle(tint, w * 0.22f, Offset(w * 0.62f, h * 0.5f))
}

@Composable
fun BrushIcon(brush: BrushType, tint: Color, size: Dp = 22.dp) {
    when (brush) {
        BrushType.Pen         -> PenIcon(tint, s = size)
        BrushType.Marker      -> MarkerIcon(tint, s = size)
        BrushType.Highlighter -> HighlighterIcon(tint, s = size)
        BrushType.Airbrush    -> AirbrushIcon(tint, s = size)
        BrushType.Eraser      -> EraserIcon(tint, s = size)
    }
}


// ═══════════════════════════════════════════════════════════════
//  导航 / 系统图标
// ═══════════════════════════════════════════════════════════════

@Composable fun BackIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawBack(tint) }

@Composable fun SettingsIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawSettings(tint) }

@Composable fun GifIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawGif(tint) }

private fun DrawScope.drawBack(tint: Color) {
    val w = size.width; val h = size.height
    val stroke = sw()
    // 左箭头
    drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f),
        stroke, StrokeCap.Round)
    val arrow = Path().apply {
        moveTo(w * 0.45f, h * 0.25f); lineTo(w * 0.2f, h * 0.5f)
        lineTo(w * 0.45f, h * 0.75f)
    }
    drawPath(arrow, tint, style = stroke(tint))
}

private fun DrawScope.drawSettings(tint: Color) {
    val w = size.width; val h = size.height
    val cx = w / 2f; val cy = h / 2f
    val r = w * 0.18f
    val ringR = w * 0.32f
    // 外圈（8 个短齿）
    repeat(8) { i ->
        val angle = Math.toRadians((i * 45).toDouble())
        val cos = kotlin.math.cos(angle).toFloat()
        val sin = kotlin.math.sin(angle).toFloat()
        drawLine(
            color = tint,
            start = Offset(cx + cos * ringR, cy + sin * ringR),
            end = Offset(cx + cos * (ringR + w * 0.10f), cy + sin * (ringR + w * 0.10f)),
            strokeWidth = sw(),
            cap = StrokeCap.Round,
        )
    }
    // 圆环
    drawCircle(tint, ringR, Offset(cx, cy), style = stroke(tint))
    // 中心
    drawCircle(tint, r, Offset(cx, cy))
}

private fun DrawScope.drawGif(tint: Color) {
    val w = size.width; val h = size.height
    // 圆角矩形外框
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.08f, h * 0.22f),
        size = Size(w * 0.84f, h * 0.56f),
        cornerRadius = CornerRadius(w * 0.10f),
        style = stroke(tint),
    )
    // "GIF" 三个短横（抽象表达）
    val y = h * 0.5f
    drawLine(tint, Offset(w * 0.20f, y), Offset(w * 0.32f, y), sw() * 0.9f, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.42f, y), Offset(w * 0.54f, y), sw() * 0.9f, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.64f, y), Offset(w * 0.80f, y), sw() * 0.9f, StrokeCap.Round)
}


// ═══════════════════════════════════════════════════════════════
//  图层 / 操作图标
// ═══════════════════════════════════════════════════════════════

@Composable fun EyeIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawEye(tint, off = false) }

@Composable fun EyeOffIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawEye(tint, off = true) }

@Composable fun EditIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawEdit(tint) }

@Composable fun ArrowUpIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawArrow(tint, up = true) }

@Composable fun ArrowDownIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawArrow(tint, up = false) }

@Composable fun OpacityIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawOpacity(tint) }

@Composable fun MergeDownIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawMergeDown(tint) }

@Composable fun DeleteIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawDelete(tint) }

@Composable fun MoreVerticalIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawMoreVertical(tint) }

// ─── 绘制实现 ───

private fun DrawScope.drawEye(tint: Color, off: Boolean) {
    val w = size.width; val h = size.height
    val stroke = sw() * 0.9f
    val path = Path().apply {
        moveTo(w * 0.1f, h * 0.5f)
        cubicTo(w * 0.3f, h * 0.2f, w * 0.7f, h * 0.2f, w * 0.9f, h * 0.5f)
        cubicTo(w * 0.7f, h * 0.8f, w * 0.3f, h * 0.8f, w * 0.1f, h * 0.5f)
        close()
    }
    drawPath(path, tint, style = StrokeStyle(
        width = stroke,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    ))
    drawCircle(tint, w * 0.10f, Offset(w * 0.5f, h * 0.5f))
    if (off) {
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.85f),
            end = Offset(w * 0.85f, h * 0.15f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawEdit(tint: Color) {
    val w = size.width; val h = size.height
    val stroke = sw() * 0.9f
    // 笔杆（斜线）
    drawLine(
        color = tint,
        start = Offset(w * 0.25f, h * 0.75f),
        end = Offset(w * 0.75f, h * 0.25f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // 笔尖（三角）
    val tip = Path().apply {
        moveTo(w * 0.18f, h * 0.82f)
        lineTo(w * 0.22f, h * 0.62f)
        lineTo(w * 0.38f, h * 0.78f)
        close()
    }
    drawPath(tip, tint)
}

private fun DrawScope.drawArrow(tint: Color, up: Boolean) {
    val w = size.width; val h = size.height
    val stroke = sw()
    val (y1, y2) = if (up) h * 0.78f to h * 0.22f else h * 0.22f to h * 0.78f
    drawLine(tint, Offset(w * 0.5f, y1), Offset(w * 0.5f, y2), stroke, StrokeCap.Round)
    val tipY = y2
    val dir = if (up) -1f else 1f
    val arrow = Path().apply {
        moveTo(w * 0.30f, tipY + dir * h * 0.15f)
        lineTo(w * 0.5f, tipY)
        lineTo(w * 0.70f, tipY + dir * h * 0.15f)
    }
    drawPath(arrow, tint, style = StrokeStyle(
        width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round,
    ))
}

private fun DrawScope.drawOpacity(tint: Color) {
    val w = size.width; val h = size.height
    val stroke = sw() * 0.9f
    // 外圈
    drawCircle(tint, w * 0.35f, Offset(w * 0.5f, h * 0.5f),
        style = StrokeStyle(width = stroke))
    // 右半实心
    val half = Path().apply {
        moveTo(w * 0.5f, h * 0.15f)
        arcTo(
            androidx.compose.ui.geometry.Rect(
                w * 0.15f, h * 0.15f, w * 0.85f, h * 0.85f,
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(half, tint)
}

private fun DrawScope.drawMergeDown(tint: Color) {
    val w = size.width; val h = size.height
    val stroke = sw() * 0.9f
    // 上方矩形
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.2f, h * 0.15f),
        size = Size(w * 0.6f, h * 0.22f),
        cornerRadius = CornerRadius(w * 0.05f),
        style = StrokeStyle(width = stroke),
    )
    // 向下箭头
    drawLine(tint, Offset(w * 0.5f, h * 0.42f), Offset(w * 0.5f, h * 0.72f),
        stroke, StrokeCap.Round)
    val arrow = Path().apply {
        moveTo(w * 0.32f, h * 0.62f)
        lineTo(w * 0.5f, h * 0.78f)
        lineTo(w * 0.68f, h * 0.62f)
    }
    drawPath(arrow, tint, style = StrokeStyle(
        width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round,
    ))
}

private fun DrawScope.drawDelete(tint: Color) {
    val w = size.width; val h = size.height
    val stroke = sw() * 0.9f
    // 垃圾桶盖
    drawLine(tint, Offset(w * 0.25f, h * 0.28f), Offset(w * 0.75f, h * 0.28f),
        stroke, StrokeCap.Round)
    // 提手
    drawLine(tint, Offset(w * 0.42f, h * 0.28f), Offset(w * 0.42f, h * 0.18f),
        stroke, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.58f, h * 0.28f), Offset(w * 0.58f, h * 0.18f),
        stroke, StrokeCap.Round)
    drawLine(tint, Offset(w * 0.42f, h * 0.18f), Offset(w * 0.58f, h * 0.18f),
        stroke, StrokeCap.Round)
    // 桶身
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.30f, h * 0.34f),
        size = Size(w * 0.40f, h * 0.50f),
        cornerRadius = CornerRadius(w * 0.04f),
        style = StrokeStyle(width = stroke),
    )
}

private fun DrawScope.drawMoreVertical(tint: Color) {
    val w = size.width; val h = size.height
    val r = w * 0.08f
    listOf(0.25f, 0.5f, 0.75f).forEach { y ->
        drawCircle(tint, r, Offset(w * 0.5f, h * y))
    }
}

@Composable fun LiquifyIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawLiquify(tint) }

private fun DrawScope.drawLiquify(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = sw() * 0.9f
    val path = Path().apply {
        moveTo(w * 0.15f, h * 0.5f)
        cubicTo(w * 0.35f, h * 0.2f, w * 0.65f, h * 0.8f, w * 0.85f, h * 0.5f)
    }
    drawPath(path, tint, style = StrokeStyle(width = stroke, cap = StrokeCap.Round))
    drawCircle(tint, w * 0.12f, Offset(w * 0.25f, h * 0.35f))
    drawCircle(tint, w * 0.12f, Offset(w * 0.75f, h * 0.65f))
}

@Composable fun ShapeIcon(tint: Color, m: Modifier = Modifier, s: Dp = 22.dp) =
    IconCanvas(m, s) { drawShapeIcon(tint) }

private fun DrawScope.drawShapeIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = sw() * 0.9f
    // 矩形
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.1f, h * 0.55f),
        size = Size(w * 0.35f, h * 0.35f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f),
        style = StrokeStyle(width = stroke),
    )
    // 圆
    drawCircle(tint, w * 0.18f, Offset(w * 0.7f, h * 0.7f),
        style = StrokeStyle(width = stroke))
    // 斜线（对角）
    drawLine(tint, Offset(w * 0.15f, h * 0.45f), Offset(w * 0.85f, h * 0.15f),
        stroke, StrokeCap.Round)
}

