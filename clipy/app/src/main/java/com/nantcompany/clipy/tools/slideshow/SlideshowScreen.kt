package com.nantcompany.clipy.tools.slideshow

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun SlideshowScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Slideshow",
        subtitle = "Build image slideshow with timing and transition settings.",
        primaryActionLabel = "Start Processing",
        onPrimaryAction = { onNavigate(AppRoute.PROCESSING) }
    )
}
