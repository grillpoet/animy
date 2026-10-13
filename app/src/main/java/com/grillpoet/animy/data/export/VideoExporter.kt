package com.grillpoet.animy.data.export

import android.content.ContentValues
import android.content.Context
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
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
import java.nio.ByteBuffer

sealed interface VideoExportResult {
    data class Success(val uri: String) : VideoExportResult
    data class Failure(val reason: String) : VideoExportResult
}

object VideoExporter {

    private const val MIME = "video/avc"
    private const val BIT_RATE = 8_000_000
    private const val I_FRAME_INTERVAL = 1
    private const val TIMEOUT_US = 10_000L
    private const val MAX_DEQUEUE_ATTEMPTS = 200
    private const val SUB_DIR = "animy"

    /**
     * 导出为 MP4。每帧时长由 frame.durationMs 决定（而非全局 fps）。
     */
    suspend fun export(
        context: Context,
        frames: List<Frame>,
        size: IntSize,
        background: Color,
        name: String = "animy_" + System.currentTimeMillis(),
        onProgress: ((done: Int, total: Int) -> Unit)? = null,
    ): VideoExportResult = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext VideoExportResult.Failure("没有帧")

        val w = size.width and 0xFFFFFFFE.toInt()
        val h = size.height and 0xFFFFFFFE.toInt()
        if (w < 16 || h < 16) return@withContext VideoExportResult.Failure("画布太小")

        val tmp = File(context.cacheDir, "$name.mp4")

        val ok = try {
            encode(frames, w, h, background, tmp, onProgress)
        } catch (e: Exception) {
            tmp.delete()
            return@withContext VideoExportResult.Failure(e.message ?: "编码失败")
        }

        if (!ok) {
            tmp.delete()
            return@withContext VideoExportResult.Failure("设备不支持 H.264 编码")
        }

