package com.example.clipystudio.ui.main.editor.components

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
fun EditorPanelHost(modifier: Modifier, timeline: Timeline, project: Project, selectedClip: TimelineClip?, viewModel: MainScreenViewModel, onClose: () -> Unit) {
  Surface(
    modifier = modifier,
    color = EditorChromeSurface.copy(alpha = 0.98f),
    shape = RoundedCornerShape(24.dp),
    shadowElevation = 18.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
      PanelHeader(timeline.selectedTool, selectedClip, onClose)
      Spacer(Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState()),
      ) {
        when {
          selectedClip != null && timeline.selectedTool == EditorTool.Edit -> ClipEditPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Audio -> AudioToolPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Text -> TextToolPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Sticker -> StickerToolPanel(timeline, viewModel)
          timeline.selectedTool == EditorTool.Filter -> FilterAdjustPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Effect -> EffectToolPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Transition -> TransitionToolPanel(timeline, viewModel)
          timeline.selectedTool == EditorTool.Canvas -> CanvasToolPanel(project.canvasRatio, timeline.canvasBackground, viewModel)
          timeline.selectedTool == EditorTool.Speed -> SpeedToolPanel(selectedClip, viewModel)
          timeline.selectedTool == EditorTool.Overlay -> OverlayToolPanel(project.importedAssets, selectedClip, viewModel)
          else -> ToolPanel(timeline, viewModel)
        }
      }
    }
  }
}

@Composable
private fun PanelHeader(tool: EditorTool, selectedClip: TimelineClip?, onClose: () -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(tool.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
      Text(
        selectedClip?.let { "${clipTypeBadge(it.clipType)} selected · ${it.title}" } ?: "Choose a clip or import media to use this tool",
        color = EditorChromeMuted,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = EditorChromeSurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder),
      ) {
        Text(
          navGlyph(tool),
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
        )
      }
      Surface(onClick = onClose, shape = RoundedCornerShape(999.dp), color = EditorChromeSurfaceLow, border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder)) {
        Text("Close", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = EditorChromeMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
