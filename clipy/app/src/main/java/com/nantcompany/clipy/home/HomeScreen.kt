package com.nantcompany.clipy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.home.components.BottomGlassNav
import com.nantcompany.clipy.home.pages.LibraryPage
import com.nantcompany.clipy.home.pages.SettingsPage
import com.nantcompany.clipy.home.pages.StudioPage
import com.nantcompany.clipy.home.pages.ToolsPage
import com.nantcompany.clipy.navigation.AppRoute
import kotlinx.coroutines.launch

enum class HomeTab(val label: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.PlayArrow),
    LIBRARY("Library", Icons.Default.Star),
    TOOLS("Tools", Icons.Default.Build),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit,
    onToolSelected: (AppRoute, ToolTarget?) -> Unit,
    recentExports: List<OutputMedia> = emptyList(),
    onRecentClick: (OutputMedia) -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { HomeTab.entries.size })
    val scope = rememberCoroutineScope()
    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFF0A0A12),
            Color(0xFF090D1D),
            Color(0xFF020617)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(colors = listOf(Color(0x55D8B4FE), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(colors = listOf(Color(0x4467E8F9), Color.Transparent)),
                    CircleShape
                )
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (HomeTab.entries[page]) {
                HomeTab.STUDIO -> StudioPage(onNavigate = onNavigate, onToolSelected = onToolSelected)
                HomeTab.LIBRARY -> LibraryPage(recentExports = recentExports, onRecentClick = onRecentClick)
                HomeTab.TOOLS -> ToolsPage(onToolSelected = onToolSelected)
                HomeTab.SETTINGS -> SettingsPage(onNavigate = onNavigate)
            }
        }

        BottomGlassNav(
            currentIndex = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            onTabClick = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        )
    }
}
