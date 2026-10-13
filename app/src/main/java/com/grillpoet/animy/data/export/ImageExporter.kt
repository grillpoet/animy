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
import com.grillpoet.animy.data.render.BitmapCompositor
import com.grillpoet.animy.domain.model.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ImageFormat(
    val ext: String,
    val mime: String,
    val label: String,
    val supportsAlpha: Boolean,
) {
    PNG("png", "image/png", "PNG · 无损，支持透明", true),
    JPG("jpg", "image/jpeg", "JPG · 小体积，无透明", false),
    WEBP("webp", "image/webp", "WebP · 现代格式", true),
}

sealed interface ImageExportResult {
    data class Success(val uri: String) : ImageExportResult
    data class Failure(val reason: String) : ImageExportResult
}

object ImageExporter {

    private const val SUB_DIR = "animy"

    suspend fun export(
        context: Context,
        frame: Frame,
        format: ImageFormat,
        size: IntSize,
        background: Color,
        name: String = "animy_" + System.currentTimeMillis(),
    ): ImageExportResult = withContext(Dispatchers.IO) {
        try {
            val image = BitmapCompositor.renderFrame(frame, size, background)
            var src = image.asAndroidBitmap()

            // 不支持透明的格式：白底拍平
            if (!format.supportsAlpha) {
                val flat = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(flat)
                c.drawColor(android.graphics.Color.WHITE)
                c.drawBitmap(src, 0f, 0f, null)
                src = flat
            }

            val compress = when (format) {
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
                ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            val quality = if (format == ImageFormat.PNG) 100 else 92

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：用 RELATIVE_PATH
                writeViaMediaStoreQ(context, src, compress, quality, format, name)
            } else {
                // Android 9-：用 DATA 指定绝对路径
                writeViaLegacyMediaStore(context, src, compress, quality, format, name)
            } ?: return@withContext ImageExportResult.Failure("无法创建文件")

            ImageExportResult.Success(uri)
        } catch (e: Exception) {
            ImageExportResult.Failure(e.message ?: "未知错误")
        }
    }

    private fun writeViaMediaStoreQ(
        context: Context, bmp: Bitmap,
        compress: Bitmap.CompressFormat, quality: Int,
        format: ImageFormat, name: String,
    ): String? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.${format.ext}")
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$SUB_DIR")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return null
        val ok = context.contentResolver.openOutputStream(uri)?.use { out ->
            bmp.compress(compress, quality, out)
        } ?: false
        if (!ok) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            return null
        }
        return uri.toString()
    }

    @Suppress("DEPRECATION")
    private fun writeViaLegacyMediaStore(
        context: Context, bmp: Bitmap,
        compress: Bitmap.CompressFormat, quality: Int,
        format: ImageFormat, name: String,
    ): String? {
        // API 26-28：先写文件，再插入 MediaStore
        val pictures = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val dir = File(pictures, SUB_DIR).apply { mkdirs() }
        val file = File(dir, "$name.${format.ext}")
        file.outputStream().use { out ->
            bmp.compress(compress, quality, out)
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, format.mime)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
        return uri?.toString() ?: file.absolutePath
    }
}
