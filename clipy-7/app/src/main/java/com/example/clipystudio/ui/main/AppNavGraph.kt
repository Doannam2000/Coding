package com.example.clipystudio.ui.main

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHostState
import com.example.clipystudio.*
import com.example.clipystudio.data.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import com.example.clipystudio.ui.main.editor.*
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    currentKey: NavKey,
    appState: AppState,
    copy: Copy,
    viewModel: MainScreenViewModel,
    snackbarHostState: SnackbarHostState,
    isPlaybackLocked: Boolean,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    onExitRequested: () -> Unit,
    onLanguageFromSettings: (Boolean) -> Unit,
    languageFromSettings: Boolean
) {
    val scope = rememberCoroutineScope()
    
    when (currentKey) {
        Intro -> IntroScreen(
            copy = copy,
            onContinue = { 
                viewModel.completeIntro()
                onNavigate(Dashboard)
            },
            onSkip = {
                viewModel.completeIntro()
                onNavigate(Dashboard)
            }
        )
        
        Dashboard -> DashboardScreen(
            appState = appState,
            copy = copy,
            onCreate = { ratio -> 
                viewModel.createProject(ratio)
                onNavigate(Import)
            },
            onOpen = { id -> 
                viewModel.openProject(id)
                onNavigate(Editor)
            },
            onRename = viewModel::renameProject,
            onDuplicate = viewModel::duplicateProject,
            onDelete = viewModel::deleteProject,
            onSettings = { onNavigate(Settings) },
            onExit = onExitRequested
        )
        
        Import -> ImportScreen(
            appState = appState,
            copy = copy,
            snackbarHostState = snackbarHostState,
            onBack = onBack,
            onAddAsset = viewModel::addImportedAsset,
            onRemove = viewModel::removeImportedAsset,
            onAddToProject = {
                viewModel.addImportsToProject()
                onNavigate(Editor)
            }
        )
        
        Editor -> EditorScreen(
            appState = appState,
            copy = copy,
            onBack = onBack,
            onImport = { onNavigate(Import) },
            onExport = { onNavigate(Export) },
            viewModel = viewModel,
            isPlaybackLocked = isPlaybackLocked
        )
        
        Export -> ExportScreen(
            appState = appState,
            copy = copy,
            onBack = onBack,
            onDashboard = { 
                viewModel.clearExportResult()
                onNavigate(Dashboard)
            },
            viewModel = viewModel
        )
        
        Settings -> SettingsScreen(
            appState = appState,
            copy = copy,
            onBack = onBack,
            onLanguage = { 
                onLanguageFromSettings(true)
                onNavigate(Language)
            },
            onClearCache = {
                viewModel.clearCache()
                scope.launch { 
                    snackbarHostState.showSnackbar(
                        if (appState.languageCode == LanguageCode.Vi) "Da xoa tep tam. Media goc va video xuat khong bi xoa." 
                        else "Temporary files cleared. Original media and exported videos were not deleted."
                    ) 
                }
            },
            onExit = onExitRequested
        )
        
        Language -> LanguageScreen(
            selected = appState.languageCode,
            copy = copy,
            showBack = true,
            onBack = onBack,
            onSave = { language ->
                viewModel.setLanguage(language)
                if (languageFromSettings) {
                    onNavigate(Settings)
                    onLanguageFromSettings(false)
                } else {
                    onNavigate(Dashboard)
                }
            }
        )
    }
}