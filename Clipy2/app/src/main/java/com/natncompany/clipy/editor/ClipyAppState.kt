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
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.absoluteValue

@Stable
class ClipyAppState(private val persistence: ClipyProjectPersistence? = null) {

    val clips = mutableStateListOf<ClipDraft>()

    var currentScreen by mutableStateOf(EditorScreen.Home)
        private set
    var projectName by mutableStateOf("Clipy Studio")
        private set
    var aspectPreset by mutableStateOf(AspectPreset.NineSixteen)
        private set
    var exportResolutionPreset by mutableStateOf(ExportResolutionPreset.FullHd)
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

    init {
        restorePersistedState()
    }

    fun goHome() {
        currentScreen = EditorScreen.Home
        persistState()
    }

    fun openEditor(
        tool: EditorTool = activeTool,
        aspectPreset: AspectPreset = this.aspectPreset
    ) {
        activeTool = tool
        this.aspectPreset = aspectPreset
        currentScreen = EditorScreen.Editor
        persistState()
    }

    fun openExport() {
        if (clips.isEmpty()) {
            statusMessage = "No media selected"
            persistState()
            return
        }
        currentScreen = EditorScreen.Export
        statusMessage = "Ready to export"
        persistState()
    }

    fun returnToEditor() {
        currentScreen = EditorScreen.Editor
        persistState()
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
            persistState()
            return
        }

        if (replaceTimeline) {
            clips.clear()
            selectedClipId = null
        }

        val sequenceStart = clips.size + 1
        val imported = uris.mapIndexedNotNull { index, uri ->
            resolveClipOrNull(
                context = context,
                uri = uri,
                sequence = sequenceStart + index
            )
        }

        if (imported.isEmpty()) {
            statusMessage = "Unable to read selected media"
            persistState()
            return
        }

