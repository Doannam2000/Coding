package com.nantcompany.clipy.edit.tools.merge

import com.nantcompany.clipy.edit.common.ValidationResult

class MergeValidator {
    fun validate(request: MergeRequest): ValidationResult {
        if (request.inputPaths.size < 2) return ValidationResult(false, "Select at least 2 videos")
        if (request.inputPaths.any { it.isBlank() }) return ValidationResult(false, "Input videos must not be empty")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        return ValidationResult(true)
    }
}
