package com.nantcompany.clipy.edit.media.validator

import com.nantcompany.clipy.edit.common.ValidationResult
import com.nantcompany.clipy.edit.media.model.MediaItem

class MediaValidator {
    fun validateSingleVideoSelection(item: MediaItem?): ValidationResult {
        if (item == null) return ValidationResult(isValid = false, errorMessage = "No media selected")
        if (!item.mimeType.startsWith("video")) {
            return ValidationResult(isValid = false, errorMessage = "Selected media is not a video")
        }
        return ValidationResult(isValid = true)
    }
}
