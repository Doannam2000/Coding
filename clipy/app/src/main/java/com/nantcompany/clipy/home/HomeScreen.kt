package com.nantcompany.clipy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nantcompany.clipy.app.ToolTarget
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit,
    onToolSelected: (AppRoute, ToolTarget?) -> Unit,
    recentExports: List<OutputMedia>,
    onRecentClick: (OutputMedia) -> Unit
) {
    ClipyScaffold(
        title = "Clipy Studio",
        showTopBar = false
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Section: Start New Project
            item(span = { GridItemSpan(2) }) {
                HeroProjectCard(onClick = { onNavigate(AppRoute.PICK_VIDEO) })
            }

            // 2. Pro Tool Grid
            item(span = { GridItemSpan(2) }) {
                Text(
                    "Professional Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(getHomeTools()) { tool ->
                HomeToolCard(
                    title = tool.title,
                    icon = tool.icon,
                    description = tool.description,
                    color = tool.color,
                    onClick = { onToolSelected(tool.route, tool.target) }
                )
            }

            // 3. Recent Exports
            if (recentExports.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Recent Works",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(recentExports.take(4), span = { GridItemSpan(2) }) { output ->
                    RecentExportItem(output, onClick = { onRecentClick(output) })
                }
            }
        }
    }
}

@Composable
private fun HeroProjectCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(ClipyDesignTokens.heroCorner),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(ClipyDesignTokens.primaryAccent, Color(0xFF4C1D95))
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Icon(Icons.Default.AddCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Start New Edit", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Tap to pick a video and enter Studio", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HomeToolCard(title: String, icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(130.dp),
        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.bgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.borderLight)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(description, color = ClipyDesignTokens.textSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RecentExportItem(output: OutputMedia, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.bgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.borderLight)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, null, tint = ClipyDesignTokens.primaryAccent)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(output.fileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(output.operation.uppercase(), color = ClipyDesignTokens.primaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = ClipyDesignTokens.textMuted)
        }
    }
}

private data class HomeTool(val id: String, val title: String, val description: String, val icon: ImageVector, val color: Color, val route: AppRoute, val target: ToolTarget?)

private fun getHomeTools() = listOf(
    HomeTool("cut", "Trim & Cut", "Precise duration control", Icons.Default.Build, Color(0xFFB76DFF), AppRoute.PICK_VIDEO, ToolTarget.CUT),
    HomeTool("compress", "Compress", "Small size, high quality", Icons.Default.ThumbUp, Color(0xFF22D3EE), AppRoute.PICK_VIDEO, ToolTarget.COMPRESS),
    HomeTool("merge", "Merge", "Join multiple clips", Icons.AutoMirrored.Filled.List, Color(0xFF34D399), AppRoute.PICK_MULTIPLE_VIDEOS, ToolTarget.MERGE),
    HomeTool("slideshow", "Slideshow", "Photos to video", Icons.Default.Face, Color(0xFFFACC15), AppRoute.PICK_IMAGES, ToolTarget.SLIDESHOW),
    HomeTool("audio", "Extract Audio", "Video to MP3", Icons.Default.Search, Color(0xFFF87171), AppRoute.PICK_VIDEO, ToolTarget.EXTRACT_AUDIO),
    HomeTool("reverse", "Reverse", "Time travel effect", Icons.Default.Refresh, Color(0xFFFB923C), AppRoute.PICK_VIDEO, ToolTarget.REVERSE)
)
