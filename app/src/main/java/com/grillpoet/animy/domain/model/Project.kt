package com.grillpoet.animy.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize

enum class ProjectMode { Animation, Paint }

val DefaultPalette = listOf(
    Color(0xFF1E293B), Color(0xFFEF4444), Color(0xFFF97316),
    Color(0xFFEAB308), Color(0xFF22C55E), Color(0xFF06B6D4),
    Color(0xFF3B82F6), Color(0xFFA855F7), Color(0xFFEC4899),
)

/** 项目 = 帧序列 + 画布配置 + 调色板 */
data class Project(
    val name: String = "Untitled",
    val mode: ProjectMode = ProjectMode.Animation,
    val canvasSize: IntSize = IntSize(1080, 1080),
    val background: Color = Color.White,
    val frames: List<Frame> = listOf(Frame()),
    val palette: List<Color> = DefaultPalette,
) {
    init {
        require(frames.isNotEmpty()) { "Project 必须至少有一帧" }
    }

    val durationMs: Long get() = frames.sumOf { it.durationMs }

    val averageFps: Int
        get() {
            val total = durationMs
            return if (total <= 0) 12 else (frames.size * 1000L / total).toInt().coerceIn(1, 60)
        }
}
