package com.nantcompany.clipy.home.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.home.components.HeaderRow
import com.nantcompany.clipy.home.components.HeroCard
import com.nantcompany.clipy.home.components.HomeToolCard
import com.nantcompany.clipy.home.model.ToolCardModel
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun StudioPage(
    onNavigate: (AppRoute) -> Unit,
    onToolSelected: (AppRoute, ToolTarget?) -> Unit
) {
    val tools = listOf(
        ToolCardModel("Cut", Icons.Default.Build, Color(0xFFFF6B9A), AppRoute.PICK_VIDEO, ToolTarget.CUT),
        ToolCardModel("Compress", Icons.Default.Star, Color(0xFF22D3EE), AppRoute.PICK_VIDEO, ToolTarget.COMPRESS, pro = true),
        ToolCardModel("Merge", Icons.AutoMirrored.Filled.ArrowForward, Color(0xFF60A5FA), AppRoute.PICK_MULTIPLE_VIDEOS, ToolTarget.MERGE),
        ToolCardModel("Crop", Icons.Default.Build, Color(0xFF4ADE80), AppRoute.PICK_VIDEO, ToolTarget.CROP),
        ToolCardModel("Speed", Icons.Default.PlayArrow, Color(0xFFFFB224), AppRoute.PICK_VIDEO, ToolTarget.SPEED),
        ToolCardModel("Filters", Icons.Default.Star, Color(0xFFF472B6), AppRoute.PICK_VIDEO, ToolTarget.FILTERS),
        ToolCardModel("Reverse", Icons.Default.Settings, Color(0xFF818CF8), AppRoute.PICK_VIDEO, ToolTarget.REVERSE),
        ToolCardModel("Extract\nAudio", Icons.Default.Star, Color(0xFFC084FC), AppRoute.PICK_VIDEO, ToolTarget.EXTRACT_AUDIO),
        ToolCardModel("Slideshow", Icons.Default.PlayArrow, Color(0xFF38BDF8), AppRoute.PICK_IMAGES, ToolTarget.SLIDESHOW),
        ToolCardModel("Rotate", Icons.Default.Build, Color(0xFFFB923C), AppRoute.PICK_VIDEO, ToolTarget.ROTATE),
        ToolCardModel("More", Icons.Default.Settings, Color(0xFF94A3B8), AppRoute.FUTURE_TOOLS)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClipyDesignTokens.screenPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeaderRow() }
        item { HeroCard(onPickVideo = { onToolSelected(AppRoute.PICK_VIDEO, null) }) }
        item {
            Text(
                "Quick Tools",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        items((tools.indices step 2).toList()) { startIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val first = tools[startIndex]
                HomeToolCard(
                    title = first.title,
                    icon = first.icon,
                    color = first.accent,
                    onClick = { onToolSelected(first.route, first.target) },
                    modifier = Modifier.weight(1f)
                )

                val secondIndex = startIndex + 1
                if (secondIndex < tools.size) {
                    val second = tools[secondIndex]
                    HomeToolCard(
                        title = second.title,
                        icon = second.icon,
                        color = second.accent,
                        onClick = { onToolSelected(second.route, second.target) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}
