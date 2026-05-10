package com.nantcompany.clipy.navigation

data class NavigationState(
    val currentRoute: AppRoute = AppRoute.SPLASH,
    val backStack: List<AppRoute> = emptyList()
)
