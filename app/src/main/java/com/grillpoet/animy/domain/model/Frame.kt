package com.grillpoet.animy.domain.model

/** 一帧 = 图层列表 + 本帧时长 */
data class Frame(
    val id: Long = newId(),
    val layers: List<Layer> = listOf(Layer()),
    val durationMs: Long = DEFAULT_FRAME_MS,
) {
    init {
        require(layers.isNotEmpty()) { "Frame 必须至少有一层" }
        require(durationMs in 16L..10_000L) { "帧时长必须在 16ms..10s" }
    }

    val topLayerIndex: Int get() = layers.lastIndex
    val isEmpty: Boolean get() = layers.all { it.isEmpty }

    companion object {
        /** 默认帧时长：12fps ≈ 83ms */
        const val DEFAULT_FRAME_MS = 83L
    }
}
