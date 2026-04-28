package com.natncompany.media

import android.content.Context
import com.natncompany.media.audio.DefaultAudioProcessor
import com.natncompany.media.cache.DefaultProjectCacheManager
import com.natncompany.media.diagnostics.DefaultMediaDiagnostics
import com.natncompany.media.importer.DefaultMediaImporter
import com.natncompany.media.metadata.AndroidMetadataReader
import com.natncompany.media.preview.ExoPlayerPreviewController
import com.natncompany.media.render.InternalRenderer
import com.natncompany.media.session.DefaultMediaSessionManager
import com.natncompany.media.transcode.FfmpegKitBackend
import com.natncompany.media.transcode.InternalTranscoder

class DefaultMediaModuleFactory : MediaModuleFactory {
    override fun createImporter(context: Context): MediaImporter {
        return DefaultMediaImporter(context, createCacheManager(context), createMetadataReader(context))
    }

    override fun createMetadataReader(context: Context): MetadataReader {
        return AndroidMetadataReader(context)
    }

    override fun createTimelineEditor(): TimelineEditor {
        return DefaultTimelineEditor()
    }

    override fun createPreviewController(context: Context): PreviewController {
        return ExoPlayerPreviewController(context)
    }

    override fun createTranscoder(context: Context): Transcoder {
        return InternalTranscoder(createCacheManager(context), createMetadataReader(context), FfmpegKitBackend())
    }

    override fun createRenderer(context: Context): Renderer {
        return InternalRenderer(createCacheManager(context), FfmpegKitBackend())
    }

    override fun createAudioProcessor(context: Context): AudioProcessor {
        return DefaultAudioProcessor(context, createMetadataReader(context))
    }

    override fun createCacheManager(context: Context): ProjectCacheManager {
        return DefaultProjectCacheManager(context)
    }

    override fun createDiagnostics(context: Context): MediaDiagnostics {
        return DefaultMediaDiagnostics()
    }

    override fun createSessionManager(context: Context): MediaSessionManager {
        return DefaultMediaSessionManager(
            importer = createImporter(context),
            metadataReader = createMetadataReader(context),
            timelineEditor = createTimelineEditor(),
            previewController = createPreviewController(context),
            transcoder = createTranscoder(context),
            renderer = createRenderer(context),
            audioProcessor = createAudioProcessor(context),
            cacheManager = createCacheManager(context),
            diagnostics = createDiagnostics(context)
        )
    }
}
