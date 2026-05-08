package com.nantcompany.clipy.edit.tools.extractaudio

import com.nantcompany.clipy.edit.common.ValidationResult

class ExtractAudioValidator {
    fun validate(request: ExtractAudioRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.format.isBlank()) return ValidationResult(false, "Audio format is required")
        return ValidationResult(true)
    }
}
