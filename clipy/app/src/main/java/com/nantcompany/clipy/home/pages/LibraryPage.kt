package com.nantcompany.clipy.home.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.export.output.OutputMedia
import com.nantcompany.clipy.home.components.ExportItemCard
import com.nantcompany.clipy.home.components.HeaderRow
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun LibraryPage(
    recentExports: List<OutputMedia>,
    onRecentClick: (OutputMedia) -> Unit
) {
    val list = recentExports

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = ClipyDesignTokens.screenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderRow() }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recent Exports",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF16233D),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { }
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB6C5E2))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LibraryFilterChip("ALL", active = true)
                LibraryFilterChip("Videos", active = false)
                LibraryFilterChip("Audio", active = false)
            }
        }

        if (list.isEmpty()) {
            item {
                Text(
                    text = "Không có item",
                    color = Color(0xFF8FA2C8),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(list.take(3)) { output ->
                ExportItemCard(output = output, onClick = { onRecentClick(output) })
            }
        }

        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

@Composable
private fun LibraryFilterChip(text: String, active: Boolean) {
    val bg = if (active) Color(0xFF2E2A50) else Color(0xFF16233A)
    val fg = if (active) Color(0xFFE3D7FF) else Color(0xFF8FA2C8)
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
