package com.grillpoet.animy.editor.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.grillpoet.animy.data.render.BitmapCompositor
import com.grillpoet.animy.domain.model.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private val ITEM_W = 64.dp
private val ITEM_H = 64.dp
private val ITEM_GAP = 6.dp

/**
 * 帧时间线。
 *
 * 交互：
 *   · 短按         → 选中帧
 *   · 长按拖动     → 重排（reorderable 库处理，稳定可靠）
 *   · 点右上角 ⋮   → 弹出菜单（左移 / 右移 / 复制 / 删除）
 *
 * 为什么菜单不放在长按：
 *   长按已经被拖拽占用。之前把"长按弹菜单"和"长按拖拽"混在一起，
 *   两个意图相互冲突，需要各种时序判断，体验很难做好。
 *   拆开后，每个操作都只有一个明确的触发器。
 */
@Composable
fun FrameTimeline(
    frames: List<Frame>,
    canvasSize: IntSize,
    background: Color,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuIndex by remember { mutableStateOf<Int?>(null) }
    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMove(from.index, to.index)
    }

    Column(modifier.fillMaxWidth()) {
        // 提示条
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "帧 · 长按拖动排序",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(ITEM_GAP),
        ) {
            itemsIndexed(frames, key = { _, f -> f.id }) { i, frame ->
                ReorderableItem(
                    state = reorderableState,
                    key = frame.id,
                ) { isDragging ->
                    FrameCell(
                        index = i,
                        frame = frame,
                        canvasSize = canvasSize,
                        background = background,
                        isSelected = i == currentIndex,
                        isDragging = isDragging,
                        menuOpen = menuIndex == i,
                        onSelect = { onSelect(i) },
                        onOpenMenu = { menuIndex = i },
                        onDismissMenu = { menuIndex = null },
                        onMoveLeft = { if (i > 0) onMove(i, i - 1); menuIndex = null },
                        onMoveRight = {
                            if (i < frames.size - 1) onMove(i, i + 1)
                            menuIndex = null
                        },
                        onDuplicate = { onDuplicate(i); menuIndex = null },
                        onDelete = {
                            if (frames.size > 1) onDelete(i)
                            menuIndex = null
                        },
                        canDelete = frames.size > 1,
                        dragHandleModifier = Modifier.longPressDraggableHandle(),
                    )
                }
            }
        }
    }
}

/**
 * 单帧卡片。
 *
 * 布局：
 *   ┌──────────────────┐
 *   │              ⋮   │   ← 右上角菜单按钮
 *   │    缩略图         │
 *   │                  │
 *   │ 1        83ms    │   ← 左下序号 / 右下时长
 *   └──────────────────┘
 */
@Composable
private fun FrameCell(
    index: Int,
    frame: Frame,
    canvasSize: IntSize,
    background: Color,
    isSelected: Boolean,
    isDragging: Boolean,
    menuOpen: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
) {
    val borderColor = when {
        isDragging -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val borderWidth = when {
        isDragging || isSelected -> 2.dp
        else -> 1.dp
    }
    val bgColor = when {
        isDragging -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        Modifier
            .zIndex(if (isDragging) 2f else 0f)
            .size(ITEM_W, ITEM_H)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            // 拖动把手覆盖整个卡片；点击事件由内部子组件拦截
            .then(dragHandleModifier)
    ) {
        // 缩略图
        FrameThumbnail(
            frame = frame,
            canvasSize = canvasSize,
            background = background,
            modifier = Modifier.fillMaxSize(),
        )

        // 点击层（选中）
        Box(
            Modifier
                .fillMaxSize()
                .clickable(onClick = onSelect)
        )

        // 左下角序号
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 10.sp,
                color = Color.White,
            )
        }

        // 右下角时长
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 3.dp, vertical = 1.dp),
        ) {
            Text(
                text = "${frame.durationMs}ms",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }

        // 右上角 ⋮ 菜单按钮
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onOpenMenu),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "帧菜单",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = onDismissMenu,
            ) {
                DropdownMenuItem(
                    text = { Text("左移") },
                    enabled = index > 0,
                    onClick = onMoveLeft,
                )
                DropdownMenuItem(
                    text = { Text("右移") },
                    enabled = index < Int.MAX_VALUE,  // 由外层判断
                    onClick = onMoveRight,
                )
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = onDuplicate,
                )
                DropdownMenuItem(
                    text = {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    },
                    enabled = canDelete,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun FrameThumbnail(
    frame: Frame,
    canvasSize: IntSize,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val thumb by produceState<ImageBitmap?>(
        initialValue = null,
        frame, canvasSize, background,
    ) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                BitmapCompositor.renderFrameThumb(
                    frame = frame,
                    canvasSize = canvasSize,
                    background = background,
                    maxDim = 128,
                )
            }.getOrNull()
        }
    }

    if (thumb != null) {
        Image(
            bitmap = thumb!!,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}
