package com.grillpoet.animy.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 取色反馈弹窗。
 *
 * 卡片本身染成取到的颜色 —— 用户一眼看到实际效果，
 * 文字颜色根据亮度自动切黑/白，保证任何颜色下都可读。
 */
@Composable
fun PickedColorPopup(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    val a = (color.alpha * 255).toInt().coerceIn(0, 255)
    val hex = String.format("#%02X%02X%02X",
        r, g, b,
    )

    val isLight = color.luminance() > 0.5f
    val fg = if (isLight) Color.Black else Color.White
    val fgDim = fg.copy(alpha = 0.6f)
    val borderColor = if (isLight) Color.Black.copy(alpha = 0.15f)
                      else Color.White.copy(alpha = 0.20f)

    // 全透明色的特殊处理：显示棋盘格
    val isTransparent = color.alpha < 0.05f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isTransparent) Color(0xFFFFE5E5) else color,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        shadowElevation = 12.dp,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：色相/透明度预览圆（若全透明则画棋盘格）
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isTransparent) Color.Transparent else color)
                    .border(2.dp, fg.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isTransparent) {
                    Text(
                        "∅",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    "已取色",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = fgDim,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    hex,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = fg,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (a < 255) "RGBA $r $g $b $a" else "R $r   G $g   B $b",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = fgDim,
                )
            }
        }
    }
}
