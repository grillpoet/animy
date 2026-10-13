package com.grillpoet.animy.domain.command

import com.grillpoet.animy.domain.model.Project

/**
 * 撤销/重做管理器。栈容量上限 200，超出丢弃最旧记录。
 *
 * 记录 lastUndoLabel / lastRedoLabel 供 UI 显示反馈。
 */
class HistoryManager(private val maxSize: Int = 200) {

    private val undoStack = ArrayDeque<EditCommand>()
    private val redoStack = ArrayDeque<EditCommand>()

    var lastUndoLabel: String? = null
        private set
    var lastRedoLabel: String? = null
        private set

    fun record(command: EditCommand) {
        undoStack.addLast(command)
        while (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
        lastUndoLabel = null
        lastRedoLabel = null
    }

    fun undo(current: Project): Project? {
        val cmd = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(cmd)
        lastUndoLabel = cmd.label
        lastRedoLabel = null
        return cmd.undo(current)
    }

    fun redo(current: Project): Project? {
        val cmd = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(cmd)
        lastRedoLabel = cmd.label
        lastUndoLabel = null
        return cmd.execute(current)
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastUndoLabel = null
        lastRedoLabel = null
    }
}
