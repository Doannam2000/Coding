package com.nantcompany.clipy.future

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.design.ClipyEmptyState
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun ComingSoonScreen(
    featureName: String,
    onNavigate: (AppRoute) -> Unit
) {
    ClipyScaffold(
        title = featureName,
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ClipyEmptyState(
                title = "Coming Soon",
                message = "$featureName is currently being developed with Clipy's high-performance architecture.",
                icon = Icons.Default.Star,
                actionLabel = "Back to Home",
                onAction = { onNavigate(AppRoute.HOME) }
            )
        }
    }
}
