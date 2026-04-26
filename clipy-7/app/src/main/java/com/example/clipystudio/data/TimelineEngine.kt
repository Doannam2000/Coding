package com.example.clipystudio.data

import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

data class ProjectTimeline(
  val id: String = UUID.randomUUID().toString(),
  val durationMs: Long = 0,
  val tracks: TimelineTracks = TimelineTracks(),
  val zoomScale: Float = 1f,
  val pixelsPerSecond: Float = 72f,
  val scrollOffsetPx: Float = 0f,
  val currentTimeMs: Long = 0,
  val selectedClipId: String? = null,
  val markers: List<TimelineMarker> = emptyList(),
  val version: Long = 1,
)

data class TimelineTracks(
  val video: List<ProjectTimelineClip> = emptyList(),
  val audio: List<ProjectTimelineClip> = emptyList(),
  val text: List<ProjectTimelineClip> = emptyList(),
  val sticker: List<ProjectTimelineClip> = emptyList(),
  val overlay: List<ProjectTimelineClip> = emptyList(),
  val effect: List<ProjectTimelineClip> = emptyList(),
) {
  fun byType(trackType: TrackType): List<ProjectTimelineClip> = when (trackType) {
    TrackType.Video -> video
    TrackType.Audio -> audio
    TrackType.Text -> text
    TrackType.Sticker -> sticker
    TrackType.Overlay -> overlay
    TrackType.Effect -> effect
  }
}

data class ProjectTimelineClip(
  val id: String,
  val type: ClipType,
  val mediaUri: String? = null,
  val startTimeMs: Long,
  val durationMs: Long,
  val trimStartMs: Long = 0,
  val trimEndMs: Long = 0,
  val speed: Float = 1f,
  val volume: Float = 1f,
  val transform: TransformState = TransformState(),
  val filter: String? = null,
  val effect: String? = null,
  val trackType: TrackType,
  val trackIndex: Int = 0,
  val keyframes: List<Keyframe> = emptyList(),
  val text: String? = null,
  val textStyleRef: String? = null,
  val animationRef: String? = null,
)

data class TimelineViewportState(
  val scrollOffsetPx: Float,
  val zoomScale: Float,
  val pixelsPerSecond: Float,
  val currentTimeMs: Long,
  val isPlaying: Boolean,
  val fixedPlayheadOffsetPx: Float,
)

data class TimelineGestureState(
  val contentOffsetPx: Float = 0f,
  val viewportWidthPx: Float = TimelineEngine.DefaultViewportWidthPx,
  val contentWidthPx: Float = 0f,
  val isDragging: Boolean = false,
  val isFlinging: Boolean = false,
  val isOverscrolling: Boolean = false,
  val lastVelocityPxPerSec: Float = 0f,
  val currentZoom: Float = 1f,
)

data class TimelinePhysicsConfig(
  val decelerationRate: Float = 0.92f,
  val edgeResistanceFactor: Float = 0.22f,
  val clipBoundaryResistanceFactor: Float = 0.28f,
  val overscrollLimitPx: Float = 116f,
  val minFlingVelocityPxPerSec: Float = 140f,
  val stopVelocityThresholdPxPerSec: Float = 24f,
  val settleSpringStiffness: Float = 0.18f,
  val settleSpringDampingRatio: Float = 0.82f,
  val autoScrollEdgeZonePx: Float = 48f,
  val maxAutoScrollVelocityPxPerSec: Float = 180f,
  val snapSettleDurationMs: Int = 110,
)

data class TimelineScrollSnapshot(
  val contentOffsetPx: Float,
  val centerPlayheadTimeMs: Long,
  val visibleStartTimeMs: Long,
  val visibleEndTimeMs: Long,
  val pixelsPerMs: Float,
  val zoom: Float,
)

data class TimelineZoomState(
  val minZoom: Float,
  val maxZoom: Float,
  val currentZoom: Float,
  val anchorViewportPx: Float,
  val anchorTimeMs: Long,
)

data class SnapCandidateTarget(
  val type: SnapTargetType,
  val timeMs: Long,
  val distancePx: Float,
  val strength: Float,
  val label: String? = null,
)

data class SnapResolution(
  val snappedTimeMs: Long? = null,
  val target: SnapCandidateTarget? = null,
  val appliedOffsetPx: Float = 0f,
  val feedbackIntensity: Float = 0f,
)

data class TimelineSnapConfig(
  val baseThresholdPx: Float = 18f,
  val strongThresholdPx: Float = 8f,
  val maxInfluencePx: Float = 28f,
  val playheadBiasMultiplier: Float = 1.2f,
  val edgeBiasMultiplier: Float = 1.08f,
)

data class ClipLayoutBounds(
  val clipId: String,
  val trackType: TrackType,
  val trackIndex: Int,
  val leftPx: Float,
  val rightPx: Float,
  val widthPx: Float,
  val topPx: Float,
  val bottomPx: Float,
  val startTimeMs: Long,
  val endTimeMs: Long,
)

data class TimelineHitTestResult(
  val type: HitTestType,
  val clipId: String? = null,
  val trackType: TrackType? = null,
  val trackIndex: Int? = null,
  val timeMs: Long,
  val localXpx: Float = 0f,
)

enum class HitTestType { Empty, ClipBody, LeftTrimHandle, RightTrimHandle, TrackBackground }
data class TimelineDragState(val clipId: String, val trackType: TrackType, val dragStartTimeMs: Long, val originalStartTimeMs: Long, val proposedStartTimeMs: Long, val snapTarget: SnapTarget? = null, val isValid: Boolean)
data class TimelineTrimState(val clipId: String, val trackType: TrackType, val handle: TrimHandle, val originalTrimStartMs: Long, val originalTrimEndMs: Long, val proposedTrimStartMs: Long, val proposedTrimEndMs: Long, val minDurationMs: Long, val isValid: Boolean)
enum class TrimHandle { Left, Right }
enum class SnapTarget { None, Playhead, ClipStart, ClipEnd }
data class TimelineMutationResult(val timeline: Timeline, val selectedClipId: String?, val currentTimeMs: Long, val changedClipIds: List<String>, val rejectedReason: String? = null)

data class TimelineZoomResult(val timeline: Timeline, val previousZoomScale: Float, val newZoomScale: Float, val previousScrollOffsetPx: Float, val newScrollOffsetPx: Float, val focalTimeMs: Long, val currentTimeMs: Long)
data class VisibleTimelineRange(val startTimeMs: Long, val endTimeMs: Long, val viewportStartPx: Float, val viewportEndPx: Float, val zoomScale: Float, val pixelsPerSecond: Float)
data class TimelineThumbnailRequest(val clipId: String, val mediaUri: String, val startTimeMs: Long, val durationMs: Long, val thumbnailTimeMs: Long, val widthPx: Int, val heightPx: Int, val cacheKey: String)
data class TimelineThumbnailState(val clipId: String, val cacheKey: String, val status: ThumbnailStatus, val requestedAtMs: Long, val lastAccessedAtMs: Long)
enum class ThumbnailStatus { Missing, Loading, Ready, Failed }
data class TimelineSnapResult(val adjustedTimeMs: Long, val targetTimeMs: Long?, val targetType: SnapTargetType, val distanceMs: Long, val sourceClipId: String?, val targetClipId: String?, val markerId: String?, val isSnapped: Boolean)
enum class SnapTargetType { None, Playhead, ClipStart, ClipEnd, TimelineStart, TimelineEnd, Marker, TransitionStart, TransitionEnd }
data class TimelineMarker(val id: String = UUID.randomUUID().toString(), val timeMs: Long, val label: String, val color: Long? = null)
data class ActiveTimelineComposition(val currentTimeMs: Long, val video: ActiveClip?, val audio: List<ActiveClip>, val text: List<ActiveClip>, val stickers: List<ActiveClip>, val overlays: List<ActiveClip>, val effects: List<ActiveClip>, val transition: ActiveTransition?)
data class ActiveClip(val clipId: String, val trackType: TrackType, val trackIndex: Int, val localTimeMs: Long, val sourceTimeMs: Long, val transform: TransformState, val opacity: Float, val volume: Float)
data class ActiveTransition(val transitionId: String, val fromClipId: String, val toClipId: String, val type: TransitionType, val startTimeMs: Long, val endTimeMs: Long, val progress: Float)
data class TimelineFlingFrame(val nextOffsetPx: Float, val nextVelocityPxPerSec: Float, val resistanceFraction: Float, val isFinished: Boolean)
data class ClipGestureResolution(val proposedStartTimeMs: Long, val resolvedStartTimeMs: Long, val snapResolution: SnapResolution, val isValid: Boolean)
data class TrimGestureResolution(val proposedTimeMs: Long, val resolvedTimeMs: Long, val snapResolution: SnapResolution, val isValid: Boolean)
data class TimelineDragUpdate(
  val nextOffsetPx: Float,
  val currentTimeMs: Long,
  val resistanceFraction: Float,
)

data class TimelineSettleFrame(
  val offsetPx: Float,
  val resistanceFraction: Float,
)

enum class GestureOwner { NONE, PREVIEW_TAP, OVERLAY_DRAG, OVERLAY_TRANSFORM, TIMELINE_SCROLL, TIMELINE_PINCH_ZOOM, CLIP_TAP, CLIP_REORDER, TRIM_HANDLE, TEXT_DOUBLE_TAP }
enum class HapticEvent { SNAP, SPLIT, DELETE, SUCCESSFUL_DROP, INVALID_ACTION }

enum class TimelineGestureMode { IDLE, SCROLLING, FLINGING, DRAGGING_CLIP, TRIMMING_CLIP, SCALING_OVERLAY, ROTATING_OVERLAY, MOVING_OVERLAY, PLAYING }
enum class AutoScrollDirection { NONE, LEFT, RIGHT }
enum class MagneticSnapTargetType { PLAYHEAD, NEIGHBOR_CLIP_EDGE, TRANSITION_BOUNDARY, BEAT_MARKER, TIMELINE_START, TIMELINE_END }

data class TimelineDecayState(
  val isFlinging: Boolean,
  val initialVelocityPxPerSecond: Float,
  val currentVelocityPxPerSecond: Float,
  val scrollOffsetPx: Float,
  val minScrollOffsetPx: Float,
  val maxScrollOffsetPx: Float,
  val currentTimeMs: Long,
  val startedAtMs: Long,
  val lastFrameTimeMs: Long? = null,
)

data class TimelineScrollJobState(
  val activeMode: TimelineGestureMode = TimelineGestureMode.IDLE,
  val hasRunningDecayJob: Boolean = false,
  val hasRunningAutoScrollJob: Boolean = false,
  val hasRunningSettleJob: Boolean = false,
  val cancelReason: String? = null,
  val lastStableScrollOffsetPx: Float = 0f,
  val lastStableCurrentTimeMs: Long = 0L,
)

