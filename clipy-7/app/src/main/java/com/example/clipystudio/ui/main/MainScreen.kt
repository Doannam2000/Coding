package com.example.clipystudio.ui.main

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipystudio.*
import com.example.clipystudio.data.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import androidx.navigation3.runtime.NavKey
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel? = null,
) {
    val context = LocalContext.current
    val resolvedViewModel = viewModel ?: viewModel {
        MainScreenViewModel(
            tempFileManager = DefaultTempFileManager(
                File(context.cacheDir, "exports")
            )
        )
    }
    val uiState by resolvedViewModel.uiState.collectAsStateWithLifecycle()
    
    when (val state = uiState) {
        MainScreenUiState.Loading -> LoadingSurface(modifier, LanguageCode.En)
        is MainScreenUiState.Error -> ErrorSurface(state.throwable.message.orEmpty(), modifier)
        is MainScreenUiState.Success -> ClipyStudioApp(
            state.appState,
            state.editorUiState,
            resolvedViewModel,
            modifier
        )
    }
}

@Composable
internal fun ClipyStudioApp(
    appState: AppState,
    editorUiState: com.example.clipystudio.editor.model.EditorUiState,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val backstack = remember { mutableStateListOf<NavKey>(Dashboard) }
    var languageFromSettings by remember { mutableStateOf(false) }
    var exitRequested by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val copy = copyFor(appState.languageCode)
    val shareEvent by viewModel.shareEvent.collectAsStateWithLifecycle()
    val isPlaybackLocked = editorUiState.panelState.isPlaybackLocked

    val currentKey = backstack.last()

    LaunchedEffect(shareEvent) {
        val event = shareEvent ?: return@LaunchedEffect
        val shareUri = event.uri.toShareUri(context)
        if (shareUri.scheme != "content" || event.mimeType != "video/mp4") {
            snackbarHostState.showSnackbar(if (appState.languageCode == LanguageCode.Vi) "Video xuat chua san sang de chia se an toan." else "The exported video is not ready to share safely.")
            viewModel.consumeShareEvent()
            return@LaunchedEffect
        }
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    event.chooserTitle
                )
            )
        }
        viewModel.consumeShareEvent()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = StudioBackground
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AppNavGraph(
                currentKey = currentKey,
                appState = appState,
                copy = copy,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                isPlaybackLocked = isPlaybackLocked,
                onNavigate = { key ->
                    if (backstack.last() != key) backstack.add(key)
                },
                onBack = {
                    if (backstack.size > 1) backstack.removeAt(backstack.size - 1)
                },
                onExitRequested = { exitRequested = true },
                onLanguageFromSettings = { languageFromSettings = it },
                languageFromSettings = languageFromSettings
            )

            if (exitRequested) {
                AlertDialog(
                    onDismissRequest = { exitRequested = false },
                    title = { Text(if (appState.languageCode == LanguageCode.Vi) "Thoat Clipy Studio?" else "Exit Clipy Studio?") },
                    text = { Text(if (appState.languageCode == LanguageCode.Vi) "Ban co the quay lai du an da tu dong luu bat cu luc nao." else "Autosaved projects will be available when you return.") },
                    confirmButton = {
                        TextButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                            Text(
                                copy.exit,
                                color = StudioDanger
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            exitRequested = false
                        }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") }
                    },
                )
            }
        }
    }
}

internal fun String.toShareUri(context: Context): Uri {
    val parsed = Uri.parse(this)
    if (parsed.scheme == "content") return parsed
    if (parsed.scheme == "file") {
        val path = parsed.path ?: return parsed
        val file = java.io.File(path)
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrDefault(parsed)
    }
    return parsed
}
