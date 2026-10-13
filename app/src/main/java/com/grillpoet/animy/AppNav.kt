package com.grillpoet.animy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.grillpoet.animy.editor.EditorScreen
import com.grillpoet.animy.home.HomeScreen

sealed interface Screen {
    data object Home : Screen
    data class Editor(val projectId: String) : Screen
}

private val ScreenSaver: Saver<Screen, List<String>> = Saver(
    save = { screen ->
        when (screen) {
            Screen.Home -> listOf("home")
            is Screen.Editor -> listOf("editor", screen.projectId)
        }
    },
    restore = { list ->
        if (list.size >= 2 && list[0] == "editor") {
            runCatching { Screen.Editor(list[1]) }.getOrDefault(Screen.Home)
        } else Screen.Home
    },
)

@Composable
fun AppNavHost() {
    var screen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Home)
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(onOpen = { id -> screen = Screen.Editor(id) })
        is Screen.Editor -> EditorScreen(
            projectId = s.projectId,
            onBack = { screen = Screen.Home },
        )
    }
}
