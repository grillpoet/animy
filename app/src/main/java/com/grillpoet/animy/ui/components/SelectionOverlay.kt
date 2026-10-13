package com.grillpoet.animy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.grillpoet.animy.domain.selection.Selection
import com.grillpoet.animy.editor.Viewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 选区蚂蚁线。
 *
 *   · 松手瞬间 → 用 selection.bounds 画白黑双虚线（立即）
 *   · 精确 path 后台构建 → 就绪后无缝切换
 *
 * 两阶段用同一套蚂蚁线样式，用户视觉上只感到"框一出现就贴合内容"。
 */
@Composable
fun SelectionOverlay(
    selection: Selection,
    viewport: Viewport,
    modifier: Modifier = Modifier,
) {
    // 精确轮廓：后台构建
    val precisePath by produceState<Path?>(initialValue = null, selection) {
        value = withContext(Dispatchers.Default) {
            runCatching { selection.buildOutlinePath() }.getOrNull()
        }
    }

    val b = selection.bounds

    Canvas(modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val s = viewport.scale
        if (s.isNaN() || s <= 0f) return@Canvas

        val origin = viewport.origin
        val lineWidth = 2f / s
        val dash = floatArrayOf(10f, 10f)

        withTransform({
            translate(origin.x, origin.y)
            scale(s, s, pivot = Offset.Zero)
        }) {
            val p = precisePath
            if (p != null) {
                // 精确轮廓 —— 完全贴合内容边缘
                drawPath(p, Color.White,
                    style = Stroke(width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dash, 0f)))
                drawPath(p, Color.Black,
                    style = Stroke(width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dash, 10f)))
            } else if (b.right > b.left && b.bottom > b.top) {
                // 过渡 —— 用 bounds 画同样的蚂蚁线
                val tl = Offset(b.left.toFloat(), b.top.toFloat())
                val sz = Size(
                    (b.right - b.left).toFloat(),
                    (b.bottom - b.top).toFloat(),
                )
                drawRect(Color.White, tl, sz,
                    style = Stroke(width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dash, 0f)))
                drawRect(Color.Black, tl, sz,
                    style = Stroke(width = lineWidth,
                        pathEffect = PathEffect.dashPathEffect(dash, 10f)))
            }
        }
    }
}
