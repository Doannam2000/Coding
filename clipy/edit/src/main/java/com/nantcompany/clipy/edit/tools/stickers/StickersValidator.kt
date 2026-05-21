package com.nantcompany.clipy.edit.tools.stickers

import com.nantcompany.clipy.edit.common.ValidationResult

class StickersValidator {
    fun validate(request: StickersRequest): ValidationResult {
        if (request.inputPath.isBlank()) return ValidationResult(false, "Input video is required")
        if (request.outputPath.isBlank()) return ValidationResult(false, "Output path is required")
        if (request.stickerPath.isBlank()) return ValidationResult(false, "Sticker image is required")
        if (request.x < 0f || request.x > 100f) return ValidationResult(false, "X position must be 0-100%")
        if (request.y < 0f || request.y > 100f) return ValidationResult(false, "Y position must be 0-100%")
        if (request.width != null && request.width <= 0) return ValidationResult(false, "Sticker width must be greater than 0")
        if (request.endTimeMs <= request.startTimeMs) return ValidationResult(false, "End time must be after start time")
        return ValidationResult(true)
    }
}