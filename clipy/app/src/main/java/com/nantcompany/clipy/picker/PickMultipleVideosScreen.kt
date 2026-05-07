package com.nantcompany.clipy.picker

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun PickMultipleVideosScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Pick Multiple Videos",
        subtitle = "Select multiple clips as merge input.",
        primaryActionLabel = "Go To Merge Tool",
        onPrimaryAction = { onNavigate(AppRoute.MERGE_VIDEO) }
    )
}
