package com.grillpoet.animy.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.grillpoet.animy.domain.model.BrushStyle
import com.grillpoet.animy.domain.model.Frame
import com.grillpoet.animy.domain.model.Layer
import com.grillpoet.animy.domain.model.Project
import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.domain.model.ShapeType
import androidx.compose.ui.graphics.ImageBitmap
import com.grillpoet.animy.domain.selection.FloatingSelection
import com.grillpoet.animy.domain.selection.Selection

enum class ActiveTool { Brush, Eyedropper, MagicWand, Liquify, Shape, Select }
enum class GestureMode { Idle, Drawing, Selecting, Transforming, Panning }
enum class SelectTool { Wand, Lasso, Rect, Ellipse }

data class OnionSkin(
    val enabled: Boolean = false,
    val prev: Int = 2,
    val next: Int = 1,
    val prevAlpha: Float = 0.35f,
    val nextAlpha: Float = 0.25f,
    val tintPrev: Color = Color(0xFFFF4444),
    val tintNext: Color = Color(0xFF4488FF),
) {
    val any: Boolean get() = enabled && (prev > 0 || next > 0)
}

data class LiquifySession(
    val sourceFrameIndex: Int,
    val sourceLayerIndex: Int,
    val backupElements: List<DrawElement>,
    val width: Int,
    val height: Int,
)

data class EditorState(
    val project: Project = Project(),
    val currentFrameIndex: Int = 0,
    val currentLayerIndex: Int = 0,

    val activeTool: ActiveTool = ActiveTool.Brush,
    val gestureMode: GestureMode = GestureMode.Idle,

    val brush: BrushStyle = BrushStyle(),
    val selectTool: SelectTool = SelectTool.Wand,

    val selection: Selection? = null,
    val floatingSelection: FloatingSelection? = null,

    val draftStart: Offset? = null,
    val draftEnd: Offset? = null,

    val shapeTool: ShapeType = ShapeType.Line,
    val shapeFilled: Boolean = false,
    val draftShapeStart: Offset? = null,
    val draftShapeEnd: Offset? = null,
    val isShapePending: Boolean = false,

    val selectedElementId: Long? = null,

    /** 正在绘制的笔画 id —— 预览与持久化共用，Airbrush 种子对齐 */
    val liveStrokeId: Long? = null,

    val isPlaying: Boolean = false,
    val onionSkin: OnionSkin = OnionSkin(),
    val showGrid: Boolean = false,

    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val exportingLabel: String? = null,
    val exportProgress: Float = 0f,
    val errorMessage: String? = null,

    /** 取色后短暂显示的颜色反馈。null 表示无。 */
    val lastPickedColor: Color? = null,

    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    val liquifySession: LiquifySession? = null,
    val liquifyPreview: ImageBitmap? = null,
    val liquifyBrushRadius: Float = 80f,
    val liquifyBrushStrength: Float = 0.5f,
) {
    val currentFrame: Frame
        get() = project.frames.getOrNull(currentFrameIndex)
            ?: project.frames.firstOrNull()
            ?: Frame()

    val currentLayer: Layer
        get() = currentFrame.layers.getOrNull(currentLayerIndex)
            ?: currentFrame.layers.firstOrNull()
            ?: Layer()

    val isAnimation: Boolean get() = project.mode == ProjectMode.Animation
    val isPaint: Boolean get() = project.mode == ProjectMode.Paint

    val isEyedropper: Boolean get() = activeTool == ActiveTool.Eyedropper
    val isMagicWand: Boolean get() = activeTool == ActiveTool.MagicWand
    val isShape: Boolean get() = activeTool == ActiveTool.Shape
    val isSelect: Boolean get() = activeTool == ActiveTool.Select

    val canDraw: Boolean
        get() = activeTool == ActiveTool.Brush &&
                gestureMode == GestureMode.Idle &&
                !isPlaying &&
                floatingSelection == null

    val hasDraft: Boolean get() = draftStart != null && draftEnd != null
    val needsOnionSkin: Boolean get() = isAnimation && onionSkin.any
    val isFloating: Boolean get() = floatingSelection != null

    val isLiquify: Boolean get() = activeTool == ActiveTool.Liquify
    val isLiquifyActive: Boolean get() = liquifySession != null
}
