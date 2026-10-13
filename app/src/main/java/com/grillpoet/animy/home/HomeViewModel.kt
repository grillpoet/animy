package com.grillpoet.animy.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grillpoet.animy.data.persistence.ProjectRepository
import com.grillpoet.animy.domain.model.Project
import com.grillpoet.animy.domain.model.ProjectMode
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProjectRepository(app)

    var projects by mutableStateOf<List<ProjectRepository.Summary>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            projects = runCatching { repo.list() }.getOrDefault(emptyList())
            loading = false
        }
    }

    fun rename(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repo.rename(id, trimmed)
            refresh()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            refresh()
        }
    }

    /** 复制项目，返回新 id */
    fun duplicate(id: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newId = runCatching { repo.duplicate(id) }.getOrNull()
            if (newId != null) {
                refresh()
                onCreated(newId)
            }
        }
    }

    /** 创建空项目并落盘。返回 id 供导航。 */
    suspend fun create(mode: ProjectMode, name: String, size: IntSize): String {
        val prefix = if (mode == ProjectMode.Paint) "paint" else "anim"
        val id = "$prefix-${System.currentTimeMillis()}-${(1000..9999).random()}"

        val displayName = name.trim().ifEmpty {
            if (mode == ProjectMode.Paint) "未命名绘画" else "未命名动画"
        }

        val project = Project(
            name = displayName,
            mode = mode,
            canvasSize = size,
        )
        repo.save(project, id)
        refresh()
        return id
    }
}
