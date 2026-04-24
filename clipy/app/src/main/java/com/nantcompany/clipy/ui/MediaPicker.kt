package com.nantcompany.clipy.ui

import android.content.ContentUris
import android.content.Context
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.collection.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nantcompany.clipy.R
import com.nantcompany.clipy.theme.ClipyBackground
import com.nantcompany.clipy.theme.ClipyMuted
import com.nantcompany.clipy.theme.ClipyOnDark
import com.nantcompany.clipy.theme.ClipyPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

enum class MediaTab {
  Videos,
  Photos,
  Live,
}

data class MediaAlbumUiModel(
  val id: String,
  val name: String,
  val count: Int,
  val coverUri: String?,
  val isSelected: Boolean,
)

data class MediaGridItemUiModel(
  val id: String,
  val uri: String,
  val type: String,
  val mimeType: String?,
  val displayName: String?,
  val thumbnailUri: String,
  val durationMs: Long?,
  val createdAtEpochMs: Long,
  val isCameraItem: Boolean,
  val selectionOrder: Int?,
  val isSelected: Boolean,
)

data class MediaQueryRequest(
  val tab: MediaTab,
  val collectionUri: Uri,
  val sortOrder: String,
)

private data class QueryExecution(
  val cursor: android.database.Cursor?,
  val mode: String,
)

data class MediaPermissionSnapshot(
  val sdkInt: Int,
  val hasReadMediaImages: Boolean,
  val hasReadMediaVideo: Boolean,
  val hasReadMediaVisualUserSelected: Boolean,
  val hasReadExternalStorage: Boolean,
)

data class MediaQueryResult(
  val albums: List<MediaAlbumUiModel>,
  val items: List<MediaGridItemUiModel>,
  val cursorCount: Int,
)

data class SelectedMediaUiModel(
  val id: String,
  val uri: String,
  val type: String,
  val mimeType: String?,
  val displayName: String?,
  val thumbnailUri: String,
  val durationMs: Long?,
  val order: Int,
)

data class MediaPickerUiState(
  val activeTab: MediaTab = MediaTab.Videos,
  val selectedAlbumId: String? = null,
  val albums: List<MediaAlbumUiModel> = emptyList(),
  val mediaItems: List<MediaGridItemUiModel> = emptyList(),
  val selectedItems: List<SelectedMediaUiModel> = emptyList(),
  val selectionLimit: Int? = 12,
  val isLoading: Boolean = false,
  val isNextEnabled: Boolean = false,
  val isProcessingContinue: Boolean = false,
  val initialScrollTarget: String? = null,
  val previewItemId: String? = null,
  val hasMediaPermission: Boolean = false,
  val canLoadMore: Boolean = false,
  val loadCursor: Int = 0,
  val lastQueryCursorCount: Int? = null,
  val isUsingPartialAccess: Boolean = false,
  val errorMessage: String? = null,
  val validationError: ContinueValidationIssue? = null,
)

enum class ContinueValidationIssue {
  MissingUri,
  UnsupportedMediaType,
  UnreadableMedia,
}

data class SelectedMediaItem(
  val id: String,
  val contentUri: String,
  val mediaType: MediaTab,
  val mimeType: String?,
  val displayName: String?,
  val durationMs: Long?,
)

data class ContinuePayload(
  val uris: List<String>,
  val primaryMediaType: String,
  val source: String,
)

internal data class ContinueSelectionResolution(
  val item: SelectedMediaUiModel?,
  val issue: ContinueValidationIssue?,
)

internal fun resolveContinueSelectionState(
  selectedItems: List<SelectedMediaUiModel>,
  visibleItems: List<MediaGridItemUiModel>? = null,
): ContinueSelectionResolution {
  val resolvedItem = when {
    visibleItems == null -> selectedItems.firstOrNull()
    selectedItems.isEmpty() -> null
    else -> resolveContinueSelection(selectedItems, visibleItems)
  }
  if (resolvedItem == null) {
    return ContinueSelectionResolution(
      item = null,
      issue = if (selectedItems.isEmpty()) ContinueValidationIssue.MissingUri else ContinueValidationIssue.UnreadableMedia,
    )
  }
  if (resolvedItem.uri.isBlank()) {
    return ContinueSelectionResolution(item = resolvedItem, issue = ContinueValidationIssue.MissingUri)
  }
  return ContinueSelectionResolution(item = resolvedItem, issue = null)
}

internal fun resolveContinueSelection(
  selectedItems: List<SelectedMediaUiModel>,
  visibleItems: List<MediaGridItemUiModel>,
): SelectedMediaUiModel? {
  selectedItems.forEach { selectedItem ->
    val visibleMatch = visibleItems.firstOrNull { !it.isCameraItem && it.id == selectedItem.id } ?: return@forEach
    return selectedItem.copy(
      uri = visibleMatch.uri,
      type = visibleMatch.type,
      mimeType = visibleMatch.mimeType ?: selectedItem.mimeType,
      displayName = visibleMatch.displayName ?: selectedItem.displayName,
      thumbnailUri = visibleMatch.thumbnailUri,
      durationMs = visibleMatch.durationMs ?: selectedItem.durationMs,
    )
  }
  return null
}

