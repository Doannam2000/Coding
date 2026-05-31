package com.nantcompany.clipy.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.design.ClipyThemeBackground
import com.nantcompany.clipy.editor.ProEditorScreen
import com.nantcompany.clipy.future.ComingSoonScreen
import com.nantcompany.clipy.future.FutureToolsPlaceholderScreen
import com.nantcompany.clipy.history.OutputHistoryScreen
import com.nantcompany.clipy.history.OutputHistoryViewModel
import com.nantcompany.clipy.home.HomeScreen
import com.nantcompany.clipy.home.HomeViewModel
import com.nantcompany.clipy.home.MainHomeScreen
import com.nantcompany.clipy.export.job.ClipyExportProvider
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.navigation.RootNavigatorViewModel
import com.nantcompany.clipy.onboarding.OnboardingScreen
import com.nantcompany.clipy.onboarding.OnboardingStateStore
import com.nantcompany.clipy.onboarding.SplashScreen
import com.nantcompany.clipy.picker.GalleryPickerScreen
import com.nantcompany.clipy.picker.MediaItemType
import com.nantcompany.clipy.picker.PickAudioScreen
import com.nantcompany.clipy.picker.VideoMetadataLoader
import com.nantcompany.clipy.player.VideoPlayerScreen
import com.nantcompany.clipy.processing.ProcessingScreen
import com.nantcompany.clipy.result.ResultScreen
import com.nantcompany.clipy.settings.SettingsScreen
import com.nantcompany.clipy.theme.ClipyTheme
import com.nantcompany.clipy.tools.compress.CompressVideoScreen
import com.nantcompany.clipy.tools.crop.CropVideoScreen
import com.nantcompany.clipy.tools.cut.CutVideoScreen
import com.nantcompany.clipy.tools.extractaudio.ExtractAudioScreen
import com.nantcompany.clipy.tools.filters.FiltersVideoScreen
import com.nantcompany.clipy.tools.merge.MergeVideoScreen
import com.nantcompany.clipy.tools.reverse.ReverseVideoScreen
import com.nantcompany.clipy.tools.rotate.RotateVideoScreen
import com.nantcompany.clipy.tools.slideshow.SlideshowScreen
import com.nantcompany.clipy.tools.speed.SpeedVideoScreen
import com.nantcompany.clipy.tools.stickers.StickersVideoScreen
import com.nantcompany.clipy.tools.textoverlay.TextOverlayVideoScreen

