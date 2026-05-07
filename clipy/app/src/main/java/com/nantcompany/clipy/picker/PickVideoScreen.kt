package com.nantcompany.clipy.picker

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickVideoScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Pick Video",
        subtitle = "Choose a single video input for cut, compress, or extract audio.",
        primaryActionLabel = "Go To Cut Tool",
        onPrimaryAction = { onNavigate(AppRoute.CUT_VIDEO) }
    )
}
