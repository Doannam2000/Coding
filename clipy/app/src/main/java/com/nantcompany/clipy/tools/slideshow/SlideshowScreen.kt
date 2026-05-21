package com.nantcompany.clipy.tools.slideshow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipyScaffold
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipySectionTitle
import com.nantcompany.clipy.edit.common.TransitionType
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute
import com.nantcompany.clipy.theme.ClipyDesignTokens
import java.io.File

@Composable
fun SlideshowScreen(
    imagePaths: List<String>,
    audioPath: String? = null,
    onRemoveAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onAddMore: () -> Unit,
    onPickAudio: () -> Unit,
    onRemoveAudio: () -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: SlideshowViewModel = viewModel()
) {
    val context = LocalContext.current
    val durations = listOf("1", "2", "3", "5")
    val aspectRatios = listOf("9:16", "1:1", "16:9")
    var secondsPerImage by remember { mutableStateOf("3") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var selectedTransition by remember { mutableStateOf(TransitionType.WIPE_RIGHT) }
    val uiState by viewModel.uiState.collectAsState()

    ClipyScaffold(
        title = "Create Slideshow",
        onBackClick = { onNavigate(AppRoute.HOME) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (imagePaths.size < 2) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33FF4B4B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Select at least 2 images to create a video.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ClipySectionTitle(text = "Images Order (${imagePaths.size})")
                        TextButton(onClick = onAddMore) {
                            Text("Add More", color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                itemsIndexed(imagePaths) { index, path ->
                    val file = File(path)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val previewBitmap = remember(path) { loadImageThumbnail(path, 160) }
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.size(54.dp).background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)))
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                                Text("Position ${index + 1}", style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.secondaryText)
                            }
                            
                            Row {
                                IconButton(onClick = { if (index > 0) onMove(index, index - 1) }, enabled = index > 0) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f))
                                }
                                IconButton(onClick = { if (index < imagePaths.size - 1) onMove(index, index + 1) }, enabled = index < imagePaths.size - 1) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = if (index < imagePaths.size - 1) Color.White else Color.White.copy(alpha = 0.2f))
                                }
                                IconButton(onClick = { onRemoveAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF4B4B).copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipySectionTitle(text = "Settings")
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                            colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Duration per Image", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    durations.forEach { d ->
                                        val isSelected = secondsPerImage == d
                                        ClipySecondaryButton(
                                            label = "${d}s",
                                            modifier = Modifier.weight(1f),
                                            enabled = !isSelected,
                                            onClick = { secondsPerImage = d }
                                        )
                                    }
                                }
                                
                                androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                
                                Text("Aspect Ratio", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    aspectRatios.forEach { r ->
                                        val isSelected = aspectRatio == r
                                        ClipySecondaryButton(
                                            label = r,
                                            modifier = Modifier.weight(1f),
                                            enabled = !isSelected,
                                            onClick = { aspectRatio = r }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipySectionTitle(text = "Transition")
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

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClipySectionTitle(text = "Background Audio")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
                            colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
                        ) {
                            if (audioPath == null) {
                                Row(
                                    modifier = Modifier.padding(20.dp).fillMaxWidth().clickable { onPickAudio() },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = ClipyDesignTokens.primaryAccent)
                                    Text("Add Music Track", color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ClipyDesignTokens.success)
                                    Text(File(audioPath).name, modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    IconButton(onClick = onRemoveAudio) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFFF4B4B))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE60A0A12))
                    .padding(16.dp)
            ) {
                ClipyPrimaryButton(
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    label = "Create Slideshow Video",
                    enabled = imagePaths.size >= 2,
                    onClick = {
                        val request = SlideshowRequest(
                            imagePaths = imagePaths,
                            outputPath = MediaFileUtils.createOutputPath(context, "slideshow", "mp4"),
                            secondsPerImage = secondsPerImage.toIntOrNull() ?: 3,
                            audioPath = audioPath,
                            backgroundMode = "fit",
                            transition = selectedTransition.ffmpegName,
                            transitionDurationMs = 1000L
                        )
                        onSubmitRequest(ProcessingRequest.Slideshow(request))
                    }
                )
            }
        }
    }
}

private fun loadImageThumbnail(path: String, size: Int): Bitmap? {
    return runCatching {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
        BitmapFactory.decodeFile(path, options)
    }.getOrNull()
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
