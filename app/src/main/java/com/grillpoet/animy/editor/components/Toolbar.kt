package com.grillpoet.animy.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grillpoet.animy.domain.model.BrushStyle
import com.grillpoet.animy.domain.model.BrushType
import com.grillpoet.animy.domain.model.ShapeType
import com.grillpoet.animy.editor.ActiveTool
import com.grillpoet.animy.ui.components.BrushIcon
import com.grillpoet.animy.ui.components.EyedropperIcon
import com.grillpoet.animy.ui.components.GifIcon
import com.grillpoet.animy.ui.components.HsvColorPicker
import com.grillpoet.animy.ui.components.LiquifyIcon
import com.grillpoet.animy.ui.components.MagicWandIcon
import com.grillpoet.animy.ui.components.ShapeIcon
import com.grillpoet.animy.ui.components.OnionIcon
import com.grillpoet.animy.ui.components.PauseIcon
import com.grillpoet.animy.ui.components.PlayIcon
import com.grillpoet.animy.ui.components.PlusIcon
import com.grillpoet.animy.ui.components.RedoIcon
import com.grillpoet.animy.ui.components.SaveIcon
import com.grillpoet.animy.ui.components.UndoIcon
import com.grillpoet.animy.ui.components.VideoIcon

@Composable
fun Toolbar(
    brush: BrushStyle,
    palette: List<Color>,
    onBrushType: (BrushType) -> Unit,
    onColor: (Color) -> Unit,
    onSize: (Float) -> Unit,
    activeTool: ActiveTool,
    onTool: (ActiveTool) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

            // ─── 色板 ───
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                palette.forEach { c ->
                    ColorDot(c, c == brush.color) { onColor(c) }
                }
                CurrentColorButton(
                    color = brush.color,
                    isCustom = brush.color !in palette,
                    onClick = { pickerOpen = true },
                )
            }

            Spacer(Modifier.height(6.dp))

            // ─── 笔刷大小 ───
            Slider(
                value = brush.width,
                onValueChange = onSize,
                valueRange = 1f..40f,
                modifier = Modifier.height(22.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )

            // ─── 笔刷 + 工具 ───
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrushType.entries.forEach { b ->
                    val active = activeTool == ActiveTool.Brush && brush.type == b
                    IconWithTip(label = b.displayName) {
                        IconCircle(
                            active = active,
                            onClick = { onBrushType(b) },
                        ) {
                            BrushIcon(
                                brush = b,
                                tint = if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                IconWithTip(label = "取色棒") {
                    IconCircle(
                        active = activeTool == ActiveTool.Eyedropper,
                        onClick = { onTool(ActiveTool.Eyedropper) },
                    ) {
                        EyedropperIcon(
                            tint = if (activeTool == ActiveTool.Eyedropper)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconWithTip(label = "魔法选取") {
                    IconCircle(
                        active = activeTool == ActiveTool.MagicWand,
                        onClick = { onTool(ActiveTool.MagicWand) },
                    ) {
                        MagicWandIcon(
                            tint = if (activeTool == ActiveTool.MagicWand)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconWithTip(label = "液化") {
                    IconCircle(
                        active = activeTool == ActiveTool.Liquify,
                        onClick = { onTool(ActiveTool.Liquify) },
                    ) {
                        LiquifyIcon(
                            tint = if (activeTool == ActiveTool.Liquify)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconWithTip(label = "形状") {
                    IconCircle(
                        active = activeTool == ActiveTool.Shape,
                        onClick = { onTool(ActiveTool.Shape) },
                    ) {
                        ShapeIcon(
                            tint = if (activeTool == ActiveTool.Shape)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ─── 底部动作按钮 ───
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }

    if (pickerOpen) {
        HsvColorPicker(
            initial = brush.color,
            onConfirm = { onColor(it); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  通用组件
// ═══════════════════════════════════════════════════════════════

/**
 * 图标 + 长按提示。
 *
 * M3 的 Tooltip 交互：
 *   · 手机：长按
 *   · 平板 / 桌面：悬停
 *
 * @param label 提示文字，同时作为 tooltip 的唯一标识。
 * @param content 图标内容。内部应该是一个 42dp 的 IconCircle。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconWithTip(
    label: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) { Text(label) }
        },
        state = rememberTooltipState(),
    ) {
        content()
    }
}

/**
 * 圆形图标按钮。统一的 42dp 点击区域，选中时背景变浅。
 */
@Composable
fun IconCircle(
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (active && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else Color.Transparent
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 底部工具栏的动作按钮。
 *
 * 传中文 label，内部映射到矢量图标。
 */
@Composable
fun ToolButton(
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconWithTip(label = label) {
        IconCircle(
            active = active,
            enabled = enabled,
            onClick = onClick,
        ) {
            val tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
            when (label) {
                "撤销" -> UndoIcon(tint)
                "重做" -> RedoIcon(tint)
                "洋葱皮" -> OnionIcon(tint)
                "播放" -> PlayIcon(tint)
                "暂停" -> PauseIcon(tint)
                "新建帧" -> PlusIcon(tint)
                "导出图片" -> SaveIcon(tint)
                "导出视频" -> VideoIcon(tint)
                "导出 GIF" -> GifIcon(tint)
                else -> Text(label, fontSize = 16.sp, color = tint)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  色板相关
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CurrentColorButton(
    color: Color,
    isCustom: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
        )
        if (color.alpha < 1f) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            val fg = if (color.luminance() > 0.55f) Color.Black.copy(alpha = 0.5f)
            else Color.White.copy(alpha = 0.7f)
            Text("＋", fontSize = 14.sp, color = fg)
        }
        if (isCustom && color.alpha == 1f) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(26.dp).border(2.dp, color, CircleShape))
        }
        Box(
            Modifier
                .size(if (selected) 16.dp else 20.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}
