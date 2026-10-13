package com.grillpoet.animy.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.grillpoet.animy.domain.model.ShapeType

/**
 * 形状选择行。
 *
 * 图标全部用 Canvas 矢量绘制，36dp 圆形按钮内居中：
 *   · 直线    ─ 斜线
 *   · 箭头    → 带实心头部的箭头
 *   · 矩形    ▭ 实心 / 空心
 *   · 椭圆    ◯ 实心 / 空心
 *
 * 选中判定 = (shapeType, filled) 联合状态。
 */
@Composable
fun ShapeToolRow(
    current: ShapeType,
    filled: Boolean,
    onSelect: (ShapeType) -> Unit,
    onToggleFilled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapeIconButton(
            selected = current == ShapeType.Line,
            onClick = { onSelect(ShapeType.Line) },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawLineIcon(c) }

        ShapeIconButton(
            selected = current == ShapeType.Arrow,
            onClick = { onSelect(ShapeType.Arrow) },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawArrowIcon(c) }

        ShapeIconButton(
            selected = current == ShapeType.Rect && filled,
            onClick = {
                onSelect(ShapeType.Rect)
                if (!filled) onToggleFilled(true)
            },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawRectIcon(c, filled = true) }

        ShapeIconButton(
            selected = current == ShapeType.Rect && !filled,
            onClick = {
                onSelect(ShapeType.Rect)
                if (filled) onToggleFilled(false)
            },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawRectIcon(c, filled = false) }

        ShapeIconButton(
            selected = current == ShapeType.Ellipse && filled,
            onClick = {
                onSelect(ShapeType.Ellipse)
                if (!filled) onToggleFilled(true)
            },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawEllipseIcon(c, filled = true) }

        ShapeIconButton(
            selected = current == ShapeType.Ellipse && !filled,
            onClick = {
                onSelect(ShapeType.Ellipse)
                if (filled) onToggleFilled(false)
            },
            activeColor = activeColor,
            idleColor = idleColor,
        ) { c -> drawEllipseIcon(c, filled = false) }
    }
}

/**
 * 单个形状按钮：36dp 圆形容器 + 20dp Canvas 图标，图标严格居中。
 */
@Composable
private fun ShapeIconButton(
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    idleColor: Color,
    draw: DrawScope.(Color) -> Unit,
) {
    val tint = if (selected) activeColor else idleColor
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            draw(tint)
        }
    }
}

// ─── 图标绘制：全部在 20dp Canvas 内，视觉中心 (0.5, 0.5) ───

private fun DrawScope.drawLineIcon(color: Color) {
    // 左下 → 右上 斜线
    drawLine(
        color = color,
        start = Offset(size.width * 0.15f, size.height * 0.85f),
        end = Offset(size.width * 0.85f, size.height * 0.15f),
        strokeWidth = size.width * 0.16f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawArrowIcon(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = w * 0.14f
    // 主干
    drawLine(
        color = color,
        start = Offset(w * 0.12f, h * 0.5f),
        end = Offset(w * 0.82f, h * 0.5f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // 实心头（三角）
    val head = Path().apply {
        moveTo(w * 0.90f, h * 0.5f)
        lineTo(w * 0.58f, h * 0.22f)
        lineTo(w * 0.58f, h * 0.78f)
        close()
    }
    drawPath(head, color)
}

private fun DrawScope.drawRectIcon(color: Color, filled: Boolean) {
    val pad = size.width * 0.15f
    val style = if (filled) Fill else Stroke(
        width = size.width * 0.11f,
        join = StrokeJoin.Round,
    )
    drawRect(
        color = color,
        topLeft = Offset(pad, pad),
        size = Size(size.width - pad * 2f, size.height - pad * 2f),
        style = style,
    )
}

private fun DrawScope.drawEllipseIcon(color: Color, filled: Boolean) {
    val pad = size.width * 0.15f
    val style = if (filled) Fill else Stroke(width = size.width * 0.11f)
    drawOval(
        color = color,
        topLeft = Offset(pad, pad),
        size = Size(size.width - pad * 2f, size.height - pad * 2f),
        style = style,
    )
}
