package com.nantcompany.clipy.home.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.navigation.AppRoute

data class ToolCardModel(
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val route: AppRoute,
    val target: ToolTarget? = null,
    val pro: Boolean = false
)