data class TouchInterruptionState(
  val interruptedMode: TimelineGestureMode,
  val touchDownX: Float,
  val touchDownTimeMs: Long,
  val scrollOffsetAtTouchDownPx: Float,
  val currentTimeAtTouchDownMs: Long,
  val shouldCancelScrollJob: Boolean,
  val shouldPreserveFingerAnchor: Boolean,
)

data class TimelineAutoScrollState(
  val isAutoScrolling: Boolean,
  val direction: AutoScrollDirection,
  val edgeZoneWidthPx: Float,
  val distanceIntoEdgeZonePx: Float,
  val velocityPxPerSecond: Float,
  val draggedClipId: String? = null,
  val fingerAnchorTimelineMs: Long = 0L,
  val lastFrameTimeMs: Long? = null,
)

data class ClipDragBoundaryState(
  val clipId: String,
  val proposedStartMs: Long,
  val proposedEndMs: Long,
  val lastValidStartMs: Long,
  val lastValidEndMs: Long,
  val minStartMs: Long,
  val maxEndMs: Long,
  val isBeyondStart: Boolean,
  val isBeyondEnd: Boolean,
  val isInvalidPlacement: Boolean,
  val resistanceOffsetPx: Float,
)

data class TrimPreviewScrubState(
  val clipId: String,
  val activeHandle: TrimHandle,
  val proposedBoundaryMs: Long,
  val previewTimeMs: Long,
  val lastValidStartMs: Long,
  val lastValidEndMs: Long,
  val isScrubbingPreview: Boolean,
)

enum class PreviewSeekSource { TIMELINE_SCROLL, TIMELINE_FLING, CLIP_TRIM_LEFT, CLIP_TRIM_RIGHT, PLAYBACK, PROGRAMMATIC }

data class PreviewSeekThrottleState(
  val isScrubbing: Boolean = false,
  val lastRequestedSeekMs: Long? = null,
  val lastSeekRequestUptimeMs: Long? = null,
  val pendingSeekMs: Long? = null,
  val minIntervalMs: Long = 48L,
  val forceFinalSeek: Boolean = false,
  val source: PreviewSeekSource = PreviewSeekSource.PROGRAMMATIC,
)

data class PreviewSeekDecision(
  val state: PreviewSeekThrottleState,
  val seekTimeMs: Long?,
  val shouldSeekImmediately: Boolean,
)

data class ExactFrameSeekState(
  val currentTimeMs: Long,
  val mappedScrollOffsetPx: Float,
  val timelineScalePxPerMs: Float,
  val isFinalFrame: Boolean,
  val sourceGestureMode: TimelineGestureMode,
  val shouldSeekImmediately: Boolean,
)

enum class PlaybackEditLockPolicy { PAUSE_BEFORE_EDIT, BLOCK_EDIT_WHILE_PLAYING }

data class PlaybackEditLockState(
  val isPlaybackRunning: Boolean,
  val requestedEditMode: TimelineGestureMode,
  val policy: PlaybackEditLockPolicy,
  val shouldPauseBeforeEdit: Boolean,
  val shouldBlockEditGesture: Boolean,
  val lockReason: String? = null,
)

data class TimelinePointerBoundsState(
  val timelineBoundsLeftPx: Float,
  val timelineBoundsTopPx: Float,
  val timelineBoundsRightPx: Float,
  val timelineBoundsBottomPx: Float,
  val isPointerInsideTimeline: Boolean,
  val activePointerId: Long? = null,
  val shouldAcceptTimelineGesture: Boolean,
)

data class PanelGestureIsolationState(
  val activePanelId: String? = null,
  val isPanelGestureActive: Boolean = false,
  val consumesPointerInput: Boolean = false,
  val blockedTimelineGestureMode: TimelineGestureMode? = null,
)

data class HandleTouchTargetState(
  val clipId: String,
  val handle: TrimHandle,
  val visualLeftPx: Float,
  val visualRightPx: Float,
  val touchLeftPx: Float,
  val touchRightPx: Float,
  val minimumTouchTargetPx: Float,
  val isPointerInsideTouchTarget: Boolean,
)

data class TouchSlopGateState(
  val pointerDownX: Float,
  val pointerDownY: Float,
  val currentX: Float,
  val currentY: Float,
  val touchSlopPx: Float,
  val hasExceededTouchSlop: Boolean,
  val pendingGestureMode: TimelineGestureMode,
  val confirmedGestureMode: TimelineGestureMode,
)

data class OverlayHitTarget(
  val overlayId: String,
  val zIndex: Float,
  val boundsPx: OverlayBoundingBox,
  val isVisible: Boolean,
  val containsTouchPoint: Boolean,
  val selectionTieBreaker: Int,
)

data class OverlayHitTestResult(
  val touchX: Float,
  val touchY: Float,
  val candidates: List<OverlayHitTarget>,
  val selectedOverlayId: String?,
  val selectionReason: String,
)

data class OverlayCanvasBoundaryState(
  val overlayId: String,
  val proposedBoundsPx: OverlayBoundingBox,
  val canvasLeftPx: Float,
  val canvasTopPx: Float,
  val canvasRightPx: Float,
  val canvasBottomPx: Float,
  val minimumVisibleWidthPx: Float,
  val minimumVisibleHeightPx: Float,
  val visibleAreaPx: Float,
  val isPartiallyOutsideCanvas: Boolean,
  val isSelectableAreaPreserved: Boolean,
  val resistanceOffsetX: Float,
  val resistanceOffsetY: Float,
  val resolvedCenterX: Float,
  val resolvedCenterY: Float,
  val showBoundaryGuide: Boolean,
)

data class MagneticSnapTarget(
  val id: String,
  val type: MagneticSnapTargetType,
  val timeMs: Long,
  val priority: Int,
  val distancePx: Float,
  val sourceClipId: String? = null,
  val isValid: Boolean = true,
)

data class MagneticSnapResolution(
  val target: MagneticSnapTarget?,
  val resolvedTimeMs: Long,
  val resolvedOffsetPx: Float,
  val priorityOrder: List<MagneticSnapTargetType>,
  val shouldShowSnapGuide: Boolean,
  val shouldEmitHaptic: Boolean,
)

data class SnapReleaseSettleState(
  val isSettling: Boolean,
  val clipId: String? = null,
  val trimHandle: TrimHandle? = null,
  val fromTimeMs: Long,
  val toTimeMs: Long,
  val durationMs: Int,
  val target: MagneticSnapTarget? = null,
  val startedAtMs: Long,
)

data class InvalidDropRecoveryState(
  val clipId: String,
  val fromStartMs: Long,
  val fromEndMs: Long,
  val lastValidStartMs: Long,
  val lastValidEndMs: Long,
  val durationMs: Int,
  val showInvalidFeedback: Boolean,
  val shouldCommitTimelineState: Boolean,
)

data class SelectionStabilityState(
  val selectedClipId: String? = null,
  val selectedOverlayId: String? = null,
  val selectionOwnerMode: TimelineGestureMode = TimelineGestureMode.IDLE,
  val clearSelectionRequested: Boolean = false,
  val pendingSelectedClipId: String? = null,
  val pendingSelectedOverlayId: String? = null,
  val canClearSelection: Boolean = true,
)

data class OverlayTransformConfig(
  val minScale: Float = 0.35f,
  val maxScale: Float = 3f,
  val centerSnapThresholdPx: Float = 18f,
  val angleSnapThresholdDegrees: Float = 4f,
  val snapAnglesDegrees: List<Float> = listOf(0f, 45f, 90f),
)

data class OverlaySnapResolution(
  val snappedCenterX: Float? = null,
  val snappedCenterY: Float? = null,
  val snappedRotationDegrees: Float? = null,
  val showVerticalCenterGuide: Boolean = false,
  val showHorizontalCenterGuide: Boolean = false,
  val feedbackIntensity: Float = 0f,
)

data class OverlayBoundingBox(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float,
  val centerX: Float,
  val centerY: Float,
  val rotationDegrees: Float,
)

data class OverlayDragSnapshot(
  val overlayId: String,
  val startCenterX: Float,
  val startCenterY: Float,
  val pointerStartX: Float,
  val pointerStartY: Float,
  val resolvedCenterX: Float,
  val resolvedCenterY: Float,
  val snapResolution: OverlaySnapResolution? = null,
)

data class OverlayTransformSnapshot(
  val overlayId: String,
  val gestureMidpointX: Float,
  val gestureMidpointY: Float,
  val startScale: Float,
  val resolvedScale: Float,
  val startRotationDegrees: Float,
  val resolvedRotationDegrees: Float,
  val boundingBox: OverlayBoundingBox,
  val snapResolution: OverlaySnapResolution? = null,
)

data class GesturePriorityState(
  val activeOwner: GestureOwner = GestureOwner.NONE,
  val activePointerCount: Int = 0,
  val selectedOverlayId: String? = null,
  val selectedClipId: String? = null,
  val isTapCandidate: Boolean = false,
  val isDoubleTapCandidate: Boolean = false,
  val isLongPressCandidate: Boolean = false,
  val startedAtMs: Long = 0,
)

data class ClipReorderInteractionState(
  val clipId: String,
  val isLongPressed: Boolean,
  val isReordering: Boolean,
  val liftedFraction: Float,
  val shadowAlpha: Float,
  val proposedIndex: Int,
  val resolvedIndex: Int? = null,
  val isValidDrop: Boolean,
)

data class EditorFeedbackState(
  val showOverlayCenterXGuide: Boolean = false,
  val showOverlayCenterYGuide: Boolean = false,
  val highlightedTrimHandleId: String? = null,
  val highlightedTool: String? = null,
  val liftedClipId: String? = null,
  val selectedOverlayId: String? = null,
  val selectedClipId: String? = null,
  val pendingHaptic: HapticEvent? = null,
)

class TimelineThumbnailCache(private val maxEntries: Int = 96) {
  private val entries = linkedMapOf<String, TimelineThumbnailState>()
  fun get(cacheKey: String): TimelineThumbnailState? = entries[cacheKey]?.also { entries[cacheKey] = it.copy(lastAccessedAtMs = System.currentTimeMillis()) }
  fun put(state: TimelineThumbnailState) {
    entries.remove(state.cacheKey)
    entries[state.cacheKey] = state
    while (entries.size > maxEntries) entries.remove(entries.keys.first())
  }
  fun snapshot(): Map<String, TimelineThumbnailState> = entries.toMap()
}

