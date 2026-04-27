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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun EditorBottomBar(modifier: Modifier = Modifier, selected: EditorTool, onSelect: (EditorTool) -> Unit) {
  val haptic = LocalHapticFeedback.current
  val items = listOf(
    BottomNavItem(EditorTool.Edit, "Edit", Icons.Default.Edit),
    BottomNavItem(EditorTool.Audio, "Audio", Icons.Default.MusicNote),
    BottomNavItem(EditorTool.Text, "Text", Icons.Default.TextFields),
    BottomNavItem(EditorTool.Sticker, "Stickers", Icons.Default.AutoAwesome),
    BottomNavItem(EditorTool.Overlay, "Overlay", Icons.Default.Layers),
    BottomNavItem(EditorTool.Filter, "Filter", Icons.Default.Tune),
    BottomNavItem(EditorTool.Effect, "Effect", Icons.Default.Bolt),
    BottomNavItem(EditorTool.Transition, "Transition", Icons.Default.SwapHoriz),
    BottomNavItem(EditorTool.Canvas, "Canvas", Icons.Default.CropLandscape),
    BottomNavItem(EditorTool.Speed, "Speed", Icons.Default.Speed),
  )
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF373B3D))
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items.forEach { item ->
      val active = selected == item.tool
      Column(
        modifier = Modifier
          .width(52.dp)
          .clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onSelect(item.tool)
          },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (active) Color(0xCC4A90E2) else Color(0xFF2F3436),
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(item.glyph, contentDescription = item.label, tint = Color.White, modifier = Modifier.size(16.dp))
          }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.label, color = Color.White.copy(alpha = if (active) 1f else 0.8f), fontSize = 10.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
      }
    }
  }
}


data class BottomNavItem(val tool: EditorTool, val label: String, val glyph: androidx.compose.ui.graphics.vector.ImageVector)


@Composable
fun AddMediaFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
  val haptic = LocalHapticFeedback.current
  Surface(
    modifier = modifier.size(56.dp),
    shape = CircleShape,
    color = EditorChromePrimary,
    shadowElevation = 18.dp,
    border = androidx.compose.foundation.BorderStroke(4.dp, EditorChromeBackground),
    onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onClick()
    },
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text("+", color = Color(0xFF07111F), fontSize = 30.sp, fontWeight = FontWeight.Black)
    }
  }
}
