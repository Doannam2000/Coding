package com.nantcompany.clipy.edit.tools.crop

import com.nantcompany.clipy.edit.common.ValidationResult

class CropValidator {
    fun validate(request: CropRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.width <= 0) return ValidationResult(false, "Crop width must be greater than 0")
        if (request.height <= 0) return ValidationResult(false, "Crop height must be greater than 0")
        if (request.x < 0) return ValidationResult(false, "X position must not be negative")
        if (request.y < 0) return ValidationResult(false, "Y position must not be negative")
        return ValidationResult(true)
    }
}