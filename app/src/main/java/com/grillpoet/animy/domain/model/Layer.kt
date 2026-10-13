package com.grillpoet.animy.domain.model

import androidx.compose.ui.graphics.BlendMode

/** 图层 = 元素列表 + 混合属性 */
data class Layer(
    val id: Long = newId(),
    val name: String = "",
    val visible: Boolean = true,
    val alpha: Float = 1f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val elements: List<DrawElement> = emptyList(),
) {
    init {
        require(alpha in 0f..1f) { "alpha 必须在 0..1" }
    }

    val isEmpty: Boolean get() = elements.isEmpty()
    val elementCount: Int get() = elements.size

    /** 渲染时是否需要 saveLayer（有擦除或非默认混合模式） */
    val needsLayerIsolation: Boolean
        get() = blendMode != BlendMode.SrcOver ||
                alpha < 1f ||
                elements.any { it is VectorStroke && it.brush.isEraser }
}
