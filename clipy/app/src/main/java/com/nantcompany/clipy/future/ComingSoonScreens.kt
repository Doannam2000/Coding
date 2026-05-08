package com.nantcompany.clipy.future

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ComingSoonScreen(
    featureName: String,
    onNavigate: (AppRoute) -> Unit
) {
    ScreenLayout(
        title = featureName,
        subtitle = "Architecture is reserved in :edit and :export for this feature.",
        primaryActionLabel = "Back",
        onPrimaryAction = { onNavigate(AppRoute.HOME) }
    )
}
