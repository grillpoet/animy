package com.grillpoet.animy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LayersIcon(tint: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = 2.dp.toPx()
        val offsets = listOf(0f, 0.18f, 0.36f)
        offsets.forEachIndexed { i, oy ->
            val alpha = 1f - i * 0.25f
            val p = Path().apply {
                moveTo(w * 0.5f, h * (0.15f + oy))
                lineTo(w * 0.9f, h * (0.38f + oy))
                lineTo(w * 0.5f, h * (0.61f + oy))
                lineTo(w * 0.1f, h * (0.38f + oy))
                close()
            }
            drawPath(
                path = p,
                color = tint.copy(alpha = alpha),
                style = Stroke(width = sw, join = StrokeJoin.Round),
            )
        }
    }
}
