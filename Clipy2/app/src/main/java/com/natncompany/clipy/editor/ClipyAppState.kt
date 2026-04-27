package com.natncompany.clipy.editor

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.absoluteValue

@Stable
class ClipyAppState {
    val clips = mutableStateListOf<ClipDraft>()

    var currentScreen by mutableStateOf(EditorScreen.Home)
        private set
    var projectName by mutableStateOf("Clipy Studio")
        private set
    var aspectPreset by mutableStateOf(AspectPreset.NineSixteen)
        private set
    var activeTool by mutableStateOf(EditorTool.Trim)
        private set
    var selectedClipId by mutableStateOf<String?>(null)
        private set
    var sourceVolume by mutableFloatStateOf(1f)
        private set
    var musicVolume by mutableFloatStateOf(0.35f)
        private set
    var voiceOverVolume by mutableFloatStateOf(0.75f)
        private set
    var statusMessage by mutableStateOf("Ready")
        private set

    val selectedClip: ClipDraft?
        get() = clips.firstOrNull { it.id == selectedClipId }

    val projectDurationMs: Long
        get() = clips.sumOf { it.visibleDurationMs() }

    val projectDurationLabel: String
        get() = formatDuration(projectDurationMs)

    fun goHome() {
        currentScreen = EditorScreen.Home
    }

    fun openEditor(
        tool: EditorTool = activeTool,
        aspectPreset: AspectPreset = this.aspectPreset
    ) {
        activeTool = tool
        this.aspectPreset = aspectPreset
        currentScreen = EditorScreen.Editor
    }

    fun openExport() {
        if (clips.isEmpty()) {
            statusMessage = "No media selected"
            return
        }
        currentScreen = EditorScreen.Export
        statusMessage = "Ready to export"
    }

    fun returnToEditor() {
        currentScreen = EditorScreen.Editor
    }

    fun importMedia(
        context: Context,
        uris: List<Uri>,
        replaceTimeline: Boolean,
        initialTool: EditorTool,
        aspectPreset: AspectPreset
    ) {
        if (uris.isEmpty()) {
            statusMessage = "No media selected"
            return
        }

        if (replaceTimeline) {
            clips.clear()
            selectedClipId = null
        }

        val imported = uris.mapIndexed { index, uri ->
            resolveClip(
                context = context,
                uri = uri,
                sequence = clips.size + index + 1
            )
        }
        clips.addAll(imported)
        selectedClipId = imported.lastOrNull()?.id ?: selectedClipId
        this.aspectPreset = aspectPreset
        activeTool = initialTool
        currentScreen = EditorScreen.Editor
        statusMessage = "${imported.size} clip loaded"
    }

    fun selectClip(id: String) {
        if (clips.none { it.id == id }) {
            return
        }
        selectedClipId = id
    }

    fun selectTool(tool: EditorTool) {
        activeTool = tool
    }

    fun updateAspectPreset(preset: AspectPreset) {
        aspectPreset = preset
        statusMessage = "Canvas ${preset.label}"
    }

