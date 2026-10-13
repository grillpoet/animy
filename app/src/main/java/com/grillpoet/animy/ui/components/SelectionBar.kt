package com.grillpoet.animy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grillpoet.animy.domain.selection.FloatingSource

/**
 * 选区 / 浮动操作栏。
 *
 *   选区态：  [填充][删除][取消]
 *   浮动态（选区来源）：[确定][填充][清空][取消]
 *   浮动态（图片导入）：[确定][取消]
 *
 * 语义：
 *   · 填充  当前色填充选区 / 替换浮动内容
 *   · 删除  删除选区内内容（立即生效）
 *   · 确定  栅格化浮动内容到图层（可撤销）
 *   · 清空  浮动内容替换为全透明
 *   · 取消  选区：放弃选择；浮动：恢复浮动前
 */
@Composable
fun SelectionBar(
    isFloating: Boolean,
    floatingSource: FloatingSource?,
    onConfirm: () -> Unit,
    onFill: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                isFloating && floatingSource == FloatingSource.ImageImport -> {
                    BarAction("确定", onConfirm, MaterialTheme.colorScheme.inversePrimary)
                    BarAction("取消", onCancel)
                }
                isFloating -> {
                    BarAction("确定", onConfirm, MaterialTheme.colorScheme.inversePrimary)
                    BarAction("填充", onFill)
                    BarAction("清空", onClear)
                    BarAction("取消", onCancel)
                }
                else -> {
                    BarAction("填充", onFill)
                    BarAction("删除", onDelete)
                    BarAction("取消", onCancel)
                }
            }
        }
    }
}

@Composable
private fun BarAction(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.inverseOnSurface,
) {
    Box(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = color)
    }
}


/**
 * 形状待确认浮条。
 *
 * 形状绘制松手后显示，用户点确定才落盘，点取消则丢弃。
 */
@Composable
fun ShapeConfirmBar(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        shadowElevation = 8.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarAction("确定", onConfirm, MaterialTheme.colorScheme.inversePrimary)
            BarAction("取消", onCancel)
        }
    }
}

