package com.nantcompany.clipy.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.future.ComingSoonScreen
import com.nantcompany.clipy.future.FutureToolsPlaceholderScreen
import com.nantcompany.clipy.home.HomeScreen
import com.nantcompany.clipy.home.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModelAlias
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.navigation.RootNavigatorViewModel
import com.nantcompany.clipy.onboarding.OnboardingScreen
import com.nantcompany.clipy.onboarding.OnboardingStateStore
import com.nantcompany.clipy.onboarding.SplashScreen
import com.nantcompany.clipy.picker.PickAudioScreen
import com.nantcompany.clipy.picker.PickImagesScreen
import com.nantcompany.clipy.picker.PickMultipleVideosScreen
import com.nantcompany.clipy.picker.PickVideoScreen
import com.nantcompany.clipy.picker.MediaPreviewScreen
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
    val homeViewModel: HomeViewModel = composeViewModelAlias()
    val homeState by homeViewModel.uiState.collectAsState()
    val currentRoute = navigationState.currentRoute
    val context = LocalContext.current
    val onboardingStateStore = remember(context) { OnboardingStateStore(context) }

    BackHandler(enabled = navigatorViewModel.canGoBack()) {
        navigatorViewModel.goBack()
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == AppRoute.HOME) {
            homeViewModel.loadRecentExports()
        }
    }

    ClipyTheme(darkTheme = true) {
        if (currentRoute == AppRoute.SPLASH) {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(900)
                val nextRoute = if (onboardingStateStore.isCompleted()) AppRoute.HOME else AppRoute.ONBOARDING
                navigatorViewModel.replace(nextRoute)
            }
            return@ClipyTheme
        }

        if (currentRoute == AppRoute.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    onboardingStateStore.setCompleted(true)
                    navigatorViewModel.replace(AppRoute.HOME)
                }
            )
            return@ClipyTheme
        }

        if (currentRoute == AppRoute.VIDEO_PLAYER) {
            VideoPlayerScreen(
                videoPath = sessionState.selectedHistoryOutput?.path,
                onBack = navigatorViewModel::goBack
            )
            return@ClipyTheme
        }

        if (currentRoute == AppRoute.CUT_VIDEO) {
            CutVideoScreen(
                inputPath = sessionState.singleVideoPath,
                onNavigate = navigatorViewModel::navigateTo,
                onSubmitRequest = {
                    sessionViewModel.setPendingRequest(it)
                    navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                }
            )
            return@ClipyTheme
        }

        ClipyScaffold(
            title = currentRoute.title,
            onHomeClick = { navigatorViewModel.navigateTo(AppRoute.HOME) },
            onSettingsClick = { navigatorViewModel.navigateTo(AppRoute.SETTINGS) }
        ) {
            when (currentRoute) {
                AppRoute.SPLASH -> Unit
                AppRoute.ONBOARDING -> Unit
                AppRoute.HOME -> HomeScreen(
                    onNavigate = navigatorViewModel::navigateTo,
                    onToolSelected = { route, target ->
                        sessionViewModel.setToolTarget(target)
                        if (target == ToolTarget.CUT) {
                            sessionViewModel.setSingleVideoPath(null)
                        }
                        navigatorViewModel.navigateTo(route)
                    },
                    recentExports = homeState.recentExports,
                    onRecentClick = { output ->
                        sessionViewModel.setSelectedHistoryOutput(output)
                        navigatorViewModel.navigateTo(AppRoute.VIDEO_PLAYER)
                    }
                )

                AppRoute.PICK_VIDEO -> com.nantcompany.clipy.picker.GalleryScreen(
                    onNavigateBack = navigatorViewModel::goBack,
                    onVideoPicked = { path ->
                        if (path != null) {
                            sessionViewModel.setSingleVideoPath(path)
                            val target = when (sessionState.toolTarget) {
                                ToolTarget.CUT -> AppRoute.CUT_VIDEO
                                ToolTarget.COMPRESS -> AppRoute.COMPRESS_VIDEO
                                ToolTarget.EXTRACT_AUDIO -> AppRoute.EXTRACT_AUDIO
                                else -> AppRoute.CUT_VIDEO
                            }
                            sessionViewModel.setToolTarget(
                                when (target) {
                                    AppRoute.CUT_VIDEO -> ToolTarget.CUT
                                    AppRoute.COMPRESS_VIDEO -> ToolTarget.COMPRESS
                                    AppRoute.EXTRACT_AUDIO -> ToolTarget.EXTRACT_AUDIO
                                    else -> null
                                }
                            )
                            navigatorViewModel.navigateTo(AppRoute.MEDIA_PREVIEW)
                        }
                    }
                )

                AppRoute.PICK_MULTIPLE_VIDEOS -> PickMultipleVideosScreen(
                    selectedPaths = sessionState.multipleVideoPaths,
                    screenTitle = "Select videos to merge",
                    instructionText = "Choose at least two videos. The order you select will be the merge order.",
                    onVideosPicked = sessionViewModel::appendMultipleVideoPaths,
                    onRemoveAt = sessionViewModel::removeMultipleVideoAt,
                    onContinue = {
                        sessionViewModel.setToolTarget(ToolTarget.MERGE)
                        navigatorViewModel.navigateTo(AppRoute.MEDIA_PREVIEW)
                    },
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PICK_IMAGES -> PickImagesScreen(
                    selectedPaths = sessionState.imagePaths,
                    screenTitle = "Select photos",
                    instructionText = "Choose at least two images. Clipy will turn them into a video slideshow.",
                    onImagesPicked = sessionViewModel::setImagePaths,
                    onRemoveAt = sessionViewModel::removeImageAt,
                    onContinue = {
                        sessionViewModel.setToolTarget(ToolTarget.SLIDESHOW)
                        navigatorViewModel.navigateTo(AppRoute.MEDIA_PREVIEW)
                    },
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.PICK_AUDIO -> PickAudioScreen(onNavigate = navigatorViewModel::navigateTo)

                AppRoute.MEDIA_PREVIEW -> MediaPreviewScreen(
                    singleVideoPath = sessionState.singleVideoPath,
                    multipleVideoPaths = sessionState.multipleVideoPaths,
                    imagePaths = sessionState.imagePaths,
                    targetRoute = when (sessionState.toolTarget) {
                        ToolTarget.CUT -> AppRoute.CUT_VIDEO
                        ToolTarget.COMPRESS -> AppRoute.COMPRESS_VIDEO
                        ToolTarget.EXTRACT_AUDIO -> AppRoute.EXTRACT_AUDIO
                        ToolTarget.MERGE -> AppRoute.MERGE_VIDEO
                        ToolTarget.SLIDESHOW -> AppRoute.SLIDESHOW
                        null -> null
                    },
                    onNavigate = navigatorViewModel::navigateTo
                )

                AppRoute.CUT_VIDEO -> Unit

                AppRoute.COMPRESS_VIDEO -> CompressVideoScreen(
                    inputPath = sessionState.singleVideoPath,
                    onSubmitRequest = {
                        sessionViewModel.setPendingRequest(it)
                        navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                    }
                )

                AppRoute.MERGE_VIDEO -> MergeVideoScreen(
                    inputPaths = sessionState.multipleVideoPaths,
                    onRemoveAt = sessionViewModel::removeMultipleVideoAt,
                    onNavigate = navigatorViewModel::navigateTo,
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
                    onRemoveAt = sessionViewModel::removeImageAt,
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

                AppRoute.VIDEO_PLAYER -> Unit
                AppRoute.SETTINGS -> SettingsScreen()
                AppRoute.FUTURE_TOOLS -> FutureToolsPlaceholderScreen(navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_FILTERS -> ComingSoonScreen("Filters", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_STICKERS -> ComingSoonScreen("Stickers", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TEXT_OVERLAY -> ComingSoonScreen("Text Overlay", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TIMELINE -> ComingSoonScreen("Timeline", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_TRANSITIONS -> ComingSoonScreen("Transitions", navigatorViewModel::navigateTo)
                AppRoute.COMING_SOON_AUDIO_EDITOR -> ComingSoonScreen("Audio Editor", navigatorViewModel::navigateTo)
                else -> {}
            }
        }
    }
}
