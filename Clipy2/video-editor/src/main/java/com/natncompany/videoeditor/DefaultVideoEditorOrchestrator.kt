package com.natncompany.videoeditor

class DefaultVideoEditorOrchestrator(
    private val timelineEngine: TimelineEngine = DefaultTimelineEngine(),
    private val effectPipeline: VideoEffectPipeline = DefaultVideoEffectPipeline(),
    private val previewPlanner: PreviewEnginePlanner = DefaultPreviewEnginePlanner(DefaultVideoEffectPipeline()),
    private val exportPlanner: ExportPlanner = DefaultExportPlanner(DefaultVideoEffectPipeline())
) {
    fun createSession(session: VideoEditorSession): VideoEditorSession {
        return timelineEngine.createSession(session)
    }

    fun createPreviewPlan(session: VideoEditorSession): PreviewPlan {
        return previewPlanner.createPlan(createSession(session))
    }

    fun createExportPlan(session: VideoEditorSession, request: ExportRequest): ExportPlan {
        return exportPlanner.createPlan(createSession(session), request)
    }

    fun export(session: VideoEditorSession, request: ExportRequest): ExportResult {
        return exportPlanner.execute(createSession(session), request)
    }

    fun usesOpenGlEffects(session: VideoEditorSession): Boolean {
        return effectPipeline.shouldUseOpenGl(createSession(session))
    }
}

private class DefaultTimelineEngine : TimelineEngine {
    override fun createSession(session: VideoEditorSession): VideoEditorSession {
        return session.copy(
            timeline = session.timeline.copy(
                clips = session.timeline.clips.filter { it.outputDurationMs > 0L }
            )
        )
    }
}

private class DefaultVideoEffectPipeline : VideoEffectPipeline {
    override fun shouldUseOpenGl(session: VideoEditorSession): Boolean {
        return session.timeline.clips.any { clip ->
            clip.visualEffect.usesOpenGl ||
                clip.transform.brightness != 0f ||
                clip.transform.contrast != 0f ||
                clip.transform.saturation != 0f
        }
    }
}

private class DefaultPreviewEnginePlanner(
    private val effectPipeline: VideoEffectPipeline
) : PreviewEnginePlanner {
    override fun createPlan(session: VideoEditorSession): PreviewPlan {
        val stages = buildList {
            add(PipelineStage.Timeline)
            add(PipelineStage.MediaCodecPreview)
            if (effectPipeline.shouldUseOpenGl(session)) {
                add(PipelineStage.OpenGlEffect)
            }
        }
        return PreviewPlan(
            engine = PreviewEngine.MediaCodecRealtime,
            stages = stages
        )
    }
}

private class DefaultExportPlanner(
    private val effectPipeline: VideoEffectPipeline
) : ExportPlanner {
    override fun createPlan(session: VideoEditorSession, request: ExportRequest): ExportPlan {
        val stages = buildList {
            add(PipelineStage.Timeline)
            if (effectPipeline.shouldUseOpenGl(session)) {
                add(PipelineStage.OpenGlEffect)
            }
            if (request.preferMediaCodec) {
                add(PipelineStage.MediaCodecExport)
                if (request.allowFfmpegFallback) {
                    add(PipelineStage.FfmpegFallback)
                }
            } else {
                add(PipelineStage.FfmpegFallback)
            }
        }
        val primary = if (request.preferMediaCodec) {
            ExportEngineType.MediaCodec
        } else {
            ExportEngineType.Ffmpeg
        }
        val fallback = if (request.preferMediaCodec && request.allowFfmpegFallback) {
            ExportEngineType.Ffmpeg
        } else {
            null
        }
        return ExportPlan(
            primaryEngine = primary,
            fallbackEngine = fallback,
            stages = stages
        )
    }

    override fun execute(session: VideoEditorSession, request: ExportRequest): ExportResult {
        val plan = createPlan(session, request)
        return ExportResult(
            engine = plan.primaryEngine,
            usedFallback = false,
            outputFileName = request.outputFileName,
            notes = when (plan.primaryEngine) {
                ExportEngineType.MediaCodec -> "MediaCodec export queued. FFmpeg fallback is available if the hardware path fails."
                ExportEngineType.Ffmpeg -> "FFmpeg export queued as the primary software pipeline."
            }
        )
    }
}
