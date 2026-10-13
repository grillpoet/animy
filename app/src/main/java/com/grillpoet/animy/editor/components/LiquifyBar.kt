package com.grillpoet.animy.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiquifyBar(
    radius: Float,
    strength: Float,
    onRadius: (Float) -> Unit,
    onStrength: (Float) -> Unit,
    onReset: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("笔刷大小", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = radius,
                    onValueChange = onRadius,
                    valueRange = 20f..200f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text("${radius.toInt()}", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("强度", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = strength,
                    onValueChange = onStrength,
                    valueRange = 0.1f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text("${(strength * 100).toInt()}%", fontSize = 12.sp)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                TextButton(onClick = onReset) { Text("重置") }
                TextButton(onClick = onConfirm) { Text("确定") }
            }
        }
    }
}
