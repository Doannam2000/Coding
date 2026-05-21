package com.nantcompany.clipy.edit.tools.compress

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressValidatorTest {

    private val validator = CompressValidator()

    @Test
    fun `valid request should return true`() {
        val request = CompressRequest("/path/in", "/path/out", 1200, 720)
        assertTrue(validator.validate(request).isValid)
    }

    @Test
    fun `zero bitrate should be invalid`() {
        val request = CompressRequest("/path/in", "/path/out", 0, 720)
        assertFalse(validator.validate(request).isValid)
    }

    @Test
    fun `negative bitrate should be invalid`() {
        val request = CompressRequest("/path/in", "/path/out", -100, 720)
        assertFalse(validator.validate(request).isValid)
    }
}