object TimelineEngine {
  const val MinClipDurationMs: Long = 600
  const val DefaultPixelsPerSecond: Float = 72f
  const val DefaultViewportWidthPx: Float = 440f
  const val MinZoomScale: Float = 0.65f
  const val MaxZoomScale: Float = 3f
  private const val SnapThresholdPx: Float = 14f
  val DefaultPhysics = TimelinePhysicsConfig()
  val DefaultSnapConfig = TimelineSnapConfig()
  val DefaultOverlayTransformConfig = OverlayTransformConfig()

  fun resolveGestureOwner(current: GesturePriorityState, requested: GestureOwner, pointerCount: Int): GesturePriorityState {
    if (current.activeOwner != GestureOwner.NONE && current.activeOwner != requested) return current.copy(activePointerCount = pointerCount)
    return current.copy(activeOwner = requested, activePointerCount = pointerCount, startedAtMs = current.startedAtMs.takeIf { it > 0 } ?: System.currentTimeMillis())
  }

  fun canStartGestureMode(current: TimelineGestureMode, requested: TimelineGestureMode): Boolean {
    return current == TimelineGestureMode.IDLE || current == requested || current == TimelineGestureMode.FLINGING || current == TimelineGestureMode.PLAYING
  }

  fun resolveGestureMode(current: TimelineGestureMode, requested: TimelineGestureMode): TimelineGestureMode {
    return if (canStartGestureMode(current, requested)) requested else current
  }

  fun interruptTimelineGesture(mode: TimelineGestureMode, touchDownX: Float, scrollOffsetPx: Float, currentTimeMs: Long, nowMs: Long = System.currentTimeMillis()): TouchInterruptionState {
    val shouldCancel = mode == TimelineGestureMode.FLINGING || mode == TimelineGestureMode.PLAYING || mode == TimelineGestureMode.DRAGGING_CLIP || mode == TimelineGestureMode.TRIMMING_CLIP
    return TouchInterruptionState(mode, touchDownX, nowMs, scrollOffsetPx, currentTimeMs, shouldCancel, shouldCancel)
  }

  fun scrollJobAfterInterruption(state: TimelineScrollJobState, interruption: TouchInterruptionState): TimelineScrollJobState {
    return state.copy(
      activeMode = TimelineGestureMode.IDLE,
      hasRunningDecayJob = false,
      hasRunningAutoScrollJob = false,
      hasRunningSettleJob = false,
      cancelReason = "touch",
      lastStableScrollOffsetPx = interruption.scrollOffsetAtTouchDownPx,
      lastStableCurrentTimeMs = interruption.currentTimeAtTouchDownMs,
    )
  }

  fun previewSeekDecision(state: PreviewSeekThrottleState, requestedSeekMs: Long, nowMs: Long, source: PreviewSeekSource, forceFinalSeek: Boolean = false): PreviewSeekDecision {
    val request = requestedSeekMs.coerceAtLeast(0L)
    val lastAt = state.lastSeekRequestUptimeMs
    val shouldSeek = forceFinalSeek || source == PreviewSeekSource.PLAYBACK || lastAt == null || nowMs - lastAt >= state.minIntervalMs
    val nextState = if (shouldSeek) {
      state.copy(
        isScrubbing = !forceFinalSeek,
        lastRequestedSeekMs = request,
        lastSeekRequestUptimeMs = nowMs,
        pendingSeekMs = null,
        forceFinalSeek = forceFinalSeek,
        source = source,
      )
    } else {
      state.copy(
        isScrubbing = true,
        pendingSeekMs = request,
        forceFinalSeek = false,
        source = source,
      )
    }
    return PreviewSeekDecision(nextState, request.takeIf { shouldSeek }, shouldSeek)
  }

  fun exactFrameSeekFromScroll(scrollOffsetPx: Float, zoomScale: Float, pixelsPerSecond: Float, durationMs: Long, viewportWidthPx: Float, sourceGestureMode: TimelineGestureMode, finalFrame: Boolean): ExactFrameSeekState {
    val mappedOffset = clampScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return ExactFrameSeekState(
      currentTimeMs = timeFromScroll(mappedOffset, zoomScale, pixelsPerSecond, durationMs, viewportWidthPx),
      mappedScrollOffsetPx = mappedOffset,
      timelineScalePxPerMs = pixelsPerMs(zoomScale, pixelsPerSecond),
      isFinalFrame = finalFrame,
      sourceGestureMode = sourceGestureMode,
      shouldSeekImmediately = finalFrame,
    )
  }

  fun resolvePlaybackEditLock(isPlaybackRunning: Boolean, requestedEditMode: TimelineGestureMode, policy: PlaybackEditLockPolicy = PlaybackEditLockPolicy.PAUSE_BEFORE_EDIT): PlaybackEditLockState {
    val editMode = requestedEditMode == TimelineGestureMode.DRAGGING_CLIP || requestedEditMode == TimelineGestureMode.TRIMMING_CLIP || requestedEditMode == TimelineGestureMode.MOVING_OVERLAY || requestedEditMode == TimelineGestureMode.SCALING_OVERLAY || requestedEditMode == TimelineGestureMode.ROTATING_OVERLAY
    val pause = isPlaybackRunning && editMode && policy == PlaybackEditLockPolicy.PAUSE_BEFORE_EDIT
    val block = isPlaybackRunning && editMode && policy == PlaybackEditLockPolicy.BLOCK_EDIT_WHILE_PLAYING
    return PlaybackEditLockState(
      isPlaybackRunning = isPlaybackRunning,
      requestedEditMode = requestedEditMode,
      policy = policy,
      shouldPauseBeforeEdit = pause,
      shouldBlockEditGesture = block,
      lockReason = when {
        pause -> "Playback paused"
        block -> "Locked during playback"
        else -> null
      },
    )
  }

  fun timelinePointerBounds(leftPx: Float, topPx: Float, rightPx: Float, bottomPx: Float, pointerXpx: Float, pointerYpx: Float, activePointerId: Long? = null): TimelinePointerBoundsState {
    val l = min(leftPx, rightPx)
    val r = max(leftPx, rightPx)
    val t = min(topPx, bottomPx)
    val b = max(topPx, bottomPx)
    val inside = pointerXpx in l..r && pointerYpx in t..b
    return TimelinePointerBoundsState(l, t, r, b, inside, activePointerId, inside)
  }

  fun panelGestureIsolation(activePanelId: String?, isActive: Boolean, blockedMode: TimelineGestureMode? = null): PanelGestureIsolationState {
    return PanelGestureIsolationState(activePanelId, isActive, consumesPointerInput = isActive, blockedTimelineGestureMode = blockedMode)
  }

  fun handleTouchTarget(clipId: String, handle: TrimHandle, visualLeftPx: Float, visualRightPx: Float, pointerXpx: Float, minimumTouchTargetPx: Float = 44f): HandleTouchTargetState {
    val visualWidth = abs(visualRightPx - visualLeftPx)
    val expansion = ((minimumTouchTargetPx - visualWidth).coerceAtLeast(0f) / 2f)
    val left = min(visualLeftPx, visualRightPx) - expansion
    val right = max(visualLeftPx, visualRightPx) + expansion
    return HandleTouchTargetState(clipId, handle, min(visualLeftPx, visualRightPx), max(visualLeftPx, visualRightPx), left, right, minimumTouchTargetPx, pointerXpx in left..right)
  }

  fun resolveOverlappingHandle(left: HandleTouchTargetState, right: HandleTouchTargetState, pointerXpx: Float): TrimHandle {
    val leftDistance = abs(pointerXpx - ((left.visualLeftPx + left.visualRightPx) / 2f))
    val rightDistance = abs(pointerXpx - ((right.visualLeftPx + right.visualRightPx) / 2f))
    return if (leftDistance <= rightDistance) TrimHandle.Left else TrimHandle.Right
  }

  fun touchSlopGate(pointerDownX: Float, pointerDownY: Float, currentX: Float, currentY: Float, touchSlopPx: Float, pendingGestureMode: TimelineGestureMode): TouchSlopGateState {
    val exceeded = kotlin.math.hypot((currentX - pointerDownX).toDouble(), (currentY - pointerDownY).toDouble()).toFloat() >= touchSlopPx.coerceAtLeast(0f)
    return TouchSlopGateState(pointerDownX, pointerDownY, currentX, currentY, touchSlopPx, exceeded, pendingGestureMode, if (exceeded) pendingGestureMode else TimelineGestureMode.IDLE)
  }

  fun resolveOverlayDrag(
    overlayId: String,
    startCenterX: Float,
    startCenterY: Float,
    pointerStartX: Float,
    pointerStartY: Float,
    pointerX: Float,
    pointerY: Float,
    previewWidthPx: Float,
    previewHeightPx: Float,
    config: OverlayTransformConfig = DefaultOverlayTransformConfig,
  ): OverlayDragSnapshot {
    val rawX = startCenterX + (pointerX - pointerStartX)
    val rawY = startCenterY + (pointerY - pointerStartY)
    val snap = resolveOverlayCenterSnap(rawX, rawY, previewWidthPx, previewHeightPx, config)
    val snappedX = snap.snappedCenterX ?: rawX
    val snappedY = snap.snappedCenterY ?: rawY
    val bounds = resolveOverlayCanvasBoundary(
      overlayId = overlayId,
      centerX = snappedX,
      centerY = snappedY,
      baseWidthPx = 112f,
      baseHeightPx = 48f,
      scale = 1f,
      rotationDegrees = 0f,
      canvasWidthPx = previewWidthPx,
      canvasHeightPx = previewHeightPx,
    )
    return OverlayDragSnapshot(
      overlayId = overlayId,
      startCenterX = startCenterX,
      startCenterY = startCenterY,
      pointerStartX = pointerStartX,
      pointerStartY = pointerStartY,
      resolvedCenterX = bounds.resolvedCenterX,
      resolvedCenterY = bounds.resolvedCenterY,
      snapResolution = snap,
    )
  }

  fun overlayHitTest(targets: List<OverlayHitTarget>, touchX: Float, touchY: Float): OverlayHitTestResult {
    val candidates = targets.filter { it.isVisible && it.containsTouchPoint }
      .sortedWith(compareByDescending<OverlayHitTarget> { it.zIndex }.thenByDescending { it.selectionTieBreaker })
    val selected = candidates.firstOrNull()
    return OverlayHitTestResult(touchX, touchY, candidates, selected?.overlayId, if (selected == null) "none" else "zIndex")
  }

