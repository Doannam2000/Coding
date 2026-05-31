package com.nantcompany.clipy.picker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyLoadingState
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File
import kotlin.math.roundToInt

@Composable
fun GalleryPickerScreen(
    type: MediaItemType,
    isMultiSelect: Boolean,
    onMediaPicked: (List<String>) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryPickerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Android 14+ Partial Access Support
    val permissions = remember(type) {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (type == MediaItemType.VIDEO) add(Manifest.permission.READ_MEDIA_VIDEO)
                else add(Manifest.permission.READ_MEDIA_IMAGES)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                }
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    var permissionState by remember {
        mutableStateOf(checkDetailedPermission(context, type))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionState = checkDetailedPermission(context, type)
        // If we have at least partial access, load media
        if (permissionState != PermissionState.DENIED) {
            viewModel.loadMedia(context, type, isMultiSelect)
        }
    }

    LaunchedEffect(Unit) {
        if (permissionState != PermissionState.DENIED) {
            viewModel.loadMedia(context, type, isMultiSelect)
        }
    }

    ClipyScaffold(
        title = if (type == MediaItemType.VIDEO) "Select Videos" else "Select Images",
        onBackClick = onBack,
        actions = {
            if (uiState.selectedItems.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearSelection() }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (permissionState == PermissionState.DENIED) {
                PermissionRequestContent(
                    message = "Clipy needs access to your gallery to pick ${if (type == MediaItemType.VIDEO) "videos" else "images"} for editing.",
                    onGrantClick = { launcher.launch(permissions.toTypedArray()) }
                )
            } else if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ClipyLoadingState(message = "Scanning gallery...")
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp, 4.dp, 4.dp, 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Partial Access Banner
                        if (permissionState == PermissionState.PARTIAL) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                PartialAccessBanner(onManageClick = { launcher.launch(permissions.toTypedArray()) })
                            }
                        }

                        if (uiState.mediaItems.isEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                    Text("No media found on device.", color = ClipyDesignTokens.secondaryText)
                                }
                            }
                        } else {
                            items(uiState.mediaItems) { item ->
                                val isSelected = uiState.selectedItems.any { it.uri == item.uri }
                                val selectionIndex = uiState.selectedItems.indexOfFirst { it.uri == item.uri }
                                
                                GalleryItem(
                                    item = item,
                                    isSelected = isSelected,
                                    selectionIndex = if (isMultiSelect) selectionIndex else -1,
                                    onClick = { viewModel.toggleSelection(item) }
                                )
                            }
                        }
                    }

                    // Floating selection bar
                    if (uiState.selectedItems.isNotEmpty()) {
                        SelectionActionBar(
                            selectedCount = uiState.selectedItems.size,
                            isMultiSelect = isMultiSelect,
                            onConfirm = {
                                val paths = uiState.selectedItems.mapNotNull { item ->
                                    runCatching {
                                        MediaFileUtils.importUriToLocalPath(
                                            context = context,
                                            uri = item.uri,
                                            folderName = "imports/${if (item.type == MediaItemType.VIDEO) "video" else "images"}",
                                            defaultExtension = if (item.type == MediaItemType.VIDEO) "mp4" else "jpg"
                                        )
                                    }.getOrNull()
                                }
                                onMediaPicked(paths)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class PermissionState { DENIED, PARTIAL, FULL }

private fun checkDetailedPermission(context: Context, type: MediaItemType): PermissionState {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val mediaPermission = if (type == MediaItemType.VIDEO) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_MEDIA_IMAGES
        val hasFull = ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
        
        if (hasFull) return PermissionState.FULL
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasPartial = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            if (hasPartial) return PermissionState.PARTIAL
        }
        return PermissionState.DENIED
    } else {
        val hasStore = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return if (hasStore) PermissionState.FULL else PermissionState.DENIED
    }
}

@Composable
private fun PartialAccessBanner(onManageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.primaryAccent.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.primaryAccent.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ClipyDesignTokens.primaryAccent, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Partial Access", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Clipy only sees selected videos.", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
            }
            TextButton(
                onClick = onManageClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = ClipyDesignTokens.primaryAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add More", fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(
    message: String,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = ClipyDesignTokens.primaryAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                Text(
                    "Gallery Access",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClipyDesignTokens.secondaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                ClipyPrimaryButton(
                    label = "Grant Access",
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    onClick = onGrantClick
                )
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    isMultiSelect: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(64.dp),
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Surface(
                    color = ClipyDesignTokens.primaryAccent,
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$selectedCount",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    if (selectedCount > 1) "Items selected" else "Item selected",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            ClipyPrimaryButton(
                label = if (isMultiSelect) "Continue" else "Select",
                modifier = Modifier.fillMaxHeight().padding(vertical = 6.dp).width(120.dp),
                onClick = onConfirm
            )
        }
    }
}

@Composable
private fun GalleryItem(
    item: MediaItemModel,
    isSelected: Boolean,
    selectionIndex: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        startY = 100f
                    )
                )
        )

        if (item.type == MediaItemType.VIDEO) {
            Text(
                text = formatDurationShort(item.durationMs ?: 0L),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, ClipyDesignTokens.primaryAccent, RoundedCornerShape(8.dp))
                    .background(ClipyDesignTokens.primaryAccent.copy(alpha = 0.2f))
            )
            
            Surface(
                shape = CircleShape,
                color = ClipyDesignTokens.primaryAccent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selectionIndex >= 0) {
                        Text(
                            "${selectionIndex + 1}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationShort(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
