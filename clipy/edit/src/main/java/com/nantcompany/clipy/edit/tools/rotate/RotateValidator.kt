package com.nantcompany.clipy.edit.tools.rotate

import com.nantcompany.clipy.edit.common.ValidationResult

class RotateValidator {
    fun validate(request: RotateRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input path is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.rotation !in setOf(0, 90, 180, 270)) return ValidationResult(false, "Rotation must be 0, 90, 180, or 270 degrees")
        return ValidationResult(true)
    }
}
