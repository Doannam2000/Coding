package com.nantcompany.clipy.edit.tools.reverse

import com.nantcompany.clipy.edit.common.ValidationResult

class ReverseValidator {
    fun validate(request: ReverseRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        return ValidationResult(true)
    }
}
