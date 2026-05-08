package com.nantcompany.clipy.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.nantcompany.clipy.player.VideoPlayerScreen
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
    navigatorViewModel: RootNavigatorViewModel = viewModel(),
    sessionViewModel: EditorSessionViewModel = viewModel()
) {
    val navigationState by navigatorViewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val currentRoute = navigationState.currentRoute

    ClipyTheme(darkTheme = true) {
        if (currentRoute == AppRoute.VIDEO_PLAYER) {
            VideoPlayerScreen(
                videoPath = sessionState.selectedHistoryOutput?.path,
                onNavigate = navigatorViewModel::navigateTo
            )
            return@ClipyTheme
        }

        ClipyScaffold(
            title = currentRoute.title,
            onHomeClick = { navigatorViewModel.navigateTo(AppRoute.HOME) },
            onHistoryClick = { navigatorViewModel.navigateTo(AppRoute.OUTPUT_HISTORY) },
            onSettingsClick = { navigatorViewModel.navigateTo(AppRoute.SETTINGS) }
        ) {
            when (currentRoute) {
                AppRoute.HOME -> HomeScreen(onNavigate = navigatorViewModel::navigateTo)

                AppRoute.PICK_VIDEO -> PickVideoScreen(
                    selectedPath = sessionState.singleVideoPath,
                    onVideoPicked = sessionViewModel::setSingleVideoPath,
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PICK_MULTIPLE_VIDEOS -> PickMultipleVideosScreen(
                    selectedPaths = sessionState.multipleVideoPaths,
                    onVideosPicked = sessionViewModel::setMultipleVideoPaths,
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PICK_IMAGES -> PickImagesScreen(
                    selectedPaths = sessionState.imagePaths,
                    onImagesPicked = sessionViewModel::setImagePaths,
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PICK_AUDIO -> PickAudioScreen(onNavigate = navigatorViewModel::navigateTo)

                AppRoute.CUT_VIDEO -> CutVideoScreen(
                    inputPath = sessionState.singleVideoPath,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    }
                )

                AppRoute.COMPRESS_VIDEO -> CompressVideoScreen(
                    inputPath = sessionState.singleVideoPath,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    }
                )

                AppRoute.MERGE_VIDEO -> MergeVideoScreen(
                    inputPaths = sessionState.multipleVideoPaths,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    }
                )

                AppRoute.EXTRACT_AUDIO -> ExtractAudioScreen(
                    inputPath = sessionState.singleVideoPath,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    }
                )

                AppRoute.SLIDESHOW -> SlideshowScreen(
                    imagePaths = sessionState.imagePaths,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    },
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PROCESSING -> ProcessingScreen(
                    sessionViewModel = sessionViewModel,
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.RESULT -> ResultScreen(
                    output = sessionState.lastOutput,
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.OUTPUT_HISTORY -> OutputHistoryScreen(
                    onOutputSelected = { output ->
                        sessionViewModel.setSelectedHistoryOutput(output)
                        navigatorViewModel.navigateTo(AppRoute.VIDEO_PLAYER)
                    }
                )
                AppRoute.VIDEO_PLAYER -> Unit
                AppRoute.SETTINGS -> SettingsScreen()
                AppRoute.COMING_SOON_FILTERS -> ComingSoonScreen("Filters", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_STICKERS -> ComingSoonScreen("Stickers", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TEXT_OVERLAY -> ComingSoonScreen("Text Overlay", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TIMELINE -> ComingSoonScreen("Timeline", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TRANSITIONS -> ComingSoonScreen("Transitions", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_AUDIO_EDITOR -> ComingSoonScreen("Audio Editor", navigatorViewModel::navigateTo)
            }
        }
    }
}
