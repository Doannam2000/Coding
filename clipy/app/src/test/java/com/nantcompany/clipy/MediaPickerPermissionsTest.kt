package com.nantcompany.clipy

import com.nantcompany.clipy.ui.MediaTab
import com.nantcompany.clipy.ui.MediaPermissionSnapshot
import com.nantcompany.clipy.ui.hasMediaAccess
import com.nantcompany.clipy.ui.isUsingPartialMediaAccess
import com.nantcompany.clipy.ui.permissionsForTab
import com.nantcompany.clipy.ui.shouldApplyVisiblePickerResult
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
}