        clips.addAll(imported)
        selectedClipId = imported.lastOrNull()?.id ?: selectedClipId
        this.aspectPreset = aspectPreset
        activeTool = initialTool
        currentScreen = EditorScreen.Editor
        statusMessage = if (imported.size == uris.size) {
            "${imported.size} clip loaded"
        } else {
            "${imported.size}/${uris.size} clip loaded"
        }
        persistState()
    }

    fun selectClip(id: String) {
        if (clips.none { it.id == id }) return
        selectedClipId = id
        persistState()
    }

    fun selectTool(tool: EditorTool) {
        activeTool = tool
        persistState()
    }

    fun updateStatus(message: String) {
        statusMessage = message
        persistState()
    }

    fun applyEditedTimeline(timeline: com.natncompany.media.Timeline) {
        applyEditedProject(null, timeline)
    }

    fun applyEditedProject(
        project: com.natncompany.media.VideoProject?,
        timeline: com.natncompany.media.Timeline = project?.timeline ?: com.natncompany.media.Timeline()
    ) {
        val originalById = clips.associateBy { it.id }
        val assetsById = project?.assets.orEmpty().associateBy { it.id }

        val ordered = timeline.tracks
            .flatMap { it.clips }
            .sortedBy { it.timelineStartMs }
            .mapNotNull { edited ->
                val asset = assetsById[edited.assetId]
                val original = originalById[edited.id]
                    ?: originalById[edited.assetId]
                    ?: originalById[edited.id.substringBefore("-split-")]
                val base = original ?: asset?.let { imported ->
                    ClipDraft(
                        id = edited.id,
                        displayName = imported.displayName,
                        uriString = imported.sourceUri,
                        mediaKind = when (imported.type) {
                            com.natncompany.media.AssetType.Image -> MediaKind.Image
                            else -> MediaKind.Video
                        },
                        sourceDurationMs = edited.sourceDurationMs.coerceAtLeast(imported.durationMs ?: edited.sourceEndMs),
                        adjustments = ClipAdjustments(trimEndMs = edited.sourceEndMs)
                    )
                } ?: return@mapNotNull null

                base.copy(
                    id = edited.id,
                    displayName = asset?.displayName ?: base.displayName,
                    uriString = asset?.sourceUri ?: base.uriString,
                    mediaKind = when (asset?.type) {
                        com.natncompany.media.AssetType.Image -> MediaKind.Image
                        com.natncompany.media.AssetType.Video -> MediaKind.Video
                        else -> base.mediaKind
                    },
                    sourceDurationMs = edited.sourceDurationMs.coerceAtLeast(base.sourceDurationMs),
                    adjustments = base.adjustments.copy(
                        trimStartMs = edited.sourceStartMs,
                        trimEndMs = edited.sourceEndMs,
                        volume = edited.audio.volume,
                        brightness = edited.transform.brightness,
                        contrast = edited.transform.contrast - 1f,
                        saturation = edited.transform.saturation - 1f,
                        filterName = edited.effect.parameters["filterName"]
                            ?: if (edited.transform.blur > 0f) "Box Blur" else base.adjustments.filterName
                    )
                )
            }

        if (ordered.isNotEmpty() || timeline.tracks.all { it.clips.isEmpty() }) {
            clips.clear()
            clips.addAll(ordered)
        }

        selectedClipId = timeline.selectedClipIds.firstOrNull()?.takeIf { selected -> clips.any { it.id == selected } }
            ?: selectedClipId?.takeIf { selected -> clips.any { it.id == selected } }
            ?: clips.lastOrNull()?.id

        persistState()
    }

    fun updateAspectPreset(preset: AspectPreset) {
        aspectPreset = preset
        statusMessage = "Canvas ${preset.label}"
        persistState()
    }

    fun updateExportResolution(preset: ExportResolutionPreset) {
        exportResolutionPreset = preset
        statusMessage = "Export ${preset.label}"
        persistState()
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
            clip.copy(adjustments = clip.adjustments.copy(speed = speed.coerceIn(0.25f, 3f)))
        }
    }

    fun updateSelectedVolume(volume: Float) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(volume = volume.coerceIn(0f, 1f)))
        }
    }

    fun updateSourceVolume(volume: Float) {
        sourceVolume = volume.coerceIn(0f, 1f)
        persistState()
    }

    fun updateMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        persistState()
    }

    fun updateVoiceOverVolume(volume: Float) {
        voiceOverVolume = volume.coerceIn(0f, 1f)
        persistState()
    }

    fun updateSelectedFilter(name: String) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(filterName = name))
        }
    }

    fun updateSelectedBackground(hex: String) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(backgroundHex = hex))
        }
    }

    fun updateSelectedBrightness(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(brightness = value.coerceIn(-1f, 1f)))
        }
    }

    fun updateSelectedContrast(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(contrast = value.coerceIn(-1f, 1f)))
        }
    }

    fun updateSelectedSaturation(value: Float) {
        updateSelectedClip { clip ->
            clip.copy(adjustments = clip.adjustments.copy(saturation = value.coerceIn(-1f, 1f)))
        }
    }

    fun copySelectedClip() {
        val selected = selectedClip ?: return
        val selectedIndex = clips.indexOfFirst { it.id == selected.id }
        if (selectedIndex == -1) return

        val duplicate = selected.copy(
            id = newId(),
            displayName = "${selected.displayName} Copy"
        )
        clips.add(selectedIndex + 1, duplicate)
        selectedClipId = duplicate.id
        activeTool = EditorTool.Copy
        statusMessage = "Clip duplicated"
        persistState()
    }

    fun splitSelectedClip() {
        val selected = selectedClip ?: return
        val selectedIndex = clips.indexOfFirst { it.id == selected.id }
        if (selectedIndex == -1) return

        val currentStart = selected.adjustments.trimStartMs
        val currentEnd = selected.trimEndMs()
        val window = currentEnd - currentStart
        if (window < 800L) {
            statusMessage = "Clip is too short to split"
            persistState()
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
        persistState()
    }

    fun clearPersistedProject() {
        persistence?.clear()
    }

    private fun updateSelectedClip(transform: (ClipDraft) -> ClipDraft) {
        val selected = selectedClip ?: return
        val index = clips.indexOfFirst { it.id == selected.id }
        if (index == -1) return

        clips[index] = transform(selected)
        persistState()
    }

    private fun resolveClipOrNull(
        context: Context,
        uri: Uri,
        sequence: Int
    ): ClipDraft? {
        return runCatching {
            resolveClip(context, uri, sequence)
        }.getOrNull()
    }

    private fun resolveClip(
        context: Context,
        uri: Uri,
        sequence: Int
    ): ClipDraft {
        if (uri.toString().isBlank()) {
            throw IllegalArgumentException("Empty media uri")
        }
        val exists = runCatching { context.contentResolver.openInputStream(uri)?.use { true } ?: false }.getOrDefault(false)
        if (!exists) {
            throw IllegalArgumentException("Unreadable media uri")
        }
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
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
        }
    }

    private fun newId(): String = "clip_${System.nanoTime().absoluteValue}"

    private fun persistState() {
        persistence?.save(
            ClipyPersistedProject(
                projectName = projectName,
                aspectPreset = aspectPreset,
                exportResolutionPreset = exportResolutionPreset,
                activeTool = activeTool,
                selectedClipId = selectedClipId,
                sourceVolume = sourceVolume,
                musicVolume = musicVolume,
                voiceOverVolume = voiceOverVolume,
                clips = clips.toList()
            )
        )
    }

    private fun restorePersistedState() {
        val snapshot = persistence?.load() ?: return
        projectName = snapshot.projectName
        aspectPreset = snapshot.aspectPreset
        exportResolutionPreset = snapshot.exportResolutionPreset
        activeTool = snapshot.activeTool
        sourceVolume = snapshot.sourceVolume
        musicVolume = snapshot.musicVolume
        voiceOverVolume = snapshot.voiceOverVolume
        clips.clear()
        clips.addAll(snapshot.clips)
        selectedClipId = snapshot.selectedClipId?.takeIf { target -> clips.any { it.id == target } }
            ?: clips.lastOrNull()?.id
    }
}

