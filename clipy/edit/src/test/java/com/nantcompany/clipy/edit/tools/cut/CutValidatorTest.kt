package com.nantcompany.clipy.edit.tools.cut

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CutValidatorTest {

    private val validator = CutValidator()

    @Test
    fun `valid request should return true`() {
        val request = CutRequest("/path/in", "/path/out", 1000, 5000)
        assertTrue(validator.validate(request).isValid)
    }

    @Test
    fun `start greater than end should be invalid`() {
        val request = CutRequest("/path/in", "/path/out", 5000, 1000)
        assertFalse(validator.validate(request).isValid)
    }

    @Test
    fun `negative start should be invalid`() {
        val request = CutRequest("/path/in", "/path/out", -100, 1000)
        assertFalse(validator.validate(request).isValid)
    }

    @Test
    fun `too short duration should be invalid`() {
        val request = CutRequest("/path/in", "/path/out", 100, 200)
        assertFalse(validator.validate(request).isValid)
    }
}
