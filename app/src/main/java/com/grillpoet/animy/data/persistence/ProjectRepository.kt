package com.grillpoet.animy.data.persistence

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.grillpoet.animy.domain.model.BitmapElement
import com.grillpoet.animy.domain.model.Project
import com.grillpoet.animy.domain.model.ProjectMode
import com.grillpoet.animy.domain.model.ShapeElement
import com.grillpoet.animy.domain.model.VectorStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 项目仓储。
 *
 * 存储结构：
 *   filesDir/projects/<id>/
 *       project.json        项目元数据
 *       project.json.corrupt  损坏备份（若解析失败）
 *       images/             位图元素
 *       thumb.png           首页缩略图
 *
 * 写入使用「临时文件 + 原子重命名」，避免半写状态。
 */
class ProjectRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val root: File get() = File(context.filesDir, "projects")

    /**
     * 写操作互斥锁。
     *
     * save / rename / delete / duplicate 都会触碰 project.json 和
     * images/ 目录。EditorViewModel 里有三条路径会调 save
     * （防抖、ON_PAUSE、返回前），协程 cancel 是协作式的，
     * 一旦进入 IO 阻塞写就不再检查取消 —— 没有这把锁会出现
     * 两个 save 抢写同一个 project.json.tmp，导致存档半写 / 错乱。
     */
    private val ioMutex = Mutex()

    // ─────────────── 查询 ───────────────

    suspend fun exists(id: String): Boolean = withContext(Dispatchers.IO) {
        File(File(root, id), "project.json").exists()
    }

    suspend fun list(): List<Summary> = withContext(Dispatchers.IO) {
        if (!root.exists()) return@withContext emptyList()
        root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir -> readSummary(dir) }
            ?.sortedByDescending { it.updatedAt }
            ?.toList()
            ?: emptyList()
    }

    private fun readSummary(dir: File): Summary? {
        val file = File(dir, "project.json")
        if (!file.exists()) return null
        return runCatching {
            val s = json.decodeFromString<SavedProject>(file.readText())
            val thumb = File(dir, "thumb.png")
            Summary(
                id = dir.name,
                name = s.name,
                updatedAt = file.lastModified(),
                frameCount = s.frames.size,
                mode = runCatching { ProjectMode.valueOf(s.mode) }
                    .getOrDefault(ProjectMode.Animation),
                thumbPath = if (thumb.exists()) thumb.absolutePath else null,
            )
        }.getOrNull()
    }

    // ─────────────── 读 ───────────────

    /**
     * 加载项目。JSON 解析失败时，把原文件备份为 .corrupt 后抛出异常，
     * 让调用方决定如何处理（避免后续 save 覆盖用户数据）。
     */
    suspend fun load(id: String): Project = withContext(Dispatchers.IO) {
        val dir = File(root, id)
        val file = File(dir, "project.json")
        check(file.exists()) { "项目不存在: $id" }
        val text = file.readText()
        try {
            val saved = json.decodeFromString<SavedProject>(text)
            Migrations.migrate(saved, File(dir, "images"))
        } catch (e: Exception) {
            runCatching {
                val corrupt = File(dir, "project.json.corrupt")
                if (!corrupt.exists()) file.copyTo(corrupt, overwrite = false)
            }
            throw e
        }
    }

    // ─────────────── 写 ───────────────

    suspend fun save(project: Project, id: String): String = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val dir = File(root, id).apply { mkdirs() }
            val imagesDir = File(dir, "images").apply { mkdirs() }

            val saved = project.toSaved(imagesDir)
            val target = File(dir, "project.json")
            val tmp = File(dir, "project.json.tmp")
            tmp.writeText(json.encodeToString(saved))

            atomicMove(tmp, target)

            // 缩略图失败不阻断保存
            runCatching {
                val thumb = renderThumb(project)
                File(dir, "thumb.png").outputStream().use { out ->
                    thumb.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 85, out)
                }
            }
            id
        }
    }

    /** 原子替换。失败时抛异常，避免静默丢数据。 */
    private fun atomicMove(from: File, to: File) {
        val ok = runCatching {
            Files.move(
                from.toPath(),
                to.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.isSuccess
        if (!ok) {
            // 回退到 renameTo（可能是跨文件系统时 ATOMIC_MOVE 不支持）
            if (!from.renameTo(to)) {
                throw IOException("无法写入 ${to.absolutePath}")
            }
        }
    }

    suspend fun rename(id: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val dir = File(root, id)
            val file = File(dir, "project.json")
            if (!file.exists()) {
                false
            } else {
                runCatching {
                    val saved = json.decodeFromString<SavedProject>(file.readText())
                    val updated = saved.copy(name = newName)
                    val tmp = File(dir, "project.json.tmp")
                    tmp.writeText(json.encodeToString(updated))
                    atomicMove(tmp, file)
                    true
                }.getOrDefault(false)
            }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            File(root, id).deleteRecursively()
        }
        Unit
    }

    /**
     * 复制项目为新项目。返回新 id。
     *
     * 深拷贝：读原 JSON，改 name，写到新目录。图片位图按需复制。
     */
    suspend fun duplicate(id: String): String? = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            runCatching {
                val srcDir = File(root, id)
                if (!srcDir.exists()) return@runCatching null

                val newId = "copy-${System.currentTimeMillis()}-${(1000..9999).random()}"
                val dstDir = File(root, newId).apply { mkdirs() }

                // 复制 images 目录
                val srcImages = File(srcDir, "images")
                if (srcImages.exists()) {
                    val dstImages = File(dstDir, "images").apply { mkdirs() }
                    srcImages.listFiles()?.forEach { f ->
                        f.copyTo(File(dstImages, f.name), overwrite = true)
                    }
                }

                // 读原 JSON，改 name
                val srcJson = File(srcDir, "project.json")
                val saved = json.decodeFromString<SavedProject>(srcJson.readText())
                val newName = if (saved.name.contains(" 副本")) saved.name
                              else "${saved.name} 副本"
                val updated = saved.copy(name = newName)
                File(dstDir, "project.json").writeText(json.encodeToString(updated))

                // 复制缩略图
                val srcThumb = File(srcDir, "thumb.png")
                if (srcThumb.exists()) {
                    srcThumb.copyTo(File(dstDir, "thumb.png"), overwrite = true)
                }

                newId
            }.getOrNull()
        }
    }

    // ─────────────── 缩略图 ───────────────

    private fun renderThumb(project: Project, maxDim: Int = 320) =
        com.grillpoet.animy.data.render.BitmapCompositor.renderThumbnail(project, maxDim)

    // ─────────────── 模型 → DTO ───────────────

    private fun Project.toSaved(imagesDir: File): SavedProject = SavedProject(
        version = CURRENT_VERSION,
        name = name,
        mode = mode.name,
        canvasWidth = canvasSize.width,
        canvasHeight = canvasSize.height,
        background = background.value.toLong(),
        palette = palette.map { it.value.toLong() },
        frames = frames.map { frame ->
            SavedFrame(
                durationMs = frame.durationMs,
                layers = frame.layers.map { layer ->
                    SavedLayer(
                        name = layer.name,
                        visible = layer.visible,
                        alpha = layer.alpha,
                        elements = layer.elements.map { e ->
                            when (e) {
                                is VectorStroke -> SavedStroke(
                                    id = e.id,
                                    points = buildList {
                                        e.points.forEach { p ->
                                            add(p.x); add(p.y); add(p.pressure)
                                        }
                                    },
                                    brushType = e.brush.type.name,
                                    color = e.brush.color.value.toLong(),
                                    width = e.brush.width,
                                    opacity = e.brush.opacity,
                                    offsetX = e.transform.offset.x,
                                    offsetY = e.transform.offset.y,
                                    scale = e.transform.scale,
                                    rotation = e.transform.rotation,
                                )
                                is ShapeElement -> SavedShape(
                                    id = e.id,
                                    shapeType = e.shapeType.name,
                                    startX = e.start.x,
                                    startY = e.start.y,
                                    endX = e.end.x,
                                    endY = e.end.y,
                                    brushType = e.brush.type.name,
                                    color = e.brush.color.value.toLong(),
                                    width = e.brush.width,
                                    opacity = e.brush.opacity,
                                    filled = e.filled,
                                    offsetX = e.transform.offset.x,
                                    offsetY = e.transform.offset.y,
                                    scale = e.transform.scale,
                                    rotation = e.transform.rotation,
                                )
                                is BitmapElement -> {
                                    val png = File(imagesDir, "${e.id}.png")
                                    png.outputStream().use { out ->
                                        e.image.asAndroidBitmap()
                                            .compress(Bitmap.CompressFormat.PNG, 100, out)
                                    }
                                    SavedBitmap(
                                        id = e.id,
                                        imagePath = png.name,
                                        offsetX = e.transform.offset.x,
                                        offsetY = e.transform.offset.y,
                                        scale = e.transform.scale,
                                        rotation = e.transform.rotation,
                                        label = e.label,
                                    )
                                }
                            }
                        },
                    )
                },
            )
        },
    )

    data class Summary(
        val id: String,
        val name: String,
        val updatedAt: Long,
        val frameCount: Int,
        val mode: ProjectMode,
        val thumbPath: String?,
    )
}
