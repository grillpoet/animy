package com.grillpoet.animy.domain.command

import com.grillpoet.animy.domain.model.Project

/**
 * 编辑器命令。
 *
 * 撤销/重做采用命令模式：
 *   - 每条命令记录"如何执行"和"如何撤销"
 *   - 只存储操作增量，不存整个 Project 快照
 *   - 内存开销 O(操作数) 而非 O(元素数 × 帧数)
 *
 * 契约：execute(state).let { undo(it) } == state
 *
 * 注意：这里的 state 是 Project（不是 EditorState）。
 * EditorState 的 UI 属性（工具、颜色等）不参与撤销。
 */
interface EditCommand {
    val label: String
    fun execute(project: Project): Project
    fun undo(project: Project): Project
}

/** 把多个命令打包为原子操作 */
class CompositeCommand(
    private val commands: List<EditCommand>,
    override val label: String,
) : EditCommand {
    override fun execute(project: Project): Project =
        commands.fold(project) { acc, cmd -> cmd.execute(acc) }

    override fun undo(project: Project): Project =
        commands.foldRight(project) { cmd, acc -> cmd.undo(acc) }
}
