package com.grillpoet.animy.domain.selection

import androidx.compose.ui.graphics.ImageBitmap
import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.ElementTransform
import com.grillpoet.animy.domain.model.Rect
import com.grillpoet.animy.domain.model.newId

/** 浮动来源，决定操作栏按钮组合 */
enum class FloatingSource {
    /** 从现有内容抠出（选区） */
    Selection,
    /** 导入的外部图片 */
    ImageImport,
}

/**
 * 浮动内容。
 *
 * 三种场景共用：
 *   1. 选区建立后拖动 → Selection
 *   2. 导入图片 → ImageImport
 *
 * 变换期间只改 transform；确定时栅格化到图层。
 */
data class FloatingSelection(
    val id: Long = newId(),

    /** 浮动的位图内容（已裁剪到自身 bounds） */
    val image: ImageBitmap,

    /** 在画布中的基准位置（图层的 canvas 坐标原点） */
    val baseX: Float,
    val baseY: Float,

    /** 相对基准位置的仿射变换 */
    val transform: ElementTransform = ElementTransform.Identity,

    /** 原始选区（仅 Selection 模式有意义；ImageImport 模式为空占位） */
    val selection: Selection,

    /** 图层原始元素列表，用于取消 / commit 的 old 引用 */
    val backupElements: List<DrawElement>,

    val sourceFrameIndex: Int,
    val sourceLayerIndex: Int,

    val source: FloatingSource = FloatingSource.Selection,
) {
    val canvasLeft: Float get() = baseX + transform.offset.x
    val canvasTop: Float get() = baseY + transform.offset.y

    val isImageImport: Boolean get() = source == FloatingSource.ImageImport

    fun bounds(): Rect {
        val w = image.width * transform.scale
        val h = image.height * transform.scale
        return Rect(canvasLeft, canvasTop, canvasLeft + w, canvasTop + h)
    }
}
