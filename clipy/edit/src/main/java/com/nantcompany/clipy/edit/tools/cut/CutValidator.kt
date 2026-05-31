package com.nantcompany.clipy.edit.tools.cut

import com.nantcompany.clipy.edit.common.ValidationResult

class CutValidator {
    fun validate(request: CutRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.startMs < 0L) return ValidationResult(false, "Start time must be >= 0")
        if (request.endMs <= request.startMs) return ValidationResult(false, "End time must be greater than start time")
        if (request.endMs - request.startMs < MIN_DURATION_MS) {
            return ValidationResult(false, "Selected clip must be at least ${MIN_DURATION_MS / 1000.0} seconds")
        }
        return ValidationResult(true)
    }

    private companion object {
        const val MIN_DURATION_MS = 500L
    }
}