interface ClipyProjectPersistence {
    fun load(): ClipyPersistedProject?
    fun save(project: ClipyPersistedProject)
    fun clear()
}

data class ClipyPersistedProject(
    val projectName: String,
    val aspectPreset: AspectPreset,
    val exportResolutionPreset: ExportResolutionPreset,
    val activeTool: EditorTool,
    val selectedClipId: String?,
    val sourceVolume: Float,
    val musicVolume: Float,
    val voiceOverVolume: Float,
    val clips: List<ClipDraft>
)

class ClipyJsonProjectPersistence(private val context: Context) : ClipyProjectPersistence {
    private var lastSerializedState: String? = null

    override fun load(): ClipyPersistedProject? {
        val file = stateFile()
        if (!file.isFile) {
            lastSerializedState = null
            return null
        }
        val raw = runCatching { file.readText() }.getOrNull() ?: return null
        lastSerializedState = raw
        return runCatching {
            val json = JSONObject(raw)
            val clipsJson = json.optJSONArray("clips") ?: JSONArray()
            val clips = buildList {
                for (i in 0 until clipsJson.length()) {
                    val item = clipsJson.optJSONObject(i) ?: continue
                    val sourceDuration = item.optLong("sourceDurationMs", 3000L)
                    add(
                        ClipDraft(
                            id = item.optString("id").ifBlank { "clip_${System.nanoTime().absoluteValue}" },
                            displayName = item.optString("displayName", "Clip"),
                            uriString = item.optString("uriString"),
                            mediaKind = item.optString("mediaKind").toMediaKind(),
                            sourceDurationMs = sourceDuration,
                            width = item.takeIf { it.has("width") }?.optInt("width")?.takeIf { it > 0 },
                            height = item.takeIf { it.has("height") }?.optInt("height")?.takeIf { it > 0 },
                            adjustments = ClipAdjustments(
                                trimStartMs = item.optLong("trimStartMs", 0L),
                                trimEndMs = item.optLong("trimEndMs", sourceDuration),
                                speed = item.optDouble("speed", 1.0).toFloat(),
                                volume = item.optDouble("volume", 1.0).toFloat(),
                                brightness = item.optDouble("brightness", 0.0).toFloat(),
                                contrast = item.optDouble("contrast", 0.0).toFloat(),
                                saturation = item.optDouble("saturation", 0.0).toFloat(),
                                backgroundHex = item.optString("backgroundHex", "#11161D"),
                                filterName = item.optString("filterName", "Original"),
                                transitionName = item.optString("transitionName", "Cut")
                            )
                        )
                    )
                }
            }

            ClipyPersistedProject(
                projectName = json.optString("projectName", "Clipy Studio"),
                aspectPreset = json.optString("aspectPreset", AspectPreset.NineSixteen.name).toAspectPreset(),
                exportResolutionPreset = json.optString("exportResolutionPreset", ExportResolutionPreset.FullHd.name).toExportResolutionPreset(),
                activeTool = json.optString("activeTool", EditorTool.Trim.name).toEditorTool(),
                selectedClipId = json.optString("selectedClipId").takeIf { it.isNotBlank() },
                sourceVolume = json.optDouble("sourceVolume", 1.0).toFloat().coerceIn(0f, 1f),
                musicVolume = json.optDouble("musicVolume", 0.35).toFloat().coerceIn(0f, 1f),
                voiceOverVolume = json.optDouble("voiceOverVolume", 0.75).toFloat().coerceIn(0f, 1f),
                clips = clips
            )
        }.getOrNull()
    }

