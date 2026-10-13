package com.grillpoet.animy.editor

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.grillpoet.animy.ui.components.BackIcon
import com.grillpoet.animy.ui.components.LayersIcon
import com.grillpoet.animy.ui.components.SettingsIcon

/**
 * 编辑器顶栏。
 *
 * 用 M3 [TopAppBar] 而非手写 Row —— 自动处理：
 *   - 状态栏 inset
 *   - 标准高度 64dp
 *   - 图标间距、涟漪效果
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    title: String,
    onBack: () -> Unit,
    onLayers: () -> Unit,
    onMenu: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                BackIcon(scheme.onSurface)
            }
        },
        actions = {
            ActionIcon(onClick = onLayers) {
                LayersIcon(scheme.onSurface)
            }
            ActionIcon(onClick = onMenu) {
                SettingsIcon(scheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surface,
            titleContentColor = scheme.onSurface,
            navigationIconContentColor = scheme.onSurface,
            actionIconContentColor = scheme.onSurface,
        ),
    )
}

/**
 * 顶栏里的图标按钮。包装 [IconButton] 使得只有图标可点击，
 * 避免点击区域溢出到相邻元素。
 */
@Composable
private fun RowScope.ActionIcon(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick) { content() }
}
