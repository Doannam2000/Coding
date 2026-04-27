package com.example.clipystudio.ui.main.editor.panels

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
fun EffectToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(EffectCategory.Basic) }
  val effects = EffectLibrary.filter { it.category == category }
  val enabled = selectedClip?.clipType in setOf(ClipType.Video, ClipType.Image, ClipType.Overlay, ClipType.Effect)
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Effects", fontWeight = FontWeight.Bold)
      Text(selectedClip?.let { "Apply effect around ${it.title}" } ?: "Choose a visual clip or place the playhead before adding an effect clip.", color = StudioTextMuted, fontSize = 13.sp)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { EffectCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(effects, key = { it.id }) { effect -> EffectTile(effect, enabled = enabled) { viewModel.addEffectAtPlayhead(effect) } } }
      LayerActions(viewModel, enabled = selectedClip?.clipType == ClipType.Effect)
    }
  }
}


@Composable
fun EffectTile(effect: EffectPreset, enabled: Boolean, onClick: () -> Unit) {
  Card(onClick = onClick, enabled = enabled, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(104.dp, 76.dp).semantics { contentDescription = "Apply ${effect.label} effect" }) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
      Text(effect.label, fontWeight = FontWeight.Bold, color = if (enabled) Color.Unspecified else StudioTextMuted)
      Text(effect.category.label, color = StudioTextMuted, fontSize = 12.sp)
    }
  }
}