    override fun save(project: ClipyPersistedProject) {
        runCatching {
            val clipsArray = JSONArray()
            project.clips.forEach { clip ->
                clipsArray.put(
                    JSONObject().apply {
                        put("id", clip.id)
                        put("displayName", clip.displayName)
                        put("uriString", clip.uriString)
                        put("mediaKind", clip.mediaKind.name)
                        put("sourceDurationMs", clip.sourceDurationMs)
                        clip.width?.let { put("width", it) }
                        clip.height?.let { put("height", it) }
                        put("trimStartMs", clip.adjustments.trimStartMs)
                        put("trimEndMs", clip.adjustments.trimEndMs)
                        put("speed", clip.adjustments.speed)
                        put("volume", clip.adjustments.volume)
                        put("brightness", clip.adjustments.brightness)
                        put("contrast", clip.adjustments.contrast)
                        put("saturation", clip.adjustments.saturation)
                        put("backgroundHex", clip.adjustments.backgroundHex)
                        put("filterName", clip.adjustments.filterName)
                        put("transitionName", clip.adjustments.transitionName)
                    }
                )
            }

            val serialized = JSONObject().apply {
                put("projectName", project.projectName)
                put("aspectPreset", project.aspectPreset.name)
                put("exportResolutionPreset", project.exportResolutionPreset.name)
                put("activeTool", project.activeTool.name)
                put("selectedClipId", project.selectedClipId)
                put("sourceVolume", project.sourceVolume)
                put("musicVolume", project.musicVolume)
                put("voiceOverVolume", project.voiceOverVolume)
                put("clips", clipsArray)
            }.toString()

            if (serialized == lastSerializedState) return@runCatching

            val file = stateFile()
            file.parentFile?.mkdirs()
            file.writeText(serialized)
            lastSerializedState = serialized
        }
    }

    override fun clear() {
        stateFile().delete()
        lastSerializedState = null
    }

    private fun stateFile(): File = File(context.filesDir, "clipy/project_state.json")
}

private fun String.toMediaKind(): MediaKind = runCatching { MediaKind.valueOf(this) }.getOrDefault(MediaKind.Video)
private fun String.toAspectPreset(): AspectPreset = runCatching { AspectPreset.valueOf(this) }.getOrDefault(AspectPreset.NineSixteen)
private fun String.toExportResolutionPreset(): ExportResolutionPreset = runCatching { ExportResolutionPreset.valueOf(this) }.getOrDefault(ExportResolutionPreset.FullHd)
private fun String.toEditorTool(): EditorTool = runCatching { EditorTool.valueOf(this) }.getOrDefault(EditorTool.Trim)

@Composable
fun rememberClipyAppState(): ClipyAppState {
    val context = LocalContext.current
    return remember { ClipyAppState(ClipyJsonProjectPersistence(context.applicationContext)) }
}

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
