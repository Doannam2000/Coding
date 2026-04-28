package com.natncompany.clipy.editor

import com.natncompany.videoeditor.AudioMix
import com.natncompany.videoeditor.ClipTransform
import com.natncompany.videoeditor.DefaultVideoEditorOrchestrator
import com.natncompany.videoeditor.ExportPlan
import com.natncompany.videoeditor.ExportRequest
import com.natncompany.videoeditor.MediaType
import com.natncompany.videoeditor.PreviewPlan
import com.natncompany.videoeditor.TimelineClip
import com.natncompany.videoeditor.VideoCanvas
import com.natncompany.videoeditor.VideoEditorSession
import com.natncompany.videoeditor.VideoTimeline
import com.natncompany.videoeditor.VisualEffect

private val videoEditorOrchestrator = DefaultVideoEditorOrchestrator()

fun ClipyAppState.buildVideoEditorSession(): VideoEditorSession {
    return VideoEditorSession(
        projectName = projectName,
        canvas = VideoCanvas(
            label = aspectPreset.label,
            previewAspectRatio = aspectPreset.previewAspectRatio
        ),
        timeline = VideoTimeline(
            clips = clips.map { clip ->
                TimelineClip(
                    id = clip.id,
                    sourceUri = clip.uriString,
                    mediaType = if (clip.mediaKind == MediaKind.Video) MediaType.Video else MediaType.Image,
                    displayName = clip.displayName,
                    sourceDurationMs = clip.sourceDurationMs,
                    trimStartMs = clip.adjustments.trimStartMs,
                    trimEndMs = clip.trimEndMs(),
                    outputDurationMs = clip.visibleDurationMs(),
                    transform = ClipTransform(
                        speed = clip.adjustments.speed,
                        brightness = clip.adjustments.brightness,
                        contrast = clip.adjustments.contrast,
                        saturation = clip.adjustments.saturation
                    ),
                    visualEffect = VisualEffect(
                        filterName = clip.adjustments.filterName,
                        usesOpenGl = clip.adjustments.filterName != "Original"
                    ),
                    backgroundHex = clip.adjustments.backgroundHex,
                    volume = clip.adjustments.volume
                )
            }
        ),
        audioMix = AudioMix(
            sourceVolume = sourceVolume,
            musicVolume = musicVolume,
            voiceOverVolume = voiceOverVolume
        )
    )
}

fun ClipyAppState.buildPreviewPlan(): PreviewPlan {
    return videoEditorOrchestrator.createPreviewPlan(buildVideoEditorSession())
}

fun ClipyAppState.buildExportPlan(): ExportPlan {
    return videoEditorOrchestrator.createExportPlan(
        session = buildVideoEditorSession(),
        request = ExportRequest(outputFileName = projectName.replace(' ', '_') + ".mp4")
    )
}