internal fun validateContinueSelection(
  selectedItems: List<SelectedMediaUiModel>,
  visibleItems: List<MediaGridItemUiModel>? = null,
): ContinueValidationIssue? {
  return resolveContinueSelectionState(
    selectedItems = selectedItems,
    visibleItems = visibleItems,
  ).issue
}

private const val MEDIA_PAGE_SIZE = 60
private const val MEDIA_PICKER_LOG_TAG = "ClipyMediaPicker"
private val thumbnailMemoryCache = LruCache<String, Bitmap>(96)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MediaPickerScreen(
  state: MediaPickerUiState,
  onBack: () -> Unit,
  onRequestPermissionRefresh: () -> Unit,
  onSelectTab: (MediaTab) -> Unit,
  onSelectAlbum: (String?) -> Unit,
  onToggleSelection: (String) -> Unit,
  onReorderSelection: (String, Int) -> Unit,
  onPreviewItem: (String?) -> Unit,
  onClearValidationError: () -> Unit,
  onConfirmSelection: () -> Unit,
  onLoadMore: () -> Unit,
) {
  val context = LocalContext.current
  val permissions = remember(state.activeTab) { permissionsForTab(state.activeTab, Build.VERSION.SDK_INT) }
  val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    onRequestPermissionRefresh()
  }
  var albumExpanded by rememberSaveable { mutableStateOf(false) }
  val pagerState = rememberPagerState(initialPage = state.activeTab.ordinal) { MediaTab.entries.size }

  LaunchedEffect(state.hasMediaPermission) {
    if (!state.hasMediaPermission) {
      permissionLauncher.launch(permissions)
    }
  }

  LaunchedEffect(state.activeTab) {
    if (pagerState.currentPage != state.activeTab.ordinal) {
      pagerState.animateScrollToPage(state.activeTab.ordinal)
    }
  }

  LaunchedEffect(pagerState.currentPage) {
    val pageTab = MediaTab.entries[pagerState.currentPage]
    if (pageTab != state.activeTab) {
      onSelectTab(pageTab)
    }
  }

  Scaffold(
    containerColor = ClipyBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xE6101010))
          .statusBarsPadding()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.media_picker_close), tint = ClipyOnDark)
          }
          Text(
            text = stringResource(
              when (state.activeTab) {
                MediaTab.Videos -> R.string.media_picker_title_videos
                MediaTab.Photos -> R.string.media_picker_title_photos
                MediaTab.Live -> R.string.media_picker_title_live
              },
            ),
            color = ClipyOnDark,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
          )
          TextButton(onClick = onConfirmSelection, enabled = state.isNextEnabled && !state.isProcessingContinue) {
            Text(
              text = stringResource(if (state.isProcessingContinue) R.string.media_picker_continue_processing else R.string.media_picker_next),
              color = if (state.isNextEnabled) ClipyPrimary else Color(0xFF6B7280),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            )
          }
        }
        Box {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF18181B),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable(enabled = state.albums.isNotEmpty()) { albumExpanded = true },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val selectedAlbum = state.albums.firstOrNull { it.isSelected }
              Text(selectedAlbum?.name ?: stringResource(R.string.media_picker_recent), color = ClipyOnDark)
              Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = ClipyMuted)
            }
          }
          DropdownMenu(expanded = albumExpanded, onDismissRequest = { albumExpanded = false }) {
            state.albums.forEach { album ->
              DropdownMenuItem(
                text = { Text("${album.name} (${album.count})") },
                onClick = {
                  albumExpanded = false
                  onSelectAlbum(album.id)
                },
              )
            }
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          MediaTab.entries.forEach { tab ->
            val active = state.activeTab == tab
            Surface(
              modifier = Modifier
                .widthIn(min = 88.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSelectTab(tab) },
              shape = RoundedCornerShape(14.dp),
              color = if (active) ClipyPrimary.copy(alpha = 0.14f) else Color(0xFF13151A),
            ) {
              Text(
                text = stringResource(
                  when (tab) {
                    MediaTab.Videos -> R.string.media_picker_tab_videos
                    MediaTab.Photos -> R.string.media_picker_tab_photos
                    MediaTab.Live -> R.string.media_picker_tab_live
                  },
                ),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = if (active) ClipyPrimary else Color(0xFF9CA3AF),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
              )
            }
          }
        }
      }
    },
    bottomBar = {
      AnimatedVisibility(visible = true) {
        Surface(
          color = Color(0xF218181B),
          tonalElevation = 0.dp,
          modifier = Modifier.navigationBarsPadding(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (state.selectedItems.isEmpty()) {
                  stringResource(R.string.media_picker_selection_helper_empty)
                } else {
                  stringResource(R.string.media_picker_selected_count, state.selectedItems.size)
                },
                color = ClipyOnDark,
                style = MaterialTheme.typography.labelLarge,
              )
              state.selectionLimit?.let {
                Text(
                  text = stringResource(R.string.media_picker_selection_limit, it),
                  color = ClipyMuted,
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            }
            Text(
              text = when {
                state.validationError != null -> stringResource(validationErrorMessageRes(state.validationError))
                state.isProcessingContinue -> stringResource(R.string.media_picker_continue_processing_detail)
                state.selectedItems.isEmpty() -> stringResource(R.string.media_picker_continue_hint_empty)
                state.isNextEnabled -> stringResource(R.string.media_picker_reorder_hint)
                else -> stringResource(R.string.media_picker_continue_hint_invalid)
              },
              color = if (state.validationError != null) Color(0xFFFFA3B3) else ClipyMuted,
              style = MaterialTheme.typography.bodySmall,
            )
            AnimatedVisibility(visible = state.validationError != null) {
              ContinueErrorBanner(
                issue = state.validationError,
                onRetry = onConfirmSelection,
                onChooseAnother = onClearValidationError,
                onCancel = onClearValidationError,
              )
            }
            AnimatedVisibility(visible = state.selectedItems.isNotEmpty()) {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
                items(state.selectedItems, key = { it.id }) { item ->
                  var dragAmount by remember(item.id) { mutableFloatStateOf(0f) }
                  Box(
                    modifier = Modifier
                      .size(width = 86.dp, height = 96.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(Color(0xFF111317))
                      .pointerInput(item.id) {
                        detectDragGestures(
                          onDragEnd = {
                            val steps = (dragAmount / 64f).roundToInt()
                            if (steps != 0) {
                              onReorderSelection(item.id, steps)
                            }
                            dragAmount = 0f
                          },
                          onDragCancel = { dragAmount = 0f },
                        ) { change, drag ->
                          change.consume()
                          dragAmount += drag.x
                        }
                      },
                  ) {
                    MediaThumbnail(
                      uri = item.thumbnailUri,
                      type = item.type,
                      modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                      modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(ClipyPrimary)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                      Text(item.order.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                      onClick = { onToggleSelection(item.id) },
                      modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
                    ) {
                      Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.media_picker_remove), tint = Color.White)
                    }
                  }
                }
              }
            }
            Button(
              onClick = onConfirmSelection,
              modifier = Modifier.fillMaxWidth().height(52.dp),
              enabled = state.isNextEnabled && !state.isProcessingContinue,
              colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary),
            ) {
              if (state.isProcessingContinue) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                  color = Color.White,
                )
                Spacer(Modifier.width(10.dp))
              }
              Text(stringResource(if (state.isProcessingContinue) R.string.media_picker_continue_processing else R.string.media_picker_next))
            }
          }
        }
      }
    },
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(ClipyBackground)
        .padding(top = padding.calculateTopPadding()),
      ) {
        PickerStatusStrip(
          state = state,
          onRetry = onRequestPermissionRefresh,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when {
          !state.hasMediaPermission -> {
            PermissionEmptyState(tab = state.activeTab, onGrant = { permissionLauncher.launch(permissions) })
        }

        state.isLoading && state.mediaItems.isEmpty() -> {
          ShimmerMediaGrid(modifier = Modifier.fillMaxSize())
        }

        state.mediaItems.isEmpty() && state.lastQueryCursorCount == 0 -> {
          EmptyMediaState(tab = state.activeTab)
        }

        else -> {
          HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
          ) { page ->
            val pageTab = MediaTab.entries[page]
            val pageItems = if (pageTab == state.activeTab) {
              state.mediaItems
            } else {
              emptyList()
            }
            MediaPickerGrid(
              tab = pageTab,
              items = pageItems,
              selectedItems = state.selectedItems,
              initialScrollTarget = if (pageTab == state.activeTab) state.initialScrollTarget else null,
              canLoadMore = pageTab == state.activeTab && state.canLoadMore,
              onLoadMore = onLoadMore,
              onItemClick = { item ->
                if (item.isCameraItem) {
                  Toast.makeText(context, R.string.media_picker_camera_unavailable, Toast.LENGTH_SHORT).show()
                } else {
                  onToggleSelection(item.id)
                }
              },
              onItemLongPress = { item ->
                if (!item.isCameraItem) {
                  onPreviewItem(item.id)
                }
              },
            )
          }
        }
      }
      state.previewItemId?.let { previewId ->
        val previewItem = state.mediaItems.firstOrNull { it.id == previewId } ?: state.selectedItems.firstOrNull { it.id == previewId }?.let {
          MediaGridItemUiModel(
            id = it.id,
            uri = it.uri,
            type = it.type,
            mimeType = it.mimeType,
            displayName = it.displayName,
            thumbnailUri = it.thumbnailUri,
            durationMs = it.durationMs,
            createdAtEpochMs = 0L,
            isCameraItem = false,
            selectionOrder = it.order,
            isSelected = true,
          )
        }
        if (previewItem != null) {
          PreviewOverlay(item = previewItem, onDismiss = { onPreviewItem(null) })
        }
      }
    }
  }
}

