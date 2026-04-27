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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo

@Composable
fun EditorTopBar(title: String, onBack: () -> Unit, onUndo: () -> Unit, onRedo: () -> Unit, canUndo: Boolean, canRedo: Boolean, onExport: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .background(Color(0xFF242728))
      .border(bottom = 1.dp, color = EditorChromeBorder)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(onClick = onBack, shape = RoundedCornerShape(12.dp), color = Color(0xFF31363A), border = androidx.compose.foundation.BorderStroke(1.dp, EditorChromeBorder), modifier = Modifier.size(36.dp)) {
      Box(contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
    }
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
      Text(
        title,
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text("Edit", color = EditorChromeMuted, fontSize = 11.sp)
    }
    IconButton(onClick = onUndo, enabled = canUndo) {
      Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = if (canUndo) Color.White else Color.Gray)
    }
    IconButton(onClick = onRedo, enabled = canRedo) {
      Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = if (canRedo) Color.White else Color.Gray)
    }
    Spacer(Modifier.width(6.dp))
    Surface(
      onClick = onExport,
      shape = RoundedCornerShape(6.dp),
      color = Color(0xFF4CCB82),
      modifier = Modifier.height(34.dp),
    ) {
      Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text("Next 1/2", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
      }
    }
  }
}

private fun Modifier.border(bottom: Dp, color: Color): Modifier = this.drawBehind {
    val strokeWidth = bottom.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
}