  fun overlayHitTargets(clips: List<TimelineClip>, touchX: Float, touchY: Float, previewWidthPx: Float, previewHeightPx: Float, baseWidthPx: Float = 112f, baseHeightPx: Float = 48f): List<OverlayHitTarget> {
    return clips.mapIndexed { index, clip ->
      val box = overlayBoundingBox(clip.transform.positionX * previewWidthPx, clip.transform.positionY * previewHeightPx, baseWidthPx, baseHeightPx, clip.transform.scale, clip.transform.rotationDegrees)
      val visible = clip.transform.opacity > 0.01f
      OverlayHitTarget(
        overlayId = clip.id,
        zIndex = clip.zIndex.toFloat(),
        boundsPx = box,
        isVisible = visible,
        containsTouchPoint = touchX in box.left..box.right && touchY in box.top..box.bottom,
        selectionTieBreaker = index,
      )
    }
  }

  fun resolveOverlayCanvasBoundary(overlayId: String, centerX: Float, centerY: Float, baseWidthPx: Float, baseHeightPx: Float, scale: Float, rotationDegrees: Float, canvasWidthPx: Float, canvasHeightPx: Float, minimumVisibleWidthPx: Float = 28f, minimumVisibleHeightPx: Float = 20f): OverlayCanvasBoundaryState {
    val canvasRight = canvasWidthPx.coerceAtLeast(1f)
    val canvasBottom = canvasHeightPx.coerceAtLeast(1f)
    val proposed = overlayBoundingBox(centerX, centerY, baseWidthPx, baseHeightPx, scale, rotationDegrees)
    val resolvedX = when {
      proposed.right < minimumVisibleWidthPx -> centerX + (minimumVisibleWidthPx - proposed.right)
      proposed.left > canvasRight - minimumVisibleWidthPx -> centerX - (proposed.left - (canvasRight - minimumVisibleWidthPx))
      else -> centerX
    }
    val resolvedY = when {
      proposed.bottom < minimumVisibleHeightPx -> centerY + (minimumVisibleHeightPx - proposed.bottom)
      proposed.top > canvasBottom - minimumVisibleHeightPx -> centerY - (proposed.top - (canvasBottom - minimumVisibleHeightPx))
      else -> centerY
    }
    val resolved = overlayBoundingBox(resolvedX, resolvedY, baseWidthPx, baseHeightPx, scale, rotationDegrees)
    val visibleW = (min(resolved.right, canvasRight) - max(resolved.left, 0f)).coerceAtLeast(0f)
    val visibleH = (min(resolved.bottom, canvasBottom) - max(resolved.top, 0f)).coerceAtLeast(0f)
    val outside = resolved.left < 0f || resolved.top < 0f || resolved.right > canvasRight || resolved.bottom > canvasBottom
    val nearEdge = resolved.left < minimumVisibleWidthPx || resolved.top < minimumVisibleHeightPx || resolved.right > canvasRight - minimumVisibleWidthPx || resolved.bottom > canvasBottom - minimumVisibleHeightPx
    return OverlayCanvasBoundaryState(
      overlayId = overlayId,
      proposedBoundsPx = resolved,
      canvasLeftPx = 0f,
      canvasTopPx = 0f,
      canvasRightPx = canvasRight,
      canvasBottomPx = canvasBottom,
      minimumVisibleWidthPx = minimumVisibleWidthPx,
      minimumVisibleHeightPx = minimumVisibleHeightPx,
      visibleAreaPx = visibleW * visibleH,
      isPartiallyOutsideCanvas = outside,
      isSelectableAreaPreserved = visibleW >= minimumVisibleWidthPx && visibleH >= minimumVisibleHeightPx,
      resistanceOffsetX = resolvedX - centerX,
      resistanceOffsetY = resolvedY - centerY,
      resolvedCenterX = resolvedX,
      resolvedCenterY = resolvedY,
      showBoundaryGuide = outside || nearEdge,
    )
  }

  fun resolveOverlayTransform(
    overlayId: String,
    centerX: Float,
    centerY: Float,
    baseWidthPx: Float,
    baseHeightPx: Float,
    gestureMidpointX: Float,
    gestureMidpointY: Float,
    startScale: Float,
    zoomChange: Float,
    startRotationDegrees: Float,
    rotationChangeDegrees: Float,
    config: OverlayTransformConfig = DefaultOverlayTransformConfig,
  ): OverlayTransformSnapshot {
    val scale = (startScale * zoomChange.coerceAtLeast(0.01f)).coerceIn(config.minScale, config.maxScale)
    val rawRotation = normalizeDegrees(startRotationDegrees + rotationChangeDegrees)
    val snap = resolveOverlayAngleSnap(rawRotation, config)
    val rotation = snap.snappedRotationDegrees ?: rawRotation
    return OverlayTransformSnapshot(
      overlayId = overlayId,
      gestureMidpointX = gestureMidpointX,
      gestureMidpointY = gestureMidpointY,
      startScale = startScale,
      resolvedScale = scale,
      startRotationDegrees = startRotationDegrees,
      resolvedRotationDegrees = rotation,
      boundingBox = overlayBoundingBox(centerX, centerY, baseWidthPx, baseHeightPx, scale, rotation),
      snapResolution = snap,
    )
  }

  fun overlayBoundingBox(centerX: Float, centerY: Float, widthPx: Float, heightPx: Float, scale: Float, rotationDegrees: Float): OverlayBoundingBox {
    val halfW = widthPx.coerceAtLeast(1f) * scale.coerceAtLeast(0.01f) / 2f
    val halfH = heightPx.coerceAtLeast(1f) * scale.coerceAtLeast(0.01f) / 2f
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cos = abs(cos(radians)).toFloat()
    val sin = abs(sin(radians)).toFloat()
    val rotatedHalfW = halfW * cos + halfH * sin
    val rotatedHalfH = halfW * sin + halfH * cos
    return OverlayBoundingBox(centerX - rotatedHalfW, centerY - rotatedHalfH, centerX + rotatedHalfW, centerY + rotatedHalfH, centerX, centerY, normalizeDegrees(rotationDegrees))
  }

  fun resolveOverlayCenterSnap(centerX: Float, centerY: Float, previewWidthPx: Float, previewHeightPx: Float, config: OverlayTransformConfig = DefaultOverlayTransformConfig): OverlaySnapResolution {
    val targetX = previewWidthPx / 2f
    val targetY = previewHeightPx / 2f
    val snapX = abs(centerX - targetX) <= config.centerSnapThresholdPx
    val snapY = abs(centerY - targetY) <= config.centerSnapThresholdPx
    val intensity = max(
      if (snapX) 1f - (abs(centerX - targetX) / config.centerSnapThresholdPx) else 0f,
      if (snapY) 1f - (abs(centerY - targetY) / config.centerSnapThresholdPx) else 0f,
    )
    return OverlaySnapResolution(
      snappedCenterX = targetX.takeIf { snapX },
      snappedCenterY = targetY.takeIf { snapY },
      showVerticalCenterGuide = snapX,
      showHorizontalCenterGuide = snapY,
      feedbackIntensity = intensity.coerceIn(0f, 1f),
    )
  }

  fun resolveOverlayAngleSnap(rotationDegrees: Float, config: OverlayTransformConfig = DefaultOverlayTransformConfig): OverlaySnapResolution {
    val normalized = normalizeDegrees(rotationDegrees)
    val candidates = (-4..8).flatMap { turn -> config.snapAnglesDegrees.map { it + turn * 90f } }
    val best = candidates.minByOrNull { angularDistance(normalized, normalizeDegrees(it)) } ?: return OverlaySnapResolution()
    val distance = angularDistance(normalized, normalizeDegrees(best))
    return if (distance <= config.angleSnapThresholdDegrees) {
      OverlaySnapResolution(
        snappedRotationDegrees = normalizeDegrees(best),
        feedbackIntensity = (1f - distance / config.angleSnapThresholdDegrees).coerceIn(0f, 1f),
      )
    } else OverlaySnapResolution()
  }

  fun normalizeDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f

  private fun angularDistance(a: Float, b: Float): Float {
    val diff = abs(normalizeDegrees(a) - normalizeDegrees(b)) % 360f
    return min(diff, 360f - diff)
  }

  fun toProjectTimeline(timeline: Timeline): ProjectTimeline = ProjectTimeline(
    id = timeline.id,
    durationMs = timeline.durationMs,
    tracks = TimelineTracks(
      video = timeline.projectClips(TrackType.Video),
      audio = timeline.projectClips(TrackType.Audio),
      text = timeline.projectClips(TrackType.Text),
      sticker = timeline.projectClips(TrackType.Sticker),
      overlay = timeline.projectClips(TrackType.Overlay),
      effect = timeline.projectClips(TrackType.Effect),
    ),
    zoomScale = timeline.zoomLevel,
    pixelsPerSecond = timeline.pixelsPerSecond,
    scrollOffsetPx = timeline.scrollOffsetPx,
    currentTimeMs = timeline.playheadMs,
    selectedClipId = timeline.selectedClipId,
    markers = timeline.markers,
    version = timeline.version,
  )

  fun pixelsPerMs(zoomScale: Float, pixelsPerSecond: Float): Float = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f

  fun centerPlayheadOffsetPx(viewportWidthPx: Float): Float = viewportWidthPx.coerceAtLeast(1f) / 2f

  fun contentWidthPx(durationMs: Long, zoomScale: Float, pixelsPerSecond: Float): Float = durationMs.coerceAtLeast(0L) * pixelsPerMs(zoomScale, pixelsPerSecond)

  fun maxScrollOffsetPx(durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    val contentWidth = contentWidthPx(durationMs, zoomScale, pixelsPerSecond)
    return (contentWidth - centerPlayheadOffsetPx(viewportWidthPx)).coerceAtLeast(0f)
  }

  fun clampScrollOffset(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return scrollOffsetPx.coerceIn(0f, maxScrollOffsetPx(durationMs, zoomScale, pixelsPerSecond, viewportWidthPx))
  }

  fun timeFromScroll(scrollOffsetPx: Float, zoomScale: Float, pixelsPerSecond: Float, durationMs: Long, viewportWidthPx: Float = DefaultViewportWidthPx): Long {
    val pxPerMs = (pixelsPerSecond * zoomScale).coerceAtLeast(1f) / 1_000f
    val centeredOffset = scrollOffsetPx + centerPlayheadOffsetPx(viewportWidthPx)
    return (centeredOffset / pxPerMs).roundToLong().coerceIn(0L, durationMs)
  }

  fun scrollFromTime(timeMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return (timeMs.coerceAtLeast(0L) * pixelsPerMs(zoomScale, pixelsPerSecond) - centerPlayheadOffsetPx(viewportWidthPx)).coerceAtLeast(0f)
  }

