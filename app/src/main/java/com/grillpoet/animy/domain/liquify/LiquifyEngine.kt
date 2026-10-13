package com.grillpoet.animy.domain.liquify

import kotlin.math.sqrt

object LiquifyEngine {

    fun applyWarp(
        pixels: IntArray,
        width: Int,
        height: Int,
        centerX: Float,
        centerY: Float,
        moveX: Float,
        moveY: Float,
        radius: Float,
        strength: Float,
    ) {
        if (radius <= 0f || strength <= 0f) return
        val src = pixels.copyOf()
        val r2 = radius * radius
        val left = (centerX - radius).toInt().coerceAtLeast(0)
        val right = (centerX + radius).toInt().coerceAtMost(width - 1)
        val top = (centerY - radius).toInt().coerceAtLeast(0)
        val bottom = (centerY + radius).toInt().coerceAtMost(height - 1)

        for (y in top..bottom) {
            val rowBase = y * width
            val dy = y - centerY
            for (x in left..right) {
                val dx = x - centerX
                val d2 = dx * dx + dy * dy
                if (d2 > r2) continue
                val d = sqrt(d2)
                val t = 1f - d / radius
                val w = t * t * strength
                val sx = x - moveX * w
                val sy = y - moveY * w
                pixels[rowBase + x] = bilinearSample(src, width, height, sx, sy)
            }
        }
    }

    private fun bilinearSample(src: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        if (x <= 0f || y <= 0f || x >= w - 1 || y >= h - 1) {
            val cx = x.toInt().coerceIn(0, w - 1)
            val cy = y.toInt().coerceIn(0, h - 1)
            return src[cy * w + cx]
        }
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = x0 + 1
        val y1 = y0 + 1
        val fx = x - x0
        val fy = y - y0

        val c00 = src[y0 * w + x0]
        val c10 = src[y0 * w + x1]
        val c01 = src[y1 * w + x0]
        val c11 = src[y1 * w + x1]

        fun lerp(v00: Int, v10: Int, v01: Int, v11: Int): Int {
            val v0 = v00 + ((v10 - v00) * fx).toInt()
            val v1 = v01 + ((v11 - v01) * fx).toInt()
            return v0 + ((v1 - v0) * fy).toInt()
        }

        val a = lerp(
            (c00 ushr 24) and 0xFF, (c10 ushr 24) and 0xFF,
            (c01 ushr 24) and 0xFF, (c11 ushr 24) and 0xFF,
        )
        val r = lerp(
            (c00 shr 16) and 0xFF, (c10 shr 16) and 0xFF,
            (c01 shr 16) and 0xFF, (c11 shr 16) and 0xFF,
        )
        val g = lerp(
            (c00 shr 8) and 0xFF, (c10 shr 8) and 0xFF,
            (c01 shr 8) and 0xFF, (c11 shr 8) and 0xFF,
        )
        val b = lerp(
            c00 and 0xFF, c10 and 0xFF,
            c01 and 0xFF, c11 and 0xFF,
        )
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
