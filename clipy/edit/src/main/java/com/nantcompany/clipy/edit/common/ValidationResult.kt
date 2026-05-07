package com.nantcompany.clipy.edit.common

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
