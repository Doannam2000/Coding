package com.nantcompany.clipy.future

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

data class FutureToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: AppRoute,
    val target: ToolTarget? = null,
    val active: Boolean
)

@Composable
fun FutureToolsPlaceholderScreen(
    onNavigate: (AppRoute) -> Unit,
    onToolSelected: (AppRoute, ToolTarget?) -> Unit
) {
    val items = listOf(
        FutureToolItem("Filters", "Adjust look and tone", Icons.Filled.Star, AppRoute.PICK_VIDEO, ToolTarget.FILTERS, true),
        FutureToolItem("Stickers", "Add expressive overlays", Icons.Filled.Settings, AppRoute.PICK_VIDEO, ToolTarget.STICKERS, true),
        FutureToolItem("Text Overlay", "Place animated text", Icons.AutoMirrored.Filled.ArrowForward, AppRoute.PICK_VIDEO, ToolTarget.TEXT_OVERLAY, true),
        FutureToolItem("Crop", "Reframe your scene", Icons.Filled.Build, AppRoute.PICK_VIDEO, ToolTarget.CROP, true),
        FutureToolItem("Rotate", "Fix orientation quickly", Icons.Filled.Build, AppRoute.PICK_VIDEO, ToolTarget.ROTATE, true),
        FutureToolItem("Speed", "Control clip pace", Icons.Filled.Star, AppRoute.PICK_VIDEO, ToolTarget.SPEED, true),
        FutureToolItem("Effects", "Apply visual FX", Icons.Filled.Settings, AppRoute.COMING_SOON_FILTERS, null, false),
        FutureToolItem("Timeline", "Multi-track editing", Icons.Filled.PlayArrow, AppRoute.COMING_SOON_TIMELINE, null, false),
        FutureToolItem("Transitions", "Smooth clip changes", Icons.Filled.PlayArrow, AppRoute.COMING_SOON_TRANSITIONS, null, false)
    )

    ClipyScaffold(
        title = "Advanced Tools",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Advanced features for professional creators.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClipyDesignTokens.secondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (item.active) 1f else 0.5f)
                        .clickable { 
                            if (item.active) {
                                if (item.target != null) onToolSelected(item.route, item.target)
                                else onNavigate(item.route)
                            }
                        },
                    shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                    colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (item.active) ClipyDesignTokens.cardBorder else Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (item.active) ClipyDesignTokens.primaryAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material3.Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (item.active) ClipyDesignTokens.primaryAccent else ClipyDesignTokens.secondaryText,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText)
                            if (!item.active) {
                                Text("Coming soon", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                ClipyPrimaryButton(
                    label = "Back to Home",
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    onClick = { onNavigate(AppRoute.HOME) }
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
