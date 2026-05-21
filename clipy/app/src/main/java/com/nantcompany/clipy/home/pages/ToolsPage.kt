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
import com.nantcompany.clipy.home.components.HomeToolCard
import com.nantcompany.clipy.home.model.ToolCardModel
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun ToolsPage(onToolSelected: (AppRoute, ToolTarget?) -> Unit) {
    val tools = listOf(
        ToolCardModel("Cut", Icons.Default.Build, Color(0xFFFF6B9A), AppRoute.PICK_VIDEO, ToolTarget.CUT),
        ToolCardModel("Compress", Icons.Default.Star, Color(0xFF22D3EE), AppRoute.PICK_VIDEO, ToolTarget.COMPRESS),
        ToolCardModel("Merge", Icons.AutoMirrored.Filled.ArrowForward, Color(0xFF60A5FA), AppRoute.PICK_MULTIPLE_VIDEOS, ToolTarget.MERGE),
        ToolCardModel("Extract Audio", Icons.Default.Star, Color(0xFFC084FC), AppRoute.PICK_VIDEO, ToolTarget.EXTRACT_AUDIO),
        ToolCardModel("Rotate", Icons.Default.Build, Color(0xFFF97316), AppRoute.PICK_VIDEO, ToolTarget.ROTATE),
        ToolCardModel("Speed", Icons.Default.Build, Color(0xFF8B5CF6), AppRoute.PICK_VIDEO, ToolTarget.SPEED),
        ToolCardModel("Crop", Icons.Default.Build, Color(0xFF10B981), AppRoute.PICK_VIDEO, ToolTarget.CROP),
        ToolCardModel("Filters", Icons.Default.Build, Color(0xFF3B82F6), AppRoute.PICK_VIDEO, ToolTarget.FILTERS),
        ToolCardModel("Reverse", Icons.Default.Build, Color(0xFFEF4444), AppRoute.PICK_VIDEO, ToolTarget.REVERSE),
        ToolCardModel("Stickers", Icons.Default.Star, Color(0xFFFACC15), AppRoute.PICK_VIDEO, ToolTarget.STICKERS),
        ToolCardModel("Text Overlay", Icons.AutoMirrored.Filled.ArrowForward, Color(0xFF818CF8), AppRoute.PICK_VIDEO, ToolTarget.TEXT_OVERLAY)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClipyDesignTokens.screenPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HeaderRow() }
        item {
            Text("Tools", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        }

        items((tools.indices step 2).toList()) { startIndex ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val first = tools[startIndex]
                HomeToolCard(
                    title = first.title,
                    icon = first.icon,
                    color = first.accent,
                    onClick = { onToolSelected(first.route, first.target) },
                    modifier = Modifier.weight(1f)
                )
                val second = tools.getOrNull(startIndex + 1)
                if (second != null) {
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