  fun scrollSnapshot(timeline: Timeline, viewportWidthPx: Float = DefaultViewportWidthPx): TimelineScrollSnapshot {
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    val visibleStart = ((timeline.scrollOffsetPx.coerceAtLeast(0f)) / pxPerMs).roundToLong().coerceIn(0L, timeline.durationMs)
    val visibleEnd = ((timeline.scrollOffsetPx + viewportWidthPx.coerceAtLeast(1f)) / pxPerMs).roundToLong().coerceIn(0L, timeline.durationMs)
    return TimelineScrollSnapshot(
      contentOffsetPx = timeline.scrollOffsetPx,
      centerPlayheadTimeMs = timeFromScroll(timeline.scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx),
      visibleStartTimeMs = visibleStart,
      visibleEndTimeMs = visibleEnd,
      pixelsPerMs = pxPerMs,
      zoom = timeline.zoomLevel,
    )
  }

  fun applyEdgeResistance(rawOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    val min = 0f
    val max = maxScrollOffsetPx(durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return when {
      rawOffsetPx < min -> (rawOffsetPx * physics.edgeResistanceFactor).coerceAtLeast(-physics.overscrollLimitPx)
      rawOffsetPx > max -> max + ((rawOffsetPx - max) * physics.edgeResistanceFactor).coerceAtMost(physics.overscrollLimitPx)
      else -> rawOffsetPx
    }
  }

  fun settleScrollOffset(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    return clampScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
  }

  fun overscrollDistancePx(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Float {
    val clamped = clampScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return abs(scrollOffsetPx - clamped)
  }

  fun resistanceFraction(scrollOffsetPx: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    return (overscrollDistancePx(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx) / physics.overscrollLimitPx)
      .coerceIn(0f, 1f)
  }

  fun dragTimelineBy(scrollOffsetPx: Float, deltaX: Float, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    return applyEdgeResistance(
      rawOffsetPx = scrollOffsetPx - deltaX,
      durationMs = durationMs,
      zoomScale = zoomScale,
      pixelsPerSecond = pixelsPerSecond,
      viewportWidthPx = viewportWidthPx,
      physics = physics,
    )
  }

  fun dragTimeline(
    scrollOffsetPx: Float,
    deltaX: Float,
    durationMs: Long,
    zoomScale: Float,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
    physics: TimelinePhysicsConfig = DefaultPhysics,
  ): TimelineDragUpdate {
    val nextOffset = dragTimelineBy(scrollOffsetPx, deltaX, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics)
    return TimelineDragUpdate(
      nextOffsetPx = nextOffset,
      currentTimeMs = timeFromScroll(nextOffset, zoomScale, pixelsPerSecond, durationMs, viewportWidthPx),
      resistanceFraction = resistanceFraction(nextOffset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics),
    )
  }

  fun decayVelocity(velocityPxPerSec: Float, frameDeltaMs: Long, physics: TimelinePhysicsConfig = DefaultPhysics): Float {
    val decayPower = (frameDeltaMs.coerceAtLeast(1L) / 16f).coerceAtLeast(0.25f)
    return velocityPxPerSec * physics.decelerationRate.toDouble().pow(decayPower.toDouble()).toFloat()
  }

  fun updateDragVelocity(previousVelocityPxPerSec: Float, deltaPx: Float, deltaMs: Long): Float {
    val instantVelocity = ((-deltaPx / deltaMs.coerceAtLeast(1L)) * 1000f)
    return if (previousVelocityPxPerSec == 0f) instantVelocity else (previousVelocityPxPerSec * 0.72f) + (instantVelocity * 0.28f)
  }

  fun advanceFling(scrollOffsetPx: Float, velocityPxPerSec: Float, frameDeltaMs: Long, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): TimelineFlingFrame {
    val frameSeconds = frameDeltaMs.coerceAtLeast(1L) / 1_000f
    val rawOffset = scrollOffsetPx + (velocityPxPerSec * frameSeconds)
    val clampedOffset = clampScrollOffset(rawOffset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    val hitBoundary = clampedOffset != rawOffset
    val nextVelocity = if (hitBoundary) 0f else decayVelocity(velocityPxPerSec, frameDeltaMs, physics)
    return TimelineFlingFrame(
      nextOffsetPx = clampedOffset,
      nextVelocityPxPerSec = nextVelocity,
      resistanceFraction = 0f,
      isFinished = hitBoundary || abs(nextVelocity) <= physics.stopVelocityThresholdPxPerSec,
    )
  }

  fun decayStateFrame(state: TimelineDecayState, frameDeltaMs: Long, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx, physics: TimelinePhysicsConfig = DefaultPhysics): TimelineDecayState {
    val frame = advanceFling(state.scrollOffsetPx, state.currentVelocityPxPerSecond, frameDeltaMs, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics)
    return state.copy(
      isFlinging = !frame.isFinished,
      currentVelocityPxPerSecond = frame.nextVelocityPxPerSec,
      scrollOffsetPx = frame.nextOffsetPx,
      currentTimeMs = timeFromScroll(frame.nextOffsetPx, zoomScale, pixelsPerSecond, durationMs, viewportWidthPx),
      lastFrameTimeMs = (state.lastFrameTimeMs ?: state.startedAtMs) + frameDeltaMs.coerceAtLeast(1L),
    )
  }

  fun settleScrollFrames(
    scrollOffsetPx: Float,
    durationMs: Long,
    zoomScale: Float,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
    physics: TimelinePhysicsConfig = DefaultPhysics,
    steps: Int = 6,
  ): List<TimelineSettleFrame> {
    val target = settleScrollOffset(scrollOffsetPx, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    if (abs(target - scrollOffsetPx) < 0.5f) return emptyList()
    val frameCount = steps.coerceAtLeast(1)
    return (1..frameCount).map { index ->
      val progress = index / frameCount.toFloat()
      val eased = 1f - (1f - progress) * (1f - progress) * (1f - progress)
      val offset = scrollOffsetPx + (target - scrollOffsetPx) * eased
      TimelineSettleFrame(
        offsetPx = offset,
        resistanceFraction = resistanceFraction(offset, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx, physics),
      )
    }
  }

  fun zoomAroundFocal(timeline: Timeline, zoomDelta: Float, focalXpx: Float, viewportWidthPx: Float): TimelineZoomResult {
    val previousZoom = timeline.zoomLevel
    val nextZoom = (previousZoom * zoomDelta.coerceIn(0.25f, 4f)).coerceIn(MinZoomScale, MaxZoomScale)
    val previousScroll = timeline.scrollOffsetPx
    val pxPerMsBefore = pixelsPerMs(previousZoom, timeline.pixelsPerSecond)
    val focalTime = ((previousScroll + focalXpx.coerceIn(0f, viewportWidthPx.coerceAtLeast(1f))) / pxPerMsBefore).roundToLong().coerceIn(0L, timeline.durationMs)
    val playheadTime = timeFromScroll(previousScroll, previousZoom, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx)
    val nextScroll = scrollFromTime(playheadTime, nextZoom, timeline.pixelsPerSecond, viewportWidthPx)
      .coerceIn(0f, maxScrollOffsetPx(timeline.durationMs, nextZoom, timeline.pixelsPerSecond, viewportWidthPx))
    val next = timeline.copy(zoomLevel = nextZoom, scrollOffsetPx = nextScroll, playheadMs = playheadTime).nextVersion()
    return TimelineZoomResult(next, previousZoom, nextZoom, previousScroll, nextScroll, focalTime, next.playheadMs)
  }

  fun anchorZoom(
    scrollOffsetPx: Float,
    previousZoom: Float,
    nextZoom: Float,
    focalXpx: Float,
    durationMs: Long,
    pixelsPerSecond: Float,
    viewportWidthPx: Float = DefaultViewportWidthPx,
  ): TimelineDragUpdate {
    val anchorViewportPx = focalXpx.coerceIn(0f, viewportWidthPx.coerceAtLeast(1f))
    val anchorTimeMs = ((scrollOffsetPx + anchorViewportPx) / pixelsPerMs(previousZoom, pixelsPerSecond)).roundToLong().coerceIn(0L, durationMs)
    val nextOffset = (anchorTimeMs * pixelsPerMs(nextZoom, pixelsPerSecond) - anchorViewportPx)
      .coerceIn(0f, maxScrollOffsetPx(durationMs, nextZoom, pixelsPerSecond, viewportWidthPx))
    return TimelineDragUpdate(
      nextOffsetPx = nextOffset,
      currentTimeMs = anchorTimeMs,
      resistanceFraction = resistanceFraction(nextOffset, durationMs, nextZoom, pixelsPerSecond, viewportWidthPx),
    )
  }

  fun withScroll(timeline: Timeline, scrollOffsetPx: Float, viewportWidthPx: Float = DefaultViewportWidthPx): Timeline {
    val current = timeFromScroll(scrollOffsetPx, timeline.zoomLevel, timeline.pixelsPerSecond, timeline.durationMs, viewportWidthPx)
    return timeline.copy(scrollOffsetPx = scrollOffsetPx, playheadMs = current).nextVersion()
  }

  fun visibleRange(timeline: Timeline, viewportWidthPx: Float, prefetchPx: Float = 160f): VisibleTimelineRange {
    val startPx = (timeline.scrollOffsetPx - prefetchPx).coerceAtLeast(0f)
    val endPx = timeline.scrollOffsetPx + viewportWidthPx.coerceAtLeast(1f) + prefetchPx
    return VisibleTimelineRange(
      startTimeMs = ((startPx / pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)).roundToLong()).coerceIn(0L, timeline.durationMs),
      endTimeMs = ((endPx / pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)).roundToLong()).coerceIn(0L, timeline.durationMs),
      viewportStartPx = startPx,
      viewportEndPx = endPx,
      zoomScale = timeline.zoomLevel,
      pixelsPerSecond = timeline.pixelsPerSecond,
    )
  }

  fun planThumbnailRequests(timeline: Timeline, visibleRange: VisibleTimelineRange, cached: Map<String, TimelineThumbnailState> = emptyMap(), cellWidthPx: Int = 72, heightPx: Int = 32): List<TimelineThumbnailRequest> {
    val visibleTypes = setOf(ClipType.Video, ClipType.Image, ClipType.Overlay)
    return timeline.tracks.flatMap { track ->
      track.clips.filter { it.clipType in visibleTypes && it.startMs < visibleRange.endTimeMs && it.startMs + it.durationMs > visibleRange.startTimeMs }.mapNotNull { clip ->
        val sourceTime = (visibleRange.startTimeMs - clip.startMs).coerceIn(0L, clip.durationMs) + clip.sourceInMs
        val mediaRef = clip.mediaUri ?: clip.assetId.orEmpty()
        val key = listOf(clip.id, mediaRef, sourceTime / 500L, cellWidthPx, heightPx, timeline.version).joinToString(":")
        val state = cached[key]
        if (state?.status == ThumbnailStatus.Ready || state?.status == ThumbnailStatus.Loading) null else TimelineThumbnailRequest(clip.id, clip.mediaUri ?: "local://${clip.clipType.name.lowercase()}/${clip.id}", clip.startMs, clip.durationMs, sourceTime, cellWidthPx, heightPx, key)
      }
    }
  }

  fun withPlaybackTick(timeline: Timeline, deltaMs: Long): Timeline {
    val nextTime = (timeline.playheadMs + deltaMs).coerceIn(0L, timeline.durationMs)
    return timeline.copy(playheadMs = nextTime, scrollOffsetPx = scrollFromTime(nextTime, timeline.zoomLevel, timeline.pixelsPerSecond), isPlaying = nextTime < timeline.durationMs).nextVersion()
  }

  fun calculateLayout(projectTimeline: ProjectTimeline, rowHeightPx: Float, rowGapPx: Float): List<ClipLayoutBounds> {
    val order = listOf(TrackType.Video, TrackType.Audio, TrackType.Text, TrackType.Sticker, TrackType.Overlay, TrackType.Effect)
    val pxPerMs = pixelsPerMs(projectTimeline.zoomScale, projectTimeline.pixelsPerSecond)
    return order.flatMapIndexed { row, type ->
      projectTimeline.tracks.byType(type).map { clip ->
        val top = row * (rowHeightPx + rowGapPx)
        val left = clip.startTimeMs * pxPerMs - projectTimeline.scrollOffsetPx
        val width = clip.durationMs.coerceAtLeast(MinClipDurationMs) * pxPerMs
        ClipLayoutBounds(clip.id, type, clip.trackIndex, left, left + width, width, top, top + rowHeightPx, clip.startTimeMs, clip.startTimeMs + clip.durationMs)
      }
    }
  }

  fun hitTest(bounds: List<ClipLayoutBounds>, xPx: Float, yPx: Float, handleWidthPx: Float, currentTimeMs: Long): TimelineHitTestResult {
    val row = bounds.firstOrNull { yPx in it.topPx..it.bottomPx }
    val clip = bounds.lastOrNull { xPx in it.leftPx..it.rightPx && yPx in it.topPx..it.bottomPx }
      ?: return TimelineHitTestResult(if (row == null) HitTestType.Empty else HitTestType.TrackBackground, trackType = row?.trackType, trackIndex = row?.trackIndex, timeMs = currentTimeMs)
    val localX = xPx - clip.leftPx
    val type = when {
      localX <= handleWidthPx -> HitTestType.LeftTrimHandle
      clip.rightPx - xPx <= handleWidthPx -> HitTestType.RightTrimHandle
      else -> HitTestType.ClipBody
    }
    return TimelineHitTestResult(type, clip.clipId, clip.trackType, clip.trackIndex, currentTimeMs, localX)
  }

  fun dragClip(timeline: Timeline, clipId: String, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val original = located.clip
    val resolution = resolveDraggedClip(timeline, clipId, (original.startMs + deltaMs).coerceAtLeast(0L))
    if (!resolution.isValid) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video clips cannot overlap")
    }
    val next = timeline.replaceClip(clipId) { it.copy(startMs = resolution.resolvedStartTimeMs) }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun trimClip(timeline: Timeline, clipId: String, handle: TrimHandle, deltaMs: Long): TimelineMutationResult {
    val located = timeline.locateClip(clipId) ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val proposedTimeMs = when (handle) {
      TrimHandle.Left -> clip.startMs + deltaMs
      TrimHandle.Right -> clip.startMs + clip.durationMs + deltaMs
    }
    val nextClip = resolveTrimmedClip(timeline, clipId, handle, proposedTimeMs)
    if (nextClip == null) {
      return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Trim would overlap video")
    }
    val next = timeline.replaceClip(clipId) { nextClip }.recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, listOf(clipId))
  }

  fun resolveDraggedClip(timeline: Timeline, clipId: String, proposedStartMs: Long): ClipGestureResolution {
    val located = timeline.locateClip(clipId) ?: return ClipGestureResolution(proposedStartMs, proposedStartMs, SnapResolution(), false)
    val maxStart = timeline.durationMs.coerceAtLeast(located.clip.durationMs) - located.clip.durationMs
    val proposed = proposedStartMs.coerceIn(0L, maxStart.coerceAtLeast(0L))
    val snap = resolveSnapResolution(timeline, located.track.type, clipId, proposed)
    val resolved = (snap.snappedTimeMs ?: proposed).coerceIn(0L, maxStart.coerceAtLeast(0L))
    val outOfBounds = proposedStartMs < 0L || proposedStartMs + located.clip.durationMs > timeline.durationMs.coerceAtLeast(located.clip.durationMs)
    val isValid = !outOfBounds && (located.track.type != TrackType.Video || !hasVideoOverlap(timeline, clipId, resolved, located.clip.durationMs))
    return ClipGestureResolution(proposed, resolved, snap, isValid)
  }

  fun resolveClipBoundaryState(timeline: Timeline, clipId: String, proposedStartMs: Long, lastValidStartMs: Long, pixelsPerSecond: Float = timeline.pixelsPerSecond, zoomScale: Float = timeline.zoomLevel, physics: TimelinePhysicsConfig = DefaultPhysics): ClipDragBoundaryState {
    val located = timeline.locateClip(clipId)
    val clip = located?.clip
    val duration = clip?.durationMs ?: 0L
    val minStart = 0L
    val maxStart = (timeline.durationMs.coerceAtLeast(duration) - duration).coerceAtLeast(0L)
    val proposedEnd = proposedStartMs + duration
    val resolved = resolveDraggedClip(timeline, clipId, proposedStartMs)
    val boundaryDeltaMs = when {
      proposedStartMs < minStart -> proposedStartMs - minStart
      proposedStartMs > maxStart -> proposedStartMs - maxStart
      else -> 0L
    }
    return ClipDragBoundaryState(
      clipId = clipId,
      proposedStartMs = proposedStartMs,
      proposedEndMs = proposedEnd,
      lastValidStartMs = lastValidStartMs.coerceIn(minStart, maxStart),
      lastValidEndMs = (lastValidStartMs.coerceIn(minStart, maxStart) + duration).coerceAtLeast(duration),
      minStartMs = minStart,
      maxEndMs = maxStart + duration,
      isBeyondStart = proposedStartMs < minStart,
      isBeyondEnd = proposedStartMs > maxStart,
      isInvalidPlacement = !resolved.isValid,
      resistanceOffsetPx = boundaryDeltaMs * pixelsPerMs(zoomScale, pixelsPerSecond) * physics.clipBoundaryResistanceFactor,
    )
  }

  fun resolveAutoScroll(fingerXpx: Float, viewportWidthPx: Float, draggedClipId: String?, fingerAnchorTimelineMs: Long, physics: TimelinePhysicsConfig = DefaultPhysics): TimelineAutoScrollState {
    val edge = physics.autoScrollEdgeZonePx.coerceAtLeast(1f)
    val width = viewportWidthPx.coerceAtLeast(edge * 2f)
    val leftDistance = (edge - fingerXpx).coerceIn(0f, edge)
    val rightDistance = (fingerXpx - (width - edge)).coerceIn(0f, edge)
    val direction = when {
      leftDistance > 0f -> AutoScrollDirection.LEFT
      rightDistance > 0f -> AutoScrollDirection.RIGHT
      else -> AutoScrollDirection.NONE
    }
    val distance = max(leftDistance, rightDistance)
    val velocity = if (direction == AutoScrollDirection.NONE) 0f else (distance / edge) * physics.maxAutoScrollVelocityPxPerSec * if (direction == AutoScrollDirection.LEFT) -1f else 1f
    return TimelineAutoScrollState(direction != AutoScrollDirection.NONE, direction, edge, distance, velocity, draggedClipId, fingerAnchorTimelineMs)
  }

  fun advanceAutoScroll(scrollOffsetPx: Float, autoScroll: TimelineAutoScrollState, frameDeltaMs: Long, durationMs: Long, zoomScale: Float, pixelsPerSecond: Float, viewportWidthPx: Float = DefaultViewportWidthPx): TimelineDragUpdate {
    val delta = autoScroll.velocityPxPerSecond * (frameDeltaMs.coerceAtLeast(1L) / 1_000f)
    val nextOffset = clampScrollOffset(scrollOffsetPx + delta, durationMs, zoomScale, pixelsPerSecond, viewportWidthPx)
    return TimelineDragUpdate(nextOffset, timeFromScroll(nextOffset, zoomScale, pixelsPerSecond, durationMs, viewportWidthPx), 0f)
  }

  fun resolveTrimGesture(timeline: Timeline, clipId: String, handle: TrimHandle, proposedTimeMs: Long): TrimGestureResolution {
    val located = timeline.locateClip(clipId) ?: return TrimGestureResolution(proposedTimeMs, proposedTimeMs, SnapResolution(), false)
    val clip = located.clip
    val nextClipStartMs = located.track.clips.filterNot { it.id == clipId }.map { it.startMs }.filter { it > clip.startMs }.minOrNull()
    val rightEdgeLimitMs = when {
      located.track.type == TrackType.Video && nextClipStartMs != null -> nextClipStartMs
      clip.sourceDurationMs != null -> clip.startMs + (clip.sourceDurationMs - clip.sourceInMs).coerceAtLeast(MinClipDurationMs)
      else -> (clip.startMs + clip.durationMs + 60_000L).coerceAtLeast(clip.startMs + MinClipDurationMs)
    }
    val rawTime = when (handle) {
      TrimHandle.Left -> proposedTimeMs.coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
      TrimHandle.Right -> proposedTimeMs.coerceIn(clip.startMs + MinClipDurationMs, rightEdgeLimitMs.coerceAtLeast(clip.startMs + MinClipDurationMs))
    }
    val snap = resolveSnapResolution(timeline, located.track.type, clipId, rawTime)
    val resolved = when (handle) {
      TrimHandle.Left -> (snap.snappedTimeMs ?: rawTime).coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
      TrimHandle.Right -> (snap.snappedTimeMs ?: rawTime).coerceIn(clip.startMs + MinClipDurationMs, rightEdgeLimitMs.coerceAtLeast(clip.startMs + MinClipDurationMs))
    }
    val nextClip = resolveTrimmedClip(timeline, clipId, handle, resolved)
    return TrimGestureResolution(rawTime, resolved, snap, nextClip != null)
  }

  fun resolveTrimPreviewScrub(timeline: Timeline, clipId: String, handle: TrimHandle, proposedTimeMs: Long): TrimPreviewScrubState {
    val located = timeline.locateClip(clipId)
    val clip = located?.clip
    val resolution = resolveTrimGesture(timeline, clipId, handle, proposedTimeMs)
    val start = if (handle == TrimHandle.Left) resolution.resolvedTimeMs else clip?.startMs ?: resolution.resolvedTimeMs
    val end = if (handle == TrimHandle.Right) resolution.resolvedTimeMs else (clip?.startMs ?: 0L) + (clip?.durationMs ?: 0L)
    return TrimPreviewScrubState(clipId, handle, resolution.resolvedTimeMs, resolution.resolvedTimeMs.coerceIn(0L, timeline.durationMs), start, end, true)
  }

  fun resolveTrimmedClip(timeline: Timeline, clipId: String, handle: TrimHandle, proposedTimeMs: Long): TimelineClip? {
    val located = timeline.locateClip(clipId) ?: return null
    val clip = located.clip
    val nextClip = when (handle) {
      TrimHandle.Left -> {
        val start = proposedTimeMs.coerceIn((clip.startMs - clip.sourceInMs).coerceAtLeast(0L), clip.startMs + clip.durationMs - MinClipDurationMs)
        val end = clip.startMs + clip.durationMs
        clip.copy(
          startMs = start,
          sourceInMs = (clip.sourceInMs + (start - clip.startMs)).coerceAtLeast(0L),
          durationMs = (end - start).coerceAtLeast(MinClipDurationMs),
        )
      }
      TrimHandle.Right -> {
        val nextClipStartMs = located.track.clips.filterNot { it.id == clipId }.map { it.startMs }.filter { it > clip.startMs }.minOrNull()
        val maxEnd = when {
          located.track.type == TrackType.Video && nextClipStartMs != null -> nextClipStartMs
          clip.sourceDurationMs != null -> clip.startMs + (clip.sourceDurationMs - clip.sourceInMs).coerceAtLeast(MinClipDurationMs)
          else -> maxOf(timeline.durationMs, clip.startMs + clip.durationMs, proposedTimeMs)
        }
        val end = proposedTimeMs.coerceAtLeast(clip.startMs + MinClipDurationMs).coerceAtMost(maxEnd)
        clip.copy(durationMs = (end - clip.startMs).coerceAtLeast(MinClipDurationMs))
      }
    }
    return if (located.track.type == TrackType.Video && hasVideoOverlap(timeline, clip.id, nextClip.startMs, nextClip.durationMs)) null else nextClip
  }

  fun splitSelectedClip(timeline: Timeline): TimelineMutationResult {
    val selectedId = timeline.selectedClipId ?: return TimelineMutationResult(timeline, null, timeline.playheadMs, emptyList(), "No selected clip")
    val located = timeline.locateClip(selectedId) ?: return TimelineMutationResult(timeline, selectedId, timeline.playheadMs, emptyList(), "Clip not found")
    val clip = located.clip
    val split = timeline.playheadMs - clip.startMs
    if (split < MinClipDurationMs || clip.durationMs - split < MinClipDurationMs) return TimelineMutationResult(timeline, selectedId, timeline.playheadMs, emptyList(), "Split too close to clip edge")
    val first = clip.copy(durationMs = split)
    val second = clip.copy(
      id = UUID.randomUUID().toString(),
      startMs = timeline.playheadMs,
      durationMs = clip.durationMs - split,
      sourceInMs = if (clip.clipType == ClipType.Video || clip.sourceDurationMs != null) clip.sourceInMs + split else clip.sourceInMs,
    )
    val next = timeline.copy(tracks = timeline.tracks.map { track -> if (track.id == located.track.id) track.copy(clips = track.clips.flatMap { if (it.id == clip.id) listOf(first, second) else listOf(it) }) else track }).recalculateDuration().nextVersion()
    return TimelineMutationResult(next, second.id, timeline.playheadMs, listOf(first.id, second.id))
  }

  fun reorderVideoClip(timeline: Timeline, clipId: String, targetIndex: Int): TimelineMutationResult {
    val video = timeline.tracks.firstOrNull { it.type == TrackType.Video } ?: return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Video track missing")
    val clips = video.clips.sortedBy { it.startMs }.toMutableList()
    val currentIndex = clips.indexOfFirst { it.id == clipId }
    if (currentIndex == -1) return TimelineMutationResult(timeline, timeline.selectedClipId, timeline.playheadMs, emptyList(), "Clip not found")
    val moved = clips.removeAt(currentIndex)
    clips.add(targetIndex.coerceIn(0, clips.size), moved)
    var cursor = 0L
    val ordered = clips.map { clip -> clip.copy(startMs = cursor).also { cursor += clip.durationMs } }
    val next = timeline.copy(tracks = timeline.tracks.map { if (it.type == TrackType.Video) it.copy(clips = ordered) else it }).recalculateDuration().nextVersion()
    return TimelineMutationResult(next, clipId, next.playheadMs, ordered.map { it.id })
  }

  fun activeClips(timeline: Timeline): List<TimelineClip> = timeline.tracks.flatMap { it.clips }.filter { timeline.playheadMs >= it.startMs && timeline.playheadMs < it.startMs + it.durationMs }.sortedBy { it.zIndex }

  fun resolveActiveComposition(timeline: Timeline, currentTimeMs: Long = timeline.playheadMs): ActiveTimelineComposition {
    val activeByTrack = timeline.tracks.sortedBy { it.orderIndex }.flatMap { track ->
      track.clips.filter { currentTimeMs >= it.startMs && currentTimeMs < it.startMs + it.durationMs }.sortedWith(compareBy<TimelineClip> { it.startMs }.thenBy { it.id }).map { clip ->
        val local = (currentTimeMs - clip.startMs).coerceAtLeast(0L)
        val transform = interpolateTransform(clip, currentTimeMs)
        ActiveClip(clip.id, track.type, track.orderIndex, local, clip.sourceInMs + (local * clip.videoProperties.speed).roundToLong(), transform, transform.opacity, clip.audioProperties.volume)
      }
    }
    return ActiveTimelineComposition(
      currentTimeMs = currentTimeMs,
      video = activeByTrack.firstOrNull { it.trackType == TrackType.Video },
      audio = activeByTrack.filter { it.trackType == TrackType.Audio },
      text = activeByTrack.filter { it.trackType == TrackType.Text },
      stickers = activeByTrack.filter { it.trackType == TrackType.Sticker },
      overlays = activeByTrack.filter { it.trackType == TrackType.Overlay },
      effects = activeByTrack.filter { it.trackType == TrackType.Effect },
      transition = activeTransition(timeline, currentTimeMs),
    )
  }

  fun transitionWindow(timeline: Timeline, transition: Transition): LongRange? {
    val from = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().firstOrNull { it.id == transition.fromClipId } ?: return null
    val to = timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().firstOrNull { it.id == transition.toClipId } ?: return null
    val boundary = transition.boundaryMs.takeIf { it > 0 } ?: from.startMs + from.durationMs
    val duration = transition.durationMs.coerceIn(300L, min(from.durationMs, to.durationMs).coerceAtLeast(300L))
    val start = (boundary - duration / 2).coerceAtLeast(from.startMs)
    val end = (start + duration).coerceAtMost(to.startMs + to.durationMs)
    return start..end
  }

  fun activeTransition(timeline: Timeline, currentTimeMs: Long = timeline.playheadMs): ActiveTransition? = timeline.transitions.firstNotNullOfOrNull { transition ->
    val window = transitionWindow(timeline, transition) ?: return@firstNotNullOfOrNull null
    if (currentTimeMs !in window) null else {
      val span = (window.last - window.first).coerceAtLeast(1L)
      ActiveTransition(transition.id, transition.fromClipId, transition.toClipId, transition.type, window.first, window.last, ((currentTimeMs - window.first).toFloat() / span).coerceIn(0f, 1f))
    }
  }

  fun interpolateTransform(clip: TimelineClip, absoluteTimeMs: Long): TransformState {
    val localTime = (absoluteTimeMs - clip.startMs).coerceIn(0L, clip.durationMs)
    fun property(property: KeyframeProperty, fallback: Float): Float {
      val frames = clip.keyframes.filter { it.property == property }.sortedBy { it.timeMs }
      if (frames.isEmpty()) return fallback
      val before = frames.lastOrNull { it.timeMs <= localTime } ?: return fallback
      val after = frames.firstOrNull { it.timeMs >= localTime } ?: return fallback
      if (before.timeMs == after.timeMs) return before.value
      val t = ((localTime - before.timeMs).toFloat() / (after.timeMs - before.timeMs)).coerceIn(0f, 1f)
      return before.value + (after.value - before.value) * t
    }
    return clip.transform.copy(
      positionX = property(KeyframeProperty.PositionX, clip.transform.positionX),
      positionY = property(KeyframeProperty.PositionY, clip.transform.positionY),
      scale = property(KeyframeProperty.Scale, clip.transform.scale),
      rotationDegrees = property(KeyframeProperty.Rotation, clip.transform.rotationDegrees),
      opacity = property(KeyframeProperty.Opacity, clip.transform.opacity),
    )
  }

  private fun Timeline.projectClips(type: TrackType): List<ProjectTimelineClip> = tracks.firstOrNull { it.type == type }?.clips.orEmpty().map { clip ->
    ProjectTimelineClip(
      id = clip.id,
      type = clip.clipType,
      mediaUri = clip.mediaUri,
      startTimeMs = clip.startMs,
      durationMs = clip.durationMs,
      trimStartMs = clip.sourceInMs,
      trimEndMs = 0,
      speed = clip.videoProperties.speed,
      volume = clip.audioProperties.volume,
      transform = clip.transform,
      filter = clip.filterAdjustments.filterId,
      effect = if (clip.clipType == ClipType.Effect) clip.filterAdjustments.filterId ?: clip.title else null,
      trackType = type,
      trackIndex = tracks.firstOrNull { it.type == type }?.orderIndex ?: 0,
      keyframes = clip.keyframes,
      text = clip.textProperties.content.takeIf { clip.clipType == ClipType.Text || clip.clipType == ClipType.Sticker },
      textStyleRef = listOf(clip.textProperties.fontSizeSp, clip.textProperties.color, clip.textProperties.backgroundColor, clip.textProperties.alignment).joinToString("|"),
      animationRef = clip.textProperties.animation,
    )
  }

  fun collectSnapCandidates(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long, snapConfig: TimelineSnapConfig = DefaultSnapConfig): List<SnapCandidateTarget> {
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    return buildList {
      add(candidateTarget(timeline.playheadMs, SnapTargetType.Playhead, proposed, pxPerMs, snapConfig, "Playhead"))
      add(candidateTarget(0L, SnapTargetType.TimelineStart, proposed, pxPerMs, snapConfig, "Start"))
      add(candidateTarget(timeline.durationMs, SnapTargetType.TimelineEnd, proposed, pxPerMs, snapConfig, "End"))
      timeline.tracks.firstOrNull { it.type == trackType }?.clips.orEmpty().filterNot { it.id == clipId }.forEach { clip ->
        add(candidateTarget(clip.startMs, SnapTargetType.ClipStart, proposed, pxPerMs, snapConfig, "Clip start"))
        add(candidateTarget(clip.startMs + clip.durationMs, SnapTargetType.ClipEnd, proposed, pxPerMs, snapConfig, "Clip end"))
      }
      timeline.markers.forEach { add(candidateTarget(it.timeMs, SnapTargetType.Marker, proposed, pxPerMs, snapConfig, it.label)) }
      timeline.transitions.forEach { transition ->
        transitionWindow(timeline, transition)?.let {
          add(candidateTarget(it.first, SnapTargetType.TransitionStart, proposed, pxPerMs, snapConfig, "Transition"))
          add(candidateTarget(it.last, SnapTargetType.TransitionEnd, proposed, pxPerMs, snapConfig, "Transition"))
        }
      }
    }
  }

  fun resolveSnapResolution(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long, snapConfig: TimelineSnapConfig = DefaultSnapConfig): SnapResolution {
    val best = collectSnapCandidates(timeline, trackType, clipId, proposed, snapConfig)
      .filter { it.distancePx <= snapConfig.baseThresholdPx.coerceAtLeast(snapConfig.maxInfluencePx) || it.distancePx <= snapConfig.maxInfluencePx }
      .minWithOrNull(compareBy<SnapCandidateTarget> { snapPriority(it.type) }.thenBy { it.distancePx })
      ?: return SnapResolution()
    val snappedTime = best.timeMs
    val pxPerMs = pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond)
    return SnapResolution(
      snappedTimeMs = snappedTime,
      target = best,
      appliedOffsetPx = (snappedTime - proposed) * pxPerMs,
      feedbackIntensity = best.strength,
    )
  }

