package com.grillpoet.animy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.grillpoet.animy.ui.theme.AnimYTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // edge-to-edge + 纯黑 scrim：内容延伸到系统栏，但关键 UI 通过
        // statusBarsPadding / navigationBarsPadding 避开刘海和导航条。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = Color(0xFF000000).toArgb(),
                darkScrim = Color(0xFF000000).toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color(0xFF000000).toArgb(),
                darkScrim = Color(0xFF000000).toArgb(),
            ),
        )
        super.onCreate(savedInstanceState)

        setContent {
            AnimYTheme {
                Surface { AppNavHost() }
            }
        }
    }
}
