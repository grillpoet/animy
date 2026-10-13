package com.grillpoet.animy.domain.transform

import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.Frame
import com.grillpoet.animy.domain.model.Layer
import com.grillpoet.animy.domain.model.Project

/**
 * 不可变变换工具。
 *
 * 所有「修改某一帧 / 图层 / 元素」的操作集中于此，
 * 避免在每个命令里重复写深拷贝逻辑。
 *
 * 约定：所有函数返回新对象，绝不修改原对象。
 */

// ═══════════ Project ═══════════

fun Project.replaceFrame(index: Int, frame: Frame): Project {
    if (index !in frames.indices) return this
    val list = frames.toMutableList()
    list[index] = frame
    return copy(frames = list)
}

fun Project.addFrame(frame: Frame, at: Int = frames.size): Project {
    val safe = at.coerceIn(0, frames.size)
    val list = frames.toMutableList()
    list.add(safe, frame)
    return copy(frames = list)
}

fun Project.removeFrameAt(index: Int): Project {
    if (frames.size <= 1 || index !in frames.indices) return this
    val list = frames.toMutableList()
    list.removeAt(index)
    return copy(frames = list)
}

fun Project.moveFrame(from: Int, to: Int): Project {
    if (from == to) return this
    if (from !in frames.indices || to !in 0..frames.size) return this
    val list = frames.toMutableList()
    val item = list.removeAt(from)
    list.add(to.coerceIn(0, list.size), item)
    return copy(frames = list)
}

// ═══════════ Frame ═══════════

fun Frame.replaceLayer(index: Int, layer: Layer): Frame {
    if (index !in layers.indices) return this
    val list = layers.toMutableList()
    list[index] = layer
    return copy(layers = list)
}

fun Frame.addLayer(layer: Layer, at: Int = layers.size): Frame {
    val safe = at.coerceIn(0, layers.size)
    val list = layers.toMutableList()
    list.add(safe, layer)
    return copy(layers = list)
}

fun Frame.removeLayerAt(index: Int): Frame {
    if (layers.size <= 1 || index !in layers.indices) return this
    val list = layers.toMutableList()
    list.removeAt(index)
    return copy(layers = list)
}

fun Frame.moveLayer(from: Int, to: Int): Frame {
    if (from == to) return this
    if (from !in layers.indices || to !in 0..layers.size) return this
    val list = layers.toMutableList()
    val item = list.removeAt(from)
    list.add(to.coerceIn(0, list.size), item)
    return copy(layers = list)
}

// ═══════════ Layer ═══════════

fun Layer.addElement(element: DrawElement): Layer =
    copy(elements = elements + element)

fun Layer.removeElement(id: Long): Layer =
    copy(elements = elements.filterNot { it.id == id })

fun Layer.replaceElement(id: Long, transform: (DrawElement) -> DrawElement): Layer =
    copy(elements = elements.map { if (it.id == id) transform(it) else it })

fun Layer.replaceElements(newElements: List<DrawElement>): Layer =
    copy(elements = newElements)