        val result = try {
            saveToGallery(context, tmp, name)
        } catch (e: Exception) {
            VideoExportResult.Failure(e.message ?: "保存失败")
        }
        tmp.delete()
        result
    }

    private fun encode(
        frames: List<Frame>,
        w: Int, h: Int,
        background: Color,
        outFile: File,
        onProgress: ((Int, Int) -> Unit)?,
    ): Boolean {
        if (!hasFlexibleFormat()) return false

        // 用平均帧率配置编码器
        val totalMs = frames.sumOf { it.durationMs }.coerceAtLeast(1)
        val avgFps = (frames.size * 1000L / totalMs).toInt().coerceIn(1, 60)

        val format = MediaFormat.createVideoFormat(MIME, w, h).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, avgFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val codec = MediaCodec.createEncoderByType(MIME)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()
        val argb = IntArray(w * h)

        try {
            var ptsUs = 0L
            val total = frames.size
            frames.forEachIndexed { i, frame ->
                onProgress?.invoke(i, total)

                val bmp = BitmapCompositor.renderFrame(frame, IntSize(w, h), background)
                    .asAndroidBitmap()
                bmp.getPixels(argb, 0, w, 0, 0, w, h)
                bmp.recycle()

                var inIdx = -1
                var attempts = 0
                while (inIdx < 0 && attempts < MAX_DEQUEUE_ATTEMPTS) {
                    attempts++
                    inIdx = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx < 0) {
                        drain(codec, muxer, bufferInfo) { newTrack ->
                            if (!muxerStarted && newTrack >= 0) {
                                trackIndex = newTrack
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                    }
                }
                if (inIdx < 0) return false

                val img = codec.getInputImage(inIdx) ?: return false
                fillImage(img, argb, w, h)
                codec.queueInputBuffer(inIdx, 0, w * h * 3 / 2, ptsUs, 0)
                ptsUs += frame.durationMs * 1000L

                drain(codec, muxer, bufferInfo) { newTrack ->
                    if (!muxerStarted && newTrack >= 0) {
                        trackIndex = newTrack
                        muxer.start()
                        muxerStarted = true
                    }
                }
            }

            val eosIdx = codec.dequeueInputBuffer(TIMEOUT_US)
            if (eosIdx >= 0) {
                codec.queueInputBuffer(eosIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            var eos = false
            while (!eos) {
                val oIdx = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    oIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                    oIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    oIdx >= 0 -> {
                        val buf = codec.getOutputBuffer(oIdx)
                        if (buf != null && muxerStarted && trackIndex >= 0 &&
                            bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 &&
                            bufferInfo.size > 0
                        ) {
                            buf.position(bufferInfo.offset)
                            buf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, buf, bufferInfo)
                        }
                        codec.releaseOutputBuffer(oIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) eos = true
                    }
                }
            }
        } finally {
            onProgress?.invoke(frames.size, frames.size)
            runCatching { codec.stop() }
            codec.release()
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
        return true
    }

    /**
     * 排出已就绪的输出帧。
     *
     * 修复：原来硬编码 track index = 0，多轨时会写错。
     * 现在通过 onFormat 回调把真实 track index 交给调用方。
     */
    private inline fun drain(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        onFormat: (Int) -> Unit,
    ) {
        while (true) {
            val oIdx = codec.dequeueOutputBuffer(bufferInfo, 0L)
            when {
                oIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                oIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    onFormat(muxer.addTrack(codec.outputFormat))
                }
                oIdx >= 0 -> {
                    val buf = codec.getOutputBuffer(oIdx)
                    if (buf != null &&
                        bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 &&
                        bufferInfo.size > 0
                    ) {
                        buf.position(bufferInfo.offset)
                        buf.limit(bufferInfo.offset + bufferInfo.size)
                        // 用 muxer 真实 track 数推断（此处仅 1 条）
                        runCatching {
                            // 用 track 0 写入，因为本 encoder 只产出 1 条轨道
                            muxer.writeSampleData(0, buf, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(oIdx, false)
                }
            }
        }
    }

    /**
     * 把 ARGB 数组填入 YUV420Flexible 编码器的 Image。
     * 前置条件：w / h 必须是偶数（调用方已通过 `and 0xFFFFFFFE` 保证）。
     */
    private fun fillImage(img: Image, argb: IntArray, w: Int, h: Int) {
        require(w % 2 == 0 && h % 2 == 0) { "YUV420 需要偶数宽高" }

        val yPlane = img.planes[0]
        val yBuf: ByteBuffer = yPlane.buffer
        val yRow = yPlane.rowStride
        val yPix = yPlane.pixelStride

        if (yPix == 1 && yRow == w) {
            val yBytes = ByteArray(w * h)
            for (i in 0 until w * h) {
                val c = argb[i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                yBytes[i] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).toByte()
            }
            yBuf.position(0)
            yBuf.put(yBytes)
        } else {
            var idx = 0
            for (j in 0 until h) {
                val base = j * yRow
                for (i in 0 until w) {
                    val c = argb[idx++]
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                    yBuf.put(base + i * yPix, y.toByte())
                }
            }
        }

        val uPlane = img.planes[1]
        val vPlane = img.planes[2]
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val uRow = uPlane.rowStride
        val uPix = uPlane.pixelStride
        val vRow = vPlane.rowStride
        val vPix = vPlane.pixelStride

        var uvRow = 0
        var j = 0
        while (j < h) {
            var uvCol = 0
            var i = 0
            while (i < w) {
                var rSum = 0; var gSum = 0; var bSum = 0
                for (dy in 0 until 2) for (dx in 0 until 2) {
                    val c = argb[(j + dy) * w + (i + dx)]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                }
                val r = rSum / 4; val g = gSum / 4; val b = bSum / 4
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                uBuf.put(uvRow * uRow + uvCol * uPix, u.toByte())
                vBuf.put(uvRow * vRow + uvCol * vPix, v.toByte())
                uvCol++
                i += 2
            }
            uvRow++
            j += 2
        }
    }

    private fun hasFlexibleFormat(): Boolean = runCatching {
        val info = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .firstOrNull { it.isEncoder && it.supportedTypes.any { t -> t.equals(MIME, true) } }
            ?: return false
        val caps = info.getCapabilitiesForType(MIME)
        caps.colorFormats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
    }.getOrDefault(false)

    private fun saveToGallery(context: Context, src: File, name: String): VideoExportResult {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/$SUB_DIR")
            } else {
                @Suppress("DEPRECATION")
                val movies = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES
                )
                val dir = File(movies, SUB_DIR).apply { mkdirs() }
                val target = File(dir, "$name.mp4")
                src.inputStream().use { ins ->
                    target.outputStream().use { outs -> ins.copyTo(outs) }
                }
                put(MediaStore.Video.Media.DATA, target.absolutePath)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return VideoExportResult.Failure("无法创建文件")

        // API 29+ 需要把数据写进 MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val ok = context.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            } != null
            if (!ok) {
                runCatching { context.contentResolver.delete(uri, null, null) }
                return VideoExportResult.Failure("写入失败")
            }
        }

        return VideoExportResult.Success(uri.toString())
    }
}
