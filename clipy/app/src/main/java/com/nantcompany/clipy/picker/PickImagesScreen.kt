package com.nantcompany.clipy.picker

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickImagesScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Pick Images",
        subtitle = "Select images as slideshow input.",
        primaryActionLabel = "Go To Slideshow Tool",
        onPrimaryAction = { onNavigate(AppRoute.SLIDESHOW) }
    )
}
