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
fun IntroScreen(copy: Copy, onContinue: () -> Unit, onSkip: () -> Unit) {
  var page by remember { mutableStateOf(0) }
  val pages = copy.onboardingPages()
  StudioScreen(horizontalPadding = 18.dp) {
    Spacer(Modifier.height(18.dp))
    Text("Clipy Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
    Text("Editor-first workspace for fast local cuts", color = StudioTextMuted)
    Spacer(Modifier.height(20.dp))
    Card(
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceHigh),
      shape = RoundedCornerShape(32.dp),
      modifier = Modifier.fillMaxWidth().weight(1f)
    ) {
      Column(
        Modifier
          .fillMaxSize()
          .background(Brush.linearGradient(listOf(StudioPrimary.copy(alpha = 0.18f), StudioSurfaceHigh, StudioSecondary.copy(alpha = 0.10f))))
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        OnboardingIllustration(pages[page].color)
        Spacer(Modifier.height(24.dp))
        Text(pages[page].title, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(pages[page].body, color = StudioTextMuted, textAlign = TextAlign.Center)
      }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
      pages.indices.forEach { Dot(selected = it == page) }
    }
    Button(
      onClick = { if (page < pages.lastIndex) page++ else onContinue() },
      modifier = Modifier.fillMaxWidth().height(56.dp),
      shape = RoundedCornerShape(999.dp),
    ) { Text(if (page == pages.lastIndex) "Open Workspace" else copy.continueAction) }
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp), shape = RoundedCornerShape(999.dp)) { Text("Skip intro") }
  }
}


@Composable
fun OnboardingIllustration(color: Color) {
  Canvas(Modifier.size(170.dp)) {
    drawRoundRect(StudioSurfaceHigh, cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f, 30f), size = size)
    drawRoundRect(color.copy(alpha = 0.24f), topLeft = Offset(26f, 34f), size = Size(size.width - 52f, 56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawLine(StudioSecondary, Offset(size.width / 2f, 22f), Offset(size.width / 2f, size.height - 22f), strokeWidth = 5f)
    drawCircle(color, 22f, Offset(size.width / 2f, 42f))
    drawRoundRect(StudioPrimary.copy(alpha = 0.65f), topLeft = Offset(26f, 112f), size = Size(62f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    drawRoundRect(StudioAccent.copy(alpha = 0.8f), topLeft = Offset(94f, 112f), size = Size(48f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
  }
}


@Composable
fun Dot(selected: Boolean) = Box(Modifier.padding(4.dp).size(if (selected) 20.dp else 8.dp, 8.dp).clip(CircleShape).background(if (selected) StudioPrimary else StudioTextMuted.copy(alpha = 0.35f)))
