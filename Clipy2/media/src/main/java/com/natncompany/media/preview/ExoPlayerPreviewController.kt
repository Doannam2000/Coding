package com.natncompany.media.preview

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.natncompany.media.AssetType
import com.natncompany.media.MediaError
import com.natncompany.media.MediaResult
import com.natncompany.media.PreviewController
import com.natncompany.media.PreviewState
import com.natncompany.media.VideoProject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ExoPlayerPreviewController(
    context: Context,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : PreviewController {
    private val appContext = context.applicationContext
    private val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + mainDispatcher)
    private val mutableState = MutableStateFlow(PreviewState())
    private var player: ExoPlayer? = null
    private var progressJob: Job? = null
    private var currentProject: VideoProject? = null
    private var timelineMapper: TimelinePositionMapper? = null

    override val state: StateFlow<PreviewState> = mutableState.asStateFlow()

    override suspend fun openProject(project: VideoProject): MediaResult<Unit> = withContext(mainDispatcher) {
        currentProject = project
        timelineMapper = TimelinePositionMapper(project)
        mutableState.value = PreviewState(
            isPrepared = false,
            durationMs = project.timeline.durationMs,
            currentClipId = timelineMapper?.activeClipIdAt(0L)
        )
        MediaResult.Success(Unit)
    }

    override suspend fun prepare(): MediaResult<Unit> = withContext(mainDispatcher) {
        val project = currentProject
            ?: return@withContext MediaResult.Failure(MediaError.InvalidInput("No project is open"))
        prepare(project)
    }

    override suspend fun prepare(project: VideoProject): MediaResult<Unit> = withContext(mainDispatcher) {
        val mapper = TimelinePositionMapper(project)
        val mediaItems = project.timeline.tracks
            .filter { it.isEnabled }
            .flatMap { it.clips }
            .sortedBy { it.timelineStartMs }
            .mapNotNull { clip ->
                val asset = project.assets.firstOrNull { it.id == clip.assetId } ?: return@mapNotNull null
                if (asset.type == AssetType.Image) {
                    null
                } else {
                    MediaItem.fromUri(Uri.fromFile(File(asset.cachedPath)))
                }
            }
        if (mediaItems.isEmpty()) {
            mutableState.value = PreviewState(
                isPrepared = true,
                durationMs = project.timeline.durationMs,
                currentClipId = mapper.activeClipIdAt(0L)
            )
            currentProject = project
            timelineMapper = mapper
            return@withContext MediaResult.Success(Unit)
        }
        runCatching {
            val previewPlayer = ensurePlayer()
            previewPlayer.setMediaItems(mediaItems)
            previewPlayer.prepare()
            currentProject = project
            timelineMapper = mapper
            mutableState.value = PreviewState(
                isPlaying = previewPlayer.isPlaying,
                isPrepared = true,
                positionMs = mapper.windowToTimeline(
                    previewPlayer.currentMediaItemIndex.coerceAtLeast(0),
                    previewPlayer.currentPosition.coerceAtLeast(0L)
                ),
                durationMs = project.timeline.durationMs,
                currentClipId = mapper.activeClipIdAt(
                    mapper.windowToTimeline(
                        previewPlayer.currentMediaItemIndex.coerceAtLeast(0),
                        previewPlayer.currentPosition.coerceAtLeast(0L)
                    )
                )
            )
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to prepare preview") }
    }

    override suspend fun setSurface(surface: Surface?): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            ensurePlayer().setVideoSurface(surface)
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to set preview surface") }
    }

    override suspend fun play(): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            val previewPlayer = ensurePlayer()
            previewPlayer.play()
            startProgressUpdates()
            updateFromPlayer(previewPlayer)
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to start preview") }
    }

    override suspend fun pause(): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            player?.pause()
            progressJob?.cancel()
            player?.let { updateFromPlayer(it) } ?: run {
                mutableState.value = mutableState.value.copy(isPlaying = false)
            }
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to pause preview") }
    }

    override suspend fun stop(): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            player?.pause()
            player?.seekTo(0L)
            progressJob?.cancel()
            mutableState.value = mutableState.value.copy(
                isPlaying = false,
                positionMs = 0L,
                currentClipId = timelineMapper?.activeClipIdAt(0L)
            )
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to stop preview") }
    }

    override suspend fun seek(positionMs: Long): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            val bounded = positionMs.coerceIn(0L, currentProject?.timeline?.durationMs ?: Long.MAX_VALUE)
            val mapper = timelineMapper
            if (mapper != null) {
                val window = mapper.timelineToWindow(bounded)
                if (window != null) {
                    player?.seekTo(window.first, window.second)
                } else {
                    player?.seekTo(0L)
                }
            } else {
                player?.seekTo(bounded)
            }
            mutableState.value = mutableState.value.copy(
                positionMs = bounded,
                currentClipId = timelineMapper?.activeClipIdAt(bounded)
            )
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to seek preview") }
    }

    override suspend fun scrub(positionMs: Long): MediaResult<Unit> {
        return seek(positionMs)
    }

    override suspend fun updateTimeline(project: VideoProject): MediaResult<Unit> {
        return prepare(project)
    }

    override suspend fun release(): MediaResult<Unit> = withContext(mainDispatcher) {
        runCatching {
            progressJob?.cancel()
            player?.release()
            player = null
            scope.cancel()
            mutableState.value = PreviewState(isPrepared = false)
            MediaResult.Success(Unit)
        }.getOrElse { failure(it.message ?: "Unable to release preview") }
    }

    private fun ensurePlayer(): ExoPlayer {
        player?.let { return it }
        return ExoPlayer.Builder(appContext).build().also { created ->
            created.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updateFromPlayer(created)
                        if (isPlaying) startProgressUpdates() else progressJob?.cancel()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        updateFromPlayer(created)
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        mutableState.value = mutableState.value.copy(error = error.message)
                    }
                }
            )
            player = created
        }
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                player?.let { updateFromPlayer(it) }
                delay(250)
            }
        }
    }

    private fun updateFromPlayer(previewPlayer: ExoPlayer) {
        val projectDuration = currentProject?.timeline?.durationMs ?: 0L
        val timelinePosition = timelineMapper?.windowToTimeline(
            previewPlayer.currentMediaItemIndex.coerceAtLeast(0),
            previewPlayer.currentPosition.coerceAtLeast(0L)
        ) ?: previewPlayer.currentPosition.coerceAtLeast(0L)
        mutableState.value = mutableState.value.copy(
            isPlaying = previewPlayer.isPlaying,
            isPrepared = true,
            positionMs = timelinePosition,
            durationMs = projectDuration.takeIf { it > 0L } ?: previewPlayer.duration.coerceAtLeast(0L),
            currentClipId = timelineMapper?.activeClipIdAt(timelinePosition),
            error = null
        )
    }

    private fun failure(message: String): MediaResult.Failure {
        mutableState.value = mutableState.value.copy(error = message)
        return MediaResult.Failure(MediaError.BackendUnavailable(message))
    }
}
