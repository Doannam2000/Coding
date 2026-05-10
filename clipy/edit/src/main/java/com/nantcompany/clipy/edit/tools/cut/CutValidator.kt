package com.nantcompany.clipy.edit.tools.cut

import com.nantcompany.clipy.edit.common.ValidationResult

class CutValidator {
    fun validate(request: CutRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.startMs < 0L) return ValidationResult(false, "Start time must be >= 0")
        if (request.endMs <= request.startMs) return ValidationResult(false, "End time must be greater than start time")
        return ValidationResult(true)
    }
}
