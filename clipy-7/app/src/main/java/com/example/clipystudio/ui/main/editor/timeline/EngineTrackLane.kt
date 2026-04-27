package com.example.clipystudio.ui.main.editor.timeline

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxScope

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clipystudio.data.*
import com.example.clipystudio.filter.*
import com.example.clipystudio.theme.*
import com.example.clipystudio.*
import com.example.clipystudio.ui.main.*
import com.example.clipystudio.ui.main.models.*
import com.example.clipystudio.ui.main.screens.*
import com.example.clipystudio.ui.main.editor.*
import com.example.clipystudio.ui.main.editor.components.*
import com.example.clipystudio.ui.main.editor.panels.*
import com.example.clipystudio.ui.main.editor.timeline.*
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.*

@Composable
fun EngineTrackLane(projectTimeline: com.example.clipystudio.data.ProjectTimeline, timeline: Timeline, track: TimelineTrack, contentWidth: Int, viewportWidthPx: Float, activeIds: Set<String>, touchSlopPx: Float, thumbnailFrames: Map<String, Bitmap?>, onSelect: (String) -> Unit, onTrim: (TrimHandle, Long) -> Unit, onMove: (Long) -> Unit, onSplit: () -> Unit, onReorder: (Int) -> Unit, activePreview: TimelineClipPreviewState?, onPreview: (TimelineClipPreviewState?) -> Unit, onAutoScroll: (Float, com.example.clipystudio.data.AutoScrollDirection) -> Unit, onPreviewSeek: (Long) -> Unit, onPreviewEnd: () -> Unit, physics: TimelinePhysicsConfig = TimelineEngine.DefaultPhysics, snapConfig: TimelineSnapConfig = TimelineEngine.DefaultSnapConfig) {
  val laneHeight = when (track.type) {
    TrackType.Text, TrackType.Sticker, TrackType.Overlay, TrackType.Effect -> 24.dp
    TrackType.Video -> 72.dp
    TrackType.Audio -> 32.dp
  }
  Row(
    Modifier
      .fillMaxWidth()
      .height(laneHeight)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(track.type.label.uppercase(), modifier = Modifier.width(46.dp), fontSize = 9.sp, color = Color.White.copy(alpha = 0.78f), fontWeight = FontWeight.Bold)
    Box(
      Modifier
        .weight(1f)
        .fillMaxHeight()
        .clip(RoundedCornerShape(4.dp))
        .background(Color(0xFF303436))
    ) {
      if (track.clips.isEmpty()) {
        Box(Modifier.fillMaxSize())
      }
      Box(Modifier.fillMaxWidth().fillMaxHeight()) {
        if (track.type == TrackType.Video || track.type == TrackType.Audio) {
          Row(
            Modifier
              .matchParentSize()
              .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat((contentWidth / 120).coerceIn(4, 24)) {
              Box(
                Modifier
                  .weight(1f)
                  .height(if (track.type == TrackType.Video) 50.dp else 24.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color.White.copy(alpha = if (track.type == TrackType.Video) 0.025f else 0.04f))
              )
            }
          }
        }
        track.clips.sortedBy { it.startMs }.forEachIndexed { index, clip ->
          EngineClipBlock(track.type, clip, index, selected = projectTimeline.selectedClipId == clip.id, active = clip.id in activeIds, zoom = projectTimeline.zoomScale, pixelsPerSecond = projectTimeline.pixelsPerSecond, scrollOffsetPx = timeline.scrollOffsetPx, transition = timeline.transitions.firstOrNull { it.fromClipId == clip.id || it.toClipId == clip.id }, timeline = timeline, viewportWidthPx = viewportWidthPx, touchSlopPx = touchSlopPx, thumbnailBitmap = thumbnailFrames[clip.id], preview = activePreview?.takeIf { it.clipId == clip.id }, onSelect = onSelect, onTrim = onTrim, onMove = onMove, onSplit = onSplit, onReorder = onReorder, onPreview = onPreview, onPreviewEnd = onPreviewEnd, onAutoScroll = onAutoScroll, onPreviewSeek = onPreviewSeek, physics = physics, snapConfig = snapConfig)
        }
      }
    }
  }
}
