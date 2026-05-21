package com.nantcompany.clipy.edit.tools.textoverlay

import com.nantcompany.clipy.edit.common.ValidationResult

class TextOverlayValidator {
    fun validate(request: TextOverlayRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.text.isBlank()) return ValidationResult(false, "Text cannot be empty")
        if (request.fontSize < 12 || request.fontSize > 200) return ValidationResult(false, "Font size must be between 12 and 200")
        if (request.x < 0f || request.x > 100f) return ValidationResult(false, "X position must be 0-100%")
        if (request.y < 0f || request.y > 100f) return ValidationResult(false, "Y position must be 0-100%")
        return ValidationResult(true)
    }
}