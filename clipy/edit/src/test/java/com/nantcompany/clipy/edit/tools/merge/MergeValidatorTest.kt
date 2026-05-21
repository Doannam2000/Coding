package com.nantcompany.clipy.edit.tools.merge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeValidatorTest {

    private val validator = MergeValidator()

    @Test
    fun `valid request should return true`() {
        val request = MergeRequest(listOf("/p1", "/p2"), "/out")
        assertTrue(validator.validate(request).isValid)
    }

    @Test
    fun `single input should be invalid`() {
        val request = MergeRequest(listOf("/p1"), "/out")
        assertFalse(validator.validate(request).isValid)
    }

    @Test
    fun `empty input should be invalid`() {
        val request = MergeRequest(emptyList(), "/out")
        assertFalse(validator.validate(request).isValid)
    }
}