  fun resolveMagneticSnap(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long, snapConfig: TimelineSnapConfig = DefaultSnapConfig): MagneticSnapResolution {
    val priorityOrder = listOf(MagneticSnapTargetType.PLAYHEAD, MagneticSnapTargetType.NEIGHBOR_CLIP_EDGE, MagneticSnapTargetType.TRANSITION_BOUNDARY, MagneticSnapTargetType.BEAT_MARKER, MagneticSnapTargetType.TIMELINE_START, MagneticSnapTargetType.TIMELINE_END)
    val target = collectSnapCandidates(timeline, trackType, clipId, proposed, snapConfig)
      .filter { it.distancePx <= snapConfig.maxInfluencePx }
      .map { candidate ->
        val type = magneticType(candidate.type)
        MagneticSnapTarget(
          id = "${type.name}:${candidate.timeMs}",
          type = type,
          timeMs = candidate.timeMs,
          priority = priorityOrder.indexOf(type).takeIf { it >= 0 } ?: Int.MAX_VALUE,
          distancePx = candidate.distancePx,
          sourceClipId = clipId,
          isValid = true,
        )
      }
      .minWithOrNull(compareBy<MagneticSnapTarget> { it.priority }.thenBy { it.distancePx })
    val resolvedTime = target?.timeMs ?: proposed
    return MagneticSnapResolution(
      target = target,
      resolvedTimeMs = resolvedTime,
      resolvedOffsetPx = (resolvedTime - proposed) * pixelsPerMs(timeline.zoomLevel, timeline.pixelsPerSecond),
      priorityOrder = priorityOrder,
      shouldShowSnapGuide = target != null,
      shouldEmitHaptic = target != null && target.distancePx <= snapConfig.strongThresholdPx,
    )
  }

