package com.nantcompany.clipy.tools.compress

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun CompressVideoScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Compress Video",
        subtitle = "Select quality profile and output target size.",
        primaryActionLabel = "Start Processing",
        onPrimaryAction = { onNavigate(AppRoute.PROCESSING) }
    )
}