@Composable
private fun PickerStatusStrip(
  state: MediaPickerUiState,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val activeItemCount = state.mediaItems.count { !it.isCameraItem }
  val messageRes = when {
    state.errorMessage != null -> R.string.media_picker_status_error
    state.isLoading && state.hasMediaPermission -> R.string.media_picker_status_refreshing
    state.isUsingPartialAccess -> R.string.media_picker_status_partial
    state.lastQueryCursorCount != null && activeItemCount > 0 -> R.string.media_picker_status_loaded
    else -> null
  } ?: return

  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (state.errorMessage != null) Color(0xFF2A1820) else Color(0xFF151C28),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (state.isLoading && state.errorMessage == null) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = ClipyPrimary,
        )
      } else {
        Icon(
          imageVector = if (state.errorMessage != null) Icons.Rounded.Close else Icons.Rounded.Refresh,
          contentDescription = null,
          tint = if (state.errorMessage != null) Color(0xFFFF8AA0) else ClipyPrimary,
          modifier = Modifier.size(16.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = stringResource(messageRes),
          color = ClipyOnDark,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        val detail = when {
          state.errorMessage != null -> state.errorMessage
          state.isUsingPartialAccess -> stringResource(R.string.media_picker_status_partial_detail, activeItemCount)
          state.lastQueryCursorCount != null && activeItemCount > 0 -> stringResource(R.string.media_picker_status_loaded_detail, activeItemCount)
          else -> stringResource(R.string.media_picker_status_refreshing_detail)
        }
        Text(
          text = detail,
          color = ClipyMuted,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (state.errorMessage != null) {
        TextButton(onClick = onRetry) {
          Text(text = stringResource(R.string.media_picker_status_retry), color = ClipyPrimary)
        }
      }
    }
  }
}

@Composable
private fun MediaPickerGrid(
  tab: MediaTab,
  items: List<MediaGridItemUiModel>,
  selectedItems: List<SelectedMediaUiModel>,
  initialScrollTarget: String?,
  canLoadMore: Boolean,
  onLoadMore: () -> Unit,
  onItemClick: (MediaGridItemUiModel) -> Unit,
  onItemLongPress: (MediaGridItemUiModel) -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val columns = if (maxWidth >= 600.dp) 4 else 3
    val gridState = rememberLazyGridState()
    val bottomPadding = if (selectedItems.isEmpty()) 24.dp else 196.dp
    val newestIndex = remember(items, initialScrollTarget) {
      initialScrollTarget?.let { targetId -> items.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } } ?: 0
    }

    LaunchedEffect(tab, items, newestIndex) {
      if (items.isNotEmpty()) {
        gridState.scrollToItem(newestIndex.coerceAtMost(items.lastIndex))
      }
    }

    LaunchedEffect(gridState, items, canLoadMore) {
      snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
        .distinctUntilChanged()
        .collect { lastVisibleIndex ->
          if (canLoadMore && lastVisibleIndex != null && lastVisibleIndex >= items.lastIndex - columns * 2) {
            onLoadMore()
          }
        }
    }

    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 4.dp, bottom = bottomPadding),
      horizontalArrangement = Arrangement.spacedBy(3.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      items(items, key = { it.id }) { item ->
        MediaGridCell(
          item = item,
          onClick = { onItemClick(item) },
          onLongPress = { onItemLongPress(item) },
        )
      }
      if (canLoadMore) {
        items(columns) { index ->
          if (index == 0) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = ClipyPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
          } else {
            Box(modifier = Modifier.aspectRatio(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionEmptyState(tab: MediaTab, onGrant: () -> Unit) {
  val icon = if (tab == MediaTab.Videos) Icons.Rounded.VideoLibrary else Icons.Rounded.Collections
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(shape = CircleShape, color = ClipyPrimary.copy(alpha = 0.14f)) {
      Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ClipyPrimary, modifier = Modifier.size(28.dp))
      }
    }
    Spacer(Modifier.height(18.dp))
    Text(stringResource(R.string.media_picker_permission_title), color = ClipyOnDark, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.media_picker_permission_body), color = ClipyMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    Button(onClick = onGrant, colors = ButtonDefaults.buttonColors(containerColor = ClipyPrimary)) {
      Text(stringResource(R.string.media_picker_permission_action))
    }
  }
}

@Composable
private fun EmptyMediaState(tab: MediaTab) {
  val bodyRes = when (tab) {
    MediaTab.Videos -> R.string.media_picker_empty_videos
    MediaTab.Photos -> R.string.media_picker_empty_photos
    MediaTab.Live -> R.string.media_picker_empty_live
  }
  val icon = if (tab == MediaTab.Videos) Icons.Rounded.VideoLibrary else Icons.Rounded.Collections
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(shape = CircleShape, color = Color(0xFF182033)) {
      Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = ClipyPrimary, modifier = Modifier.size(28.dp))
      }
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.media_picker_empty_title), color = ClipyOnDark, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(bodyRes), color = ClipyMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(R.string.media_picker_empty_hint),
      color = ClipyMuted,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
    )
  }
}

