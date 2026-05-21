package com.nantcompany.clipy.edit.tools.speed

import com.nantcompany.clipy.edit.common.ValidationResult

class SpeedValidator {
    fun validate(request: SpeedRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.speedFactor < 0.25f || request.speedFactor > 4f) return ValidationResult(false, "Speed must be between 0.25x and 4x")
        return ValidationResult(true)
    }
}