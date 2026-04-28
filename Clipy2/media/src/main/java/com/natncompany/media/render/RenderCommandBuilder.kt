package com.natncompany.media.render

import com.natncompany.media.Asset
import com.natncompany.media.AssetType
import com.natncompany.media.RenderConfig
import com.natncompany.media.RenderRequest
import com.natncompany.media.TimelineClip
import java.util.Locale

internal object RenderCommandBuilder {
    fun build(request: RenderRequest, outputPath: String, config: RenderConfig = request.config): String {
        val sequence = buildSequence(request)
        if (sequence.isEmpty()) {
            return listOf(
                "-y",
                "-f", "lavfi",
                "-i", quoted("color=c=black:s=${config.targetWidth}x${config.targetHeight}:d=1"),
                "-f", "lavfi",
                "-i", quoted("anullsrc=r=48000:cl=stereo"),
                "-shortest",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                quoted(outputPath)
            ).joinToString(" ")
        }

        val tokens = mutableListOf("-y")
        val filterParts = mutableListOf<String>()
        val concatInputs = mutableListOf<String>()
        var inputIndex = 0

        sequence.forEach { entry ->
            val durationSec = entry.durationMs.toSecondsString()
            when (entry.kind) {
                SegmentKind.Gap -> {
                    tokens += listOf("-f", "lavfi", "-t", durationSec, "-i", quoted("color=c=black:s=${config.targetWidth}x${config.targetHeight}"))
                    tokens += listOf("-f", "lavfi", "-t", durationSec, "-i", quoted("anullsrc=r=48000:cl=stereo"))
                    filterParts += "[$inputIndex:v]setsar=1,fps=${config.fps}[v$inputIndex]"
                    concatInputs += "[v$inputIndex][${inputIndex + 1}:a]"
                    inputIndex += 2
                }
                SegmentKind.Clip -> when (entry.asset?.type) {
                    AssetType.Image -> {
                        tokens += listOf("-loop", "1", "-t", durationSec, "-i", quoted(entry.asset.cachedPath))
                        tokens += listOf("-f", "lavfi", "-t", durationSec, "-i", quoted("anullsrc=r=48000:cl=stereo"))

                        filterParts += "[$inputIndex:v]scale=${config.targetWidth}:${config.targetHeight}:force_original_aspect_ratio=decrease,pad=${config.targetWidth}:${config.targetHeight}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1,fps=${config.fps}[v$inputIndex]"
                        concatInputs += "[v$inputIndex][${inputIndex + 1}:a]"
                        inputIndex += 2
                    }
                    AssetType.Video, AssetType.Unknown -> {
                        tokens += listOf(
                            "-ss", entry.sourceStartMs.toSecondsString(),
                            "-t", durationSec,
                            "-i", quoted(entry.asset.cachedPath)
                        )
                        tokens += listOf("-f", "lavfi", "-t", durationSec, "-i", quoted("anullsrc=r=48000:cl=stereo"))

                        filterParts += "[$inputIndex:v]scale=${config.targetWidth}:${config.targetHeight}:force_original_aspect_ratio=decrease,pad=${config.targetWidth}:${config.targetHeight}:(ow-iw)/2:(oh-ih)/2:color=black,setsar=1,fps=${config.fps}[v$inputIndex]"
                        concatInputs += "[v$inputIndex][${inputIndex + 1}:a]"
                        inputIndex += 2
                    }
                    AssetType.Audio, null -> Unit
                }
            }
        }

        if (concatInputs.isEmpty()) {
            return listOf(
                "-y",
                "-f", "lavfi",
                "-i", quoted("color=c=black:s=${config.targetWidth}x${config.targetHeight}:d=${(request.timeline.durationMs.coerceAtLeast(1000L)).toSecondsString()}"),
                "-f", "lavfi",
                "-i", quoted("anullsrc=r=48000:cl=stereo"),
                "-shortest",
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                quoted(outputPath)
            ).joinToString(" ")
        }

        filterParts += concatInputs.joinToString(separator = "") + "concat=n=${concatInputs.size}:v=1:a=1[outv][outa]"
        tokens += listOf(
            "-filter_complex", quoted(filterParts.joinToString(";")),
            "-map", quoted("[outv]"),
            "-map", quoted("[outa]"),
            "-c:v", "libx264",
            "-preset", "fast",
            "-crf", "23",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", config.audioBitrate.toString(),
            "-movflags", "+faststart",
            quoted(outputPath)
        )
        return tokens.joinToString(" ")
    }

    internal fun buildSequence(request: RenderRequest): List<RenderSequenceEntry> {
        val enabledTracks = request.timeline.tracks.filter { it.isEnabled }
        val assetsById = request.project.assets.associateBy { it.id }
        val clips = enabledTracks
            .flatMap { it.clips }
            .sortedBy { it.timelineStartMs }
            .mapNotNull { clip ->
                val asset = assetsById[clip.assetId] ?: return@mapNotNull null
                if (clip.visibleDurationMs <= 0L) return@mapNotNull null
                RenderSequenceEntry(
                    kind = SegmentKind.Clip,
                    clip = clip,
                    asset = asset,
                    durationMs = clip.visibleDurationMs,
                    sourceStartMs = clip.sourceStartMs,
                    sourceEndMs = clip.sourceEndMs
                )
            }

        if (clips.isEmpty()) return emptyList()

        val sequence = mutableListOf<RenderSequenceEntry>()
        var cursorMs = 0L
        clips.forEach { entry ->
            val clip = entry.clip ?: return@forEach
            if (clip.timelineStartMs > cursorMs) {
                sequence += RenderSequenceEntry(
                    kind = SegmentKind.Gap,
                    clip = null,
                    asset = null,
                    durationMs = clip.timelineStartMs - cursorMs
                )
            }
            sequence += entry
            cursorMs = maxOf(cursorMs, clip.timelineEndMs)
        }
        if (request.timeline.durationMs > cursorMs) {
            sequence += RenderSequenceEntry(
                kind = SegmentKind.Gap,
                clip = null,
                asset = null,
                durationMs = request.timeline.durationMs - cursorMs
            )
        }
        return sequence
    }

    internal data class RenderSequenceEntry(
        val kind: SegmentKind,
        val clip: TimelineClip?,
        val asset: Asset?,
        val durationMs: Long,
        val sourceStartMs: Long = 0L,
        val sourceEndMs: Long = durationMs
    )

    internal enum class SegmentKind {
        Gap,
        Clip
    }

    private fun quoted(value: String): String = "\"$value\""

    private fun Long.toSecondsString(): String = String.format(Locale.US, "%.3f", this / 1000.0)
}
