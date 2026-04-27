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
fun AudioToolPanel(selectedClip: TimelineClip?, viewModel: MainScreenViewModel) {
  var tab by remember { mutableStateOf(AudioSource.BuiltInMusic) }
  val items = when (tab) {
    AudioSource.DeviceMusic -> emptyList()
    AudioSource.BuiltInMusic -> listOf("Neon pulse" to "00:18", "Lo-fi creator bed" to "00:30")
    AudioSource.ExtractedAudio -> if (selectedClip?.clipType == ClipType.Video) listOf("Extract from ${selectedClip.title}" to "linked") else emptyList()
    AudioSource.SoundEffect -> listOf("Camera click" to "00:01", "Whoosh pop" to "00:02")
  }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Audio", fontWeight = FontWeight.Bold)
      Text(selectedClip?.let { "${it.title} · ${it.durationMs.asTimecode()}" } ?: "Add music or select an audio/video clip to edit its sound settings.", color = StudioTextMuted, fontSize = 13.sp)
      Spacer(Modifier.height(8.dp))
      Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { AudioSource.entries.forEach { source -> FilterChip(selected = tab == source, onClick = { tab = source }, label = { Text(source.label) }) } }
      if (items.isEmpty()) {
        Text(
          when (tab) {
            AudioSource.DeviceMusic -> "Use Import Audio to choose real device audio from the system picker."
            AudioSource.ExtractedAudio -> "Select a video clip before extracting source audio."
            else -> "No audio items are available for this source."
          },
          color = StudioTextMuted,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
      items.forEach { item ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) { Text(item.first, fontWeight = FontWeight.Bold); Text(item.second, color = StudioTextMuted, fontSize = 12.sp) }
          Button(onClick = { viewModel.addAudioClipAtPlayhead(item.first, tab) }, shape = RoundedCornerShape(999.dp)) { Text("Add") }
        }
      }
      selectedClip?.takeIf { it.clipType == ClipType.Audio || it.clipType == ClipType.Video }?.let { clip ->
        Spacer(Modifier.height(10.dp))
        AdjustmentControl("Volume", clip.audioProperties.volume, 0f, 1f) { viewModel.updateSelectedAudio(it, clip.audioProperties.fadeInMs, clip.audioProperties.fadeOutMs, clip.audioProperties.loopEnabled) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = clip.audioProperties.fadeInMs > 0, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, if (clip.audioProperties.fadeInMs > 0) 0 else 600, clip.audioProperties.fadeOutMs, clip.audioProperties.loopEnabled) }, label = { Text("Fade in") })
          FilterChip(selected = clip.audioProperties.fadeOutMs > 0, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, clip.audioProperties.fadeInMs, if (clip.audioProperties.fadeOutMs > 0) 0 else 600, clip.audioProperties.loopEnabled) }, label = { Text("Fade out") })
          FilterChip(selected = clip.audioProperties.loopEnabled, onClick = { viewModel.updateSelectedAudio(clip.audioProperties.volume, clip.audioProperties.fadeInMs, clip.audioProperties.fadeOutMs, !clip.audioProperties.loopEnabled) }, label = { Text("Loop") })
        }
        LayerActions(viewModel, enabled = true)
      }
    }
  }
}