@Composable
private fun ShimmerMediaGrid(modifier: Modifier = Modifier) {
  BoxWithConstraints(modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
    val columns = if (maxWidth >= 600.dp) 4 else 3
    LazyVerticalGrid(
      columns = GridCells.Fixed(columns),
      contentPadding = PaddingValues(bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      items(15) {
        ShimmerTile()
      }
    }
  }
}

@Composable
private fun ShimmerTile() {
  val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pickerShimmer")
  val shimmerOffset by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
      animation = androidx.compose.animation.core.tween(durationMillis = 900),
      repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
    ),
    label = "pickerShimmerOffset",
  )
  val brush = Brush.linearGradient(
    colors = listOf(Color(0xFF141923), Color(0xFF20293B), Color(0xFF141923)),
    start = androidx.compose.ui.geometry.Offset.Zero,
    end = androidx.compose.ui.geometry.Offset(280f * shimmerOffset, 280f * shimmerOffset),
  )
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(12.dp))
      .background(brush),
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridCell(item: MediaGridItemUiModel, onClick: () -> Unit, onLongPress: () -> Unit) {
  val scale by animateFloatAsState(
    targetValue = if (item.isSelected) 0.97f else 1f,
    animationSpec = spring(stiffness = 700f, dampingRatio = 0.82f),
    label = "pickerScale",
  )
  Box(
    modifier = Modifier
      .aspectRatio(1f)
      .scale(scale)
      .semantics {
        contentDescription = buildString {
          append(
            when {
              item.isCameraItem -> "Camera"
              item.type == "video" -> "Video"
              item.type == "live" -> "Live photo"
              else -> "Photo"
            },
          )
          item.durationMs?.takeIf { item.type == "video" }?.let {
            append(", ${formatDuration(it)}")
          }
          item.selectionOrder?.let {
            append(", selected $it")
          }
        }
      }
      .clip(RoundedCornerShape(10.dp))
      .background(if (item.isCameraItem) Color(0xFF18181B) else Color(0xFF0F1115))
      .border(if (item.isSelected) 2.dp else 1.dp, if (item.isSelected) ClipyPrimary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
      .combinedClickable(onClick = onClick, onLongClick = onLongPress),
  ) {
    if (item.isCameraItem) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(Icons.Rounded.CameraAlt, contentDescription = stringResource(R.string.media_picker_camera), tint = ClipyOnDark, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.media_picker_camera), color = ClipyOnDark, style = MaterialTheme.typography.labelMedium)
      }
    } else {
      MediaThumbnail(uri = item.thumbnailUri, type = item.type, modifier = Modifier.fillMaxSize())
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .align(Alignment.TopCenter)
          .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
      )
      if (item.type == "video") {
        Row(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
          Text(formatDuration(item.durationMs ?: 0L), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
      }
      Text(
        text = stringResource(R.string.media_picker_hold_preview),
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(6.dp)
          .clip(RoundedCornerShape(999.dp))
          .background(Color.Black.copy(alpha = 0.38f))
          .padding(horizontal = 7.dp, vertical = 3.dp),
        color = Color.White.copy(alpha = 0.92f),
        style = MaterialTheme.typography.labelSmall,
      )
      if (item.type == "live") {
        Text(
          text = stringResource(R.string.media_picker_live_badge),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
          color = Color.White,
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
    val selectionLabel = item.selectionOrder?.toString()
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(6.dp)
        .size(24.dp)
        .clip(CircleShape)
        .background(if (item.isSelected) ClipyPrimary else Color.Black.copy(alpha = 0.28f))
        .border(1.dp, if (item.isSelected) ClipyPrimary else Color.White.copy(alpha = 0.3f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      when {
        selectionLabel != null -> Text(selectionLabel, color = Color.White, style = MaterialTheme.typography.labelSmall)
        item.isCameraItem -> Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Transparent, modifier = Modifier.size(12.dp))
        else -> Spacer(Modifier.size(10.dp))
      }
    }
  }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun PreviewOverlay(item: MediaGridItemUiModel, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val player = remember(item.id) { if (item.type == "video") ExoPlayer.Builder(context).build() else null }

  LaunchedEffect(item.id) {
    player?.apply {
      setMediaItem(MediaItem.fromUri(item.uri))
      prepare()
      playWhenReady = true
    }
  }

  androidx.compose.runtime.DisposableEffect(item.id) {
    onDispose { player?.release() }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.84f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF111317),
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.media_picker_close), tint = Color.White)
          }
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF090C11)),
          contentAlignment = Alignment.Center,
        ) {
          if (item.type == "video") {
            AndroidView(
              factory = { viewContext ->
                PlayerView(viewContext).apply {
                  useController = false
                  resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                  this.player = player
                }
              },
              update = { it.player = player },
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            MediaThumbnail(uri = item.thumbnailUri, type = item.type, modifier = Modifier.fillMaxSize())
          }
        }
      }
    }
  }
}

