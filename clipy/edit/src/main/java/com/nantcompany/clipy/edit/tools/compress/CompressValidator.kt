package com.nantcompany.clipy.edit.tools.compress

import com.nantcompany.clipy.edit.common.ValidationResult

class CompressValidator {
    fun validate(request: CompressRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.bitrateKbps <= 0) return ValidationResult(false, "Bitrate must be greater than 0")
        return ValidationResult(true)
    }
}
