package com.grillpoet.animy.data.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import com.grillpoet.animy.domain.model.Frame
import com.grillpoet.animy.domain.model.Layer

/**
 * 帧渲染缓存。双检锁 + 锁外渲染。
 *
 * 三组缓存：
 *   - 整帧（编辑中的正常显示）
 *   - 排除某层（橡皮预览：其他层 + 当前层隔离）
 *   - 单层（橡皮预览：当前层）
 *
 * 内存：最多 3 张同尺寸位图。
 */
class RenderCache {

    private data class FrameKey(val frame: Frame, val size: IntSize, val bg: Color)
    private data class ExceptKey(val frame: Frame, val exceptIndex: Int, val size: IntSize, val bg: Color)
    private data class LayerKey(val layer: Layer, val size: IntSize)

    @Volatile private var frameKey: FrameKey? = null
    @Volatile private var frameBmp: ImageBitmap? = null

    @Volatile private var exceptKey: ExceptKey? = null
    @Volatile private var exceptBmp: ImageBitmap? = null

    @Volatile private var layerKey: LayerKey? = null
    @Volatile private var layerBmp: ImageBitmap? = null

    private val lock = Any()

    fun getOrRender(frame: Frame, size: IntSize, background: Color): ImageBitmap {
        val k = FrameKey(frame, size, background)
        frameKey?.let { if (it == k) frameBmp?.let { b -> return b } }
        val bmp = BitmapCompositor.renderFrame(frame, size, background)
        synchronized(lock) {
            if (frameKey == k) return frameBmp ?: bmp
            frameKey = k
            frameBmp = bmp
            return bmp
        }
    }

    fun getOrRenderExcluding(
        frame: Frame,
        exceptIndex: Int,
        size: IntSize,
        background: Color,
    ): ImageBitmap {
        val k = ExceptKey(frame, exceptIndex, size, background)
        exceptKey?.let { if (it == k) exceptBmp?.let { b -> return b } }
        val bmp = BitmapCompositor.renderFrameExcept(frame, exceptIndex, size, background)
        synchronized(lock) {
            if (exceptKey == k) return exceptBmp ?: bmp
            exceptKey = k
            exceptBmp = bmp
            return bmp
        }
    }

    fun getOrRenderLayer(layer: Layer, size: IntSize): ImageBitmap {
        val k = LayerKey(layer, size)
        layerKey?.let { if (it == k) layerBmp?.let { b -> return b } }
        val bmp = BitmapCompositor.renderLayer(layer, size)
        synchronized(lock) {
            if (layerKey == k) return layerBmp ?: bmp
            layerKey = k
            layerBmp = bmp
            return bmp
        }
    }

    fun invalidate() {
        synchronized(lock) {
            frameKey = null; frameBmp = null
            exceptKey = null; exceptBmp = null
            layerKey = null; layerBmp = null
        }
    }

    val hasCache: Boolean get() = frameBmp != null
}
