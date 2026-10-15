package com.grillpoet.animy.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grillpoet.animy.domain.model.DrawElement
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.domain.model.Rect
import com.grillpoet.animy.domain.model.transformedBounds
import com.grillpoet.animy.editor.components.BackgroundPickerDialog
import com.grillpoet.animy.editor.components.DrawingCanvas
import com.grillpoet.animy.editor.components.FloatingSelectionLayer
import com.grillpoet.animy.editor.components.FrameDurationDialog
import com.grillpoet.animy.editor.components.ImageExportDialog
import com.grillpoet.animy.editor.components.LayerPanel
import com.grillpoet.animy.editor.components.OnionSkinRangeDialog
import com.grillpoet.animy.editor.components.PickedColorPopup
import com.grillpoet.animy.editor.components.canvasGestures
import com.grillpoet.animy.ui.components.SelectionBar
import com.grillpoet.animy.ui.components.ShapeConfirmBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit,
    vm: EditorViewModel = viewModel(
        key = projectId,
        factory = EditorViewModel.factory(projectId),
    ),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val backScope = rememberCoroutineScope()

    // onPause 时立即保存
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) vm.forceSaveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var viewport by remember {
        mutableStateOf(Viewport(canvasSize = s.project.canvasSize, viewSize = IntSize.Zero))
    }

    LaunchedEffect(s.project.canvasSize) {
        if (viewport.canvasSize != s.project.canvasSize) {
            viewport = viewport.copy(canvasSize = s.project.canvasSize)
        }
    }

    // 屏幕旋转 / 尺寸变化：按比例保留 pan，重置不丢用户位置
    var prevViewSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(viewport.viewSize) {
        val newSize = viewport.viewSize
        if (newSize != IntSize.Zero && newSize.width > 0) {
            if (prevViewSize != IntSize.Zero && prevViewSize.width > 0) {
                // 按宽高比缩放 pan，视觉上画布保持相对位置
                val kx = newSize.width.toFloat() / prevViewSize.width
                val ky = newSize.height.toFloat() / prevViewSize.height
                viewport = viewport.copy(
                    pan = androidx.compose.ui.geometry.Offset(
                        viewport.pan.x * kx,
                        viewport.pan.y * ky,
                    ),
                )
            }
            prevViewSize = newSize
        }
    }

    // 播放循环
    LaunchedEffect(s.isPlaying, s.currentFrame.durationMs) {
        if (!s.isPlaying) return@LaunchedEffect
        while (isActive) {
            delay(s.currentFrame.durationMs.coerceAtLeast(16L))
            vm.onIntent(EditorIntent.NextFrame)
        }
    }

    BackHandler {
        backScope.launch {
            vm.saveNow()
            onBack()
        }
    }

    if (s.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var layerPanelOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<EditorDialog?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.onIntent(EditorIntent.ImportImage(it)) } }

    // 洋葱皮帧
    val prevFrames = remember(s.currentFrameIndex, s.project.frames, s.onionSkin) {
        val start = (s.currentFrameIndex - s.onionSkin.prev).coerceAtLeast(0)
        s.project.frames.subList(start, s.currentFrameIndex).toList()
    }
    val nextFrames = remember(s.currentFrameIndex, s.project.frames, s.onionSkin) {
        val end = (s.currentFrameIndex + s.onionSkin.next + 1).coerceAtMost(s.project.frames.size)
        s.project.frames.subList(s.currentFrameIndex + 1, end).toList()
    }

    // 当前选中元素。仅「选择 / 移动」工具下有效；切走工具后自动隐藏虚线框。
    val selectedElement: DrawElement? = remember(
        s.selectedElementId, s.currentFrame, s.isSelect,
    ) {
        if (!s.isSelect) null
        else s.selectedElementId?.let { id ->
            s.currentFrame.layers.asSequence()
                .flatMap { it.elements.asSequence() }
                .firstOrNull { it.id == id }
        }
    }

    // 各自的 bounds —— canvasGestures 里按优先级判定
    val floatingBounds: Rect? = remember(s.floatingSelection) {
        s.floatingSelection?.bounds()
    }
    val elementBounds: Rect? = remember(selectedElement) {
        selectedElement?.transformedBounds()
    }

    Column(Modifier.fillMaxSize()) {
        // ─── 顶栏 ───
        EditorTopBar(
            title = s.project.name,
            onBack = {
                backScope.launch {
                    vm.saveNow()
                    onBack()
                }
            },
            onLayers = { layerPanelOpen = true },
            onMenu = { menuOpen = true },
        )

        // ─── 画布 ───
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged { sz ->
                    val newSize = IntSize(sz.width, sz.height)
                    if (viewport.viewSize != newSize) {
                        viewport = viewport.copy(viewSize = newSize)
                    }
                }
                .canvasGestures(
                    viewport = viewport,
                    activeTool = s.activeTool,
                    selectTool = s.selectTool,
                    isLiquify = s.isLiquifyActive,
                    floatingBounds = floatingBounds,
                    elementBounds = elementBounds,
                    activeSelection = s.selection,
                    onViewportChange = { viewport = it },
                    onStrokeStart = { vm.onIntent(EditorIntent.StrokeStart(it)) },
                    onStrokeMove = { vm.onIntent(EditorIntent.StrokeMove(it)) },
                    onStrokeEnd = { vm.onIntent(EditorIntent.StrokeEnd) },
                    onStrokeCancel = { vm.onIntent(EditorIntent.StrokeCancel) },
                    onSelectionDraftStart = { vm.onIntent(EditorIntent.SelectionDraftStart(it)) },
                    onSelectionDraftMove = { vm.onIntent(EditorIntent.SelectionDraftMove(it)) },
                    onSelectionDraftEnd = { vm.onIntent(EditorIntent.SelectionDraftEnd) },
                    onElementDragStart = {
                        val id = s.floatingSelection?.id ?: s.selectedElementId ?: -1L
                        vm.onIntent(EditorIntent.TransformStart(id))
                    },
                    onElementDragDelta = { vm.onIntent(EditorIntent.ElementDragDelta(it)) },
                    onElementTransform = { zoom, rot, pan ->
                        vm.onIntent(EditorIntent.ElementTransformDelta(zoom, rot, pan))
                    },
                    onElementDragEnd = { vm.onIntent(EditorIntent.TransformEnd) },
                    onStartFloating = { vm.startFloatingFromSelection() },
                    onShapeDraftStart = { vm.onIntent(EditorIntent.ShapeDraftStart(it)) },
                    onShapeDraftMove = { vm.onIntent(EditorIntent.ShapeDraftMove(it)) },
                    onShapeDraftEnd = { vm.onIntent(EditorIntent.ShapeDraftEnd) },
                    onLiquifyStart = { vm.onIntent(EditorIntent.LiquifyStart(it)) },
                    onLiquifyMove = { vm.onIntent(EditorIntent.LiquifyMove(it)) },
                    onLiquifyEnd = { vm.onIntent(EditorIntent.LiquifyEnd) },
                    onTap = { p ->
                        when {
                            s.isFloating -> {}   // 浮动中点击忽略
                            s.isEyedropper -> vm.onIntent(EditorIntent.PickColor(p))
                            s.isMagicWand && s.selectTool == SelectTool.Wand ->
                                vm.onIntent(EditorIntent.SelectionTap(p))
                            s.isMagicWand -> {}
                            s.isSelect -> vm.onIntent(EditorIntent.SelectElementAt(p))
                            else -> vm.onIntent(EditorIntent.DeselectElement)
                        }
                    },
                    onDoubleTap = { viewport = viewport.reset() },
                    onTapOutsideSelection = { vm.onIntent(EditorIntent.SelectionCancel) },
                ),
        ) {
            DrawingCanvas(
                frame = s.currentFrame,
                canvasSize = s.project.canvasSize,
                background = s.project.background,
                viewport = viewport,
                liveStroke = vm.liveStroke,
                liveBrush = s.brush,
                liveStrokeId = s.liveStrokeId,
                showGrid = s.showGrid,
                onion = s.onionSkin,
                prevFrames = prevFrames,
                nextFrames = nextFrames,
                floatingSelection = s.floatingSelection,
                draftPath = vm.draftSelectionPath,
                draftStart = s.draftStart,
                draftEnd = s.draftEnd,
                draftShapeStart = s.draftShapeStart,
                draftShapeEnd = s.draftShapeEnd,
                shapeTool = s.shapeTool,
                shapeFilled = s.shapeFilled,
                shapeBrush = s.brush,
                selectTool = s.selectTool,
                selectedElement = selectedElement,
                currentLayerIndex = s.currentLayerIndex,
                eyedropper = s.isEyedropper,
                renderCache = vm.renderCache,
                liquifyPreview = s.liquifyPreview,
                liquifyLayerIndex = if (s.isLiquifyActive) s.liquifySession?.sourceLayerIndex else null,
            )

            s.floatingSelection?.let { fs ->
                FloatingSelectionLayer(
                    floating = fs,
                    viewport = viewport,
                )
            }

            LayerPanel(
                expanded = layerPanelOpen,
                onDismiss = { layerPanelOpen = false },
                layers = s.currentFrame.layers,
                current = s.currentLayerIndex,
                onSelect = { vm.onIntent(EditorIntent.SelectLayer(it)) },
                onToggle = { vm.onIntent(EditorIntent.ToggleLayerVisible(it)) },
                onAlpha = { i, a -> vm.onIntent(EditorIntent.SetLayerAlpha(i, a)) },
                onAdd = { name -> vm.onIntent(EditorIntent.AddLayer(name)) },
                onDelete = { vm.onIntent(EditorIntent.DeleteLayer(it)) },
                onMergeDown = { vm.onIntent(EditorIntent.MergeDown(it)) },
                onRename = { i, name -> vm.onIntent(EditorIntent.RenameLayer(i, name)) },
                onMoveUp = { vm.onIntent(EditorIntent.MoveLayerUp(it)) },
                onMoveDown = { vm.onIntent(EditorIntent.MoveLayerDown(it)) },
                onMove = { from, to -> vm.onIntent(EditorIntent.MoveLayer(from, to)) },
            )

            // 取色反馈弹窗
            // 取色提示的自动消失由 ViewModel 的 Job 控制，不依赖 Compose 生命周期
            s.lastPickedColor?.let { color ->
                PickedColorPopup(
                    color = color,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )
            }

            if (s.floatingSelection != null) {
                SelectionBar(
                    isFloating = s.floatingSelection != null,
                    floatingSource = s.floatingSelection?.source,
                    onConfirm = { vm.onIntent(EditorIntent.SelectionCommit) },
                    onFill = { vm.onIntent(EditorIntent.SelectionFill) },
                    onDelete = { vm.onIntent(EditorIntent.SelectionDelete) },
                    onClear = { vm.onIntent(EditorIntent.SelectionClear) },
                    onCancel = { vm.onIntent(EditorIntent.SelectionCancel) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }

            if (s.isShapePending) {
                ShapeConfirmBar(
                    onConfirm = { vm.onIntent(EditorIntent.ShapeConfirm) },
                    onCancel = { vm.onIntent(EditorIntent.ShapeCancel) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            }
        }

        // ─── 底部区域 ───
        EditorBottomArea(
            vm = vm,
            state = s,
            onRequestExportImage = { dialog = EditorDialog.ExportImage },
        )
    }

    // ─── 顶栏菜单 ───
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text("画布背景") },
            onClick = { menuOpen = false; dialog = EditorDialog.Background },
        )
        DropdownMenuItem(
            text = { Text(if (s.showGrid) "隐藏网格" else "显示网格") },
            onClick = { menuOpen = false; vm.onIntent(EditorIntent.ToggleGrid) },
        )
        if (s.project.mode == ProjectMode.Animation) {
            DropdownMenuItem(
                text = { Text("洋葱皮范围（前${s.onionSkin.prev} / 后${s.onionSkin.next}）") },
                onClick = { menuOpen = false; dialog = EditorDialog.OnionRange },
            )
            DropdownMenuItem(
                text = { Text("帧时长（${s.currentFrame.durationMs}ms）") },
                onClick = { menuOpen = false; dialog = EditorDialog.FrameDuration },
            )
        }
        DropdownMenuItem(
            text = { Text("导入图片") },
            onClick = { menuOpen = false; imagePicker.launch("image/*") },
        )
    }

    // ─── 对话框 ───
    if (dialog == EditorDialog.Background) {
        BackgroundPickerDialog(
            initial = s.project.background,
            onDismiss = { dialog = null },
            onConfirm = { c -> vm.onIntent(EditorIntent.SetBackground(c)); dialog = null },
        )
    }

    if (dialog == EditorDialog.FrameDuration) {
        FrameDurationDialog(
            initial = s.currentFrame.durationMs,
            onDismiss = { dialog = null },
            onConfirm = { ms ->
                vm.onIntent(EditorIntent.SetFrameDuration(s.currentFrameIndex, ms))
                dialog = null
            },
        )
    }

    if (dialog == EditorDialog.OnionRange) {
        OnionSkinRangeDialog(
            initialPrev = s.onionSkin.prev,
            initialNext = s.onionSkin.next,
            onDismiss = { dialog = null },
            onConfirm = { p, n ->
                vm.onIntent(EditorIntent.SetOnionConfig(p, n))
                dialog = null
            },
        )
    }

    if (dialog == EditorDialog.ExportImage) {
        ImageExportDialog(
            onDismiss = { dialog = null },
            onConfirm = { fmt ->
                vm.onIntent(EditorIntent.ExportImage(fmt))
                dialog = null
            },
        )
    }

    if (s.isExporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(s.exportingLabel ?: "导出中…") },
            text = {
                Column {
                    // 图片导出没有进度（瞬间完成），不显示进度条
                    if (s.exportingLabel?.contains("视频") == true ||
                        s.exportingLabel?.contains("GIF") == true) {
                        LinearProgressIndicator(
                            progress = { s.exportProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("${(s.exportProgress * 100).toInt()}%", fontSize = 13.sp)
                    } else {
                        Text("请稍候…", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
        )
    }

    s.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.onIntent(EditorIntent.DismissError) },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { vm.onIntent(EditorIntent.DismissError) }) {
                    Text("好的")
                }
            },
        )
    }
}

/** 编辑器顶层对话框的互斥状态。同时只有一个能打开。 */
private enum class EditorDialog {
    Background,
    FrameDuration,
    OnionRange,
    ExportImage,
}
