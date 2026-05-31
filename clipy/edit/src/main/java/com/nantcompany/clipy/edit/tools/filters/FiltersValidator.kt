package com.nantcompany.clipy.edit.tools.filters

import com.nantcompany.clipy.edit.common.ValidationResult

class FiltersValidator {
    fun validate(request: FiltersRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.brightness < -1f || request.brightness > 1f) return ValidationResult(false, "Brightness must be between -1.0 and 1.0")
        if (request.contrast < -1f || request.contrast > 1f) return ValidationResult(false, "Contrast must be between -1.0 and 1.0")
        if (request.saturation < 0f || request.saturation > 3f) return ValidationResult(false, "Saturation must be between 0 and 3")
        if (request.filterIntensity < 0f || request.filterIntensity > 1f) return ValidationResult(false, "Filter intensity must be between 0 and 1")
        return ValidationResult(true)
    }
}
