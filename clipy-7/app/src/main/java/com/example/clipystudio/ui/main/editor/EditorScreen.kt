package com.example.clipystudio.ui.main.editor

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
fun EditorScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit, viewModel: MainScreenViewModel, isPlaybackLocked: Boolean) {
  val project = appState.activeProject
  if (project == null) {
    StudioScreen { EmptyState(onCreate = onImport) }
    return
  }
  val timeline = project.timeline
  val selectedClip = timeline.selectedRealClip()
  var showExitDialog by remember { mutableStateOf(false) }
  BackHandler { showExitDialog = true }
  val activeToolTitle = remember(timeline.selectedTool) {
    when (timeline.selectedTool) {
      EditorTool.Edit -> "Edit"
      EditorTool.Audio -> "Audio"
      EditorTool.Text -> "Text"
      EditorTool.Sticker -> "Sticker"
      EditorTool.Overlay -> "Overlay"
      EditorTool.Filter -> "Filter"
      EditorTool.Effect -> "Effect"
      EditorTool.Transition -> "Transition"
      EditorTool.Canvas -> "Canvas"
      EditorTool.Speed -> "Speed"
      EditorTool.Export -> "Export"
    }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(Color(0xFF09111C), EditorChromeBackground, Color(0xFF04070D)))),
  ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
      val editorDockHeight = 212.dp
      val railHeight = 60.dp
      val showToolPanel = timeline.selectedTool != EditorTool.Edit
      Column(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Transparent),
      ) {
        EditorTopBar(
          title = project.name,
          onBack = { showExitDialog = true },
          onUndo = viewModel::undo,
          onRedo = viewModel::redo,
          canUndo = appState.undoStack.isNotEmpty(),
          canRedo = appState.redoStack.isNotEmpty(),
          onExport = onExport,
        )
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(activeToolTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text(selectedClip?.title ?: if (isPlaybackLocked) "Playback locked" else "No layer selected", color = EditorChromeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            EditorPlaybackBar(timeline = timeline, onPlay = viewModel::togglePlayback, onSeekBy = viewModel::seekBy)
          }

          Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = EditorChromeSurface),
            modifier = Modifier.fillMaxWidth().weight(1f),
            border = BorderStroke(1.dp, EditorChromeBorder),
          ) {
            EditorPreviewSection(
              modifier = Modifier.fillMaxSize(),
              ratio = project.canvasRatio,
              timeline = timeline,
              onSelect = viewModel::selectClip,
              onClearSelection = viewModel::clearSelection,
              onDelete = viewModel::deleteSelectedClip,
              onTransform = viewModel::transformSelectedClipAbsolute,
              onEditText = viewModel::updateSelectedTool,
              onRatio = viewModel::updateCanvasRatio,
              onSeek = viewModel::seekTo,
            )
          }
          Spacer(Modifier.height(editorDockHeight))
        }
      }

      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(editorDockHeight),
        color = Color(0xFF242728),
        shape = RectangleShape,
      ) {
        Column(Modifier.fillMaxSize()) {
          Row(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 8.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
            ) {
              EditorTimelineSection(
                modifier = Modifier.fillMaxSize(),
                timeline = timeline,
                timelineContent = {
                  TimelineView(
                    timeline = timeline,
                    onSelect = viewModel::selectClip,
                    onSeek = viewModel::seekTo,
                    onScroll = viewModel::scrollTimelineTo,
                    onZoom = viewModel::updateTimelineZoom,
                    onTrim = viewModel::trimSelectedClipEdge,
                    onMove = viewModel::dragSelectedClip,
                    onSplit = viewModel::splitSelectedClip,
                    onReorder = viewModel::reorderSelectedVideoClip,
                  )
                },
              )

              if (showToolPanel) {
                Box(
                  Modifier
                    .matchParentSize()
                    .animateContentSize()
                ) {
                  EditorPanelHost(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color(0xFF242728)),
                    timeline = timeline,
                    project = project,
                    selectedClip = selectedClip,
                    viewModel = viewModel,
                    onClose = { viewModel.updateSelectedTool(EditorTool.Edit) },
                  )
                }
              } else {
                Box(Modifier.align(Alignment.BottomCenter).padding(8.dp)) {
                  EditorContextualPanel(
                    hasSelection = selectedClip != null,
                    onSplit = viewModel::splitSelectedClip,
                    onSpeed = { viewModel.updateSelectedTool(EditorTool.Speed) },
                    onVolume = { viewModel.updateSelectedTool(EditorTool.Audio) },
                    onAnimation = { viewModel.updateSelectedTool(EditorTool.Effect) },
                    onDelete = viewModel::deleteSelectedClip,
                    onCrop = { viewModel.updateSelectedTool(EditorTool.Canvas) },
                  )
                }
              }
            }

            Spacer(Modifier.width(8.dp))

            Surface(
              onClick = onImport,
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF31363A),
              border = BorderStroke(1.dp, EditorChromeBorder),
              modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("+", color = EditorChromePrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          EditorBottomBar(
            modifier = Modifier.fillMaxWidth().height(railHeight).background(Color(0xFF373B3D)),
            selected = timeline.selectedTool,
            onSelect = viewModel::updateSelectedTool,
          )
        }
      }

    }
  }
  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      title = { Text(if (appState.languageCode == LanguageCode.Vi) "Luu truoc khi thoat?" else "Save before leaving?") },
      text = { Text(if (appState.languageCode == LanguageCode.Vi) "Du an da duoc tu dong luu. Ban co the luu va thoat, bo qua thay doi dang chon, hoac tiep tuc bien tap." else "Your project is autosaved. Save and exit, discard the current editor selection, or keep editing.") },
      confirmButton = { TextButton(onClick = onBack) { Text(if (appState.languageCode == LanguageCode.Vi) "Luu & thoat" else "Save & Exit") } },
      dismissButton = {
        Row {
          TextButton(onClick = onBack) { Text(if (appState.languageCode == LanguageCode.Vi) "Bo qua" else "Discard", color = StudioDanger) }
          TextButton(onClick = { showExitDialog = false }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") }
        }
      },
    )
  }
}
