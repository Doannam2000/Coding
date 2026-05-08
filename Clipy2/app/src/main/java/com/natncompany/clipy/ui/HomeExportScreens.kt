package com.natncompany.clipy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.natncompany.clipy.R
import com.natncompany.clipy.editor.ClipyAppState
import com.natncompany.clipy.editor.HomeFeature
import com.natncompany.clipy.editor.ExportResolutionPreset
import com.natncompany.clipy.editor.exportWithMediaPipeline
import com.natncompany.media.MediaResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    features: List<HomeFeature>,
    projectName: String,
    clipCount: Int,
    durationLabel: String,
    hasProject: Boolean,
    onContinueProject: () -> Unit,
    onFeatureClick: (HomeFeature) -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1C20), Color(0xFF0B0D12))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B0D12)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
                .systemBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.home_welcome),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = projectName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color(0xFF252830),
                    border = BorderStroke(1.dp, Color(0xFF2A2D35))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Project Card
            if (hasProject) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContinueProject() },
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF5B8DEF),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_continue_editing),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.home_clip_count_duration, clipCount, durationLabel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Text(
                text = stringResource(R.string.home_create_new),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = features) { feature ->
                    FeatureItem(feature = feature, onClick = { onFeatureClick(feature) })
                }
            }

            // Bottom Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E2128),
                border = BorderStroke(1.dp, Color(0xFF2A2D35))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD700))
                        Text(
                            text = stringResource(R.string.home_ai_magic_effects),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun FeatureItem(feature: HomeFeature, onClick: () -> Unit) {
    val icon = when (feature.shortLabel) {
        "ED" -> Icons.Default.Edit
        "FX" -> Icons.Default.AutoAwesome
        "CV" -> Icons.Default.CropSquare
        "LY" -> Icons.Default.MusicNote
        "TP" -> Icons.Default.Dashboard
        "PP" -> Icons.Default.Layers
        else -> Icons.Default.Add
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E2128))
            .border(1.dp, Color(0xFF2A2D35), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF2A2D35)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = feature.title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private enum class ExportUiState {
    Idle,
    Running,
    Completed
}

@Composable
fun ExportScreen(
    appState: ClipyAppState,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportUiState by rememberSaveable { mutableStateOf(ExportUiState.Idle.name) }
    var exportProgress by rememberSaveable { mutableStateOf(0f) }
    var exportMessage by rememberSaveable { mutableStateOf(context.getString(R.string.export_ready)) }
    var exportError by rememberSaveable { mutableStateOf<String?>(null) }
    var exportOutputPath by rememberSaveable { mutableStateOf<String?>(null) }
    val isExporting = exportUiState == ExportUiState.Running.name
    val isCompleted = exportUiState == ExportUiState.Completed.name
    val isIdle = exportUiState == ExportUiState.Idle.name

    fun startExport() {
        if (isExporting) return
        if (appState.clips.isEmpty()) {
            exportError = context.getString(R.string.export_no_media)
            exportMessage = context.getString(R.string.export_failed)
            exportOutputPath = null
            return
        }
        exportUiState = ExportUiState.Running.name
        exportProgress = 0f
        exportMessage = context.getString(R.string.export_preparing)
        exportError = null
        exportOutputPath = null
        scope.launch {
            try {
                when (val result = appState.exportWithMediaPipeline(
                    context = context,
                    onProgress = { progress ->
                        exportProgress = progress.progressPercent.coerceIn(0, 100) / 100f
                        exportMessage = progress.message
                    }
                )) {
                    is MediaResult.Success -> {
                        exportProgress = 1f
                        exportMessage = context.getString(R.string.export_saved_to_movies)
                        exportOutputPath = result.value
                        exportUiState = ExportUiState.Completed.name
                    }
                    is MediaResult.Failure -> {
                        exportError = result.error.message
                        exportMessage = context.getString(R.string.export_failed)
                        exportUiState = ExportUiState.Completed.name
                    }
                }
            } catch (_: CancellationException) {
                exportError = context.getString(R.string.export_cancelled)
                exportMessage = context.getString(R.string.export_cancelled)
                exportUiState = ExportUiState.Idle.name
            } catch (_: Throwable) {
                exportError = context.getString(R.string.export_unexpected_error)
                exportMessage = context.getString(R.string.export_failed)
                exportUiState = ExportUiState.Completed.name
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0D12)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackHome) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.export_back), tint = Color.White)
                }
                Text(
                    text = stringResource(R.string.export_project_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isIdle) {
                Text(
                    text = stringResource(R.string.export_settings),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2128),
                    border = BorderStroke(1.dp, Color(0xFF2A2D35))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.export_resolution),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportResolutionPreset.entries.forEach { preset ->
                                val selected = appState.exportResolutionPreset == preset
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { appState.updateExportResolution(preset) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) Color(0xFF5B8DEF) else Color(0xFF2A2D35),
                                    border = if (selected) null else BorderStroke(1.dp, Color(0xFF2A2D35))
                                ) {
                                    Text(
                                        text = preset.label,
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E2128).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF2A2D35))
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF5B8DEF))
                        Text(
                            text = stringResource(R.string.export_resolution_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { startExport() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B8DEF))
                ) {
                    Text(
                        text = stringResource(R.string.export_start),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.size(160.dp),
                            strokeWidth = 12.dp,
                            color = Color(0xFF5B8DEF),
                            trackColor = Color(0xFF1E2128)
                        )
                        Text(
                            text = "${(exportProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = exportMessage,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    exportOutputPath?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    exportError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFF6B6B),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { if (!isExporting) onBackHome() },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF2A2D35))
                    ) {
                        Text(
                            text = if (exportError == null && exportOutputPath != null) stringResource(R.string.export_done) else stringResource(R.string.export_back),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    if (isCompleted) {
                        Button(
                            onClick = { startExport() },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B8DEF))
                        ) {
                            Text(
                                text = stringResource(R.string.export_again),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2D35))
                        ) {
                            Text(
                                text = stringResource(R.string.exporting),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
