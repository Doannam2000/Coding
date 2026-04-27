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
fun EditorTimelineSection(
  modifier: Modifier,
  timeline: Timeline,
  timelineContent: @Composable () -> Unit,
) {
  val selectedClip = timeline.findClip(timeline.selectedClipId)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF2A2D2F)),
  ) {
    Box(Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)) {
      timelineContent()
    }
  }
}


@Composable
fun EditorTimelineToolbar(
  timeline: Timeline,
  canUndo: Boolean,
  canRedo: Boolean,
  hasSelection: Boolean,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onSplit: () -> Unit,
  onSpeed: () -> Unit,
  onAnimation: () -> Unit,
  onVolume: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .background(EditorChromeSurface)
      .border(1.dp, EditorChromeBorder)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
      CompactToolbarIconButton(toolbarGlyph("undo"), "Undo edit", canUndo, onUndo)
      CompactToolbarIconButton(toolbarGlyph("redo"), "Redo edit", canRedo, onRedo)
      Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(16.dp).background(EditorChromeBorder))
      CompactToolbarAction(toolbarGlyph("split"), "Split", hasSelection, onSplit)
      CompactToolbarAction(toolbarGlyph("speed"), "Speed", hasSelection, onSpeed)
    }
    Surface(
      color = EditorChromePrimary.copy(alpha = 0.10f),
      shape = RoundedCornerShape(6.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromePrimary.copy(alpha = 0.20f)),
    ) {
      Text(
        timeline.playheadMs.asTimecode(),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        color = EditorChromePrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
      CompactToolbarAction(toolbarGlyph("anim"), "Anim", true, onAnimation)
      CompactToolbarAction(toolbarGlyph("volume"), "Vol", hasSelection, onVolume)
      CompactToolbarIconButton(toolbarGlyph("delete"), "Delete selected clip", hasSelection, onDelete, tint = EditorChromeDanger)
    }
  }
}


@Composable
fun CompactToolbarIconButton(
  glyph: String,
  description: String,
  enabled: Boolean,
  onClick: () -> Unit,
  tint: Color = EditorChromeMuted,
) {
  IconButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.size(36.dp).semantics { contentDescription = description },
  ) {
    Text(glyph, color = if (enabled) tint else tint.copy(alpha = 0.35f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
  }
}


@Composable
fun CompactToolbarAction(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color.Transparent)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 8.dp)
      .semantics { contentDescription = label },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(glyph, color = if (enabled) EditorChromeMuted else EditorChromeMuted.copy(alpha = 0.35f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Text(label, color = if (enabled) EditorChromeMuted else EditorChromeMuted.copy(alpha = 0.35f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
  }
}