  fun resolveSnapReleaseSettle(clipId: String?, trimHandle: TrimHandle?, fromTimeMs: Long, snap: MagneticSnapResolution, nowMs: Long = System.currentTimeMillis(), physics: TimelinePhysicsConfig = DefaultPhysics): SnapReleaseSettleState {
    return SnapReleaseSettleState(
      isSettling = snap.target != null && snap.resolvedTimeMs != fromTimeMs,
      clipId = clipId,
      trimHandle = trimHandle,
      fromTimeMs = fromTimeMs,
      toTimeMs = snap.resolvedTimeMs,
      durationMs = physics.snapSettleDurationMs.coerceIn(80, 140),
      target = snap.target,
      startedAtMs = nowMs,
    )
  }

  fun settleTimeAt(fromTimeMs: Long, toTimeMs: Long, elapsedMs: Long, durationMs: Int): Long {
    val progress = (elapsedMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f)
    val eased = 1f - (1f - progress) * (1f - progress)
    return (fromTimeMs + (toTimeMs - fromTimeMs) * eased).roundToLong()
  }

  fun invalidDropRecovery(clipId: String, fromStartMs: Long, fromEndMs: Long, lastValidStartMs: Long, lastValidEndMs: Long, physics: TimelinePhysicsConfig = DefaultPhysics): InvalidDropRecoveryState {
    return InvalidDropRecoveryState(clipId, fromStartMs, fromEndMs, lastValidStartMs, lastValidEndMs, physics.snapSettleDurationMs.coerceIn(80, 140), showInvalidFeedback = true, shouldCommitTimelineState = false)
  }

