package com.grillpoet.animy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grillpoet.animy.editor.SelectTool

@Composable
fun SelectToolRow(
    current: SelectTool,
    onSelect: (SelectTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectTool.entries.forEach { t ->
            val active = t == current
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(t) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    t.glyph(),
                    fontSize = 16.sp,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun SelectTool.glyph(): String = when (this) {
    SelectTool.Wand -> "✦"
    SelectTool.Lasso -> "◌"
    SelectTool.Rect -> "▢"
    SelectTool.Ellipse -> "◯"
}
