package com.nantcompany.clipy.edit.common

enum class TransitionType(val label: String, val ffmpegName: String) {
    NONE("None", "none"),
    FADE("Fade", "fade"),
    CROSSFADE("Crossfade", "fade"),
    WIPE_LEFT("Wipe Left", "wipeleft"),
    WIPE_RIGHT("Wipe Right", "wiperight"),
    SLIDE_LEFT("Slide Left", "slideleft"),
    SLIDE_RIGHT("Slide Right", "slideright"),
    CIRCLE_OPEN("Circle", "circleopen"),
    ZOOM_IN("Zoom In", "zoominstay")
}
