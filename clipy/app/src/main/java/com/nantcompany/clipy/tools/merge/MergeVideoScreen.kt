package com.nantcompany.clipy.tools.merge

import android.media.MediaMetadataRetriever
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.edit.common.TransitionType
import com.nantcompany.clipy.edit.tools.merge.MergeRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import java.util.Locale

@Composable
fun MergeVideoScreen(
    inputPaths: List<String>,
    onRemoveAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onAddMore: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    viewModel: MergeVideoViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTransition by remember { mutableStateOf(TransitionType.CROSSFADE) }
    val uiState by viewModel.uiState.collectAsState()
    val clipSpecs = remember(inputPaths) { inputPaths.map(::readClipSpec) }
    
    val distinctResolutions = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            if (width != null && height != null && width > 0 && height > 0) "${width}x${height}" else null
        }.toSet()
    }
    
    val distinctOrientations = remember(clipSpecs) {
        clipSpecs.mapNotNull { spec ->
            val width = spec.width
            val height = spec.height
            when {
                width == null || height == null || width <= 0 || height <= 0 -> null
                width > height -> "Landscape"
                width < height -> "Portrait"
                else -> "Square"
            }
        }.toSet()
    }
    
    val showMixedWarning = distinctResolutions.size > 1 || distinctOrientations.size > 1
    var warningExpanded by remember { mutableStateOf(false) }

    if (!showMixedWarning) {
        warningExpanded = false
    }

    ClipyScaffold(
        title = "Merge Videos",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showMixedWarning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03).copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF97316))
                                    Text("Mixed formats detected", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "Clips have different orientation or resolution. Clipy will fit clips automatically for a safe export.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFED7AA)
                                )
                                TextButton(onClick = { warningExpanded = !warningExpanded }) {
                                    Text(if (warningExpanded) "Hide details" else "Show details", color = Color(0xFFF97316), fontWeight = FontWeight.Bold)
                                }
                                if (warningExpanded) {
                                    Text("Resolutions: ${distinctResolutions.ifEmpty { setOf("Unknown") }.joinToString()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFED7AA))
                                    Text("Orientations: ${distinctOrientations.ifEmpty { setOf("Unknown") }.joinToString()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFED7AA))
                                }
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ClipySectionTitle(text = "Clip Order (${inputPaths.size})")
                        TextButton(onClick = onAddMore) {
                            Text("Add More", color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                itemsIndexed(inputPaths) { index, path ->
                    val spec = clipSpecs.getOrNull(index)
                    val file = File(path)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ClipyDesignTokens.primaryAccent, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                spec?.let {
                                    Text(
                                        "${it.width ?: "?"}x${it.height ?: "?"} • ${formatDurationShort(it.durationMs ?: 0L)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ClipyDesignTokens.secondaryText
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { if (index > 0) onMove(index, index - 1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f))
                                }
                                IconButton(onClick = { if (index < inputPaths.size - 1) onMove(index, index + 1) }, enabled = index < inputPaths.size - 1) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < inputPaths.size - 1) Color.White else Color.White.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipySectionTitle(text = "Transition Effect")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                            colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                        ) {
                            LazyRow(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(TransitionType.entries) { transition ->
                                    val isSelected = selectedTransition == transition
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) ClipyDesignTokens.primaryAccent.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { selectedTransition = transition }
                                            .padding(8.dp)
                                            .width(70.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(44.dp),
                                            shape = CircleShape,
                                            color = if (isSelected) ClipyDesignTokens.primaryAccent else Color.White.copy(alpha = 0.05f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.Black else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            transition.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else ClipyDesignTokens.secondaryText,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE60A0A12))
                    .padding(16.dp)
            ) {
                ClipyPrimaryButton(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    label = "Merge ${inputPaths.size} Clips",
                    enabled = inputPaths.size >= 2,
                    onClick = {
                        val request = MergeRequest(
                            inputPaths = inputPaths,
                            outputPath = MediaFileUtils.createOutputPath(context, "merge", "mp4"),
                            transition = selectedTransition.ffmpegName,
                            transitionDurationMs = 1000L
                        )
                        onSubmitRequest(ProcessingRequest.Merge(request))
                    }
                )
            }
        }
    }
}

private data class ClipSpec(
    val width: Int?,
    val height: Int?,
    val durationMs: Long?
)

private fun readClipSpec(path: String): ClipSpec {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        retriever.release()
        
        if (rot == 90 || rot == 270) {
            ClipSpec(h, w, d)
        } else {
            ClipSpec(w, h, d)
        }
    }.getOrDefault(ClipSpec(null, null, null))
}

private fun formatDurationShort(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
