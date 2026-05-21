package com.nantcompany.clipy.export.job

import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class FFmpegCommandValidationTest {

    private val manager = ProcessingJobManager()

    @Test
    fun `cut-fast should use copy codec`() {
        val request = ProcessingRequest.Cut(
            CutRequest("/in", "/out", 0, 1000)
        )
        val plan = manager.buildExecutionPlan(request, preciseCut = false)
        
        assertTrue(plan.arguments.contains("copy"))
        assertTrue(plan.arguments.contains("-ss"))
        assertTrue(plan.arguments.contains("-to"))
    }

    @Test
    fun `cut-precise should use libx264`() {
        val request = ProcessingRequest.Cut(
            CutRequest("/in", "/out", 0, 1000)
        )
        val plan = manager.buildExecutionPlan(request, preciseCut = true)
        
        assertTrue(plan.arguments.contains("libx264"))
        assertTrue(plan.arguments.contains("aac"))
    }

    @Test
    fun `compress should set bitrate`() {
        val request = ProcessingRequest.Compress(
            CompressRequest("/in", "/out", 1500, 1080)
        )
        val plan = manager.buildExecutionPlan(request)
        
        assertTrue(plan.arguments.contains("1500k"))
        assertTrue(plan.arguments.contains("scale=-2:1080"))
    }

    @Test
    fun `merge-fast should use copy codec`() {
        val request = ProcessingRequest.Merge(
            MergeRequest(listOf("/v1", "/v2"), "/out")
        )
        val plan = manager.buildExecutionPlan(request, mergeReencode = false)
        
        assertTrue(plan.arguments.contains("copy"))
        assertTrue(plan.arguments.contains("concat"))
    }
}
