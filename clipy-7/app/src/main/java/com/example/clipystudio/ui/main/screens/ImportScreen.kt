package com.example.clipystudio.ui.main.screens

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
fun ImportScreen(appState: AppState, copy: Copy, snackbarHostState: SnackbarHostState, onBack: () -> Unit, onAddAsset: (MediaType, String?, String?, Long?, Long?) -> Unit, onRemove: (String) -> Unit, onAddToProject: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var pendingPicker by remember { mutableStateOf<MediaType?>(null) }
  var permissionNotice by remember { mutableStateOf<ImportPermissionNotice?>(null) }
  val largeFileLimitBytes = 512L * 1024L * 1024L
  fun importMessage(en: String, vi: String) = if (appState.languageCode == LanguageCode.Vi) vi else en
  fun permissionNotice(titleEn: String, titleVi: String, bodyEn: String, bodyVi: String, openSettings: Boolean = false) =
    ImportPermissionNotice(
      title = importMessage(titleEn, titleVi),
      body = importMessage(bodyEn, bodyVi),
      confirmLabel = if (openSettings) importMessage("Open settings", "Mo cai dat") else importMessage("Choose media", "Chon media"),
      dismissLabel = importMessage("Cancel", "Huy"),
      openSettings = openSettings,
    )
  val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
    if (uris.isEmpty()) {
      scope.launch { snackbarHostState.showSnackbar(importMessage("Media selection cancelled. Your project was not changed.", "Da huy chon media. Du an khong thay doi.")) }
      return@rememberLauncherForActivityResult
    }
    uris.forEach { uri ->
      context.persistReadPermission(uri)
      val metadata = context.readUriMetadataSafely(uri)
      val mimeType = metadata?.mimeType
      if (metadata == null) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This media item could not be read. Try another file.", "Khong the doc media nay. Hay thu tep khac.")) }
      } else if (mimeType == null || (!mimeType.startsWith("image") && !mimeType.startsWith("video"))) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This file type is not supported for image/video import.", "Loai tep nay khong duoc ho tro cho anh/video.")) }
      } else if ((metadata.sizeBytes ?: 0L) > largeFileLimitBytes) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This file is too large to import safely in this MVP build.", "Tep qua lon de nhap an toan trong ban MVP.")) }
      } else {
        onAddAsset(if (mimeType.startsWith("image")) MediaType.Image else MediaType.Video, uri.toString(), metadata.displayName, metadata.sizeBytes, metadata.durationMs)
      }
    }
  }
  val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    if (uris.isEmpty()) {
      scope.launch { snackbarHostState.showSnackbar(importMessage("Audio selection cancelled. Your project was not changed.", "Da huy chon am thanh. Du an khong thay doi.")) }
      return@rememberLauncherForActivityResult
    }
    uris.forEach { uri ->
      if (!context.persistReadPermission(uri)) {
        permissionNotice = permissionNotice(
          titleEn = "Limited audio access",
          titleVi = "Quyen truy cap am thanh bi gioi han",
          bodyEn = "Clipy Studio can still use this audio in the current session, but Android did not grant long-term access. Re-pick it if it is missing later.",
          bodyVi = "Clipy Studio van co the dung tep am thanh nay trong phien hien tai, nhung Android khong cap quyen truy cap lau dai. Hay chon lai neu tep bi mat sau do.",
        )
      }
      val metadata = context.readUriMetadataSafely(uri)
      if (metadata == null || metadata.mimeType?.startsWith("audio") != true) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This audio file is not supported.", "Tep am thanh khong duoc ho tro.")) }
      } else if ((metadata.sizeBytes ?: 0L) > largeFileLimitBytes) {
        scope.launch { snackbarHostState.showSnackbar(importMessage("This audio file is too large to import safely.", "Tep am thanh qua lon de nhap an toan.")) }
      } else {
        onAddAsset(MediaType.Audio, uri.toString(), metadata.displayName, metadata.sizeBytes, metadata.durationMs)
      }
    }
  }
  StudioScreen(horizontalPadding = 18.dp) {
    TopStrip(title = copy.import, onBack = onBack)
    Text(
      if (appState.languageCode == LanguageCode.Vi) "Chon media that su tu Android picker. Chua co media mau hoac file fake trong flow moi."
      else "Import real media through Android system pickers. This rewrite removes sample shortcuts and fake assets.",
      color = StudioTextMuted,
    )
    Spacer(Modifier.height(16.dp))
    Card(
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh),
      shape = RoundedCornerShape(28.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        Modifier
          .background(
            Brush.linearGradient(
              listOf(StudioSurfaceHigh, StudioPrimary.copy(alpha = 0.12f), StudioSecondary.copy(alpha = 0.10f))
            )
          )
          .padding(18.dp),
      ) {
        Text(if (appState.languageCode == LanguageCode.Vi) "Nguon media" else "Media source", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(if (appState.languageCode == LanguageCode.Vi) "Them video, image, va audio vao draft hien tai truoc khi vao editor." else "Add videos, images, and audio to the current draft before opening the editor.", color = StudioTextMuted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(onClick = { pendingPicker = MediaType.Video }, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f).height(52.dp)) {
            Text(if (appState.languageCode == LanguageCode.Vi) "Anh + Video" else "Images + Video")
          }
          OutlinedButton(onClick = { pendingPicker = MediaType.Audio }, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f).height(52.dp)) {
            Text(if (appState.languageCode == LanguageCode.Vi) "Am thanh" else "Audio")
          }
        }
      }
    }
    permissionNotice?.let { notice ->
      Spacer(Modifier.height(12.dp))
      OutlinedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(notice.title, color = StudioAccent, fontWeight = FontWeight.Bold)
          Text(notice.body, color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
          TextButton(onClick = { permissionNotice = null }) { Text(if (appState.languageCode == LanguageCode.Vi) "Dong" else "Dismiss") }
        }
      }
    }
    if (pendingPicker != null) {
      val isAudio = pendingPicker == MediaType.Audio
      AlertDialog(
        onDismissRequest = { pendingPicker = null },
        title = { Text(if (isAudio) importMessage("Choose audio safely", "Chon am thanh an toan") else importMessage("Choose media safely", "Chon media an toan")) },
        text = {
          Text(
            if (isAudio) {
              importMessage(
                "Clipy Studio opens the Android document picker for audio and only reads files you choose. It does not need broad Music and audio permission for this import path.",
                "Clipy Studio mo bo chon tai lieu Android cho am thanh va chi doc cac tep ban chon. Duong nhap nay khong can quyen Nhac va am thanh rong.",
              )
            } else {
              importMessage(
                "Clipy Studio only reads the files you choose for editing and export. Clearing temporary files will not delete original media.",
                "Clipy Studio chi doc nhung tep ban chon de tao va xuat video. Media goc khong bi xoa khi xoa cache.",
              )
            },
          )
        },
        confirmButton = {
          TextButton(onClick = {
            pendingPicker = null
            if (isAudio) {
              audioPicker.launch(arrayOf("audio/*"))
            } else {
              photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
          }) { Text(if (appState.languageCode == LanguageCode.Vi) "Chon media" else "Choose media") }
        },
        dismissButton = { TextButton(onClick = { pendingPicker = null }) { Text(if (appState.languageCode == LanguageCode.Vi) "Huy" else "Cancel") } },
      )
    }
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("Selected (${appState.selectedImports.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      Text(
        appState.selectedImports.sumOf { it.durationMs }.asTimecode(),
        color = StudioTextMuted,
        fontSize = 13.sp,
      )
    }
    if (appState.selectedImports.isEmpty()) {
      Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(if (appState.languageCode == LanguageCode.Vi) "Chua co media nao" else "No media selected", fontWeight = FontWeight.Bold)
          Text(if (appState.languageCode == LanguageCode.Vi) "Chon tep anh, video hoac am thanh tu bo chon he thong de them vao timeline. Ung dung khong them media mau gia." else "Pick images, videos, or audio from Android system pickers to add real local media to the timeline. Sample media shortcuts have been removed.", color = StudioTextMuted, fontSize = 13.sp)
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items(appState.selectedImports, key = { it.id }) { asset -> MediaAssetCard(asset, onRemove) }
    }
    Button(onClick = onAddToProject, enabled = appState.selectedImports.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(if (appState.languageCode == LanguageCode.Vi) "Mo editor voi media da chon" else "Open Editor With Selection") }
  }
}


@Composable
fun MediaAssetCard(asset: MediaAsset, onRemove: (String) -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(if (asset.type == MediaType.Audio) StudioSecondary else StudioPrimary),
        contentAlignment = Alignment.Center,
      ) {
        Text(asset.type.label.take(1), fontWeight = FontWeight.Black)
      }
      Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) {
        Text(asset.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${asset.durationMs.asTimecode()} · ${asset.sizeBytes.asSizeLabel()}${if (asset.sizeBytes > 40_000_000) " · Large file" else ""}", color = StudioTextMuted, fontSize = 13.sp)
      }
      TextButton(onClick = { onRemove(asset.id) }) { Text("Remove", color = StudioDanger) }
    }
  }
}