@Composable
fun ClipyApp(
    navigatorViewModel: RootNavigatorViewModel = viewModel(),
    sessionViewModel: EditorSessionViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    historyViewModel: OutputHistoryViewModel = viewModel(),
    proEditorViewModel: ProEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val navState by navigatorViewModel.uiState.collectAsState()
    val sessionState by sessionViewModel.state.collectAsState()
    val currentRoute = navState.currentRoute
    
    val historyState by historyViewModel.uiState.collectAsState()

    LaunchedEffect(context) {
        ClipyExportProvider.getOutputRepository(context)
        historyViewModel.loadHistory()
    }

    ClipyTheme {
        ClipyThemeBackground {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxSize(),
                label = "ScreenTransition"
            ) { targetRoute ->
                // Individual screens now manage their own Scaffolds/Toolbars
                // We REMOVED ClipyScaffold from here to prevent double toolbars.
                when (targetRoute) {
                    AppRoute.SPLASH -> SplashScreen(
                        onFinish = { isCompleted ->
                            if (isCompleted) navigatorViewModel.navigateTo(AppRoute.HOME)
                            else navigatorViewModel.navigateTo(AppRoute.ONBOARDING)
                        }
                    )

                    AppRoute.ONBOARDING -> OnboardingScreen(
                        onFinish = {
                            OnboardingStateStore(context).setCompleted(true)
                            navigatorViewModel.navigateTo(AppRoute.HOME)
                        }
                    )

                    AppRoute.HOME -> MainHomeScreen(
                        onNavigate = navigatorViewModel::navigateTo,
                        onToolSelected = { route, target ->
                            sessionViewModel.setToolTarget(target)
                            navigatorViewModel.navigateTo(route)
                        },
                        recentExports = historyState.outputs,
                        onRecentClick = { output ->
                            if (output == null) {
                                navigatorViewModel.navigateTo(AppRoute.OUTPUT_HISTORY)
                            } else {
                                sessionViewModel.setLastOutput(output)
                                navigatorViewModel.navigateTo(AppRoute.VIDEO_PLAYER)
                            }
                        }
                    )

                    AppRoute.PICK_VIDEO -> GalleryPickerScreen(
                        type = MediaItemType.VIDEO,
                        isMultiSelect = false,
                        onBack = { navigatorViewModel.goBack() },
                        onMediaPicked = { paths ->
                            val path = paths.firstOrNull() ?: return@GalleryPickerScreen
                            sessionViewModel.setSingleVideoPath(path)

                            val target = sessionState.toolTarget
                            when (target) {
                                ToolTarget.CUT -> navigatorViewModel.navigateTo(AppRoute.CUT_VIDEO)
                                ToolTarget.COMPRESS -> navigatorViewModel.navigateTo(AppRoute.COMPRESS_VIDEO)
                                ToolTarget.EXTRACT_AUDIO -> navigatorViewModel.navigateTo(AppRoute.EXTRACT_AUDIO)
                                ToolTarget.ROTATE -> navigatorViewModel.navigateTo(AppRoute.ROTATE_VIDEO)
                                ToolTarget.SPEED -> navigatorViewModel.navigateTo(AppRoute.SPEED_VIDEO)
                                ToolTarget.CROP -> navigatorViewModel.navigateTo(AppRoute.CROP_VIDEO)
                                ToolTarget.FILTERS -> navigatorViewModel.navigateTo(AppRoute.FILTERS_VIDEO)
                                ToolTarget.REVERSE -> navigatorViewModel.navigateTo(AppRoute.REVERSE_VIDEO)
                                ToolTarget.STICKERS -> navigatorViewModel.navigateTo(AppRoute.STICKERS_VIDEO)
                                ToolTarget.TEXT_OVERLAY -> navigatorViewModel.navigateTo(AppRoute.TEXT_OVERLAY_VIDEO)
                                else -> {
                                    val metadata = VideoMetadataLoader.load(path)
                                    proEditorViewModel.setVideoPath(path)
                                    proEditorViewModel.setDuration(metadata.durationMs ?: 0L)
                                    navigatorViewModel.navigateTo(AppRoute.PRO_EDITOR)
                                }
                            }
                        }
                    )

                    AppRoute.PICK_MULTIPLE_VIDEOS -> GalleryPickerScreen(
                        type = MediaItemType.VIDEO,
                        isMultiSelect = true,
                        onBack = {
                            sessionViewModel.clearPendingMergeInsertIndex()
                            navigatorViewModel.goBack()
                        },
                        onMediaPicked = { paths ->
                            val pendingInsertIndex = sessionState.pendingMergeInsertIndex
                            if (sessionState.multipleVideoPaths.isNotEmpty() && pendingInsertIndex != null) {
                                sessionViewModel.insertMultipleVideoPaths(pendingInsertIndex, paths)
                            } else {
                                sessionViewModel.setMultipleVideoPaths(paths)
                            }
                            navigatorViewModel.navigateTo(AppRoute.MERGE_VIDEO)
                        }
                    )

                    AppRoute.PICK_IMAGES -> GalleryPickerScreen(
                        type = MediaItemType.IMAGE,
                        isMultiSelect = true,
                        onBack = { navigatorViewModel.goBack() },
                        onMediaPicked = { paths ->
                            sessionViewModel.setImagePaths(paths)
                            navigatorViewModel.navigateTo(AppRoute.SLIDESHOW)
                        }
                    )

                    AppRoute.PICK_AUDIO -> PickAudioScreen(
                        onNavigate = navigatorViewModel::navigateTo,
                        onAudioPicked = { path ->
                            sessionViewModel.setSlideshowAudioPath(path)
                            navigatorViewModel.goBack()
                        }
                    )

                    AppRoute.CUT_VIDEO -> CutVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.COMPRESS_VIDEO -> CompressVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.MERGE_VIDEO -> MergeVideoScreen(
                        inputPaths = sessionState.multipleVideoPaths,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        },
                        onAddMoreAt = { insertIndex ->
                            sessionViewModel.setPendingMergeInsertIndex(insertIndex)
                            navigatorViewModel.navigateTo(AppRoute.PICK_MULTIPLE_VIDEOS)
                        },
                        onRemoveAt = sessionViewModel::removeMultipleVideoAt,
                        onMove = sessionViewModel::moveMultipleVideo
                    )

                    AppRoute.EXTRACT_AUDIO -> ExtractAudioScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.SLIDESHOW -> SlideshowScreen(
                        imagePaths = sessionState.imagePaths,
                        audioPath = sessionState.slideshowAudioPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        },
                        onAddMore = { navigatorViewModel.navigateTo(AppRoute.PICK_IMAGES) },
                        onPickAudio = { navigatorViewModel.navigateTo(AppRoute.PICK_AUDIO) },
                        onRemoveAudio = { sessionViewModel.clearSlideshowAudioPath() },
                        onRemoveAt = sessionViewModel::removeImageAt,
                        onMove = sessionViewModel::moveImage
                    )

                    AppRoute.ROTATE_VIDEO -> RotateVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.SPEED_VIDEO -> SpeedVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.CROP_VIDEO -> CropVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.FILTERS_VIDEO -> FiltersVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.REVERSE_VIDEO -> ReverseVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.STICKERS_VIDEO -> StickersVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.TEXT_OVERLAY_VIDEO -> TextOverlayVideoScreen(
                        inputPath = sessionState.singleVideoPath,
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        }
                    )

                    AppRoute.PRO_EDITOR -> ProEditorScreen(
                        onNavigate = navigatorViewModel::navigateTo,
                        onSubmitRequest = { request ->
                            sessionViewModel.setPendingRequest(request)
                            navigatorViewModel.navigateTo(AppRoute.PROCESSING)
                        },
                        viewModel = proEditorViewModel
                    )

                    AppRoute.PROCESSING -> {
                        ProcessingScreen(
                            sessionViewModel = sessionViewModel,
                            onNavigate = { route ->
                                if (route == AppRoute.RESULT) historyViewModel.loadHistory()
                                navigatorViewModel.navigateTo(route)
                            }
                        )
                    }

                    AppRoute.RESULT -> {
                        ResultScreen(
                            output = sessionState.lastOutput,
                            onNavigate = navigatorViewModel::navigateTo
                        )
                    }

                    AppRoute.VIDEO_PLAYER -> {
                        VideoPlayerScreen(
                            videoPath = sessionState.lastOutput?.path,
                            onBack = { navigatorViewModel.goBack() }
                        )
                    }

                    AppRoute.OUTPUT_HISTORY -> OutputHistoryScreen(
                        onNavigate = navigatorViewModel::navigateTo,
                        onOutputSelected = { output ->
                            sessionViewModel.setLastOutput(output)
                            navigatorViewModel.navigateTo(AppRoute.RESULT)
                        },
                        onPlayOutput = { output ->
                            sessionViewModel.setLastOutput(output)
                            navigatorViewModel.navigateTo(AppRoute.VIDEO_PLAYER)
                        }
                    )

                    AppRoute.SETTINGS -> SettingsScreen(
                        onNavigate = navigatorViewModel::navigateTo
                    )

                    AppRoute.FUTURE_TOOLS -> FutureToolsPlaceholderScreen(
                        onNavigate = navigatorViewModel::navigateTo,
                        onToolSelected = { route, target ->
                            sessionViewModel.setToolTarget(target)
                            navigatorViewModel.navigateTo(route)
                        }
                    )

                    AppRoute.COMING_SOON_FILTERS -> ComingSoonScreen("Filters", navigatorViewModel::navigateTo)
                    AppRoute.COMING_SOON_STICKERS -> ComingSoonScreen("Stickers", navigatorViewModel::navigateTo)
                    AppRoute.COMING_SOON_TEXT_OVERLAY -> ComingSoonScreen("Text Overlay", navigatorViewModel::navigateTo)
                    AppRoute.COMING_SOON_TIMELINE -> ComingSoonScreen("Timeline", navigatorViewModel::navigateTo)
                    AppRoute.COMING_SOON_TRANSITIONS -> ComingSoonScreen("Transitions", navigatorViewModel::navigateTo)
                    AppRoute.COMING_SOON_AUDIO_EDITOR -> ComingSoonScreen("Audio Editor", navigatorViewModel::navigateTo)

                    else -> { /* Fallback */ }
                }
            }
        }
    }
}
