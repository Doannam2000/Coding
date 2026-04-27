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
fun StickerToolPanel(timeline: Timeline, viewModel: MainScreenViewModel) {
  var category by remember { mutableStateOf(StickerCategory.Emoji) }
  val assets = if (category == StickerCategory.Recent) timeline.recentStickers.ifEmpty { StickerLibrary.take(4) } else StickerLibrary.filter { it.category == category }
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(14.dp)) {
      Text("Sticker library", fontWeight = FontWeight.Bold)
      Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StickerCategory.entries.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.label) }) } }
      LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(assets, key = { it.id }) { asset -> StickerTile(asset) { viewModel.addStickerAtPlayhead(asset) } } }
      LayerActions(viewModel, enabled = timeline.selectedRealClip()?.clipType == ClipType.Sticker)
    }
  }
}


@Composable
fun StickerTile(asset: StickerAsset, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(86.dp).semantics { contentDescription = "Insert ${asset.label} sticker" }) {
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Text(asset.symbol, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
      Text(asset.label, color = StudioTextMuted, fontSize = 11.sp, maxLines = 1)
    }
  }
}