    fun updateSelectedTrim(startMs: Long, endMs: Long) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    trimStartMs = startMs.coerceAtLeast(0L),
                    trimEndMs = endMs.coerceAtMost(clip.sourceDurationMs).coerceAtLeast(startMs + 250L)
                )
            )
        }
    }

    fun updateSelectedSpeed(speed: Float) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    speed = speed.coerceIn(0.25f, 3f)
                )
            )
        }
    }

    fun updateSelectedVolume(volume: Float) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    volume = volume.coerceIn(0f, 1f)
                )
            )
        }
    }

    fun updateSourceVolume(volume: Float) {
        sourceVolume = volume.coerceIn(0f, 1f)
    }

    fun updateMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
    }

    fun updateVoiceOverVolume(volume: Float) {
        voiceOverVolume = volume.coerceIn(0f, 1f)
    }

    fun updateSelectedFilter(name: String) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    filterName = name
                )
            )
        }
    }

    fun updateSelectedBackground(hex: String) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    backgroundHex = hex
                )
            )
        }
    }

    fun updateSelectedBrightness(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    brightness = value.coerceIn(-1f, 1f)
                )
            )
        }
    }

    fun updateSelectedContrast(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    contrast = value.coerceIn(-1f, 1f)
                )
            )
        }
    }

    fun updateSelectedSaturation(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(
                adjustments = clip.adjustments.copy(
                    saturation = value.coerceIn(-1f, 1f)
                )
            )
        }
    }

    fun copySelectedClip() {
        val selected = selectedClip ?: return
        val selectedIndex = clips.indexOfFirst { it.id == selected.id }
        if (selectedIndex == -1) {
            return
        }
        val duplicate = selected.copy(
            id = newId(),
            displayName = "${selected.displayName} Copy"
        )
        clips.add(selectedIndex + 1, duplicate)
        selectedClipId = duplicate.id
        activeTool = EditorTool.Copy
        statusMessage = "Clip duplicated"
    }

    fun splitSelectedClip() {
        val selected = selectedClip ?: return
        val selectedIndex = clips.indexOfFirst { it.id == selected.id }
        if (selectedIndex == -1) {
            return
        }
        val currentStart = selected.adjustments.trimStartMs
        val currentEnd = selected.trimEndMs()
        val window = currentEnd - currentStart
        if (window < 800L) {
            statusMessage = "Clip is too short to split"
            return
        }
        val splitPoint = currentStart + window / 2L
        val left = selected.copy(
            id = newId(),
            displayName = "${selected.displayName} A",
            adjustments = selected.adjustments.copy(trimEndMs = splitPoint)
        )
        val right = selected.copy(
            id = newId(),
            displayName = "${selected.displayName} B",
            adjustments = selected.adjustments.copy(trimStartMs = splitPoint)
        )
        clips.removeAt(selectedIndex)
        clips.add(selectedIndex, left)
        clips.add(selectedIndex + 1, right)
        selectedClipId = left.id
        activeTool = EditorTool.Split
        statusMessage = "Clip split in two"
    }

    private fun updateSelectedClip(transform: (ClipDraft) -> ClipDraft) {
        val selected = selectedClip ?: return
        val index = clips.indexOfFirst { it.id == selected.id }
        if (index == -1) {
            return
        }
        clips[index] = transform(selected)
    }

    private fun resolveClip(
        context: Context,
        uri: Uri,
        sequence: Int
    ): ClipDraft {
        val mimeType = context.contentResolver.getType(uri).orEmpty()
        val displayName = queryDisplayName(context, uri) ?: "Clip $sequence"
        if (mimeType.startsWith("image/")) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            return ClipDraft(
                id = newId(),
                displayName = displayName,
                uriString = uri.toString(),
                mediaKind = MediaKind.Image,
                sourceDurationMs = 3000L,
                width = bounds.outWidth.takeIf { it > 0 },
                height = bounds.outHeight.takeIf { it > 0 },
                adjustments = ClipAdjustments(trimEndMs = 3000L)
            )
        }

        val retriever = MediaMetadataRetriever()
        var durationMs = 3000L
        var width: Int? = null
        var height: Int? = null
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1000L)
                ?: 3000L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        } catch (_: Exception) {
            durationMs = 3000L
        } finally {
            runCatching { retriever.release() }
        }
        return ClipDraft(
            id = newId(),
            displayName = displayName,
            uriString = uri.toString(),
            mediaKind = MediaKind.Video,
            sourceDurationMs = durationMs,
            width = width,
            height = height,
            adjustments = ClipAdjustments(trimEndMs = durationMs)
        )
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) {
                cursor.getString(column)
            } else {
                null
            }
        }
    }

    private fun newId(): String = "clip_${System.nanoTime().absoluteValue}"
}

@Composable
fun rememberClipyAppState(): ClipyAppState = remember { ClipyAppState() }

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
