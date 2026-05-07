package com.nantcompany.clipy.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RootNavigatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NavigationState())
    val uiState: StateFlow<NavigationState> = _uiState.asStateFlow()

    fun navigateTo(route: AppRoute) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }
}
