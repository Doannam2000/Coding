package com.nantcompany.clipy.export.job

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyErrorTest {

    private val manager = ProcessingJobManager()

    @Test
    fun `should return file not found message`() {
        val error = RuntimeException("No such file or directory")
        val friendly = manager.asFriendlyError(error)
        assertEquals("Could not read file.", friendly.message)
    }

    @Test
    fun `should return unsupported format message`() {
        val error = RuntimeException("Invalid data found when processing input")
        val friendly = manager.asFriendlyError(error)
        assertEquals("Format not supported.", friendly.message)
    }

    @Test
    fun `should return not enough storage message`() {
        val error = RuntimeException("ENOSPC: No space left on device")
        val friendly = manager.asFriendlyError(error)
        assertEquals("Not enough storage.", friendly.message)
    }

    @Test
    fun `should return default message for unknown errors`() {
        val error = RuntimeException("Something very weird happened")
        val friendly = manager.asFriendlyError(error)
        assertEquals("Export failed. Please try another file.", friendly.message)
    }
}
