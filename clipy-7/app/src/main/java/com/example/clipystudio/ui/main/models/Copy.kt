package com.example.clipystudio.ui.main.models

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

data class Copy(
  val create: String,
  val recent: String,
  val settings: String,
  val exit: String,
  val import: String,
  val export: String,
  val language: String,
  val continueAction: String,
  val dashboard: String,
  val editor: String,
)


fun copyFor(languageCode: LanguageCode) = if (languageCode == LanguageCode.Vi) {
  Copy("Tao du an moi", "Du an gan day", "Cai dat", "Thoat", "Them media", "Xuat", "Ngon ngu", "Tiep tuc", "Bang du an", "Trinh bien tap")
} else {
  Copy("Create New Project", "Recent Projects", "Settings", "Exit", "Add Media", "Export", "Language", "Continue", "Dashboard", "Editor")
}


@Composable
fun Copy.onboardingPages() = if (language == "Ngon ngu") {
  listOf(
    IntroPage("Nhap media cuc bo", "Dua video, anh va am thanh vao du an offline-friendly.", StudioPrimary),
    IntroPage("Dong bo moi chinh sua", "Playhead trung tam giu preview, clip va timecode khop nhau.", StudioSecondary),
    IntroPage("Xuat va chia se", "Render preset MP4 cho Shorts, Reels, TikTok va clip ca nhan.", StudioAccent),
  )
} else {
  listOf(
    IntroPage("Import local media", "Bring videos, images, and audio into an offline-friendly project.", StudioPrimary),
    IntroPage("Sync every edit", "A centered playhead keeps preview, clips, overlays, and timecode aligned.", StudioSecondary),
    IntroPage("Export and share", "Render MP4 presets for Shorts, Reels, TikTok, and personal clips.", StudioAccent),
  )
}