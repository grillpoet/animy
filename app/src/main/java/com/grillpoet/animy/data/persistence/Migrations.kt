package com.grillpoet.animy.data.persistence

import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.grillpoet.animy.domain.model.BitmapElement
import com.grillpoet.animy.domain.model.BrushStyle
import com.grillpoet.animy.domain.model.BrushType
import com.grillpoet.animy.domain.model.DefaultPalette
import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.ElementTransform
import com.grillpoet.animy.domain.model.Frame
import com.grillpoet.animy.domain.model.Layer
import com.grillpoet.animy.domain.model.Project
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.domain.model.ShapeElement
import com.grillpoet.animy.domain.model.ShapeType
import com.grillpoet.animy.domain.model.StrokePoint
import com.grillpoet.animy.domain.model.VectorStroke
import com.grillpoet.animy.domain.model.newId
import java.io.File

/**
 * 存档版本迁移。
 *
 * v1: 只有 frames[].layers[].strokes（无 mode、无 durationMs）
 * v2: 加 mode / canvasSize / background
 * v3: 加每帧 durationMs、元素变换、palette
 *
 * 未知版本按 v1 兜底解析；损坏的存档会被 ProjectRepository
 * 备份为 .corrupt，不覆盖用户数据。
 */
object Migrations {

    fun migrate(saved: SavedProject, imagesDir: File): Project {
        if (saved.version > CURRENT_VERSION) {
            throw IllegalStateException(
                "存档版本 ${saved.version} 高于当前支持的 $CURRENT_VERSION"
            )
        }
        val v = saved.version

        return Project(
            name = saved.name,
            mode = runCatching { ProjectMode.valueOf(saved.mode) }
                .getOrDefault(ProjectMode.Animation),
            canvasSize = IntSize(saved.canvasWidth, saved.canvasHeight),
            background = Color(saved.background.toULong()),
            palette = saved.palette
                .map { Color(it.toULong()) }
                .ifEmpty { DefaultPalette },
            frames = saved.frames.map { sf ->
                Frame(
                    id = newId(),
                    durationMs = if (v >= 3) sf.durationMs.coerceIn(16L, 10_000L)
                                 else Frame.DEFAULT_FRAME_MS,
                    layers = sf.layers.map { sl -> sl.toLayer(imagesDir) },
                )
            },
        )
    }

    // ────────── DTO → Model ──────────

    private fun SavedLayer.toLayer(imagesDir: File): Layer = Layer(
        id = newId(),
        name = name,
        visible = visible,
        alpha = alpha.coerceIn(0f, 1f),
        elements = elements.mapNotNull { it.toElementOrNull(imagesDir) },
    )

    private fun SavedElement.toElementOrNull(imagesDir: File): DrawElement? = when (this) {
        is SavedStroke -> VectorStroke(
            id = id,
            transform = ElementTransform(
                offset = Offset(offsetX, offsetY),
                scale = scale,
                rotation = rotation,
            ),
            points = points.chunked(3).mapNotNull { c ->
                if (c.size < 2) null
                else StrokePoint(
                    x = c[0], y = c[1],
                    pressure = c.getOrElse(2) { 1f },
                )
            },
            brush = BrushStyle(
                type = runCatching { BrushType.valueOf(brushType) }
                    .getOrDefault(BrushType.Pen),
                color = Color(color.toULong()),
                width = width,
                opacity = opacity.coerceIn(0f, 1f),
            ),
        )

        is SavedShape -> ShapeElement(
            id = id,
            transform = ElementTransform(
                offset = Offset(offsetX, offsetY),
                scale = scale,
                rotation = rotation,
            ),
            shapeType = runCatching { ShapeType.valueOf(shapeType) }
                .getOrDefault(ShapeType.Line),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            brush = BrushStyle(
                type = runCatching { BrushType.valueOf(brushType) }
                    .getOrDefault(BrushType.Pen),
                color = Color(color.toULong()),
                width = width,
                opacity = opacity.coerceIn(0f, 1f),
            ),
            filled = filled,
        )

        is SavedBitmap -> {
            val file = File(imagesDir, imagePath)
            if (!file.exists()) null
            else {
                val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
                BitmapElement(
                    id = id,
                    transform = ElementTransform(
                        offset = Offset(offsetX, offsetY),
                        scale = scale,
                        rotation = rotation,
                    ),
                    image = bmp.asImageBitmap(),
                    label = label,
                )
            }
        }
    }
}
