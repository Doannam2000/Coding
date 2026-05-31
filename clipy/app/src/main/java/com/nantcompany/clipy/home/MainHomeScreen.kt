package com.nantcompany.clipy.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.home.components.BottomGlassNav
import com.nantcompany.clipy.home.pages.LibraryPage
import com.nantcompany.clipy.home.pages.SettingsPage
import com.nantcompany.clipy.home.pages.StudioPage
import com.nantcompany.clipy.home.pages.ToolsPage
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.export.output.OutputMedia
import kotlinx.coroutines.launch

@Composable
fun MainHomeScreen(
    onNavigate: (AppRoute) -> Unit,
    onToolSelected: (AppRoute, ToolTarget?) -> Unit,
    recentExports: List<OutputMedia>,
    onRecentClick: (OutputMedia?) -> Unit,
    initialTab: HomeTab = HomeTab.STUDIO
) {
    val pagerState = rememberPagerState(
        initialPage = HomeTab.entries.indexOf(initialTab).coerceAtLeast(0),
        pageCount = { HomeTab.entries.size }
    )
    val scope = rememberCoroutineScope()

    ClipyScaffold(showTopBar = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                beyondViewportPageCount = 1,
                userScrollEnabled = true
            ) { pageIndex ->
                when (HomeTab.entries[pageIndex]) {
                    HomeTab.STUDIO -> StudioPage(
                        onNavigate = onNavigate,
                        onToolSelected = onToolSelected
                    )
                    HomeTab.TOOLS -> ToolsPage(
                        onToolSelected = onToolSelected
                    )
                    HomeTab.LIBRARY -> LibraryPage(
                        recentExports = recentExports,
                        onRecentClick = onRecentClick
                    )
                    HomeTab.SETTINGS -> SettingsPage(
                        onNavigate = onNavigate
                    )
                }
            }

            BottomGlassNav(
                currentIndex = pagerState.currentPage,
                onTabClick = { index ->
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}
