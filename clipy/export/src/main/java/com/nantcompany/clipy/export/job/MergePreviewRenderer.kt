package com.nantcompany.clipy.export.job

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.min

object MergePreviewRenderer {
    private const val PreviewSegmentMs = 2200L
    private const val MinimumTransitionMs = 200L

    suspend fun renderGapPreview(
        context: Context,
        leftPath: String,
        rightPath: String,
        transition: String,
        transitionDurationMs: Long = 1000L
    ): String? = withContext(Dispatchers.IO) {
        val leftDurationMs = probeDurationMs(leftPath)
        val rightDurationMs = probeDurationMs(rightPath)
        if (leftDurationMs <= 0L || rightDurationMs <= 0L) return@withContext null

        val leftSegmentMs = min(PreviewSegmentMs, leftDurationMs).coerceAtLeast(1L)
        val rightSegmentMs = min(PreviewSegmentMs, rightDurationMs).coerceAtLeast(1L)
        val requestedTransition = normalizeTransitionName(transition)
        val transitionLimitMs = min(leftSegmentMs, rightSegmentMs) - 100L
        val canRenderTransition = requestedTransition != "none" && transitionLimitMs >= MinimumTransitionMs
        val effectiveTransition = if (canRenderTransition) requestedTransition else "none"
        val effectiveTransitionMs = if (canRenderTransition) {
            min(transitionDurationMs.coerceAtLeast(MinimumTransitionMs), transitionLimitMs)
        } else {
            0L
        }

        val leftFile = File(leftPath)
        val rightFile = File(rightPath)
        val outputDir = File(context.cacheDir, "merge-preview")
        outputDir.mkdirs()
        val outputFile = File(
            outputDir,
            "gap_${previewKey(leftFile, rightFile, effectiveTransition, effectiveTransitionMs)}.mp4"
        )
        if (outputFile.exists() && outputFile.length() > 0L) return@withContext outputFile.absolutePath

        outputDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("mp4", ignoreCase = true) && it.name != outputFile.name }
            ?.forEach { runCatching { it.delete() } }

        val leftStartMs = (leftDurationMs - leftSegmentMs).coerceAtLeast(0L)
        val leftChain = "trim=duration=${formatSeconds(leftSegmentMs)},setpts=PTS-STARTPTS,${previewVideoChain()}"
        val rightChain = "trim=duration=${formatSeconds(rightSegmentMs)},setpts=PTS-STARTPTS,${previewVideoChain()}"
        val filter = if (effectiveTransition == "none") {
            "[0:v]$leftChain[v0];[1:v]$rightChain[v1];[v0][v1]concat=n=2:v=1:a=0[outv]"
        } else {
            val offset = formatSeconds((leftSegmentMs - effectiveTransitionMs).coerceAtLeast(0L))
            val duration = formatSeconds(effectiveTransitionMs)
            "[0:v]$leftChain[v0];[1:v]$rightChain[v1];" +
                "[v0][v1]xfade=transition=$effectiveTransition:duration=$duration:offset=$offset[outv]"
        }

        val args = listOf(
            "-y",
            "-ss",
            formatSeconds(leftStartMs),
            "-i",
            leftPath,
            "-i",
            rightPath,
            "-filter_complex",
            filter,
            "-map",
            "[outv]",
            "-an",
            "-c:v",
            "libx264",
            "-preset",
            "ultrafast",
            "-pix_fmt",
            "yuv420p",
            "-movflags",
            "+faststart",
            outputFile.absolutePath
        )

        val session = FFmpegKit.executeWithArguments(args.toTypedArray())
        if (ReturnCode.isSuccess(session.returnCode) && outputFile.exists() && outputFile.length() > 0L) {
            outputFile.absolutePath
        } else {
            runCatching { outputFile.delete() }
            null
        }
    }

    private fun probeDurationMs(path: String): Long {
        return runCatching {
            val session = FFprobeKit.getMediaInformation(path)
            (session.mediaInformation?.duration?.toDouble()?.times(1000))?.toLong()
        }.getOrNull() ?: 0L
    }

    private fun normalizeTransitionName(name: String): String {
        return when (name.lowercase(Locale.US)) {
            "xfade", "crossfade" -> "fade"
            "custom" -> "none"
            else -> name.lowercase(Locale.US)
        }
    }

    private fun previewVideoChain(): String {
        return "scale=720:1280:force_original_aspect_ratio=decrease," +
            "pad=720:1280:(ow-iw)/2:(oh-ih)/2," +
            "setsar=1,format=yuv420p,fps=30"
    }

    private fun previewKey(
        leftFile: File,
        rightFile: File,
        transition: String,
        transitionDurationMs: Long
    ): String {
        val source = buildString {
            append(leftFile.absolutePath).append('|')
            append(leftFile.lastModified()).append('|')
            append(leftFile.length()).append('|')
            append(rightFile.absolutePath).append('|')
            append(rightFile.lastModified()).append('|')
            append(rightFile.length()).append('|')
            append(transition).append('|')
            append(transitionDurationMs)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }.take(24)
    }

    private fun formatSeconds(ms: Long): String = String.format(Locale.US, "%.3f", ms / 1000.0)
}
