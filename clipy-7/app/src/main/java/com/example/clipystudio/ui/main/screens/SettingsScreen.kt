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
fun SettingsScreen(appState: AppState, copy: Copy, onBack: () -> Unit, onLanguage: () -> Unit, onClearCache: () -> Unit, onExit: () -> Unit) {
  StudioScreen(horizontalPadding = 18.dp) {
    TopStrip(title = copy.settings, onBack = onBack)
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
      Column(Modifier.padding(18.dp)) {
        Text("Workspace settings", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("Manage language, cache, export defaults, and app safety without leaving your editing flow.", color = StudioTextMuted, fontSize = 13.sp)
      }
    }
    SettingsRow(copy.language, if (appState.languageCode == LanguageCode.En) "English" else "Tieng Viet", onLanguage)
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Mac dinh xuat" else "Export defaults", "${appState.defaultExportSettings.resolution.label}, ${appState.defaultExportSettings.fps} FPS, ${appState.defaultExportSettings.qualityPreset.label}", {})
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Luu tru va cache" else "Storage & Cache", "${appState.cacheUsageMb} MB thumbnail/proxy cache", onClearCache, action = if (appState.languageCode == LanguageCode.Vi) "Xoa" else "Clear")
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (appState.languageCode == LanguageCode.Vi) "Bao mat quyen rieng tu" else "Privacy-safe storage", fontWeight = FontWeight.Bold)
        Text(
          if (appState.languageCode == LanguageCode.Vi) {
            "Media nhap vao van nam o vi tri ban chon. Xoa tep tam chi xoa cache cua ung dung va khong xoa video da xuat."
          } else {
            "Imported media stays in the location you selected. Clearing temporary files only removes app cache and does not delete exported videos."
          },
          color = StudioTextMuted,
          fontSize = 13.sp,
        )
      }
    }
    Spacer(Modifier.height(12.dp))
    SettingsRow(if (appState.languageCode == LanguageCode.Vi) "Thong tin ung dung" else "App Info", if (appState.languageCode == LanguageCode.Vi) "Bien tap cuc bo offline-friendly MVP - version 1.0" else "Offline-friendly local editing MVP - version 1.0", {})
    SettingsRow(copy.exit, "Close after autosave/export confirmation", onExit, danger = true)
  }
}


@Composable
fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit, action: String = "Open", danger: Boolean = false) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = StudioSurface), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, color = if (danger) StudioDanger else Color.Unspecified, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted, fontSize = 13.sp) }
      Text(action, color = if (danger) StudioDanger else StudioSecondary)
    }
  }
}
