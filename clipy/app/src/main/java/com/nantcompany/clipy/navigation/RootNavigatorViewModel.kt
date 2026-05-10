package com.nantcompany.clipy.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RootNavigatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NavigationState())
    val uiState: StateFlow<NavigationState> = _uiState.asStateFlow()

    fun navigateTo(route: AppRoute) {
        val current = _uiState.value
        if (current.currentRoute == route) return
        _uiState.value = current.copy(
            currentRoute = route,
            backStack = current.backStack + current.currentRoute
        )
    }

    fun replace(route: AppRoute) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }

    fun canGoBack(): Boolean = _uiState.value.backStack.isNotEmpty()

    fun goBack() {
        val current = _uiState.value
        val previous = current.backStack.lastOrNull() ?: return
        _uiState.value = current.copy(
            currentRoute = previous,
            backStack = current.backStack.dropLast(1)
        )
    }
}
