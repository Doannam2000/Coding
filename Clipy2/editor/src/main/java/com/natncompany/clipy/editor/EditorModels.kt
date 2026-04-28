package com.natncompany.clipy.editor

import com.natncompany.clipy.filter.GpuImageFilterLibrary

enum class EditorScreen {
    Home,
    Editor,
    Export
}

enum class MediaKind {
    Video,
    Image
}

enum class EditorTool(val label: String) {
    Trim("Trim"),
    Canvas("Canvas"),
    Copy("Copy"),
    Background("Background"),
    Speed("Speed"),
    Split("Split"),
    Volume("Volume"),
    Filter("Filter")
}

enum class AspectPreset(
    val label: String,
    val previewAspectRatio: Float
) {
    NineSixteen("9:16", 9f / 16f),
    OneOne("1:1", 1f),
    FourFive("4:5", 4f / 5f),
    SixteenNine("16:9", 16f / 9f)
}

data class ClipAdjustments(
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val backgroundHex: String = "#11161D",
    val filterName: String = "Original",
    val transitionName: String = "Cut"
)

data class ClipDraft(
    val id: String,
    val displayName: String,
    val uriString: String,
    val mediaKind: MediaKind,
    val sourceDurationMs: Long,
    val width: Int? = null,
    val height: Int? = null,
    val adjustments: ClipAdjustments = ClipAdjustments()
) {
    fun trimEndMs(): Long = adjustments.trimEndMs.takeIf { it > 0L } ?: sourceDurationMs

    fun visibleDurationMs(): Long {
        val trimmedDuration = (trimEndMs() - adjustments.trimStartMs).coerceAtLeast(250L)
        return (trimmedDuration / adjustments.speed.coerceAtLeast(0.25f)).toLong().coerceAtLeast(250L)
    }
}

data class HomeFeature(
    val title: String,
    val shortLabel: String,
    val defaultTool: EditorTool,
    val defaultAspect: AspectPreset,
    val replaceTimeline: Boolean
)

val editorFeatures = listOf(
    HomeFeature(
        title = "Edit",
        shortLabel = "ED",
        defaultTool = EditorTool.Trim,
        defaultAspect = AspectPreset.NineSixteen,
        replaceTimeline = true
    ),
    HomeFeature(
        title = "Effects",
        shortLabel = "FX",
        defaultTool = EditorTool.Filter,
        defaultAspect = AspectPreset.NineSixteen,
        replaceTimeline = true
    ),
    HomeFeature(
        title = "Cover",
        shortLabel = "CV",
        defaultTool = EditorTool.Canvas,
        defaultAspect = AspectPreset.OneOne,
        replaceTimeline = true
    ),
    HomeFeature(
        title = "Lyrics",
        shortLabel = "LY",
        defaultTool = EditorTool.Trim,
        defaultAspect = AspectPreset.NineSixteen,
        replaceTimeline = true
    ),
    HomeFeature(
        title = "Template",
        shortLabel = "TP",
        defaultTool = EditorTool.Speed,
        defaultAspect = AspectPreset.FourFive,
        replaceTimeline = true
    ),
    HomeFeature(
        title = "PIP",
        shortLabel = "PP",
        defaultTool = EditorTool.Background,
        defaultAspect = AspectPreset.SixteenNine,
        replaceTimeline = true
    )
)

val filterOptions = GpuImageFilterLibrary.presets.map { it.label }
val backgroundOptions = listOf("#11161D", "#1B2330", "#244537", "#5C4520", "#702F38", "#E6E1D7")
