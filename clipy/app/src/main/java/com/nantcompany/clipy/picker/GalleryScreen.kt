package com.nantcompany.clipy.picker

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.navigation.AppRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val durationMs: Long
)

@Composable
fun GalleryScreen(
    onVideoPicked: (String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkPermissions(context)) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<MediaItem?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results.values.all { it }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            mediaItems = loadVideos(context)
        } else {
            launcher.launch(permissions)
        }
    }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Gallery Video",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            // Filters & Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF06B6D4), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                
                Text("STOCK VIDEOS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFF06B6D4))
            }

            // Tabs: Videos / Photos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Box(modifier = Modifier.weight(1f).padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Text("Videos", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.matchParentSize().padding(top = 24.dp).height(2.dp).background(Color(0xFF06B6D4)))
                }
                Box(modifier = Modifier.weight(1f).padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Text("Photos", color = Color.Gray)
                }
                Box(modifier = Modifier.weight(1f).padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Text("Live Photos", color = Color.Gray)
                }
            }

            // Grid
            if (!hasPermission) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Permission required to view videos", color = Color.Gray)
                }
            } else if (mediaItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No videos found", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(mediaItems) { item ->
                        val isSelected = selectedItem?.id == item.id
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF06B6D4) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedItem = if (isSelected) null else item
                                }
                        ) {
                            AsyncThumbnail(media = item, modifier = Modifier.fillMaxSize())
                            
                            // Duration Overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatDuration(item.durationMs),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF06B6D4).copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Bar (Glassmorphism style)
        if (selectedItem != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 Clip selected", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    
                    Button(
                        onClick = {
                            if (isProcessing) return@Button
                            isProcessing = true
                            coroutineScope.launch {
                                val path = runCatching {
                                    MediaFileUtils.importUriToLocalPath(
                                        context = context,
                                        uri = selectedItem!!.uri,
                                        folderName = "imports/video",
                                        defaultExtension = "mp4"
                                    )
                                }.getOrNull()
                                isProcessing = false
                                onVideoPicked(path)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isProcessing) "Loading..." else "Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun checkPermissions(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private suspend fun loadVideos(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DURATION
    )
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    
    context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val duration = cursor.getLong(durationColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            items.add(MediaItem(id, uri, duration))
        }
    }
    items
}

@Composable
fun AsyncThumbnail(
    media: MediaItem,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(media.id) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(media.id) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bitmap = context.contentResolver.loadThumbnail(
                        media.uri,
                        Size(300, 300),
                        null
                    )
                } else {
                    @Suppress("DEPRECATION")
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        media.id,
                        MediaStore.Video.Thumbnails.MINI_KIND,
                        null
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(Color(0xFF1E293B)))
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
