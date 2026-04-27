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
fun LanguageScreen(selected: LanguageCode, copy: Copy, showBack: Boolean, onBack: () -> Unit, onSave: (LanguageCode) -> Unit) {
  var current by remember(selected) { mutableStateOf(selected) }
  StudioScreen(horizontalPadding = 18.dp) {
    TopStrip(title = copy.language, onBack = if (showBack) onBack else null)
    Spacer(Modifier.height(18.dp))
    Card(colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
      Column(Modifier.padding(18.dp)) {
        Text(if (selected == LanguageCode.Vi) "Chon ngon ngu" else "Choose your language", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(if (selected == LanguageCode.Vi) "Thay doi ngon ngu cho toan bo workspace va panel editor." else "Change the language for the full workspace and editor panels.", color = StudioTextMuted)
      }
    }
    Spacer(Modifier.height(20.dp))
    LanguageCard("English", "English", current == LanguageCode.En) { current = LanguageCode.En }
    Spacer(Modifier.height(12.dp))
    LanguageCard("Tieng Viet", "Vietnamese", current == LanguageCode.Vi) { current = LanguageCode.Vi }
    Spacer(Modifier.weight(1f))
    Button(onClick = { onSave(current) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(999.dp)) { Text(copy.continueAction) }
  }
}


@Composable
fun LanguageCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
  Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) StudioPrimary.copy(alpha = 0.35f) else StudioSurface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title language option ${if (selected) "selected" else "not selected"}" }) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = StudioTextMuted) }
      Text(if (selected) "Selected" else "Select", color = if (selected) StudioSecondary else StudioTextMuted)
    }
  }
}
