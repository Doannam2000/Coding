package com.nantcompany.clipy.picker

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickAudioScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Pick Audio",
        subtitle = "Audio import flow is reserved for upcoming audio editor features.",
        primaryActionLabel = "Open Audio Editor Placeholder",
        onPrimaryAction = { onNavigate(AppRoute.COMING_SOON_AUDIO_EDITOR) }
    )
}