  fun selectionAfterGesture(state: SelectionStabilityState, mode: TimelineGestureMode, emptyTap: Boolean = false, selectedClipId: String? = null, selectedOverlayId: String? = null): SelectionStabilityState {
    if (selectedClipId != null || selectedOverlayId != null) return state.copy(selectedClipId = selectedClipId ?: state.selectedClipId, selectedOverlayId = selectedOverlayId ?: state.selectedOverlayId, selectionOwnerMode = mode, clearSelectionRequested = false, canClearSelection = false)
    val canClear = mode == TimelineGestureMode.IDLE
    return if (emptyTap && canClear) state.copy(selectedClipId = null, selectedOverlayId = null, selectionOwnerMode = mode, clearSelectionRequested = true, canClearSelection = true) else state.copy(selectionOwnerMode = mode, clearSelectionRequested = emptyTap, canClearSelection = canClear)
  }

  private fun snapPriority(type: SnapTargetType): Int = when (type) {
    SnapTargetType.Playhead -> 0
    SnapTargetType.ClipStart, SnapTargetType.ClipEnd -> 1
    SnapTargetType.TransitionStart, SnapTargetType.TransitionEnd -> 2
    SnapTargetType.Marker -> 3
    SnapTargetType.TimelineStart, SnapTargetType.TimelineEnd -> 4
    SnapTargetType.None -> Int.MAX_VALUE
  }

  private fun magneticType(type: SnapTargetType): MagneticSnapTargetType = when (type) {
    SnapTargetType.Playhead -> MagneticSnapTargetType.PLAYHEAD
    SnapTargetType.ClipStart, SnapTargetType.ClipEnd -> MagneticSnapTargetType.NEIGHBOR_CLIP_EDGE
    SnapTargetType.TransitionStart, SnapTargetType.TransitionEnd -> MagneticSnapTargetType.TRANSITION_BOUNDARY
    SnapTargetType.Marker -> MagneticSnapTargetType.BEAT_MARKER
    SnapTargetType.TimelineStart -> MagneticSnapTargetType.TIMELINE_START
    SnapTargetType.TimelineEnd, SnapTargetType.None -> MagneticSnapTargetType.TIMELINE_END
  }

  fun resolveSnap(timeline: Timeline, trackType: TrackType, clipId: String, proposed: Long): TimelineSnapResult {
    val resolution = resolveSnapResolution(timeline, trackType, clipId, proposed)
    val target = resolution.target
    return if (target != null && resolution.snappedTimeMs != null) {
      TimelineSnapResult(
        adjustedTimeMs = resolution.snappedTimeMs,
        targetTimeMs = resolution.snappedTimeMs,
        targetType = target.type,
        distanceMs = abs(resolution.snappedTimeMs - proposed),
        sourceClipId = clipId,
        targetClipId = null,
        markerId = null,
        isSnapped = true,
      )
    } else TimelineSnapResult(proposed, null, SnapTargetType.None, Long.MAX_VALUE, clipId, null, null, false)
  }

  private fun candidateTarget(timeMs: Long, type: SnapTargetType, proposed: Long, pxPerMs: Float, snapConfig: TimelineSnapConfig, label: String?): SnapCandidateTarget {
    val distancePx = abs(timeMs - proposed) * pxPerMs
    val bias = when (type) {
      SnapTargetType.Playhead -> snapConfig.playheadBiasMultiplier
      SnapTargetType.ClipStart, SnapTargetType.ClipEnd, SnapTargetType.TimelineStart, SnapTargetType.TimelineEnd -> snapConfig.edgeBiasMultiplier
      else -> 1f
    }
    val normalized = 1f - (distancePx / snapConfig.maxInfluencePx).coerceIn(0f, 1f)
    val strongBoost = if (distancePx <= snapConfig.strongThresholdPx) 1f else normalized
    return SnapCandidateTarget(type = type, timeMs = timeMs, distancePx = distancePx, strength = (strongBoost * bias).coerceIn(0f, 1.5f), label = label)
  }

  private fun hasVideoOverlap(timeline: Timeline, clipId: String, startMs: Long, durationMs: Long): Boolean {
    val endMs = startMs + durationMs
    return timeline.tracks.firstOrNull { it.type == TrackType.Video }?.clips.orEmpty().filterNot { it.id == clipId }.any { startMs < it.startMs + it.durationMs && endMs > it.startMs }
  }

  private data class LocatedClip(val track: TimelineTrack, val clip: TimelineClip)
  private fun Timeline.locateClip(clipId: String): LocatedClip? = tracks.firstNotNullOfOrNull { track -> track.clips.firstOrNull { it.id == clipId }?.let { LocatedClip(track, it) } }
  private fun Timeline.replaceClip(clipId: String, transform: (TimelineClip) -> TimelineClip): Timeline = copy(tracks = tracks.map { track -> track.copy(clips = track.clips.map { if (it.id == clipId) transform(it) else it }) })
}

fun Timeline.nextVersion(): Timeline = copy(version = version + 1)

private fun Double.pow(exponent: Double): Double = exp(kotlin.math.ln(this) * exponent)
