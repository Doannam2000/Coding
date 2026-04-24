package com.nantcompany.clipy

import com.nantcompany.clipy.ui.MediaTab
import com.nantcompany.clipy.ui.permissionsForTab
import org.junit.Assert.assertArrayEquals
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
}
