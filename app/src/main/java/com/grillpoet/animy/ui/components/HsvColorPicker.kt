package com.grillpoet.animy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush as ComposeBrush

/**
 * HSV 调色板。
 *
 * 纯 Compose 实现 —— 不依赖 android.graphics.Color。
 * 拖动三个滑条调色，实时预览 + HEX 显示。
 */
@Composable
fun HsvColorPicker(
    initial: Color,
    onConfirm: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(initial) { initial.toHsv() }
    var h by remember { mutableFloatStateOf(initialHsv[0]) }
    var s by remember { mutableFloatStateOf(initialHsv[1]) }
    var v by remember { mutableFloatStateOf(initialHsv[2]) }

    val current = hsv(h, s, v)
    val hex = remember(current) { current.toHex() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("选择颜色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 预览 + HEX
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(current)
                            .border(1.dp, MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        hex,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                HsvSlider(
                    label = "色相",
                    value = h,
                    range = 0f..360f,
                    trackBrush = ComposeBrush.horizontalGradient(
                        (0..6).map { i -> hsv(i * 60f, 1f, 1f) }
                    ),
                    onValue = { h = it },
                )

                HsvSlider(
                    label = "饱和度",
                    value = s,
                    range = 0f..1f,
                    trackBrush = ComposeBrush.horizontalGradient(
                        listOf(hsv(h, 0f, v), hsv(h, 1f, v))
                    ),
                    onValue = { s = it },
                )

                HsvSlider(
                    label = "明度",
                    value = v,
                    range = 0f..1f,
                    trackBrush = ComposeBrush.horizontalGradient(
                        listOf(hsv(h, s, 0f), hsv(h, s, 1f))
                    ),
                    onValue = { v = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    trackBrush: ComposeBrush,
    onValue: (Float) -> Unit,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth()) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .align(Alignment.Center)
            ) {
                drawRoundRect(
                    brush = trackBrush,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                )
            }
            Slider(
                value = value,
                onValueChange = onValue,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  HSV ↔ RGB
// ═══════════════════════════════════════════════════════════════

private fun hsv(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val hh = (h % 360f) / 60f
    val x = c * (1 - abs(hh % 2 - 1))
    val (r1, g1, b1) = when {
        hh < 1f -> Triple(c, x, 0f)
        hh < 2f -> Triple(x, c, 0f)
        hh < 3f -> Triple(0f, c, x)
        hh < 4f -> Triple(0f, x, c)
        hh < 5f -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }
    val m = v - c
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

private fun Color.toHsv(): FloatArray {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val v = max
    val s = if (max == 0f) 0f else (max - min) / max
    val h = when {
        max == min -> 0f
        max == r -> (60 * ((g - b) / (max - min)) + 360) % 360
        max == g -> 60 * ((b - r) / (max - min)) + 120
        else -> 60 * ((r - g) / (max - min)) + 240
    }
    return floatArrayOf(h, s, v)
}

private fun Color.toHex(): String {
    val r = (red * 255).roundToInt().coerceIn(0, 255)
    val g = (green * 255).roundToInt().coerceIn(0, 255)
    val b = (blue * 255).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}
