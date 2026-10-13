package com.grillpoet.animy.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.editor.components.FrameTimeline
import com.grillpoet.animy.editor.components.ShapeToolRow
import com.grillpoet.animy.editor.components.LiquifyBar
import com.grillpoet.animy.editor.components.ToolButton
import com.grillpoet.animy.editor.components.Toolbar
import com.grillpoet.animy.ui.components.SelectToolRow

/**
 * 编辑器底部区域。
 *
 * 三块：
 *   1. 选区工具行（仅当魔棒激活且无浮动时显示）
 *   2. 工具栏（颜色 + 大小 + 笔刷 + 工具 + 动作按钮）
 *   3. 帧时间线（仅动画模式）
 */
@Composable
fun EditorBottomArea(
    vm: EditorViewModel,
    state: EditorState,
    onRequestExportImage: () -> Unit,
) {
    if (state.isMagicWand && !state.isFloating) {
        SelectToolRow(
            current = state.selectTool,
            onSelect = { vm.onIntent(EditorIntent.SetSelectTool(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    if (state.isShape && !state.isFloating) {
        ShapeToolRow(
            current = state.shapeTool,
            filled = state.shapeFilled,
            onSelect = { vm.onIntent(EditorIntent.SetShapeTool(it)) },
            onToggleFilled = { vm.onIntent(EditorIntent.SetShapeFilled(it)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }

    Column(Modifier.navigationBarsPadding()) {
        if (state.isLiquifyActive) {
            LiquifyBar(
                radius = state.liquifyBrushRadius,
                strength = state.liquifyBrushStrength,
                onRadius = { vm.onIntent(EditorIntent.SetLiquifyBrushRadius(it)) },
                onStrength = { vm.onIntent(EditorIntent.SetLiquifyBrushStrength(it)) },
                onReset = { vm.onIntent(EditorIntent.ResetLiquify) },
                onConfirm = { vm.onIntent(EditorIntent.ConfirmLiquify) },
                onCancel = { vm.onIntent(EditorIntent.ExitLiquify) },
            )
        }

        Toolbar(
            brush = state.brush,
            palette = state.project.palette,
            onBrushType = { vm.onIntent(EditorIntent.SetBrushType(it)) },
            onColor = { vm.onIntent(EditorIntent.SetColor(it)) },
            onSize = { vm.onIntent(EditorIntent.SetBrushSize(it)) },
            activeTool = state.activeTool,
            onTool = { vm.onIntent(EditorIntent.SetTool(it)) },
        ) {
            ToolButton(
                label = "撤销",
                enabled = state.canUndo && !state.isFloating,
            ) { vm.onIntent(EditorIntent.Undo) }

            ToolButton(
                label = "重做",
                enabled = state.canRedo && !state.isFloating,
            ) { vm.onIntent(EditorIntent.Redo) }

            when (state.project.mode) {
                ProjectMode.Animation -> {
                    ToolButton(
                        label = "洋葱皮",
                        active = state.onionSkin.enabled,
                    ) { vm.onIntent(EditorIntent.ToggleOnion) }

                    ToolButton(
                        label = if (state.isPlaying) "暂停" else "播放",
                        active = state.isPlaying,
                    ) { vm.onIntent(EditorIntent.TogglePlay) }

                    ToolButton(label = "新建帧") {
                        vm.onIntent(EditorIntent.AddFrame)
                    }
                    ToolButton(label = "导出视频") {
                        vm.onIntent(EditorIntent.ExportVideo)
                    }
                    ToolButton(label = "导出 GIF") {
                        vm.onIntent(EditorIntent.ExportGif)
                    }
                }
                ProjectMode.Paint -> {
                    ToolButton(label = "导出图片") { onRequestExportImage() }
                }
            }
        }

        if (state.project.mode == ProjectMode.Animation) {
            FrameTimeline(
                frames = state.project.frames,
                canvasSize = state.project.canvasSize,
                background = state.project.background,
                currentIndex = state.currentFrameIndex,
                onSelect = { vm.onIntent(EditorIntent.SelectFrame(it)) },
                onMove = { from, to -> vm.onIntent(EditorIntent.MoveFrame(from, to)) },
                onDuplicate = { i -> vm.onIntent(EditorIntent.DuplicateFrame(i)) },
                onDelete = { i -> vm.onIntent(EditorIntent.DeleteFrame(i)) },
            )
        }
    }
}
