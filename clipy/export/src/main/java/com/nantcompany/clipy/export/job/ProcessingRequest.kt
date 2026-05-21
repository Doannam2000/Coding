package com.nantcompany.clipy.export.job

import com.google.gson.Gson
import com.nantcompany.clipy.export.model.AudioTrack
import com.nantcompany.clipy.export.model.TextLayer
import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.crop.CropRequest
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.reverse.ReverseRequest
import com.nantcompany.clipy.edit.tools.rotate.RotateRequest
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.edit.tools.speed.SpeedRequest
import com.nantcompany.clipy.edit.tools.stickers.StickersRequest
import com.nantcompany.clipy.edit.tools.textoverlay.TextOverlayRequest

/**
 * A composite request for the Studio editor.
 * Supports multiple text layers, audio tracks, and full visual transforms.
 */
data class StudioRequest(
    val inputPath: String,
    val outputPath: String,
    val startMs: Long,
    val endMs: Long,
    val rotation: Int,
    val flipHorizontal: Boolean,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val filterName: String,
    val speedFactor: Float,
    val textLayers: List<TextLayer> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val mainVideoVolume: Float = 1.0f
)

sealed class ProcessingRequest {
    abstract val outputPath: String

    data class Cut(val request: CutRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Compress(val request: CompressRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Merge(val request: MergeRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class ExtractAudio(val request: ExtractAudioRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Slideshow(val request: SlideshowRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Rotate(val request: RotateRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Speed(val request: SpeedRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Crop(val request: CropRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Filters(val request: FiltersRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Reverse(val request: ReverseRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Stickers(val request: StickersRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class TextOverlay(val request: TextOverlayRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    data class Studio(val request: StudioRequest) : ProcessingRequest() {
        override val outputPath: String = request.outputPath
    }

    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): ProcessingRequest? = runCatching {
            Gson().fromJson(json, ProcessingRequest::class.java)
        }.getOrNull()
    }
}
