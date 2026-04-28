package com.natncompany.videoeditor

interface TimelineEngine {
    fun createSession(session: VideoEditorSession): VideoEditorSession
}

interface PreviewEnginePlanner {
    fun createPlan(session: VideoEditorSession): PreviewPlan
}

interface VideoEffectPipeline {
    fun shouldUseOpenGl(session: VideoEditorSession): Boolean
}

interface ExportPlanner {
    fun createPlan(session: VideoEditorSession, request: ExportRequest): ExportPlan
    fun execute(session: VideoEditorSession, request: ExportRequest): ExportResult
}
