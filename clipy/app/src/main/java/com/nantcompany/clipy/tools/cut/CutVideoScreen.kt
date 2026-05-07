package com.nantcompany.clipy.tools.cut

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun CutVideoScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Cut Video",
        subtitle = "Configure start/end trim points and queue processing.",
        primaryActionLabel = "Start Processing",
        onPrimaryAction = { onNavigate(AppRoute.PROCESSING) }
    )
}