@Composable
private fun MediaThumbnail(uri: String, type: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  var bitmap by remember(uri, type) { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(uri, type) {
    bitmap = thumbnailMemoryCache.get(uri) ?: withContext(Dispatchers.IO) { loadThumbnailBitmap(context, uri, type, 420) }?.also {
      thumbnailMemoryCache.put(uri, it)
    }
  }
  Box(modifier = modifier.background(Color(0xFF1A1D22)), contentAlignment = Alignment.Center) {
    bitmap?.let {
      androidx.compose.foundation.Image(
        bitmap = it.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } ?: CircularProgressIndicator(color = ClipyPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
  }
}

private suspend fun loadThumbnailBitmap(context: Context, uriString: String, type: String, sizePx: Int): Bitmap? {
  val uri = Uri.parse(uriString)
  return runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
    } else if (type == "video") {
      MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      }
    } else {
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
      }
    }
  }.getOrNull()?.let { bitmap ->
    if (bitmap.width > sizePx * 2 || bitmap.height > sizePx * 2) {
      Bitmap.createScaledBitmap(bitmap, sizePx.coerceAtLeast(1), sizePx.coerceAtLeast(1), true)
    } else {
      bitmap
    }
  }
}

fun queryDeviceMedia(
  context: Context,
  tab: MediaTab,
  limit: Int = MEDIA_PAGE_SIZE,
  offset: Int = 0,
): MediaQueryResult {
  val request = buildMediaQueryRequest(tab)
  val collection = request.collectionUri
  val projection = arrayOf(
    MediaStore.MediaColumns._ID,
    MediaStore.MediaColumns.DATE_ADDED,
    MediaStore.MediaColumns.MIME_TYPE,
    MediaStore.MediaColumns.BUCKET_ID,
    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
    MediaStore.MediaColumns.DISPLAY_NAME,
    if (tab == MediaTab.Videos) MediaStore.Video.VideoColumns.DURATION else MediaStore.Images.ImageColumns.DATE_TAKEN,
  )
  val sortOrder = request.sortOrder
  val rows = mutableListOf<MediaGridItemUiModel>()
  val albums = linkedMapOf<String, Pair<String, MutableList<MediaGridItemUiModel>>>()
  val permissionSnapshot = mediaPermissionSnapshot(context)
  val grantedPermissions = buildList {
    if (permissionSnapshot.hasReadMediaImages) add(android.Manifest.permission.READ_MEDIA_IMAGES)
    if (permissionSnapshot.hasReadMediaVideo) add(android.Manifest.permission.READ_MEDIA_VIDEO)
    if (permissionSnapshot.hasReadMediaVisualUserSelected) add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    if (permissionSnapshot.hasReadExternalStorage) add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
  }
  Log.d(
    MEDIA_PICKER_LOG_TAG,
    "queryDeviceMedia sdk=${permissionSnapshot.sdkInt} tab=$tab permissions=$grantedPermissions uri=$collection offset=$offset limit=$limit",
  )
  var cursorCount = 0
  val execution = queryMediaCursor(
    context = context,
    collection = collection,
    projection = projection,
    sortOrder = sortOrder,
    limit = limit,
    offset = offset,
  )
  val cursor = execution.cursor
  if (cursor == null) {
    Log.w(MEDIA_PICKER_LOG_TAG, "queryDeviceMedia returned null cursor for tab=$tab uri=$collection mode=${execution.mode}")
  }
  cursor?.use { cursor ->
    cursorCount = cursor.count
    Log.d(MEDIA_PICKER_LOG_TAG, "queryDeviceMedia cursorCount=$cursorCount tab=$tab uri=$collection mode=${execution.mode}")
    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
    val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
    val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
    val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
    val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
    val durationOrDateColumn = if (tab == MediaTab.Videos) {
      cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)
    } else {
      cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
    }
    while (cursor.moveToNext()) {
      val mimeType = cursor.getString(mimeColumn).orEmpty()
      val inferredType = when {
        tab == MediaTab.Videos -> "video"
        mimeType.contains("motion", ignoreCase = true) || mimeType.contains("live", ignoreCase = true) -> "live"
        tab == MediaTab.Live -> "live"
        else -> "photo"
      }
      if (tab == MediaTab.Live && inferredType != "live") continue
      if (tab == MediaTab.Photos && inferredType == "live") continue
      val id = cursor.getLong(idColumn)
      val contentUri = ContentUris.withAppendedId(collection, id)
      val displayName = cursor.getString(displayNameColumn).orEmpty()
      val durationMs = if (tab == MediaTab.Videos) cursor.getLong(durationOrDateColumn).coerceAtLeast(0L) else null
      Log.d(
        MEDIA_PICKER_LOG_TAG,
        "queryDeviceMedia row tab=$tab id=$id uri=$contentUri displayName=$displayName durationMs=$durationMs mimeType=$mimeType",
      )
      val item = MediaGridItemUiModel(
        id = "asset-$id",
        uri = contentUri.toString(),
        type = inferredType,
        mimeType = mimeType.ifBlank { null },
        displayName = displayName.ifBlank { null },
        thumbnailUri = contentUri.toString(),
        durationMs = durationMs,
        createdAtEpochMs = cursor.getLong(dateColumn) * 1000L,
        isCameraItem = false,
        selectionOrder = null,
        isSelected = false,
      )
      rows += item
      val bucketId = cursor.getString(bucketIdColumn) ?: "recent"
      val bucketName = cursor.getString(bucketNameColumn) ?: context.getString(R.string.media_picker_album)
      albums.getOrPut(bucketId) { bucketName to mutableListOf() }.second.add(item)
    }
  }

  if (offset == 0) {
    context.contentResolver.query(
      collection,
      arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.BUCKET_ID,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
      ),
      null,
      null,
      sortOrder,
    )?.use { albumCursor ->
      val idColumn = albumCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
      val bucketIdColumn = albumCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
      val bucketNameColumn = albumCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
      while (albumCursor.moveToNext()) {
        val bucketId = albumCursor.getString(bucketIdColumn) ?: "recent"
        val bucketName = albumCursor.getString(bucketNameColumn) ?: context.getString(R.string.media_picker_album)
        val id = albumCursor.getLong(idColumn)
        val coverUri = ContentUris.withAppendedId(collection, id).toString()
        val bucketEntry = albums.getOrPut(bucketId) { bucketName to mutableListOf() }
        if (bucketEntry.second.isEmpty()) {
          bucketEntry.second += MediaGridItemUiModel(
            id = "cover-$id",
            uri = coverUri,
            type = if (tab == MediaTab.Videos) "video" else "photo",
            mimeType = null,
            displayName = null,
            thumbnailUri = coverUri,
            durationMs = null,
            createdAtEpochMs = 0L,
            isCameraItem = false,
            selectionOrder = null,
            isSelected = false,
          )
        }
      }
    }
  }

  val recentAlbum = MediaAlbumUiModel(
    id = "recent",
    name = context.getString(R.string.media_picker_recent),
    count = if (offset == 0) rows.size else -1,
    coverUri = rows.firstOrNull()?.thumbnailUri,
    isSelected = true,
  )
  val albumModels = buildList {
    add(recentAlbum)
    albums.entries.forEach { entry ->
      val first = entry.value.second.firstOrNull()
      add(
        MediaAlbumUiModel(
          id = entry.key,
          name = entry.value.first,
          count = entry.value.second.size,
          coverUri = first?.thumbnailUri,
          isSelected = false,
        ),
      )
    }
  }
  val withCamera = listOf(
    MediaGridItemUiModel(
      id = "camera",
      uri = "",
      type = "camera",
      mimeType = null,
      displayName = null,
      thumbnailUri = "",
      durationMs = null,
      createdAtEpochMs = Long.MAX_VALUE,
      isCameraItem = true,
      selectionOrder = null,
      isSelected = false,
    ),
  ) + rows
  Log.d(MEDIA_PICKER_LOG_TAG, "queryDeviceMedia finalListSize=${rows.size} tab=$tab offset=$offset")
  return MediaQueryResult(
    albums = albumModels,
    items = withCamera,
    cursorCount = cursorCount,
  )
}

