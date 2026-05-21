package com.nantcompany.clipy.edit.tools.slideshow

import com.nantcompany.clipy.edit.common.ValidationResult

class SlideshowValidator {
    fun validate(request: SlideshowRequest): ValidationResult {
        if (request.imagePaths.isEmpty()) return ValidationResult(false, "Select at least 1 image")
        if (request.imagePaths.any { it.isBlank() }) return ValidationResult(false, "Image path must not be empty")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.secondsPerImage <= 0) return ValidationResult(false, "Seconds per image must be greater than 0")
        if (request.backgroundMode !in setOf("fit", "fill", "black")) return ValidationResult(false, "Background mode is not supported")
        if (request.transition !in setOf("none", "fade")) return ValidationResult(false, "Transition is not supported")
        return ValidationResult(true)
    }
}
