package com.example.clipystudio

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.data.DefaultTempFileManager
import com.example.clipystudio.ui.main.MainScreen
import com.example.clipystudio.ui.main.MainScreenViewModel
import java.io.File

@Composable
fun MainNavigation(repository: DataRepository) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(Main)
    val tempFileManager =
        remember(context) { DefaultTempFileManager(File(context.cacheDir, "exports")) }
    val mainViewModel: MainScreenViewModel =
        viewModel { MainScreenViewModel(repository, tempFileManager) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Main> {
                    MainScreen(
                        onItemClick = { navKey -> backStack.add(navKey) },
                        modifier = Modifier
                            .safeDrawingPadding(),
                        viewModel = mainViewModel
                    )
                }
            },
    )
}