@Composable
private fun ContinueErrorBanner(
  issue: ContinueValidationIssue?,
  onRetry: () -> Unit,
  onChooseAnother: () -> Unit,
  onCancel: () -> Unit,
) {
  if (issue == null) return
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1820)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = stringResource(R.string.media_picker_continue_error_title),
        color = ClipyOnDark,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      )
      Text(
        text = stringResource(validationErrorMessageRes(issue)),
        color = Color(0xFFFFA3B3),
        style = MaterialTheme.typography.bodySmall,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onRetry) {
          Text(text = stringResource(R.string.media_picker_status_retry), color = ClipyPrimary)
        }
        TextButton(onClick = onChooseAnother) {
          Text(text = stringResource(R.string.media_picker_continue_choose_another), color = ClipyPrimary)
        }
        TextButton(onClick = onCancel) {
          Text(text = stringResource(R.string.media_picker_continue_cancel), color = ClipyMuted)
        }
      }
    }
  }
}

private fun validationErrorMessageRes(issue: ContinueValidationIssue): Int {
  return when (issue) {
    ContinueValidationIssue.MissingUri -> R.string.media_picker_continue_error_missing_uri
    ContinueValidationIssue.UnsupportedMediaType -> R.string.media_picker_continue_error_unsupported_type
    ContinueValidationIssue.UnreadableMedia -> R.string.media_picker_continue_error_unreadable
  }
}

