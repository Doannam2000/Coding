package com.example.clipystudio

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.clipystudio.data.DataRepository
import com.example.clipystudio.ui.main.MainScreen
import com.example.clipystudio.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(repository: DataRepository) {
  val backStack = rememberNavBackStack(Main)
  val mainViewModel: MainScreenViewModel = viewModel { MainScreenViewModel(repository) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(16.dp), viewModel = mainViewModel)
        }
      },
  )
}
