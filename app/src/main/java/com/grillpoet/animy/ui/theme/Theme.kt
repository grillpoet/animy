package com.grillpoet.animy.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ============================================================
// 亮色：白底黑字
// ============================================================
private val LightColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Color(0xFF000000),

    secondary = Color(0xFF444444),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF000000),

    tertiary = Color(0xFF888888),
    onTertiary = Color(0xFFFFFFFF),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),

    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF666666),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEDEDED),
    surfaceContainerHighest = Color(0xFFE0E0E0),

    outline = Color(0xFF888888),
    outlineVariant = Color(0xFFE0E0E0),

    error = Color(0xFF000000),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFEDEDED),
    onErrorContainer = Color(0xFF000000),

    inverseSurface = Color(0xFF1A1A1A),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFE5E5E5),
    scrim = Color(0xFF000000),
)

// ============================================================
// 暗色：纯黑底白字
//   · background 纯黑 #000000
//   · surface 深灰 #0F0F0F（分层）
//   · surfaceVariant 稍亮 #1A1A1A（卡片 / 输入框）
// ============================================================
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFFFFFFF),

    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color(0xFFFFFFFF),

    tertiary = Color(0xFF888888),
    onTertiary = Color(0xFF000000),

    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),

    surface = Color(0xFF0F0F0F),
    onSurface = Color(0xFFFFFFFF),

    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFAAAAAA),

    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0F0F0F),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2A2A2A),

    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF2A2A2A),

    error = Color(0xFFFFFFFF),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF1F1F1F),
    onErrorContainer = Color(0xFFFFFFFF),

    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF2A2A2A),
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AnimYTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = AppShapes,
        content = content,
    )
}
