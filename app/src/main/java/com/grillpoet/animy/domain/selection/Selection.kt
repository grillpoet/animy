package com.grillpoet.animy.domain.selection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntRect
import java.nio.ByteBuffer
import android.graphics.Path as AndroidPath

/**
 * 选区 = 与画布像素一一对应的 mask。
 *
 * 生成方式：魔棒（洪水填充）/ 套索 / 矩形 / 椭圆。
 * 性能：洪水填充使用扫描线算法；套索用 ALPHA_8 Bitmap 光栅化。
 */
class Selection(
    val mask: BooleanArray,
    val width: Int,
    val height: Int,
) {
    val bounds: IntRect by lazy { computeBounds() }
    val isEmpty: Boolean get() = bounds.isEmpty

    fun contains(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        return mask[y * width + x]
    }

    fun contains(p: Offset): Boolean = contains(p.x.toInt(), p.y.toInt())

    /** 选区内的像素数量 */
    val pixelCount: Int by lazy { mask.count { it } }

    /** 轮廓 Path（蚂蚁线）。只遍历选区 bounds 内的像素。 */
    fun buildOutlinePath(): Path {
        val path = Path()
        if (isEmpty) return path
        val w = width; val m = mask
        val bl = bounds.left; val bt = bounds.top
        val br = bounds.right; val bb = bounds.bottom

        for (y in bt until bb) {
            val row = y * w
            for (x in bl until br) {
                val idx = row + x
                if (!m[idx]) continue
                if (x == 0 || !m[idx - 1]) {
                    path.moveTo(x.toFloat(), y.toFloat())
                    path.lineTo(x.toFloat(), (y + 1).toFloat())
                }
                if (x == w - 1 || !m[idx + 1]) {
                    path.moveTo((x + 1).toFloat(), y.toFloat())
                    path.lineTo((x + 1).toFloat(), (y + 1).toFloat())
                }
                if (y == 0 || !m[idx - w]) {
                    path.moveTo(x.toFloat(), y.toFloat())
                    path.lineTo((x + 1).toFloat(), y.toFloat())
                }
                if (y == height - 1 || !m[idx + w]) {
                    path.moveTo(x.toFloat(), (y + 1).toFloat())
                    path.lineTo((x + 1).toFloat(), (y + 1).toFloat())
                }
            }
        }
        return path
    }

    private fun computeBounds(): IntRect {
        var minX = width; var minY = height
        var maxX = -1;   var maxY = -1
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (mask[row + x]) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        return if (maxX < 0) IntRect(0, 0, 0, 0)
        else IntRect(minX, minY, maxX + 1, maxY + 1)
    }

    companion object {

        /**
         * 魔棒：扫描线洪水填充。
         * 已修复重复入栈问题（入栈前先判 visited）。
         */
        fun magicWand(bitmap: Bitmap, tap: Offset, tolerance: Int = 60): Selection? {
            val w = bitmap.width
            val h = bitmap.height
            if (w == 0 || h == 0) return null
            val x = tap.x.toInt().coerceIn(0, w - 1)
            val y = tap.y.toInt().coerceIn(0, h - 1)

            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

            // ⚠ 关键：种子必须是**不透明**的像素
            //   否则用户点空白处会选中整个透明背景
            val seed = pixels[y * w + x]
            if ((seed ushr 24) < ALPHA_THRESHOLD) return null

            val tol2 = tolerance * tolerance
            val mask = BooleanArray(w * h)
            val queued = BooleanArray(w * h)

            // 判断像素是否可选中：必须不透明 + 颜色相近
            fun match(c: Int): Boolean =
                (c ushr 24) >= ALPHA_THRESHOLD && colorDist(c, seed) <= tol2

            val stack = ArrayDeque<Int>()
            stack.addLast(y * w + x)
            queued[y * w + x] = true

            while (stack.isNotEmpty()) {
                val idx = stack.removeLast()
                if (mask[idx]) continue
                if (!match(pixels[idx])) continue

                val cy = idx / w
                val rowBase = cy * w

                var lx = idx % w
                while (lx > 0 && !mask[rowBase + lx - 1] &&
                    match(pixels[rowBase + lx - 1])) lx--
                var rx = idx % w
                while (rx < w - 1 && !mask[rowBase + rx + 1] &&
                    match(pixels[rowBase + rx + 1])) rx++

                for (i in lx..rx) mask[rowBase + i] = true

                for (dy in intArrayOf(-1, 1)) {
                    val ny = cy + dy
                    if (ny < 0 || ny >= h) continue
                    val nRow = ny * w
                    var i = lx
                    while (i <= rx) {
                        while (i <= rx && (mask[nRow + i] || !match(pixels[nRow + i]))) i++
                        if (i <= rx) {
                            if (!queued[nRow + i]) {
                                stack.addLast(nRow + i)
                                queued[nRow + i] = true
                            }
                            while (i <= rx && !mask[nRow + i] && match(pixels[nRow + i])) i++
                        }
                    }
                }
            }

            val sel = Selection(mask, w, h)
            return if (sel.isEmpty) null else sel
        }

        /** 不透明阈值。alpha < 8 视为完全透明 */
        private const val ALPHA_THRESHOLD = 8

        /** 矩形选区。左闭右开，避免多选一像素。 */
        fun rect(x0: Float, y0: Float, x1: Float, y1: Float, w: Int, h: Int): Selection? {
            val l = minOf(x0, x1).toInt().coerceIn(0, w - 1)
            val r = maxOf(x0, x1).toInt().coerceIn(0, w - 1)
            val t = minOf(y0, y1).toInt().coerceIn(0, h - 1)
            val b = maxOf(y0, y1).toInt().coerceIn(0, h - 1)
            if (l >= r || t >= b) return null
            val mask = BooleanArray(w * h)
            for (y in t until b) {
                val row = y * w
                for (x in l until r) mask[row + x] = true
            }
            return Selection(mask, w, h)
        }

        /** 椭圆选区。 */
        fun ellipse(x0: Float, y0: Float, x1: Float, y1: Float, w: Int, h: Int): Selection? {
            val l = minOf(x0, x1).toInt().coerceIn(0, w - 1)
            val r = maxOf(x0, x1).toInt().coerceIn(0, w - 1)
            val t = minOf(y0, y1).toInt().coerceIn(0, h - 1)
            val b = maxOf(y0, y1).toInt().coerceIn(0, h - 1)
            if (l >= r || t >= b) return null
            val cx = (l + r) / 2f
            val cy = (t + b) / 2f
            val rx = (r - l) / 2f
            val ry = (b - t) / 2f
            val mask = BooleanArray(w * h)
            for (y in t until b) {
                val row = y * w
                val dy = (y + 0.5f - cy) / ry
                val dxMax = rx * kotlin.math.sqrt((1f - dy * dy).coerceAtLeast(0f))
                val xl = (cx - dxMax).toInt().coerceAtLeast(l)
                val xr = (cx + dxMax).toInt().coerceAtMost(r - 1)
                for (x in xl..xr) mask[row + x] = true
            }
            val sel = Selection(mask, w, h)
            return if (sel.isEmpty) null else sel
        }

        /**
         * 套索：把用户路径光栅化为 mask。
         * 使用 ALPHA_8 Bitmap + copyPixelsToBuffer，比逐像素 getPixel 快 ~10x。
         */
        fun lasso(points: List<Offset>, w: Int, h: Int): Selection? {
            if (points.size < 3) return null
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
            val canvas = Canvas(bmp)
            val paint = Paint().apply {
                isAntiAlias = false
                style = Paint.Style.FILL
                color = android.graphics.Color.WHITE
            }
            val path = AndroidPath().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                close()
            }
            canvas.drawPath(path, paint)

            val bytes = ByteArray(w * h)
            bmp.copyPixelsToBuffer(ByteBuffer.wrap(bytes))
            bmp.recycle()

            val mask = BooleanArray(w * h)
            var any = false
            for (i in bytes.indices) {
                if (bytes[i].toInt() != 0) {
                    mask[i] = true
                    any = true
                }
            }
            return if (any) Selection(mask, w, h) else null
        }

        private fun colorDist(c1: Int, c2: Int): Int {
            val dr = ((c1 shr 16) and 0xFF) - ((c2 shr 16) and 0xFF)
            val dg = ((c1 shr 8) and 0xFF) - ((c2 shr 8) and 0xFF)
            val db = (c1 and 0xFF) - (c2 and 0xFF)
            val da = ((c1 ushr 24) and 0xFF) - ((c2 ushr 24) and 0xFF)
            return dr * dr + dg * dg + db * db + da * da
        }

        /** 便捷：从 Compose ImageBitmap 走魔棒 */
        fun magicWand(image: ImageBitmap, tap: Offset, tolerance: Int = 60): Selection? =
            magicWand(image.asAndroidBitmap(), tap, tolerance)
    }
}
