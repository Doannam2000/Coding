package com.natncompany.media.session

import android.view.Surface
import com.natncompany.media.ActiveMediaJob
import com.natncompany.media.Asset
import com.natncompany.media.AudioProcessor
import com.natncompany.media.MediaDiagnostics
import com.natncompany.media.MediaError
import com.natncompany.media.MediaExportConfig
import com.natncompany.media.MediaImporter
import com.natncompany.media.MediaImportInput
import com.natncompany.media.MediaJobType
import com.natncompany.media.MediaResult
import com.natncompany.media.MediaSessionEvent
import com.natncompany.media.MediaSessionManager
import com.natncompany.media.MediaSessionState
import com.natncompany.media.MetadataReader
import com.natncompany.media.PreviewController
import com.natncompany.media.ProjectCacheManager
import com.natncompany.media.RenderRequest
import com.natncompany.media.Renderer
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineEditor
import com.natncompany.media.TranscodeRequest
import com.natncompany.media.Transcoder
import com.natncompany.media.VideoProject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.UUID

class DefaultMediaSessionManager(
    private val importer: MediaImporter,
    private val metadataReader: MetadataReader,
    private val timelineEditor: TimelineEditor,
    private val previewController: PreviewController,
    private val transcoder: Transcoder,
    private val renderer: Renderer,
    private val audioProcessor: AudioProcessor,
    private val cacheManager: ProjectCacheManager,
    private val diagnostics: MediaDiagnostics,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaSessionManager {
    private val mutableState = MutableStateFlow(MediaSessionState())
    private val mutableEvents = MutableSharedFlow<MediaSessionEvent>(extraBufferCapacity = 64)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var previewSyncJob: Job? = null

    override val state = mutableState.asStateFlow()
    override val events: Flow<MediaSessionEvent> = mutableEvents.asSharedFlow()

    override suspend fun openProject(project: VideoProject): MediaResult<VideoProject> = withContext(ioDispatcher) {
        if (project.id.isBlank()) {
            return@withContext failure("Project id is required")
        }
        val layout = when (val result = cacheManager.createProjectCache(project.id)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }
        val opened = project.copy(
            rootCachePath = project.rootCachePath.ifBlank { layout.projectRoot },
            timeline = when (val validation = timelineEditor.validate(project.timeline)) {
                is MediaResult.Success -> validation.value
                is MediaResult.Failure -> return@withContext validation
            }
        )
        previewController.openProject(opened)
        mutableState.value = MediaSessionState(
            currentProject = opened,
            currentTimeline = opened.timeline,
            previewState = previewController.state.value
        )
        mutableEvents.emit(MediaSessionEvent.ProjectOpened(opened))
        MediaResult.Success(opened)
    }

    override suspend fun closeProject(): MediaResult<Unit> = withContext(ioDispatcher) {
        val projectId = mutableState.value.currentProject?.id
        previewController.pause()
        mutableState.value = MediaSessionState()
        mutableEvents.emit(MediaSessionEvent.ProjectClosed(projectId))
        MediaResult.Success(Unit)
    }

    override suspend fun importMedia(input: MediaImportInput): MediaResult<Asset> = withContext(ioDispatcher) {
        val project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        val job = ActiveMediaJob(id = "import_${UUID.randomUUID()}", type = MediaJobType.Import)
        addJob(job)
        mutableEvents.emit(MediaSessionEvent.ImportStarted(job, input))

        val result = importer.import(
            com.natncompany.media.ImportRequest(
                projectId = project.id,
                uri = input.uri,
                filePath = input.filePath
            )
        )

        when (result) {
            is MediaResult.Success -> {
                val asset = result.value.asset
                if (asset == null) {
                    val error = MediaError.InvalidInput("Importer returned no asset")
                    removeJob(job)
                    mutableEvents.emit(MediaSessionEvent.ImportFailed(job, error))
                    return@withContext MediaResult.Failure(error)
                }
                val updatedProject = project.copy(assets = project.assets.filterNot { it.id == asset.id } + asset)
                updateProject(updatedProject)
                removeJob(job)
                mutableEvents.emit(MediaSessionEvent.ImportCompleted(job, asset))
                MediaResult.Success(asset)
            }
            is MediaResult.Failure -> {
                removeJob(job)
                mutableEvents.emit(MediaSessionEvent.ImportFailed(job, result.error))
                result
            }
        }
    }

    override suspend fun importBatch(inputs: List<MediaImportInput>): MediaResult<List<Asset>> = withContext(ioDispatcher) {
        var project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        val imported = mutableListOf<Asset>()
        val job = ActiveMediaJob(id = "import_batch_${UUID.randomUUID()}", type = MediaJobType.Import)
        addJob(job)

        importer.importBatch(inputs, project.id).collect { progress ->
            updateJob(job.copy(progressPercent = progress.progressPercent))
            progress.latestAsset?.let { asset ->
                imported += asset
                project = project.copy(assets = project.assets.filterNot { it.id == asset.id } + asset)
                updateProject(project)
                mutableEvents.emit(MediaSessionEvent.ImportCompleted(job, asset))
            }
            progress.latestError?.let { error ->
                mutableEvents.emit(MediaSessionEvent.ImportFailed(job, error))
            }
        }
        removeJob(job)
        MediaResult.Success(imported)
    }

    override suspend fun preparePreview(surface: Surface?): MediaResult<Unit> = withContext(ioDispatcher) {
        val project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        when (val prepare = previewController.prepare(project)) {
            is MediaResult.Failure -> {
                mutableEvents.emit(MediaSessionEvent.PreviewError(prepare.error))
                updateLastError(prepare.error)
                prepare
            }
            is MediaResult.Success -> {
                when (val surfaceResult = previewController.setSurface(surface)) {
                    is MediaResult.Failure -> {
                        mutableEvents.emit(MediaSessionEvent.PreviewError(surfaceResult.error))
                        updateLastError(surfaceResult.error)
                        surfaceResult
                    }
                    is MediaResult.Success -> {
                        syncPreviewState()
                        mutableEvents.emit(MediaSessionEvent.PreviewReady)
                        MediaResult.Success(Unit)
                    }
                }
            }
        }
    }

    override suspend fun play(): MediaResult<Unit> = delegatePreview { previewController.play() }.also { result ->
        if (result is MediaResult.Success) startPreviewSync()
    }

    override suspend fun pause(): MediaResult<Unit> = delegatePreview { previewController.pause() }.also { stopPreviewSync() }

    override suspend fun stop(): MediaResult<Unit> = delegatePreview { previewController.stop() }.also { stopPreviewSync() }

    override suspend fun seekTo(positionMs: Long): MediaResult<Unit> = delegatePreview { previewController.seek(positionMs) }

    override suspend fun scrubTo(positionMs: Long): MediaResult<Unit> = delegatePreview { previewController.scrub(positionMs) }

    override suspend fun updateTimeline(timeline: Timeline): MediaResult<Timeline> = withContext(ioDispatcher) {
        val project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        val validated = when (val result = timelineEditor.validate(timeline)) {
            is MediaResult.Success -> result.value
            is MediaResult.Failure -> return@withContext result
        }
        val updatedProject = project.copy(timeline = validated)
        updateProject(updatedProject)
        previewController.updateTimeline(updatedProject)
        syncPreviewState()
        MediaResult.Success(validated)
    }

    override suspend fun transcodeAsset(assetId: String): MediaResult<Unit> = withContext(ioDispatcher) {
        val project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        val asset = project.assets.firstOrNull { it.id == assetId }
            ?: return@withContext MediaResult.Failure(MediaError.InvalidInput("Asset $assetId not found"))
        val job = ActiveMediaJob(id = "transcode_${UUID.randomUUID()}", type = MediaJobType.Transcode, assetId = asset.id)
        addJob(job)
        mutableEvents.emit(MediaSessionEvent.TranscodeStarted(job, asset))

        var finalError: MediaError? = null
        var outputAsset: Asset? = null
        transcoder.transcode(TranscodeRequest(asset = asset, projectId = project.id, jobId = job.id)).collect { update ->
            updateJob(job.copy(progressPercent = update.progressPercent))
            update.asset?.let { outputAsset = it }
            update.error?.let { finalError = it }
        }

        removeJob(job)
        return@withContext if (finalError != null) {
            mutableEvents.emit(MediaSessionEvent.TranscodeFailed(job, finalError!!))
            updateLastError(finalError!!)
            MediaResult.Failure(finalError!!)
        } else {
            val normalized = outputAsset ?: asset
            val updatedProject = project.copy(assets = project.assets.map { if (it.id == asset.id) normalized else it })
            updateProject(updatedProject)
            previewController.updateTimeline(updatedProject)
            syncPreviewState()
            mutableEvents.emit(MediaSessionEvent.TranscodeCompleted(job, normalized))
            MediaResult.Success(Unit)
        }
    }

    override suspend fun export(config: MediaExportConfig): MediaResult<Unit> = withContext(ioDispatcher) {
        val project = currentProjectOrFailure()?.let { return@withContext it } ?: mutableState.value.currentProject!!
        val timeline = config.timeline ?: project.timeline
        val renderProject = project.copy(timeline = timeline)
        val job = ActiveMediaJob(id = "render_${UUID.randomUUID()}", type = MediaJobType.Render)
        val request = RenderRequest(
            project = renderProject,
            outputFileName = config.outputFileName,
            timeline = timeline,
            jobId = job.id,
            config = config.renderConfig
        )
        addJob(job)
        mutableEvents.emit(MediaSessionEvent.RenderStarted(job, request))

        var outputPath: String? = null
        var finalError: MediaError? = null
        renderer.render(request).collect { update ->
            updateJob(job.copy(progressPercent = update.progressPercent))
            outputPath = update.outputPath ?: outputPath
            update.error?.let { finalError = it }
            if (!update.completed) {
                mutableEvents.tryEmit(MediaSessionEvent.RenderProgress(job, update.progressPercent))
            }
        }

        removeJob(job)
        return@withContext if (finalError != null) {
            mutableEvents.emit(MediaSessionEvent.RenderFailed(job, finalError!!))
            updateLastError(finalError!!)
            MediaResult.Failure(finalError!!)
        } else {
            mutableEvents.emit(MediaSessionEvent.RenderCompleted(job, outputPath.orEmpty()))
            MediaResult.Success(Unit)
        }
    }

    override suspend fun cancelAllJobs(): MediaResult<Unit> = withContext(ioDispatcher) {
        val snapshot = mutableState.value
        val projectId = snapshot.currentProject?.id
        if (projectId != null) {
            transcoder.cancel(projectId)
            renderer.cancel(projectId)
        }
        previewController.pause()
        val jobs = snapshot.activeImportJobs + snapshot.activeTranscodeJobs + snapshot.activeRenderJobs
        mutableState.update {
            it.copy(
                activeImportJobs = emptyList(),
                activeTranscodeJobs = emptyList(),
                activeRenderJobs = emptyList(),
                previewState = previewController.state.value
            )
        }
        jobs.forEach { mutableEvents.emit(MediaSessionEvent.JobCancelled(it)) }
        MediaResult.Success(Unit)
    }

    private suspend fun delegatePreview(block: suspend () -> MediaResult<Unit>): MediaResult<Unit> = withContext(ioDispatcher) {
        when (val result = block()) {
            is MediaResult.Success -> {
                syncPreviewState()
                result
            }
            is MediaResult.Failure -> {
                updateLastError(result.error)
                mutableEvents.emit(MediaSessionEvent.PreviewError(result.error))
                result
            }
        }
    }

    private fun currentProjectOrFailure(): MediaResult.Failure? {
        return if (mutableState.value.currentProject == null) {
            MediaResult.Failure(MediaError.InvalidInput("No project is open"))
        } else {
            null
        }
    }

    private fun updateProject(project: VideoProject) {
        mutableState.update {
            it.copy(
                currentProject = project,
                currentTimeline = project.timeline,
                previewState = previewController.state.value,
                lastError = null
            )
        }
    }

    private fun syncPreviewState() {
        mutableState.update { it.copy(previewState = previewController.state.value) }
    }

    private fun startPreviewSync() {
        if (previewSyncJob?.isActive == true) return
        previewSyncJob = scope.launch {
            while (true) {
                syncPreviewState()
                delay(250)
            }
        }
    }

    private fun stopPreviewSync() {
        previewSyncJob?.cancel()
        previewSyncJob = null
        syncPreviewState()
    }

    private fun updateLastError(error: MediaError) {
        mutableState.update { it.copy(lastError = error) }
    }

    private fun addJob(job: ActiveMediaJob) {
        updateJob(job)
    }

    private fun updateJob(job: ActiveMediaJob) {
        mutableState.update { state ->
            when (job.type) {
                MediaJobType.Import -> state.copy(activeImportJobs = state.activeImportJobs.replaceJob(job))
                MediaJobType.Transcode -> state.copy(activeTranscodeJobs = state.activeTranscodeJobs.replaceJob(job))
                MediaJobType.Render -> state.copy(activeRenderJobs = state.activeRenderJobs.replaceJob(job))
            }
        }
    }

    private fun removeJob(job: ActiveMediaJob) {
        mutableState.update { state ->
            when (job.type) {
                MediaJobType.Import -> state.copy(activeImportJobs = state.activeImportJobs.filterNot { it.id == job.id })
                MediaJobType.Transcode -> state.copy(activeTranscodeJobs = state.activeTranscodeJobs.filterNot { it.id == job.id })
                MediaJobType.Render -> state.copy(activeRenderJobs = state.activeRenderJobs.filterNot { it.id == job.id })
            }
        }
    }

    private fun List<ActiveMediaJob>.replaceJob(job: ActiveMediaJob): List<ActiveMediaJob> {
        return if (any { it.id == job.id }) {
            map { if (it.id == job.id) job else it }
        } else {
            this + job
        }
    }

    private fun failure(message: String): MediaResult.Failure {
        return MediaResult.Failure(MediaError.InvalidInput(message))
    }
}
