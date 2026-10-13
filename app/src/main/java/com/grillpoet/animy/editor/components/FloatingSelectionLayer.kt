package com.grillpoet.animy.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.grillpoet.animy.domain.selection.FloatingSelection
import com.grillpoet.animy.editor.Viewport

/**
 * 浮动选区渲染层。
 *
 * 变换语义与 BitmapCompositor.drawBitmapElement 严格一致：
 *   - 图片左上角在画布坐标 (baseX + transform.offset.x, baseY + transform.offset.y)
 *   - scale 和 rotation 都绕图片中心
 *
 * 关键：layout 尺寸是「屏幕像素」(w * viewport.scale)，
 *   这样 transformOrigin = (0.5, 0.5) 的 pivot 就正好在图片中心的屏幕位置。
 *   scale 只应用用户的 transform.scale（画布缩放已隐含在 layout 尺寸里）。
 */
@Composable
fun FloatingSelectionLayer(
    floating: FloatingSelection,
    viewport: Viewport,
    modifier: Modifier = Modifier,
) {
    val s = viewport.scale
    if (s.isNaN() || s <= 0f) return

    val density = LocalDensity.current
    val origin = viewport.origin
    val canvasLeft = floating.baseX + floating.transform.offset.x
    val canvasTop = floating.baseY + floating.transform.offset.y
    val screenX = origin.x + canvasLeft * s
    val screenY = origin.y + canvasTop * s

    // 屏幕上未旋转未缩放时的尺寸（已包含画布缩放）
    val screenW = floating.image.width * s
    val screenH = floating.image.height * s

    Box(modifier.fillMaxSize()) {
        Image(
            bitmap = floating.image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .graphicsLayer {
                    translationX = screenX
                    translationY = screenY
                    scaleX = floating.transform.scale
                    scaleY = floating.transform.scale
                    rotationZ = floating.transform.rotation
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
                .then(
                    with(density) {
                        Modifier.requiredSize(
                            width = screenW.toDp(),
                            height = screenH.toDp(),
                        )
                    }
                ),
        )
    }
}