private fun queryMediaCursor(
  context: Context,
  collection: Uri,
  projection: Array<String>,
  sortOrder: String,
  limit: Int,
  offset: Int,
): QueryExecution {
  fun legacyQuery(): QueryExecution {
    val cursor = context.contentResolver.query(
      collection,
      projection,
      null,
      null,
      "$sortOrder LIMIT $limit OFFSET $offset",
    )
    return QueryExecution(cursor = cursor, mode = "legacy_sort_order")
  }

  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
    return legacyQuery()
  }

  val argsCursor = runCatching {
    val queryArgs = Bundle().apply {
      putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
      putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
      putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
    }
    context.contentResolver.query(collection, projection, queryArgs, null)
  }.onFailure {
    Log.w(MEDIA_PICKER_LOG_TAG, "queryMediaCursor queryArgs failed for uri=$collection offset=$offset", it)
  }.getOrNull()

  if (argsCursor != null) {
    val count = runCatching { argsCursor.count }.getOrDefault(0)
    if (count > 0 || offset > 0) {
      return QueryExecution(cursor = argsCursor, mode = "query_args")
    }
    Log.d(MEDIA_PICKER_LOG_TAG, "queryMediaCursor queryArgs returned empty, retrying legacy mode for uri=$collection")
    argsCursor.close()
  }

  return legacyQuery()
}

