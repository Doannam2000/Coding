package com.nantcompany.clipy.export.job

import com.nantcompany.clipy.edit.tools.compress.CompressRequest
import com.nantcompany.clipy.edit.tools.crop.CropRequest
import com.nantcompany.clipy.edit.tools.cut.CutRequest
import com.nantcompany.clipy.edit.tools.extractaudio.ExtractAudioRequest
import com.nantcompany.clipy.edit.tools.filters.FiltersRequest
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.edit.tools.reverse.ReverseRequest
import com.nantcompany.clipy.edit.tools.rotate.RotateRequest
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.edit.tools.speed.SpeedRequest
import com.nantcompany.clipy.edit.tools.stickers.StickersRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class FFmpegCommandValidationTest {

    private val manager = ProcessingJobManager()

    @Test
    fun `cut should re-encode for compatibility`() {
        val request = ProcessingRequest.Cut(
            CutRequest("/in", "/out", 0, 1000)
        )
        val plan = manager.buildExecutionPlan(request)
        
        assertTrue(plan.arguments.contains("libx264"))
        assertTrue(plan.arguments.contains("aac"))
        assertTrue(plan.arguments.contains("-ss"))
        assertTrue(plan.arguments.contains("-t"))
        assertTrue(plan.arguments.contains("1.000"))
    }

    @Test
    fun `compress should set bitrate`() {
        val request = ProcessingRequest.Compress(
            CompressRequest("/in", "/out", 1500, 1080)
        )
        val plan = manager.buildExecutionPlan(request)
        
        assertTrue(plan.arguments.contains("1500k"))
        assertTrue(plan.arguments.any { it.contains("scale=-2:1080") })
    }

    @Test
    fun `merge without transition should use normalized concat filter`() {
        val request = ProcessingRequest.Merge(
            MergeRequest(listOf("/v1", "/v2"), "/out")
        )
        val plan = manager.buildExecutionPlan(request)
        val graph = plan.arguments[plan.arguments.indexOf("-filter_complex") + 1]
        
        assertTrue(plan.arguments.contains("-filter_complex"))
        assertTrue(graph.contains("concat=n=2:v=1:a=1"))
        assertTrue(graph.contains("scale=720:1280"))
        assertTrue(plan.arguments.contains("libx264"))
    }

    @Test
    fun `merge transition should normalize legacy crossfade name`() {
        val request = ProcessingRequest.Merge(
            MergeRequest(listOf("/v1", "/v2"), "/out", transition = "xfade")
        )
        val plan = manager.buildExecutionPlan(request)
        val graph = plan.arguments[plan.arguments.indexOf("-filter_complex") + 1]

        assertTrue(graph.contains("xfade=transition=fade"))
        assertTrue(graph.contains("acrossfade="))
    }

    @Test
    fun `extract audio should honor output format and bitrate`() {
        val request = ProcessingRequest.ExtractAudio(
            ExtractAudioRequest("/in", "/out.m4a", format = "m4a", bitrateKbps = 192)
        )
        val plan = manager.buildExecutionPlan(request)

        assertTrue(plan.arguments.contains("aac"))
        assertTrue(plan.arguments.contains("192k"))
        assertTrue(plan.arguments.contains("-vn"))
    }

    @Test
    fun `slideshow without transition should normalize images before concat`() {
        val request = ProcessingRequest.Slideshow(
            SlideshowRequest(listOf("/i1.jpg", "/i2.jpg"), "/out.mp4", secondsPerImage = 2, transition = "none")
        )
        val plan = manager.buildExecutionPlan(request)
        val graph = plan.arguments[plan.arguments.indexOf("-filter_complex") + 1]

        assertTrue(graph.contains("scale=720:1280"))
        assertTrue(graph.contains("fps=30"))
        assertTrue(graph.contains("concat=n=2:v=1:a=0"))
    }

    @Test
    fun `slideshow with audio should declare audio input before filter graph`() {
        val request = ProcessingRequest.Slideshow(
            SlideshowRequest(listOf("/i1.jpg", "/i2.jpg"), "/out.mp4", secondsPerImage = 2, transition = "wipeleft", audioPath = "/music.mp3")
        )
        val plan = manager.buildExecutionPlan(request)

        val filterIndex = plan.arguments.indexOf("-filter_complex")
        val audioInputIndex = plan.arguments.indexOf("/music.mp3")
        assertTrue(audioInputIndex in 0 until filterIndex)
        assertTrue(plan.arguments.contains("2:a"))
        assertTrue(plan.arguments.contains("-shortest"))
    }

    @Test
    fun `rotate speed crop reverse should force compatible video output`() {
        val plans = listOf(
            manager.buildExecutionPlan(ProcessingRequest.Rotate(RotateRequest("/in", "/out", 90))),
            manager.buildExecutionPlan(ProcessingRequest.Speed(SpeedRequest("/in", "/out", 0.25f))),
            manager.buildExecutionPlan(ProcessingRequest.Crop(CropRequest("/in", "/out", 0, 0, 320, 321))),
            manager.buildExecutionPlan(ProcessingRequest.Reverse(ReverseRequest("/in", "/out")))
        )

        plans.forEach { plan ->
            assertTrue(plan.arguments.contains("libx264"))
            assertTrue(plan.arguments.contains("aac"))
            assertTrue(plan.arguments.any { it.contains("format=yuv420p") })
        }
    }

    @Test
    fun `stickers should map filtered video and optional source audio`() {
        val request = ProcessingRequest.Stickers(
            StickersRequest("/in", "/out", "/sticker.png", x = 10f, y = 20f, width = 120, startTimeMs = 500, endTimeMs = 1500)
        )
        val plan = manager.buildExecutionPlan(request)
        val graph = plan.arguments[plan.arguments.indexOf("-filter_complex") + 1]

        assertTrue(graph.contains("[1:v]scale=120:-1[sticker]"))
        assertTrue(graph.contains("overlay=(W*10.0/100):(H*20.0/100)"))
        assertTrue(graph.contains("between(t\\,0.500\\,1.500)"))
        assertTrue(plan.arguments.contains("[v]"))
        assertTrue(plan.arguments.contains("0:a?"))
    }

    @Test
    fun `filters should build visible color transform chain`() {
        val request = ProcessingRequest.Filters(
            FiltersRequest(
                inputPath = "/in",
                outputPath = "/out",
                brightness = 0.25f,
                contrast = 0.4f,
                saturation = 2.0f,
                filterName = "CYBERPUNK"
            )
        )
        val plan = manager.buildExecutionPlan(request)
        val vf = plan.arguments[plan.arguments.indexOf("-vf") + 1]

        assertTrue(vf.contains("hue=h=-150:s=1.85"))
        assertTrue(vf.contains("eq=contrast=1.5500:brightness=0.0400"))
        assertTrue(vf.contains("eq=brightness=0.2500:contrast=1.4000:saturation=2.0000"))
        assertTrue(vf.contains("format=yuv420p"))
    }

    @Test
    fun `filter style should not bake in slider presets`() {
        val request = ProcessingRequest.Filters(
            FiltersRequest(
                inputPath = "/in",
                outputPath = "/out",
                brightness = 0f,
                contrast = 0f,
                saturation = 1f,
                filterName = "SEPIA"
            )
        )
        val plan = manager.buildExecutionPlan(request)
        val vf = plan.arguments[plan.arguments.indexOf("-vf") + 1]

        assertTrue(vf.contains("hue=h=28:s=0.45"))
        assertTrue(vf.contains("eq=contrast=1.2800:brightness=0.0600"))
        assertTrue(vf.contains("eq=brightness=0.0000:contrast=1.0000:saturation=1.0000"))
    }

    @Test
    fun `filter export should include selected style transform`() {
        val expectedTransforms = mapOf(
            "SEPIA" to "hue=h=28:s=0.45",
            "GRAYSCALE" to "hue=s=0",
            "INVERT" to "negate",
            "WARM" to "hue=h=22:s=1.28",
            "COOL" to "hue=h=-28:s=1.30",
            "VINTAGE" to "hue=h=22:s=0.38",
            "DRAMATIC" to "hue=s=0.65",
            "TOON" to "eq=contrast=1.7500:saturation=1.6500",
            "SKETCH" to "eq=contrast=1.8500:brightness=0.0800",
            "VIGNETTE" to "eq=brightness=-0.1800:contrast=1.3000",
            "KUWAHARA" to "eq=contrast=0.7000:saturation=1.4500",
            "PIXEL" to "eq=contrast=1.5500:saturation=1.3500",
            "POSTER" to "eq=contrast=1.7500:saturation=1.5500",
            "LOMO" to "hue=h=12:s=1.42",
            "CYBERPUNK" to "hue=h=-150:s=1.85"
        )

        expectedTransforms.forEach { (filterName, expectedTransform) ->
            val request = ProcessingRequest.Filters(
                FiltersRequest(
                    inputPath = "/in",
                    outputPath = "/out",
                    filterName = filterName
                )
            )
            val plan = manager.buildExecutionPlan(request)
            val vf = plan.arguments[plan.arguments.indexOf("-vf") + 1]

            assertTrue("$filterName missing $expectedTransform in $vf", vf.contains(expectedTransform))
            assertTrue("$filterName missing final pixel format", vf.contains("format=yuv420p"))
        }
    }

    @Test
    fun `filter intensity should attenuate selected style transform`() {
        val request = ProcessingRequest.Filters(
            FiltersRequest(
                inputPath = "/in",
                outputPath = "/out",
                filterIntensity = 0.5f,
                filterName = "INVERT"
            )
        )
        val plan = manager.buildExecutionPlan(request)
        val vf = plan.arguments[plan.arguments.indexOf("-vf") + 1]

        assertTrue(vf.contains("lutrgb="))
        assertTrue(vf.contains("+127.5000"))
        assertTrue(vf.contains("eq=brightness=0.0000:contrast=1.0000:saturation=1.0000"))
        assertTrue(vf.contains("format=yuv420p"))
    }
}
