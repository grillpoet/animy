package com.grillpoet.animy.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.grillpoet.animy.data.persistence.ProjectRepository
import com.grillpoet.animy.domain.model.CanvasPreset
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.ui.components.PlusIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页。
 *
 * 布局参考 Mixy：
 *   - 顶栏紧凑（titleLarge + 无装饰）
 *   - 纯色背景（无渐变）
 *   - 卡片紧凑、圆角 12dp、surfaceVariant 底色
 *   - FAB 右下角
 */
@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val scope = rememberCoroutineScope()

    var showNewDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectRepository.Summary?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectRepository.Summary?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ─── 顶栏 ───
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "animy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                if (vm.projects.isNotEmpty()) {
                    Text(
                        text = "${vm.projects.size} 个作品",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ─── 内容区 ───
            Box(Modifier.weight(1f)) {
                when {
                    vm.loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    vm.projects.isEmpty() -> EmptyState()

                    else -> LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(vm.projects, key = { it.id }) { p ->
                            ProjectCard(
                                summary = p,
                                onClick = { onOpen(p.id) },
                                onRename = { renameTarget = p },
                                onDuplicate = { vm.duplicate(p.id) {} },
                                onDelete = { deleteTarget = p },
                            )
                        }
                    }
                }
            }
        }

        // ─── FAB ───
        FloatingActionButton(
            onClick = { showNewDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            PlusIcon(MaterialTheme.colorScheme.onPrimary, s = 22.dp)
        }
    }

    if (showNewDialog) {
        NewProjectDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { mode, name, size ->
                showNewDialog = false
                scope.launch {
                    val id = vm.create(mode, name, size)
                    onOpen(id)
                }
            },
        )
    }

    renameTarget?.let { p ->
        RenameDialog(
            initial = p.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                vm.rename(p.id, newName)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除作品") },
            text = { Text("「${p.name}」将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(p.id)
                    deleteTarget = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "还没有作品",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "点击右下角 + 开始",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 项目卡片。
 *
 * 参考 Mixy 的 FolderCard 布局：
 *   - 左侧 64dp 缩略图
 *   - 中间标题 + 元信息
 *   - 右侧 ⋮ 菜单
 */
@Composable
private fun ProjectCard(
    summary: ProjectRepository.Summary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 缩略图
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            val bitmapState = produceState<ImageBitmap?>(
                initialValue = null,
                summary.thumbPath, summary.updatedAt,
            ) {
                value = withContext(Dispatchers.IO) {
                    summary.thumbPath?.let { path ->
                        runCatching {
                            BitmapFactory.decodeFile(path)?.asImageBitmap()
                        }.getOrNull()
                    }
                }
            }
            val bitmap = bitmapState.value
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = if (summary.mode == ProjectMode.Animation) "帧" else "绘",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // 信息
        Column(Modifier.weight(1f)) {
            Text(
                text = summary.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    append(if (summary.mode == ProjectMode.Animation) "动画" else "绘画")
                    append(" · ")
                    if (summary.mode == ProjectMode.Animation) {
                        append("${summary.frameCount} 帧 · ")
                    }
                    append(formatTime(summary.updatedAt))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 菜单
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text("重命名") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { menuOpen = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text("复制") },
                    onClick = { menuOpen = false; onDuplicate() },
                )
                DropdownMenuItem(
                    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete, null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 40) text = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (text.isEmpty()) {
                    Text(
                        "输入名称",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun NewProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (ProjectMode, String, IntSize) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(ProjectMode.Animation) }
    var preset by remember { mutableStateOf(CanvasPreset.Square) }
    var useCustom by remember { mutableStateOf(false) }
    var customW by remember { mutableStateOf("") }
    var customH by remember { mutableStateOf("") }

    val customValid =
        customW.toIntOrNull()?.let { it in 16..4096 } == true &&
        customH.toIntOrNull()?.let { it in 16..4096 } == true
    val canCreate = !useCustom || customValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建作品") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 名称
                Text(
                    "名称",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { if (it.length <= 40) name = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (name.isEmpty()) {
                        Text(
                            "输入名称（可留空）",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }

                // 模式
                Text(
                    "模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip("帧动画", mode == ProjectMode.Animation,
                        { mode = ProjectMode.Animation }, Modifier.weight(1f))
                    ModeChip("绘画", mode == ProjectMode.Paint,
                        { mode = ProjectMode.Paint }, Modifier.weight(1f))
                }

                // 尺寸
                Text(
                    "画布尺寸",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CanvasPreset.entries.forEach { p ->
                        PresetRow(p, !useCustom && preset == p, {
                            useCustom = false; preset = p
                        })
                    }
                    CustomSizeRow(
                        selected = useCustom,
                        widthText = customW,
                        heightText = customH,
                        onToggle = { useCustom = it },
                        onWidth = { customW = it.filter { c -> c.isDigit() }.take(4) },
                        onHeight = { customH = it.filter { c -> c.isDigit() }.take(4) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canCreate,
                onClick = {
                    val size = if (useCustom) IntSize(customW.toInt(), customH.toInt())
                    else preset.size
                    onConfirm(mode, name, size)
                },
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun ModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PresetRow(
    preset: CanvasPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            preset.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${preset.size.width} × ${preset.size.height}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CustomSizeRow(
    selected: Boolean,
    widthText: String,
    heightText: String,
    onToggle: (Boolean) -> Unit,
    onWidth: (String) -> Unit,
    onHeight: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onToggle(!selected) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "自定义像素",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                "16 – 4096",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SizeInput("宽", widthText, onWidth, Modifier.weight(1f))
                Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SizeInput("高", heightText, onHeight, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SizeInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Box(Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(
                    "0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        diff < 2_592_000_000L -> "${diff / 86_400_000} 天前"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(ms))
    }
}
