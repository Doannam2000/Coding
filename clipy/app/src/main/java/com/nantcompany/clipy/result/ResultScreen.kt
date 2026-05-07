package com.nantcompany.clipy.result

import androidx.compose.runtime.Composable
import com.nantcompany.clipy.design.ScreenLayout
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ResultScreen(onNavigate: (AppRoute) -> Unit) {
    ScreenLayout(
        title = "Result",
        subtitle = "Output generated. Save to gallery, share, or open.",
        primaryActionLabel = "View History",
        onPrimaryAction = { onNavigate(AppRoute.OUTPUT_HISTORY) }
    )
}
