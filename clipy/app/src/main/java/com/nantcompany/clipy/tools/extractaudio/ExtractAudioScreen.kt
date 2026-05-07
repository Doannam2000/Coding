package com.nantcompany.clipy.tools.extractaudio

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ExtractAudioScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Extract Audio",
        subtitle = "Choose output format and extract audio track from source video.",
        primaryActionLabel = "Start Processing",
        onPrimaryAction = { onNavigate(AppRoute.PROCESSING) }
    )
}
