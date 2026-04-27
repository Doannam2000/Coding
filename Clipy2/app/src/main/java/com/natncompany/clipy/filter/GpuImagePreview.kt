package com.natncompany.clipy.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.natncompany.clipy.editor.ClipDraft
import com.natncompany.clipy.editor.MediaKind
import jp.co.cyberagent.android.gpuimage.GPUImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun GpuImagePreview(
    clip: ClipDraft,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by rememberPreviewBitmap(context = context, clip = clip)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1318))
    ) {
        val previewBitmap = bitmap
        if (previewBitmap == null) {
            Text(
                text = "Loading filter preview",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val resolvedBitmap: Bitmap = previewBitmap
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    GPUImageView(viewContext).apply {
                        setImage(resolvedBitmap)
                        setFilter(GpuImageFilterLibrary.buildFilter(clip.adjustments))
                    }
                },
                update = { gpuImageView ->
                    gpuImageView.setImage(resolvedBitmap)
                    gpuImageView.setFilter(GpuImageFilterLibrary.buildFilter(clip.adjustments))
                    gpuImageView.requestRender()
                }
            )

            if (clip.mediaKind == MediaKind.Video) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = "GPUImage frame",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberPreviewBitmap(
    context: Context,
    clip: ClipDraft
) = produceState<Bitmap?>(
    initialValue = null,
    clip.id,
    clip.uriString,
    clip.mediaKind,
    clip.adjustments.trimStartMs,
    clip.adjustments.trimEndMs
) {
    value = withContext(Dispatchers.IO) {
        loadPreviewBitmap(
            context = context,
            clip = clip,
            targetLongEdgePx = 1280
        )
    }
}

private fun loadPreviewBitmap(
    context: Context,
    clip: ClipDraft,
    targetLongEdgePx: Int
): Bitmap? {
    val uri = Uri.parse(clip.uriString)
    return when (clip.mediaKind) {
        MediaKind.Image -> decodeScaledBitmap(context, uri, targetLongEdgePx)
        MediaKind.Video -> extractVideoFrame(context, uri, clip, targetLongEdgePx)
    }
}

private fun decodeScaledBitmap(
    context: Context,
    uri: Uri,
    targetLongEdgePx: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        return null
    }

    val longEdge = max(bounds.outWidth, bounds.outHeight)
    val sampleSize = max(1, longEdge / targetLongEdgePx)
    val options = BitmapFactory.Options().apply {
        inSampleSize = Integer.highestOneBit(sampleSize).coerceAtLeast(1)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
    return bitmap?.scaleDown(targetLongEdgePx)
}

private fun extractVideoFrame(
    context: Context,
    uri: Uri,
    clip: ClipDraft,
    targetLongEdgePx: Int
): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val frameTimeMs = clip.adjustments.trimStartMs
            .coerceAtLeast(0L)
            .coerceAtMost((clip.sourceDurationMs - 50L).coerceAtLeast(0L))
        retriever.getFrameAtTime(
            frameTimeMs * 1000L,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )?.scaleDown(targetLongEdgePx)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun Bitmap.scaleDown(targetLongEdgePx: Int): Bitmap {
    val longEdge = max(width, height)
    if (longEdge <= targetLongEdgePx) {
        return this
    }
    val scale = targetLongEdgePx.toFloat() / longEdge.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}
