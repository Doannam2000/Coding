package com.nantcompany.clipy.picker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.nantcompany.clipy.design.ClipyEmptyState
import com.nantcompany.clipy.design.ClipyLoadingState
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipyTextField
import com.nantcompany.clipy.design.ClipyTopBar
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.util.Locale

import com.nantcompany.clipy.design.ClipyScaffold

@Composable
fun PickAudioScreen(
    onAudioPicked: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: PickAudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            viewModel.loadAudios(context)
        }
    }

    var playingUri by remember { mutableStateOf<Uri?>(null) }
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    LaunchedEffect(playingUri) {
        if (playingUri != null) {
            player.setMediaItem(MediaItem.fromUri(playingUri!!))
            player.prepare()
            player.play()
        } else {
            player.stop()
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.loadAudios(context)
        }
    }

    val filteredAudio = uiState.audioList.filter {
        it.displayName.contains(uiState.searchQuery, ignoreCase = true)
    }

    ClipyScaffold(
        title = "Pick Audio",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        if (!permissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ClipyEmptyState(
                    title = "Permission Required",
                    message = "Media access is needed to pick audio files.",
                    icon = Icons.Default.Search
                )

                ClipyPrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Grant Permission",
                    onClick = { launcher.launch(permission) }
                )
                
                ClipySecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Open Settings",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ClipyTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = "Search audio files...",
                    modifier = Modifier.padding(top = 16.dp)
                )

                if (uiState.isLoading) {
                    ClipyLoadingState(message = "Scanning media...")
                } else if (filteredAudio.isEmpty()) {
                    ClipyEmptyState(
                        title = if (uiState.searchQuery.isEmpty()) "No audio found" else "No results",
                        message = if (uiState.searchQuery.isEmpty()) "Add some music or recordings to your device." else "Try a different search term.",
                        icon = Icons.Default.Search
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filteredAudio) { item ->
                            AudioPickerItem(
                                item = item,
                                isPlaying = playingUri == item.uri,
                                onPreviewClick = {
                                    playingUri = if (playingUri == item.uri) null else item.uri
                                },
                                onClick = { 
                                    item.uri.path?.let { onAudioPicked(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPickerItem(
    item: MediaItemModel,
    isPlaying: Boolean,
    onPreviewClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onPreviewClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = ClipyDesignTokens.primaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.durationMs?.let {
                    Text(
                        text = formatDuration(it),
                        color = ClipyDesignTokens.secondaryText,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
