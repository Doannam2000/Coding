package com.nantcompany.clipy.tools.slideshow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nantcompany.clipy.app.MediaFileUtils
import com.nantcompany.clipy.edit.tools.slideshow.SlideshowRequest
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.navigation.AppRoute

@Composable
fun SlideshowScreen(
    imagePaths: List<String>,
    onRemoveAt: (Int) -> Unit,
    onSubmitRequest: (ProcessingRequest) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    viewModel: SlideshowViewModel = viewModel()
) {
    val context = LocalContext.current
    val durations = listOf("1", "2", "3", "5")
    val aspectRatios = listOf("9:16", "1:1", "16:9", "Original")
    val backgroundModes = listOf("fit", "fill", "black")
    var secondsPerImage by remember { mutableStateOf("3") }
    var aspectRatio by remember { mutableStateOf("9:16") }
    var backgroundMode by remember { mutableStateOf("fit") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Slideshow", style = MaterialTheme.typography.headlineSmall)
        Text("Selected images: ${imagePaths.size}", style = MaterialTheme.typography.bodyMedium)

        if (imagePaths.size < 2) {
            Text(
                "Select at least 2 images to create a slideshow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (imagePaths.isNotEmpty()) {
            imagePaths.forEachIndexed { index, path ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val previewBitmap = remember(path) { loadImageThumbnail(path, 160) }
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Selected image preview",
                                modifier = Modifier
                                    .size(44.dp)
                                    .height(44.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            "#${index + 1}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onRemoveAt(index) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        Button(onClick = { onNavigate(AppRoute.PICK_IMAGES) }) {
            Text(if (imagePaths.isEmpty()) "Select Images" else "Add more images")
        }

        val previewAspectRatio = when (aspectRatio) {
            "9:16" -> 9f / 16f
            "1:1" -> 1f
            "16:9" -> 16f / 9f
            else -> 16f / 9f
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspectRatio)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text("Preview frame · $aspectRatio", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (imagePaths.isNotEmpty()) "Frame reflects selected aspect ratio."
                        else "Select images to preview slideshow frame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            "Output aspect ratio: $aspectRatio",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Duration per image", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            durations.forEach { duration ->
                val selected = secondsPerImage == duration
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, onClick = { secondsPerImage = duration }),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("${duration}s", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(
            value = secondsPerImage,
            onValueChange = { value -> secondsPerImage = value.filter { it.isDigit() } },
            label = { Text("Seconds per image") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        val totalSecondsEstimate = (secondsPerImage.toIntOrNull() ?: 0) * imagePaths.size
        Text(
            if (totalSecondsEstimate > 0) "Estimated slideshow length: ${totalSecondsEstimate}s"
            else "Enter seconds per image to preview total length.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Aspect ratio", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            aspectRatios.forEach { ratio ->
                val selected = aspectRatio == ratio
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, onClick = { aspectRatio = ratio }),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(ratio, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Transition", style = MaterialTheme.typography.titleSmall)
                Text("Fade · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Slide · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Zoom · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Music: Add music · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Background mode", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    backgroundModes.forEach { mode ->
                        val selected = backgroundMode == mode
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(selected = selected, onClick = { backgroundMode = mode }),
                            border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(mode.replaceFirstChar { it.uppercase() }, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text("Blur · Coming soon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "Selected background: ${backgroundMode.replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        uiState.validationError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = imagePaths.size >= 2,
            onClick = {
                val request = SlideshowRequest(
                    imagePaths = imagePaths,
                    outputPath = MediaFileUtils.createOutputPath(context, "slideshow", "mp4"),
                    secondsPerImage = secondsPerImage.toIntOrNull() ?: -1,
                    backgroundMode = backgroundMode
                )
                if (viewModel.validate(request)) {
                    onSubmitRequest(ProcessingRequest.Slideshow(request))
                }
            }
        ) {
            Text("Create Slideshow")
        }
    }
}

private fun loadImageThumbnail(path: String, targetSizePx: Int): Bitmap? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while ((bounds.outWidth / sampleSize) > targetSizePx || (bounds.outHeight / sampleSize) > targetSizePx) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
            inTempStorage = ByteArray(16 * 1024)
        }
        BitmapFactory.decodeFile(path, decodeOptions)
    }.getOrNull()
}
