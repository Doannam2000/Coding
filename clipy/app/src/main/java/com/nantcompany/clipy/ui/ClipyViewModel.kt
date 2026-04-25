package com.nantcompany.clipy.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nantcompany.clipy.data.ClipyRepository
import com.nantcompany.clipy.data.ClipyRepository.AppSnapshot
import com.nantcompany.clipy.model.AppLanguage
import com.nantcompany.clipy.model.CropRatio
import com.nantcompany.clipy.model.ExportFormat
import com.nantcompany.clipy.model.Mp4Quality
import com.nantcompany.clipy.model.UserPreferences
import com.nantcompany.clipy.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClipyViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = ClipyRepository.getInstance(application)
  private val mediaPickerState = MutableStateFlow(MediaPickerUiState())
  private val pickerTabSnapshots = mutableMapOf<MediaTab, PickerTabSnapshot>()
  private val pickerRefreshRequestIds = mutableMapOf<MediaTab, Long>()
  private var nextPickerRefreshRequestId = 0L

  val appState: StateFlow<AppSnapshot> = repository.appSnapshot.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5_000),
    AppSnapshot(UserPreferences(), repository.draft.value, repository.exportJob.value, emptyList()),
  )
  val pickerState: StateFlow<MediaPickerUiState> = mediaPickerState.asStateFlow()

  init {
    viewModelScope.launch { repository.applyDefaultSettingsToDraft() }
    refreshMediaPermissionAndContent()
  }

  fun completeOnboarding(language: AppLanguage) {
    viewModelScope.launch { repository.completeOnboarding(language.code) }
  }

  fun importVideo(uri: Uri) {
    repository.loadVideo(uri)
  }

  fun openMediaPicker(tab: MediaTab = MediaTab.Videos) {
    mediaPickerState.value = mediaPickerState.value.copy(activeTab = tab, previewItemId = null)
    refreshMediaPermissionAndContent(forceTab = tab)
  }

  fun refreshMediaPermissionAndContent(forceTab: MediaTab? = null) {
    val context = getApplication<Application>()
    val tab = forceTab ?: mediaPickerState.value.activeTab
    val permissionSnapshot = mediaPermissionSnapshot(context)
    val hasPermission = hasMediaAccess(permissionSnapshot, tab)
    val requestId = nextPickerRefreshRequestId + 1L
    nextPickerRefreshRequestId = requestId
    pickerRefreshRequestIds[tab] = requestId
    val cachedSnapshot = pickerTabSnapshots[tab]
    Log.d(
      "ClipyMediaPicker",
      "refreshMediaPermissionAndContent sdk=${permissionSnapshot.sdkInt} tab=$tab hasPermission=$hasPermission partial=${isUsingPartialMediaAccess(permissionSnapshot, tab)} requestId=$requestId",
    )
    if (!hasPermission) {
      pickerTabSnapshots.remove(tab)
      mediaPickerState.value = mediaPickerState.value.copy(
        activeTab = tab,
        hasMediaPermission = false,
        isLoading = false,
        albums = emptyList(),
        mediaItems = emptyList(),
        canLoadMore = false,
        loadCursor = 0,
        lastQueryCursorCount = null,
        isUsingPartialAccess = false,
        errorMessage = null,
      )
      return
    }
    viewModelScope.launch {
      mediaPickerState.value = mediaPickerState.value.withVisibleTabSnapshot(
        tab = tab,
        snapshot = cachedSnapshot,
        hasPermission = true,
        isLoading = true,
        isUsingPartialAccess = isUsingPartialMediaAccess(permissionSnapshot, tab),
        errorMessage = null,
      )
      val selectedIds = mediaPickerState.value.selectedItems.map { it.id }
      val selectedAlbumId = cachedSnapshot?.selectedAlbumId ?: mediaPickerState.value.selectedAlbumId
      runCatching {
        withContext(Dispatchers.IO) { queryDeviceMedia(context, tab, offset = 0) }
      }.onSuccess { result ->
        val albums = result.albums.mapIndexed { index, album ->
          album.copy(isSelected = (selectedAlbumId ?: "recent").let { current -> if (index == 0 && selectedAlbumId == null) true else album.id == current })
        }
        val items = applySelection(result.items, selectedIds)
        val snapshot = PickerTabSnapshot(
          albums = albums,
          items = items,
          selectedAlbumId = selectedAlbumId,
          canLoadMore = result.items.count { !it.isCameraItem } >= 60,
          loadCursor = result.items.count { !it.isCameraItem },
          lastQueryCursorCount = result.cursorCount,
          initialScrollTarget = result.items.firstOrNull { !it.isCameraItem }?.id,
        )
        pickerTabSnapshots[tab] = snapshot
        Log.d(
          "ClipyMediaPicker",
          "refreshMediaPermissionAndContent loaded tab=$tab cursorCount=${result.cursorCount} finalSize=${result.items.count { !it.isCameraItem }} requestId=$requestId applyVisible=${shouldApplyVisiblePickerResult(mediaPickerState.value.activeTab, tab, pickerRefreshRequestIds[tab], requestId)}",
        )
        if (shouldApplyVisiblePickerResult(mediaPickerState.value.activeTab, tab, pickerRefreshRequestIds[tab], requestId)) {
          mediaPickerState.value = mediaPickerState.value.withVisibleTabSnapshot(
            tab = tab,
            snapshot = snapshot,
            hasPermission = true,
            isLoading = false,
            isUsingPartialAccess = isUsingPartialMediaAccess(permissionSnapshot, tab),
            errorMessage = null,
          )
        }
        if (selectedIds.isNotEmpty()) {
          rebuildSelectedItems()
        }
      }.onFailure { throwable ->
        Log.e("ClipyMediaPicker", "refreshMediaPermissionAndContent failed for tab=$tab", throwable)
        if (shouldApplyVisiblePickerResult(mediaPickerState.value.activeTab, tab, pickerRefreshRequestIds[tab], requestId)) {
          mediaPickerState.value = mediaPickerState.value.withVisibleTabSnapshot(
            tab = tab,
            snapshot = cachedSnapshot,
            hasPermission = true,
            isLoading = false,
            isUsingPartialAccess = isUsingPartialMediaAccess(permissionSnapshot, tab),
            errorMessage = throwable.message,
          )
        }
      }
    }
  }

  fun loadMorePickerItems() {
    val currentState = mediaPickerState.value
    if (currentState.isLoading || !currentState.canLoadMore || currentState.selectedAlbumId != null && currentState.selectedAlbumId != "recent") {
      return
    }
    val context = getApplication<Application>()
    val tab = currentState.activeTab
    viewModelScope.launch {
      mediaPickerState.value = currentState.copy(isLoading = true)
      val result = withContext(Dispatchers.IO) {
        queryDeviceMedia(context, tab, offset = currentState.loadCursor)
      }
      val existingIds = mediaPickerState.value.mediaItems.map { it.id }.toSet()
      val appended = result.items.filterNot { it.id in existingIds || it.isCameraItem }
      val mergedItems = mediaPickerState.value.mediaItems + appended
      val updatedState = mediaPickerState.value.copy(
        isLoading = false,
        mediaItems = applySelection(mergedItems, mediaPickerState.value.selectedItems.map { it.id }),
        canLoadMore = appended.isNotEmpty() && appended.size >= 60,
        loadCursor = currentState.loadCursor + appended.size,
        lastQueryCursorCount = result.cursorCount,
      )
      mediaPickerState.value = updatedState
      pickerTabSnapshots[tab] = PickerTabSnapshot(
        albums = updatedState.albums,
        items = updatedState.mediaItems,
        selectedAlbumId = updatedState.selectedAlbumId,
        canLoadMore = updatedState.canLoadMore,
        loadCursor = updatedState.loadCursor,
        lastQueryCursorCount = updatedState.lastQueryCursorCount,
        initialScrollTarget = updatedState.initialScrollTarget,
      )
    }
  }

  fun selectPickerTab(tab: MediaTab) {
    val permissionSnapshot = mediaPermissionSnapshot(getApplication())
    mediaPickerState.value = mediaPickerState.value.withVisibleTabSnapshot(
      tab = tab,
      snapshot = pickerTabSnapshots[tab],
      hasPermission = hasMediaAccess(permissionSnapshot, tab),
      isLoading = hasMediaAccess(permissionSnapshot, tab),
      isUsingPartialAccess = isUsingPartialMediaAccess(permissionSnapshot, tab),
      errorMessage = null,
    )
    refreshMediaPermissionAndContent(forceTab = tab)
  }

  fun selectPickerAlbum(albumId: String?) {
    val normalizedId = albumId ?: "recent"
    val currentState = mediaPickerState.value
    val baseItems = currentState.mediaItems
    val filtered = if (normalizedId == "recent") {
      baseItems
    } else {
      val context = getApplication<Application>()
      val allItems = queryDeviceMedia(context, mediaPickerState.value.activeTab).items
      val matchingUris = mutableSetOf<String>()
      context.contentResolver.query(
        when (mediaPickerState.value.activeTab) {
          MediaTab.Videos -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
          MediaTab.Photos, MediaTab.Live -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        },
        arrayOf(android.provider.MediaStore.MediaColumns._ID, android.provider.MediaStore.MediaColumns.BUCKET_ID),
        "${android.provider.MediaStore.MediaColumns.BUCKET_ID}=?",
        arrayOf(normalizedId),
        "${android.provider.MediaStore.MediaColumns.DATE_ADDED} DESC",
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
        while (cursor.moveToNext()) {
          val contentUri = android.content.ContentUris.withAppendedId(
            when (mediaPickerState.value.activeTab) {
              MediaTab.Videos -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
              MediaTab.Photos, MediaTab.Live -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            },
            cursor.getLong(idColumn),
          )
          matchingUris += contentUri.toString()
        }
      }
      val cameraItem = allItems.firstOrNull { it.isCameraItem } ?: baseItems.firstOrNull { it.isCameraItem }
      listOfNotNull(cameraItem) + allItems.filter { !it.isCameraItem && it.uri in matchingUris }
    }
    mediaPickerState.value = mediaPickerState.value.copy(
      selectedAlbumId = normalizedId,
      albums = mediaPickerState.value.albums.map { it.copy(isSelected = it.id == normalizedId || (normalizedId == "recent" && it.id == "recent")) },
      mediaItems = applySelection(filtered, mediaPickerState.value.selectedItems.map { it.id }),
      initialScrollTarget = filtered.firstOrNull { !it.isCameraItem }?.id,
    )
    pickerTabSnapshots[mediaPickerState.value.activeTab] = PickerTabSnapshot(
      albums = mediaPickerState.value.albums,
      items = mediaPickerState.value.mediaItems,
      selectedAlbumId = mediaPickerState.value.selectedAlbumId,
      canLoadMore = mediaPickerState.value.canLoadMore,
      loadCursor = mediaPickerState.value.loadCursor,
      lastQueryCursorCount = mediaPickerState.value.lastQueryCursorCount,
      initialScrollTarget = mediaPickerState.value.initialScrollTarget,
    )
  }

  fun togglePickerSelection(itemId: String) {
    val state = mediaPickerState.value
    val existingIndex = state.selectedItems.indexOfFirst { it.id == itemId }
    val updatedSelection = if (existingIndex >= 0) {
      state.selectedItems.filterNot { it.id == itemId }
    } else {
      val selectedItem = state.mediaItems.firstOrNull { it.id == itemId && !it.isCameraItem } ?: return
      if (state.selectionLimit != null && state.selectedItems.size >= state.selectionLimit) return
      state.selectedItems + SelectedMediaUiModel(
        id = selectedItem.id,
        uri = selectedItem.uri,
        type = selectedItem.type,
        mimeType = selectedItem.mimeType,
        displayName = selectedItem.displayName,
        thumbnailUri = selectedItem.thumbnailUri,
        durationMs = selectedItem.durationMs,
        order = state.selectedItems.size + 1,
      )
    }
    mediaPickerState.value = state.copy(
      selectedItems = normalizeSelectionOrders(updatedSelection),
      validationError = null,
      isProcessingContinue = false,
    )
    rebuildSelectedItems()
  }

  fun reorderPickerSelection(itemId: String, delta: Int) {
    val current = mediaPickerState.value.selectedItems.toMutableList()
    val fromIndex = current.indexOfFirst { it.id == itemId }
    if (fromIndex < 0) return
    val toIndex = (fromIndex + delta).coerceIn(0, current.lastIndex)
    if (toIndex == fromIndex) return
    val moved = current.removeAt(fromIndex)
    current.add(toIndex, moved)
    mediaPickerState.value = mediaPickerState.value.copy(selectedItems = normalizeSelectionOrders(current))
    rebuildSelectedItems()
  }

  fun previewPickerItem(itemId: String?) {
    mediaPickerState.value = mediaPickerState.value.copy(previewItemId = itemId)
  }

  fun clearContinueValidationError() {
    mediaPickerState.value = mediaPickerState.value.copy(validationError = null, isProcessingContinue = false)
  }

  fun confirmPickerSelection(): ContinuePayload? {
    val state = mediaPickerState.value
    val resolution = resolveContinueSelectionState(state.selectedItems, state.mediaItems)
    val resolvedSelection = resolution.item
    val validationError = resolution.issue
    if (validationError != null) {
      mediaPickerState.value = state.copy(validationError = validationError, isProcessingContinue = false)
      return null
    }

    mediaPickerState.value = state.copy(isProcessingContinue = true, validationError = null)
    val selectedItem = resolvedSelection
    if (selectedItem == null) {
      mediaPickerState.value = mediaPickerState.value.copy(
        isProcessingContinue = false,
        validationError = ContinueValidationIssue.UnreadableMedia,
      )
      return null
    }

    val payloadMediaType = selectedItem.asResolvedContinueMediaType()
    if (payloadMediaType == null) {
      mediaPickerState.value = mediaPickerState.value.copy(
        isProcessingContinue = false,
        validationError = ContinueValidationIssue.UnsupportedMediaType,
      )
      return null
    }

    val uri = runCatching { Uri.parse(selectedItem.uri) }.getOrNull()
    if (uri == null || selectedItem.uri.isBlank() || uri.scheme != ContentResolver.SCHEME_CONTENT || !canOpenSelectedUri(uri)) {
      mediaPickerState.value = mediaPickerState.value.copy(
        isProcessingContinue = false,
        validationError = ContinueValidationIssue.UnreadableMedia,
      )
      return null
    }

    val didLoad = runCatching {
      repository.loadSelectedMedia(
        uri = uri,
        mediaType = payloadMediaType,
        displayNameHint = selectedItem.displayName,
      )
    }.onFailure {
      Log.e("ClipyMediaPicker", "confirmPickerSelection failed for uri=$uri", it)
    }.isSuccess
    if (!didLoad) {
      mediaPickerState.value = mediaPickerState.value.copy(
        isProcessingContinue = false,
        validationError = ContinueValidationIssue.UnreadableMedia,
      )
      return null
    }

    mediaPickerState.value = mediaPickerState.value.copy(isProcessingContinue = false, validationError = null)
    return ContinuePayload(
      uris = listOf(selectedItem.uri),
      primaryMediaType = payloadMediaType,
      source = "media_picker",
    )
  }

  private fun rebuildSelectedItems() {
    val selectedIds = mediaPickerState.value.selectedItems.map { it.id }
    mediaPickerState.value = mediaPickerState.value.copy(
      selectedItems = normalizeSelectionOrders(mediaPickerState.value.selectedItems),
      mediaItems = applySelection(mediaPickerState.value.mediaItems, selectedIds),
      isNextEnabled = selectedIds.isNotEmpty() && validateContinueSelection(mediaPickerState.value.selectedItems, mediaPickerState.value.mediaItems) == null,
    )
  }

  private fun canOpenSelectedUri(uri: Uri): Boolean {
    val resolver = getApplication<Application>().contentResolver
    return runCatching {
      resolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    }.getOrElse {
      Log.w("ClipyMediaPicker", "Unable to open selected uri=$uri", it)
      false
    }
  }

  private fun applySelection(items: List<MediaGridItemUiModel>, selectedIds: List<String>): List<MediaGridItemUiModel> {
    return items.map { item ->
      val order = selectedIds.indexOf(item.id).takeIf { it >= 0 }?.plus(1)
      item.copy(isSelected = order != null, selectionOrder = order)
    }
  }

  private fun normalizeSelectionOrders(items: List<SelectedMediaUiModel>): List<SelectedMediaUiModel> {
    return items.mapIndexed { index, item -> item.copy(order = index + 1) }
  }

  fun updateTrimStart(value: Long) {
    repository.updateTrimWindow(startMs = value)
  }

  fun updateTrimEnd(value: Long) {
    repository.updateTrimWindow(endMs = value)
  }

  fun updatePlayhead(value: Long) {
    repository.setPlayhead(value)
  }

  fun stepPlayheadForward() {
    repository.stepPlayhead(1)
  }

  fun stepPlayheadBackward() {
    repository.stepPlayhead(-1)
  }

  fun updateTimelineZoom(zoom: Float) {
    repository.updateTimelineZoom(zoom)
  }

  fun updateCropRatio(ratio: CropRatio) {
    repository.updateDraft { it.copy(cropRatio = ratio) }
  }

  fun updateSpeed(speed: Float) {
    repository.updateDraft { it.copy(speedMultiplier = speed) }
  }

  fun toggleMuted() {
    repository.updateDraft { it.copy(isMuted = !it.isMuted) }
  }

  fun toggleReverse() {
    repository.updateDraft { it.copy(isReversed = !it.isReversed) }
  }

  fun toggleBoomerang() {
    repository.updateDraft { it.copy(isBoomerang = !it.isBoomerang) }
  }

  fun updateWatermark(text: String) {
    repository.updateDraft { it.copy(watermarkText = text) }
  }

  fun updateWatermarkPosition(position: WatermarkPosition) {
    repository.updateDraft { it.copy(watermarkPosition = position) }
  }

  fun updateFormat(format: ExportFormat) {
    repository.updateDraft { it.copy(exportFormat = format) }
  }

  fun updateGifFps(fps: Int) {
    repository.updateDraft { it.copy(gifFps = fps) }
  }

  fun updateGifResolution(resolution: String) {
    repository.updateDraft { it.copy(gifResolution = resolution) }
  }

  fun updateMp4Quality(quality: Mp4Quality) {
    repository.updateDraft { it.copy(mp4Quality = quality) }
  }

  fun updateOutputName(name: String) {
    repository.updateDraft { it.copy(outputName = name.ifBlank { "clipy_export" }) }
  }

  fun saveSettings(updated: UserPreferences) {
    viewModelScope.launch { repository.updateSettings(updated) }
  }

  fun reuseHistoryRecord(recordId: Long) {
    viewModelScope.launch { repository.reuseHistoryRecord(recordId) }
  }

  fun clearHistory() {
    viewModelScope.launch { repository.clearHistory() }
  }

  fun startExport(): Boolean {
    if (!repository.canExport()) {
      repository.blockExport()
      return false
    }
    viewModelScope.launch { repository.startExport() }
    return true
  }

  fun cancelExport() {
    repository.cancelExport()
  }

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = ClipyViewModel(application) as T
      }
  }
}