private fun buildMediaQueryRequest(tab: MediaTab): MediaQueryRequest {
  return MediaQueryRequest(
    tab = tab,
    collectionUri = when (tab) {
      MediaTab.Videos -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
      MediaTab.Photos, MediaTab.Live -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    },
    sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC",
  )
}

private fun formatDuration(durationMs: Long): String {
  val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return "%d:%02d".format(minutes, seconds)
}

internal fun mediaPermissionSnapshot(context: Context): MediaPermissionSnapshot {
  fun isGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }

  return MediaPermissionSnapshot(
    sdkInt = Build.VERSION.SDK_INT,
    hasReadMediaImages = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isGranted(android.Manifest.permission.READ_MEDIA_IMAGES),
    hasReadMediaVideo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isGranted(android.Manifest.permission.READ_MEDIA_VIDEO),
    hasReadMediaVisualUserSelected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && isGranted(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
    hasReadExternalStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && isGranted(android.Manifest.permission.READ_EXTERNAL_STORAGE),
  )
}

internal fun hasMediaAccess(snapshot: MediaPermissionSnapshot, tab: MediaTab = MediaTab.Videos): Boolean {
  return when {
    snapshot.sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
      when (tab) {
        MediaTab.Videos -> snapshot.hasReadMediaVideo || snapshot.hasReadMediaVisualUserSelected
        MediaTab.Photos, MediaTab.Live -> snapshot.hasReadMediaImages || snapshot.hasReadMediaVisualUserSelected
      }
    }
    snapshot.sdkInt >= Build.VERSION_CODES.TIRAMISU -> {
      when (tab) {
        MediaTab.Videos -> snapshot.hasReadMediaVideo
        MediaTab.Photos, MediaTab.Live -> snapshot.hasReadMediaImages
      }
    }
    else -> snapshot.hasReadExternalStorage
  }
}

fun hasMediaAccess(context: Context, tab: MediaTab = MediaTab.Videos): Boolean {
  return hasMediaAccess(mediaPermissionSnapshot(context), tab)
}

internal fun isUsingPartialMediaAccess(snapshot: MediaPermissionSnapshot, tab: MediaTab): Boolean {
  if (snapshot.sdkInt < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || !snapshot.hasReadMediaVisualUserSelected) {
    return false
  }
  return when (tab) {
    MediaTab.Videos -> !snapshot.hasReadMediaVideo
    MediaTab.Photos, MediaTab.Live -> !snapshot.hasReadMediaImages
  }
}

internal fun permissionsForTab(tab: MediaTab, sdkInt: Int): Array<String> {
  return when {
    sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> when (tab) {
      MediaTab.Videos -> arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
      MediaTab.Photos, MediaTab.Live -> arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }
    sdkInt >= Build.VERSION_CODES.TIRAMISU -> when (tab) {
      MediaTab.Videos -> arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO)
      MediaTab.Photos, MediaTab.Live -> arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
    }
    else -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
  }
}
