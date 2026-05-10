package com.nantcompany.clipy.future

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.navigation.AppRoute

data class FutureToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun FutureToolsPlaceholderScreen(
    onNavigate: (AppRoute) -> Unit
) {
    val items = listOf(
        FutureToolItem("Filters", "Adjust look and tone", Icons.Filled.Star),
        FutureToolItem("Stickers", "Add expressive overlays", Icons.Filled.Settings),
        FutureToolItem("Text Overlay", "Place animated text", Icons.AutoMirrored.Filled.ArrowForward),
        FutureToolItem("Crop", "Reframe your scene", Icons.Filled.Build),
        FutureToolItem("Rotate", "Fix orientation quickly", Icons.Filled.Build),
        FutureToolItem("Speed", "Control clip pace", Icons.Filled.Star),
        FutureToolItem("Effects", "Apply visual FX", Icons.Filled.Settings),
        FutureToolItem("GPU Preview", "Smoother realtime preview", Icons.Filled.PlayArrow),
        FutureToolItem("Timeline Editor", "Multi-track editing", Icons.Filled.PlayArrow)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("More tools coming soon", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Clipy will expand into advanced editing tools in upcoming updates.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF9FB0C4)
        )

        items.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.72f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2029))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = Color(0xFF7CA6F0)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB9C7D9))
                        Text("Disabled · Coming soon", style = MaterialTheme.typography.labelSmall, color = Color(0xFF7CA6F0))
                    }
                }
            }
        }

        Button(onClick = { onNavigate(AppRoute.HOME) }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home")
        }
    }
}
