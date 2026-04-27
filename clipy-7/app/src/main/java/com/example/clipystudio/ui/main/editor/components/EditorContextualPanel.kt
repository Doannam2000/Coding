package com.example.clipystudio.ui.main.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clipystudio.ui.main.models.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun EditorContextualPanel(
    hasSelection: Boolean,
    onSplit: () -> Unit,
    onSpeed: () -> Unit,
    onVolume: () -> Unit,
    onAnimation: () -> Unit,
    onDelete: () -> Unit,
    onCrop: () -> Unit
) {
    if (!hasSelection) return

    val actions = listOf(
        EditAction("Split", Icons.Default.ContentCut, onSplit),
        EditAction("Speed", Icons.Default.Speed, onSpeed),
        EditAction("Volume", Icons.Default.VolumeUp, onVolume),
        EditAction("Animation", Icons.Default.AutoAwesome, onAnimation),
        EditAction("Delete", Icons.Default.Delete, onDelete),
        EditAction("Crop", Icons.Default.Crop, onCrop)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(EditorChromeSurfaceLow)
            .border(1.dp, EditorChromeBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                onClick = action.onClick,
                shape = RoundedCornerShape(16.dp),
                color = EditorChromeSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(action.icon, contentDescription = action.label, tint = EditorChromeMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(action.label, color = EditorChromeMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class EditAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)
