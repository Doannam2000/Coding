package com.nantcompany.clipy.export.job

import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest

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
}
