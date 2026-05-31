package com.nantcompany.clipy.export.job

import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingRequestSerializationTest {

    @Test
    fun `request json uses stable type name and restores concrete request`() {
        val request = ProcessingRequest.Compress(
            CompressRequest(
                inputPath = "/input.mp4",
                outputPath = "/output.mp4",
                bitrateKbps = 1200,
                targetHeight = 720,
                keepAudio = true
            )
        )

        val json = request.toJson()
        val restored = ProcessingRequest.fromJson(json)

        assertTrue(json.contains("\"type_name\":\"compress\""))
        assertEquals(request, restored)
    }

    @Test
    fun `filter request json preserves selected style and adjustments`() {
        val request = ProcessingRequest.Filters(
            FiltersRequest(
                inputPath = "/input.mp4",
                outputPath = "/output.mp4",
                brightness = 0.2f,
                contrast = -0.1f,
                saturation = 1.7f,
                filterIntensity = 0.65f,
                filterName = "INVERT"
            )
        )

        val json = request.toJson()
        val restored = ProcessingRequest.fromJson(json)

        assertTrue(json.contains("\"type_name\":\"filters\""))
        assertTrue(json.contains("\"filterName\":\"INVERT\""))
        assertTrue(json.contains("\"filterIntensity\":0.65"))
        assertEquals(request, restored)
    }

    @Test
    fun `legacy filter request json defaults intensity to full strength`() {
        val json = """
            {
              "request": {
                "inputPath": "/input.mp4",
                "outputPath": "/output.mp4",
                "brightness": 0.0,
                "contrast": 0.0,
                "saturation": 1.0,
                "filterName": "SEPIA"
              },
              "type_name": "filters"
            }
        """.trimIndent()

        val restored = ProcessingRequest.fromJson(json) as ProcessingRequest.Filters

        assertEquals(1f, restored.request.filterIntensity)
    }
}
