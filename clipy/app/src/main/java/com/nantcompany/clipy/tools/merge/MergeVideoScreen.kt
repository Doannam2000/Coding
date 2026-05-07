package com.nantcompany.clipy.tools.merge

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun MergeVideoScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Merge Video",
        subtitle = "Arrange multiple clips and merge into one output file.",
        primaryActionLabel = "Start Processing",
        onPrimaryAction = { onNavigate(AppRoute.PROCESSING) }
    )
}
