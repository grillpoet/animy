package com.grillpoet.animy.domain.model

import androidx.compose.ui.unit.IntSize

enum class CanvasPreset(val label: String, val size: IntSize) {
    Square("1:1   正方形", IntSize(1080, 1080)),
    Portrait("9:16  竖屏", IntSize(1080, 1920)),
    Landscape("16:9  横屏", IntSize(1920, 1080)),
    Story("4:5   图文", IntSize(1080, 1350)),
}
