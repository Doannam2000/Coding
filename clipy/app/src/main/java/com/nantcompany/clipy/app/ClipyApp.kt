package com.nantcompany.clipy.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.future.ComingSoonScreen
import com.nantcompany.clipy.history.OutputHistoryScreen
import com.nantcompany.clipy.home.HomeScreen
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.navigation.RootNavigatorViewModel
import com.nantcompany.clipy.picker.PickAudioScreen
import com.nantcompany.clipy.picker.PickImagesScreen
import com.nantcompany.clipy.picker.PickMultipleVideosScreen
import com.nantcompany.clipy.picker.PickVideoScreen
import com.nantcompany.clipy.processing.ProcessingScreen
import com.nantcompany.clipy.result.ResultScreen
import com.nantcompany.clipy.settings.SettingsScreen
import com.nantcompany.clipy.theme.ClipyTheme
import com.nantcompany.clipy.tools.compress.CompressVideoScreen
import com.nantcompany.clipy.tools.cut.CutVideoScreen
import com.nantcompany.clipy.tools.extractaudio.ExtractAudioScreen
import com.nantcompany.clipy.tools.merge.MergeVideoScreen
import com.nantcompany.clipy.tools.slideshow.SlideshowScreen

@Composable
fun ClipyApp(
    navigatorViewModel: RootNavigatorViewModel = viewModel()
) {
    val uiState by navigatorViewModel.uiState.collectAsState()
    val currentRoute = uiState.currentRoute

    ClipyTheme(darkTheme = true) {
        ClipyScaffold(
            title = currentRoute.title,
            onHomeClick = { navigatorViewModel.navigateTo(AppRoute.HOME) },
            onHistoryClick = { navigatorViewModel.navigateTo(AppRoute.OUTPUT_HISTORY) },
            onSettingsClick = { navigatorViewModel.navigateTo(AppRoute.SETTINGS) }
        ) {
            when (currentRoute) {
                AppRoute.HOME -> HomeScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.PICK_VIDEO -> PickVideoScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.PICK_MULTIPLE_VIDEOS -> PickMultipleVideosScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.PICK_IMAGES -> PickImagesScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.PICK_AUDIO -> PickAudioScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.CUT_VIDEO -> CutVideoScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.COMPRESS_VIDEO -> CompressVideoScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.MERGE_VIDEO -> MergeVideoScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.EXTRACT_AUDIO -> ExtractAudioScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.SLIDESHOW -> SlideshowScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.PROCESSING -> ProcessingScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.RESULT -> ResultScreen(onNavigate = navigatorViewModel::navigateTo)
                AppRoute.OUTPUT_HISTORY -> OutputHistoryScreen()
                AppRoute.SETTINGS -> SettingsScreen()
                AppRoute.COMING_SOON_FILTERS -> ComingSoonScreen("Filters")
                AppRoute.COMING_SOON_STICKERS -> ComingSoonScreen("Stickers")
                AppRoute.COMING_SOON_TEXT_OVERLAY -> ComingSoonScreen("Text Overlay")
                AppRoute.COMING_SOON_TIMELINE -> ComingSoonScreen("Timeline")
                AppRoute.COMING_SOON_TRANSITIONS -> ComingSoonScreen("Transitions")
                AppRoute.COMING_SOON_AUDIO_EDITOR -> ComingSoonScreen("Audio Editor")
            }
        }
    }
}