internal data class PickerTabSnapshot(
  val albums: List<MediaAlbumUiModel>,
  val items: List<MediaGridItemUiModel>,
  val selectedAlbumId: String?,
  val canLoadMore: Boolean,
  val loadCursor: Int,
  val lastQueryCursorCount: Int?,
  val initialScrollTarget: String?,
)

internal fun MediaPickerUiState.withVisibleTabSnapshot(
  tab: MediaTab,
  snapshot: PickerTabSnapshot?,
  hasPermission: Boolean,
  isLoading: Boolean,
  isUsingPartialAccess: Boolean,
  errorMessage: String?,
): MediaPickerUiState {
  return copy(
    activeTab = tab,
    hasMediaPermission = hasPermission,
    isLoading = isLoading,
    isUsingPartialAccess = isUsingPartialAccess,
    errorMessage = errorMessage,
    albums = snapshot?.albums ?: emptyList(),
    mediaItems = snapshot?.items ?: emptyList(),
    selectedAlbumId = snapshot?.selectedAlbumId,
    canLoadMore = snapshot?.canLoadMore ?: false,
    loadCursor = snapshot?.loadCursor ?: 0,
    lastQueryCursorCount = snapshot?.lastQueryCursorCount,
    initialScrollTarget = snapshot?.initialScrollTarget,
  )
}

internal fun shouldApplyVisiblePickerResult(
  activeTab: MediaTab,
  resultTab: MediaTab,
  latestRequestId: Long?,
  requestId: Long,
): Boolean {
  return activeTab == resultTab && latestRequestId == requestId
}
