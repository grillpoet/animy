package com.grillpoet.animy.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize
import com.bumptech.glide.gifencoder.AnimatedGifEncoder
import com.grillpoet.animy.data.render.BitmapCompositor
import com.grillpoet.animy.domain.model.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface GifExportResult {
    data class Success(val uri: String) : GifExportResult
    data class Failure(val reason: String) : GifExportResult
}

object GifExporter {

    private const val SUB_DIR = "animy"
    private const val MAX_DIM = 480

    suspend fun export(
        context: Context,
        frames: List<Frame>,
        size: IntSize,
        background: Color,
        name: String = "animy_" + System.currentTimeMillis(),
        onProgress: ((Int, Int) -> Unit)? = null,
    ): GifExportResult = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext GifExportResult.Failure("没有帧")

        // 目标尺寸（等比缩放到 MAX_DIM 内，偶数）
        val k = MAX_DIM.toFloat() / maxOf(size.width, size.height)
        val targetW = (size.width * k).toInt().coerceAtLeast(16) and 0xFFFFFFFE.toInt()
        val targetH = (size.height * k).toInt().coerceAtLeast(16) and 0xFFFFFFFE.toInt()
        val targetSize = IntSize(targetW, targetH)

        val tmp = File(context.cacheDir, "$name.gif")

        val ok = try {
            tmp.outputStream().use { out ->
                encodeTo(out, frames, size, targetSize, background, onProgress)
            }
            true
        } catch (e: Exception) {
            tmp.delete()
            return@withContext GifExportResult.Failure(e.message ?: "编码失败")
        }

        if (!ok) {
            tmp.delete()
            return@withContext GifExportResult.Failure("编码失败")
        }

        val result = try {
            saveToGallery(context, tmp, name)
        } catch (e: Exception) {
            GifExportResult.Failure(e.message ?: "保存失败")
        }
        tmp.delete()
        result
    }

    /**
     * 逐帧渲染 + 缩放 + 编码。
     *
     * @param canvasSize  原始画布尺寸（渲染用）
     * @param targetSize  GIF 目标尺寸（缩放用）
     */
    private fun encodeTo(
        out: java.io.OutputStream,
        frames: List<Frame>,
        canvasSize: IntSize,
        targetSize: IntSize,
        background: Color,
        onProgress: ((Int, Int) -> Unit)?,
    ): Boolean {
        val encoder = AnimatedGifEncoder()
        encoder.start(out)
        encoder.setRepeat(0)      // 无限循环
        encoder.setQuality(10)    // 10 = 质量优先

        val needScale = canvasSize != targetSize
        val total = frames.size

        frames.forEachIndexed { i, frame ->
            onProgress?.invoke(i, total)

            // 1. 按原始尺寸完整渲染
            val full: Bitmap = BitmapCompositor
                .renderFrame(frame, canvasSize, background)
                .asAndroidBitmap()

            // 2. 缩放到目标尺寸
            val frameBmp: Bitmap = if (needScale) {
                val scaled = Bitmap.createScaledBitmap(
                    full, targetSize.width, targetSize.height, true,
                )
                if (scaled !== full) full.recycle()
                scaled
            } else full

            try {
                encoder.setDelay(frame.durationMs.toInt())
                encoder.addFrame(frameBmp)
                // AnimatedGifEncoder.addFrame 是同步的：内部会立即
                // 把像素读走并转成调色板索引，返回后即可安全 recycle。
                // 每帧及时释放，避免长动画 OOM。
            } finally {
                if (!frameBmp.isRecycled) frameBmp.recycle()
            }
        }
        onProgress?.invoke(total, total)

        encoder.finish()
        return true
    }

    private fun saveToGallery(context: Context, src: File, name: String): GifExportResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.gif")
                put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$SUB_DIR")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return GifExportResult.Failure("无法创建文件")

            val ok = context.contentResolver.openOutputStream(uri)?.use { os ->
                src.inputStream().use { it.copyTo(os) }
            } != null

            return if (ok) GifExportResult.Success(uri.toString())
            else {
                runCatching { context.contentResolver.delete(uri, null, null) }
                GifExportResult.Failure("写入失败")
            }
        } else {
            @Suppress("DEPRECATION")
            val pictures = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val dir = File(pictures, SUB_DIR).apply { mkdirs() }
            val target = File(dir, "$name.gif")
            src.inputStream().use { ins ->
                target.outputStream().use { outs -> ins.copyTo(outs) }
            }
            return GifExportResult.Success(target.absolutePath)
        }
    }
}
