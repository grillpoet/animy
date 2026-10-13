package com.grillpoet.animy.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.grillpoet.animy.domain.model.Layer
import com.grillpoet.animy.ui.components.ArrowDownIcon
import com.grillpoet.animy.ui.components.ArrowUpIcon
import com.grillpoet.animy.ui.components.DeleteIcon
import com.grillpoet.animy.ui.components.EditIcon
import com.grillpoet.animy.ui.components.EyeIcon
import com.grillpoet.animy.ui.components.EyeOffIcon
import com.grillpoet.animy.ui.components.LayersIcon
import com.grillpoet.animy.ui.components.MergeDownIcon
import com.grillpoet.animy.ui.components.OpacityIcon
import com.grillpoet.animy.ui.components.PlusIcon
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LayerPanel(
    expanded: Boolean,
    onDismiss: () -> Unit,
    layers: List<Layer>,
    current: Int,
    onSelect: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    onAlpha: (Int, Float) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onMergeDown: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    if (!expanded) return

    var renamingIndex by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
        ) {
            Column {
                // 头部
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LayersIcon(MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "图层",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${layers.size} 层 · 长按拖动排序",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // 图层列表
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    // from / to 是视觉索引（视觉 0 = 顶层）
                    // real index = layers.lastIndex - visualIdx
                    val realFrom = layers.lastIndex - from.index
                    val realTo = layers.lastIndex - to.index
                    onMove(realFrom, realTo)
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    itemsIndexed(
                        items = layers.asReversed(),
                        key = { _, layer -> layer.id },
                    ) { visualIdx, layer ->
                        val realIdx = layers.lastIndex - visualIdx

                        ReorderableItem(reorderableState, key = layer.id) { isDragging ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .longPressDraggableHandle()
                            ) {
                                LayerRow(
                                    name = layer.name.ifBlank { "图层 ${realIdx + 1}" },
                                    visible = layer.visible,
                                    alpha = layer.alpha,
                                    selected = realIdx == current,
                                    canDelete = layers.size > 1,
                                    canMerge = realIdx > 0,
                                    canMoveUp = realIdx < layers.lastIndex,
                                    canMoveDown = realIdx > 0,
                                    isDragging = isDragging,
                                    onClick = { onSelect(realIdx) },
                                    onToggle = { onToggle(realIdx) },
                                    onAlpha = { a -> onAlpha(realIdx, a) },
                                    onDelete = { onDelete(realIdx) },
                                    onMerge = { onMergeDown(realIdx) },
                                    onRename = { renamingIndex = realIdx },
                                    onMoveUp = { onMoveUp(realIdx) },
                                    onMoveDown = { onMoveDown(realIdx) },
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // 新建图层
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { adding = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlusIcon(MaterialTheme.colorScheme.primary, s = 16.dp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "新建图层",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    // 重命名对话框
    renamingIndex?.let { idx ->
        val layer = layers.getOrNull(idx)
        if (layer != null) {
            LayerNameDialog(
                title = "重命名图层",
                initial = layer.name.ifBlank { "图层 ${idx + 1}" },
                confirmLabel = "确定",
                onDismiss = { renamingIndex = null },
                onConfirm = { newName ->
                    onRename(idx, newName)
                    renamingIndex = null
                },
            )
        } else {
            renamingIndex = null
        }
    }

    // 新建图层对话框
    if (adding) {
        LayerNameDialog(
            title = "新建图层",
            initial = "",
            placeholder = "输入名称（可留空）",
            confirmLabel = "创建",
            onDismiss = { adding = false },
            onConfirm = { name ->
                onAdd(name.trim())
                adding = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun LayerRow(
    name: String,
    visible: Boolean,
    alpha: Float,
    selected: Boolean,
    canDelete: Boolean,
    canMerge: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onAlpha: (Float) -> Unit,
    onDelete: () -> Unit,
    onMerge: () -> Unit,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var showAlpha by remember { mutableStateOf(false) }
    val bg = when {
        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> Color.Transparent
    }

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 可见性
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                val tint = if (visible) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                if (visible) EyeIcon(tint) else EyeOffIcon(tint)
            }

            // 名字
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (visible) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            )

            IconAction(enabled = true, onClick = onRename) { tint ->
                EditIcon(tint, s = 18.dp)
            }
            IconAction(enabled = canMoveUp, onClick = onMoveUp) { tint ->
                ArrowUpIcon(tint, s = 18.dp)
            }
            IconAction(enabled = canMoveDown, onClick = onMoveDown) { tint ->
                ArrowDownIcon(tint, s = 18.dp)
            }
            IconAction(enabled = true, onClick = { showAlpha = !showAlpha }) { tint ->
                OpacityIcon(tint, s = 18.dp)
            }
            if (canMerge) {
                IconAction(enabled = true, onClick = onMerge) { tint ->
                    MergeDownIcon(tint, s = 18.dp)
                }
            }
            if (canDelete) {
                IconAction(enabled = true, onClick = onDelete) { tint ->
                    DeleteIcon(tint, s = 18.dp)
                }
            }
        }

        if (showAlpha) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "透明度 ${(alpha * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = alpha,
                    onValueChange = onAlpha,
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

@Composable
private fun IconAction(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(tint)
    }
}

@Composable
private fun LayerNameDialog(
    title: String,
    initial: String,
    placeholder: String = "输入名称",
    confirmLabel: String = "确定",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 30) text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onConfirm(text.trim()) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text(confirmLabel, fontSize = 15.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", fontSize = 15.sp) }
        },
    )
}
