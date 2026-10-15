package com.grillpoet.animy.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grillpoet.animy.data.export.ImageFormat
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Size as ComposeSize

// ─────────── 背景色 ───────────

private val BgColors = listOf(
    Color.White, Color(0xFFF5F5F5), Color(0xFFFFF8E1),
    Color(0xFFE3F2FD), Color(0xFFFCE4EC), Color(0xFFE8F5E9),
    Color(0xFF1A1A1A), Color.Black, Color.Transparent,
)

@Composable
fun BackgroundPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    var picked by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("画布背景") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (picked == Color.Transparent) CheckerSquare(Modifier.fillMaxWidth().height(64.dp))
                    else Box(Modifier.fillMaxWidth().height(64.dp).background(picked))
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BgColors.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { c -> Swatch(c, c == picked) { picked = c } }
                            repeat(5 - row.size) { Box(Modifier.size(44.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (color == Color.Transparent) CheckerSquare(Modifier.fillMaxWidth().height(44.dp))
        else Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(9.dp)).background(color))
    }
}

@Composable
private fun CheckerSquare(modifier: Modifier) {
    Canvas(modifier) {
        val cell = 8.dp.toPx()
        val cols = (size.width / cell).toInt() + 1
        val rows = (size.height / cell).toInt() + 1
        for (r in 0 until rows) for (c in 0 until cols) {
            drawRect(
                color = if ((r + c) % 2 == 0) Color.White else Color(0xFFCCCCCC),
                topLeft = Offset(c * cell, r * cell),
                size = ComposeSize(cell, cell),
            )
        }
    }
}

// ─────────── 帧速率 ───────────

private const val MIN_FPS = 1
private const val MAX_FPS = 62   // 1000 / 62 ≈ 16ms，Frame 时长下限

/**
 * 帧速率对话框。
 *
 * 输入是 fps 整数，落盘时换算成 ms（四舍五入）。
 * 62fps ≈ 16ms（Frame 的下限），1fps = 1000ms。
 */
@Composable
fun FrameDurationDialog(
    initial: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    // 初始 ms → fps，四舍五入；夹到合法区间
    val initialFps = (1000.0 / initial.toDouble()).roundToInt()
        .coerceIn(MIN_FPS, MAX_FPS)
    var fpsText by remember { mutableStateOf(initialFps.toString()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val fps = fpsText.toIntOrNull()
    val valid = fps != null && fps in MIN_FPS..MAX_FPS
    val ms: Long = if (fps != null && fps in MIN_FPS..MAX_FPS) {
        (1000.0 / fps).roundToInt().toLong()
    } else initial

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("帧速率") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = fpsText,
                            onValueChange = { s ->
                                fpsText = s.filter { it.isDigit() }.take(2)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (valid) onConfirm(ms) },
                            ),
                            modifier = Modifier
                                .width(64.dp)
                                .focusRequester(focusRequester),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "fps",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = if (valid) "约 ${ms} ms / 帧"
                           else "请输入 ${MIN_FPS} – ${MAX_FPS} 之间的整数",
                    fontSize = 13.sp,
                    color = if (valid) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "常见：24（电影）、30（视频）、12（手绘）",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(ms) },
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ─────────── 导出图片 ───────────

@Composable
fun ImageExportDialog(
    onDismiss: () -> Unit,
    onConfirm: (ImageFormat) -> Unit,
) {
    var picked by remember { mutableStateOf(ImageFormat.PNG) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出图片") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageFormat.entries.forEach { fmt ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(
                                if (fmt == picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { picked = fmt }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (fmt == picked) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (fmt == picked) {
                                Box(Modifier.size(10.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(fmt.name, fontSize = 15.sp)
                            Text(fmt.label, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(picked) }) { Text("导出") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}


// ─────────── 洋葱皮范围 ───────────

@Composable
fun OnionSkinRangeDialog(
    initialPrev: Int,
    initialNext: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var prev by remember { mutableFloatStateOf(initialPrev.toFloat()) }
    var next by remember { mutableFloatStateOf(initialNext.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("洋葱皮范围") },
        text = {
            Column {
                Text("前 ${prev.toInt()} 帧（红）", fontSize = 14.sp)
                Slider(
                    value = prev,
                    onValueChange = { prev = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
                Spacer(Modifier.height(12.dp))
                Text("后 ${next.toInt()} 帧（蓝）", fontSize = 14.sp)
                Slider(
                    value = next,
                    onValueChange = { next = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(prev.toInt(), next.toInt()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
