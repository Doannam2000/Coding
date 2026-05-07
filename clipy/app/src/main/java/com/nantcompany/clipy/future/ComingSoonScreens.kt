package com.nantcompany.clipy.future

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout

@Composable
fun ComingSoonScreen(featureName: String) {
    ScreenLayout(
        title = featureName,
        subtitle = "Architecture is reserved in :edit and :export for this feature.",
        primaryActionLabel = "Back",
        onPrimaryAction = {}
    )
}
