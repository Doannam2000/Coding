package com.nantcompany.clipy

import com.nantcompany.clipy.ui.MediaTab
import com.nantcompany.clipy.ui.MediaPermissionSnapshot
import com.nantcompany.clipy.ui.SelectedMediaUiModel
import com.nantcompany.clipy.ui.ContinueValidationIssue
import com.nantcompany.clipy.ui.MediaGridItemUiModel
import com.nantcompany.clipy.ui.hasMediaAccess
import com.nantcompany.clipy.ui.isUsingPartialMediaAccess
import com.nantcompany.clipy.ui.permissionsForTab
import com.nantcompany.clipy.ui.resolveContinueSelection
import com.nantcompany.clipy.ui.resolveContinueSelectionState
import com.nantcompany.clipy.ui.shouldApplyVisiblePickerResult
import com.nantcompany.clipy.ui.validateContinueSelection
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPickerPermissionsTest {
  @Test
  fun videoTab_requestsVideoPermissionOnAndroid13Plus() {
    assertArrayEquals(
      arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO),
      permissionsForTab(tab = MediaTab.Videos, sdkInt = 33),
    )
  }

  @Test
  fun photoTab_requestsImagesPermissionOnAndroid13Plus() {
    assertArrayEquals(
      arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
      permissionsForTab(tab = MediaTab.Photos, sdkInt = 33),
    )
  }

  @Test
  fun preAndroid13_usesExternalStoragePermission() {
    assertArrayEquals(
      arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
      permissionsForTab(tab = MediaTab.Live, sdkInt = 32),
    )
  }

  @Test
  fun android14_requestsPrimaryAndPartialPermissions() {
    assertArrayEquals(
      arrayOf(android.Manifest.permission.READ_MEDIA_VIDEO, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED),
      permissionsForTab(tab = MediaTab.Videos, sdkInt = 34),
    )
  }

  @Test
  fun android14_partialVideoAccessStillCountsAsMediaAccess() {
    val snapshot = MediaPermissionSnapshot(
      sdkInt = 34,
      hasReadMediaImages = false,
      hasReadMediaVideo = false,
      hasReadMediaVisualUserSelected = true,
      hasReadExternalStorage = false,
    )

    assertTrue(hasMediaAccess(snapshot, MediaTab.Videos))
    assertTrue(isUsingPartialMediaAccess(snapshot, MediaTab.Videos))
  }

  @Test
  fun android14_fullImageAccessIsNotMarkedPartial() {
    val snapshot = MediaPermissionSnapshot(
      sdkInt = 34,
      hasReadMediaImages = true,
      hasReadMediaVideo = false,
      hasReadMediaVisualUserSelected = true,
      hasReadExternalStorage = false,
    )

    assertTrue(hasMediaAccess(snapshot, MediaTab.Photos))
    assertFalse(isUsingPartialMediaAccess(snapshot, MediaTab.Photos))
  }

  @Test
  fun visiblePickerResult_appliesOnlyToMatchingLatestTabRequest() {
    assertTrue(
      shouldApplyVisiblePickerResult(
        activeTab = MediaTab.Photos,
        resultTab = MediaTab.Photos,
        latestRequestId = 7L,
        requestId = 7L,
      ),
    )

    assertFalse(
      shouldApplyVisiblePickerResult(
        activeTab = MediaTab.Photos,
        resultTab = MediaTab.Videos,
        latestRequestId = 7L,
        requestId = 7L,
      ),
    )

    assertFalse(
      shouldApplyVisiblePickerResult(
        activeTab = MediaTab.Photos,
        resultTab = MediaTab.Photos,
        latestRequestId = 8L,
        requestId = 7L,
      ),
    )
  }

  @Test
  fun continueValidation_rejectsEmptySelection() {
    assertTrue(validateContinueSelection(emptyList()) == ContinueValidationIssue.MissingUri)
  }

  @Test
  fun continueValidation_acceptsPhotoSelectionForDefensiveDestinationHandling() {
    val selected = SelectedMediaUiModel(
      id = "asset-1",
      uri = "content://media/external/images/media/1",
      type = "photo",
      mimeType = "image/jpeg",
      displayName = "photo.jpg",
      thumbnailUri = "content://media/external/images/media/1",
      durationMs = null,
      order = 1,
    )

    assertTrue(validateContinueSelection(listOf(selected)) == null)
  }

  @Test
  fun continueSelectionState_leavesPhotoSelectionToDestinationGuard() {
    val selected = SelectedMediaUiModel(
      id = "asset-photo",
      uri = "content://media/external/images/media/12",
      type = "photo",
      mimeType = "image/jpeg",
      displayName = "photo.jpg",
      thumbnailUri = "content://media/external/images/media/12",
      durationMs = null,
      order = 1,
    )

    val resolution = resolveContinueSelectionState(selectedItems = listOf(selected))

    assertTrue(resolution.issue == null)
    assertTrue(resolution.item?.type == "photo")
  }

  @Test
  fun continueValidation_acceptsVideoSelection() {
    val selected = SelectedMediaUiModel(
      id = "asset-2",
      uri = "content://media/external/video/media/2",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/2",
      durationMs = 4_000L,
      order = 1,
    )

    assertTrue(validateContinueSelection(listOf(selected)) == null)
  }

  @Test
  fun continueValidation_rejectsSelectionMissingFromVisibleItems() {
    val selected = SelectedMediaUiModel(
      id = "asset-2",
      uri = "content://media/external/video/media/2",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/2",
      durationMs = 4_000L,
      order = 1,
    )

    assertTrue(validateContinueSelection(listOf(selected), visibleItems = emptyList()) == ContinueValidationIssue.UnreadableMedia)
    assertTrue(resolveContinueSelection(listOf(selected), visibleItems = emptyList()) == null)
  }

  @Test
  fun continueValidation_acceptsVideoMimeWhenTypeLabelIsStale() {
    val selected = SelectedMediaUiModel(
      id = "asset-3",
      uri = "content://media/external/images/media/3",
      type = "photo",
      mimeType = "image/jpeg",
      displayName = "wrong-label.jpg",
      thumbnailUri = "content://media/external/images/media/3",
      durationMs = null,
      order = 1,
    )
    val visibleItem = MediaGridItemUiModel(
      id = "asset-3",
      uri = "content://media/external/video/media/3",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/3",
      durationMs = 8_000L,
      createdAtEpochMs = 0L,
      isCameraItem = false,
      selectionOrder = 1,
      isSelected = true,
    )

    assertTrue(validateContinueSelection(listOf(selected), visibleItems = listOf(visibleItem)) == null)
  }

  @Test
  fun continueSelection_resolvesFirstVisibleSelectedItemWhenLeadingSelectionIsStale() {
    val staleSelected = SelectedMediaUiModel(
      id = "missing-photo",
      uri = "content://media/external/images/media/9",
      type = "photo",
      mimeType = "image/jpeg",
      displayName = "stale.jpg",
      thumbnailUri = "content://media/external/images/media/9",
      durationMs = null,
      order = 1,
    )
    val validSelected = SelectedMediaUiModel(
      id = "asset-4",
      uri = "content://media/external/video/media/4",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/4",
      durationMs = 5_000L,
      order = 2,
    )
    val visibleItem = MediaGridItemUiModel(
      id = "asset-4",
      uri = "content://media/external/video/media/4",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/4",
      durationMs = 5_000L,
      createdAtEpochMs = 0L,
      isCameraItem = false,
      selectionOrder = 2,
      isSelected = true,
    )

    val resolved = resolveContinueSelection(
      selectedItems = listOf(staleSelected, validSelected),
      visibleItems = listOf(visibleItem),
    )

    assertTrue(resolved?.id == "asset-4")
    assertTrue(validateContinueSelection(listOf(staleSelected, validSelected), visibleItems = listOf(visibleItem)) == null)
  }

  @Test
  fun continueSelectionState_prefersVisibleVideoMetadataOverStalePhotoSelection() {
    val selected = SelectedMediaUiModel(
      id = "asset-5",
      uri = "content://media/external/images/media/5",
      type = "photo",
      mimeType = "image/jpeg",
      displayName = "stale-photo.jpg",
      thumbnailUri = "content://media/external/images/media/5",
      durationMs = null,
      order = 1,
    )
    val visibleItem = MediaGridItemUiModel(
      id = "asset-5",
      uri = "content://media/external/video/media/5",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/5",
      durationMs = 7_500L,
      createdAtEpochMs = 0L,
      isCameraItem = false,
      selectionOrder = 1,
      isSelected = true,
    )

    val resolution = resolveContinueSelectionState(
      selectedItems = listOf(selected),
      visibleItems = listOf(visibleItem),
    )

    assertTrue(resolution.issue == null)
    assertTrue(resolution.item?.uri == visibleItem.uri)
    assertTrue(resolution.item?.type == "video")
  }

  @Test
  fun continueSelectionState_reportsUnreadableWhenOnlySelectedItemIsNoLongerVisible() {
    val selected = SelectedMediaUiModel(
      id = "asset-6",
      uri = "content://media/external/video/media/6",
      type = "video",
      mimeType = "video/mp4",
      displayName = "clip.mp4",
      thumbnailUri = "content://media/external/video/media/6",
      durationMs = 6_000L,
      order = 1,
    )

    val resolution = resolveContinueSelectionState(
      selectedItems = listOf(selected),
      visibleItems = emptyList(),
    )

    assertTrue(resolution.item == null)
    assertTrue(resolution.issue == ContinueValidationIssue.UnreadableMedia)
  }
}
