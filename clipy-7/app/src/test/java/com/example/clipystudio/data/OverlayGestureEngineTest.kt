package com.example.clipystudio.data

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class OverlayGestureEngineTest {
  @Test
  fun overlayDrag_followsPointerDeltaAndSnapsToCenter() {
    val free = TimelineEngine.resolveOverlayDrag("text", 100f, 120f, 40f, 40f, 70f, 54f, 300f, 400f)
    assertEquals(130f, free.resolvedCenterX, 0.001f)
    assertEquals(134f, free.resolvedCenterY, 0.001f)
    assertFalse(free.snapResolution?.showVerticalCenterGuide == true)

    val snapped = TimelineEngine.resolveOverlayDrag("text", 140f, 190f, 0f, 0f, 8f, 7f, 300f, 400f)
    assertEquals(150f, snapped.resolvedCenterX, 0.001f)
    assertEquals(200f, snapped.resolvedCenterY, 0.001f)
    assertTrue(snapped.snapResolution?.showVerticalCenterGuide == true)
    assertTrue(snapped.snapResolution?.showHorizontalCenterGuide == true)
  }

  @Test
  fun overlayTransform_clampsScaleAndUpdatesBoundingBox() {
    val tiny = TimelineEngine.resolveOverlayTransform("sticker", 150f, 200f, 100f, 50f, 150f, 200f, 1f, 0.05f, 0f, 0f)
    assertEquals(TimelineEngine.DefaultOverlayTransformConfig.minScale, tiny.resolvedScale, 0.001f)

    val large = TimelineEngine.resolveOverlayTransform("sticker", 150f, 200f, 100f, 50f, 150f, 200f, 1f, 9f, 0f, 45f)
    assertEquals(TimelineEngine.DefaultOverlayTransformConfig.maxScale, large.resolvedScale, 0.001f)
    assertTrue(large.boundingBox.right > large.boundingBox.left)
    assertTrue(large.boundingBox.bottom > large.boundingBox.top)
  }

  @Test
  fun overlayRotation_accumulatesAndSnapsAcrossZeroBoundary() {
    val nearZero = TimelineEngine.resolveOverlayTransform("title", 100f, 100f, 80f, 40f, 100f, 100f, 1f, 1f, 359f, 2f)
    assertEquals(0f, nearZero.resolvedRotationDegrees, 0.001f)

    val smooth = TimelineEngine.resolveOverlayTransform("title", 100f, 100f, 80f, 40f, 100f, 100f, 1f, 1f, 20f, 11f)
    assertEquals(31f, smooth.resolvedRotationDegrees, 0.001f)

    val snapped45 = TimelineEngine.resolveOverlayTransform("title", 100f, 100f, 80f, 40f, 100f, 100f, 1f, 1f, 42f, 2f)
    assertEquals(45f, snapped45.resolvedRotationDegrees, 0.001f)
  }

  @Test
  fun gestureOwner_existingOwnerPreventsConflictingTakeover() {
    val drag = TimelineEngine.resolveGestureOwner(GesturePriorityState(), GestureOwner.OVERLAY_DRAG, 1)
    val blocked = TimelineEngine.resolveGestureOwner(drag, GestureOwner.TIMELINE_SCROLL, 1)
    assertEquals(GestureOwner.OVERLAY_DRAG, blocked.activeOwner)

    val transform = TimelineEngine.resolveGestureOwner(GesturePriorityState(), GestureOwner.OVERLAY_TRANSFORM, 2)
    assertEquals(GestureOwner.OVERLAY_TRANSFORM, transform.activeOwner)
    assertEquals(2, transform.activePointerCount)
  }
}
