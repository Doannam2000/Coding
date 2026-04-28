package com.natncompany.media

class DefaultTimelineEditor : TimelineEditor {
    override fun addTrack(timeline: Timeline, track: TimelineTrack, index: Int): MediaResult<Timeline> {
        if (track.id.isBlank()) {
            return failure("Track id is required")
        }
        if (timeline.tracks.any { it.id == track.id }) {
            return failure("Track ${track.id} already exists")
        }
        if (index !in 0..timeline.tracks.size) {
            return failure("Track index $index is out of bounds")
        }
        val updated = timeline.copy(tracks = timeline.tracks.toMutableList().apply { add(index, track) })
        return validateAndRecord(timeline, updated, EditOperationType.Add, trackId = track.id, details = "add track")
    }

    override fun removeTrack(timeline: Timeline, trackId: String): MediaResult<Timeline> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        if (timeline.tracks.size == 1) {
            return failure("Timeline must contain at least one track")
        }
        val removedClipIds = track.clips.mapTo(linkedSetOf()) { it.id }
        val updated = timeline.copy(
            tracks = timeline.tracks.filterNot { it.id == trackId },
            selectedClipIds = timeline.selectedClipIds - removedClipIds,
            clipGroups = timeline.clipGroups.mapNotNull { group ->
                val remaining = group.clipIds - removedClipIds
                when {
                    remaining.isEmpty() -> null
                    remaining.size == 1 -> null
                    else -> group.copy(clipIds = remaining)
                }
            }
        )
        return validateAndRecord(timeline, updated, EditOperationType.Remove, trackId = trackId, details = "remove track")
    }

    override fun reorderTrack(timeline: Timeline, trackId: String, newIndex: Int): MediaResult<Timeline> {
        val currentIndex = timeline.tracks.indexOfFirst { it.id == trackId }
        if (currentIndex == -1) {
            return failure("Track $trackId not found")
        }
        if (newIndex !in timeline.tracks.indices) {
            return failure("Track index $newIndex is out of bounds")
        }
        val tracks = timeline.tracks.toMutableList()
        val track = tracks.removeAt(currentIndex)
        tracks.add(newIndex, track)
        return success(timeline.copy(tracks = tracks))
    }

    override fun setTrackEnabled(timeline: Timeline, trackId: String, enabled: Boolean): MediaResult<Timeline> {
        return updateTrackFlag(timeline, trackId) { it.copy(isEnabled = enabled) }
    }

    override fun setTrackLocked(timeline: Timeline, trackId: String, locked: Boolean): MediaResult<Timeline> {
        return updateTrackFlag(timeline, trackId) { it.copy(isLocked = locked) }
    }

    override fun setTrackMuted(timeline: Timeline, trackId: String, muted: Boolean): MediaResult<Timeline> {
        return updateTrackFlag(timeline, trackId) { it.copy(isMuted = muted) }
    }

    override fun calculateTrackDuration(timeline: Timeline, trackId: String): MediaResult<Long> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        return MediaResult.Success(TimelineCalculations.calculateTrackDuration(track))
    }

    override fun addClip(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<Timeline> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        ensureTrackEditable(track)?.let { return it }
        if (timeline.containsClipId(clip.id)) {
            return failure("Clip ${clip.id} already exists")
        }
        val normalized = normalizeClip(clip, timeline.settings)
        val validation = validateClipPositionInternal(timeline, track, normalized, ignoreClipId = null)
        if (validation is MediaResult.Failure) {
            return validation
        }
        val updatedTrack = track.copy(clips = (track.clips + normalized).sortedBy { it.timelineStartMs })
        val updated = timeline.replaceTrack(updatedTrack)
        return validateAndRecord(timeline, updated, EditOperationType.Add, clipId = clip.id, trackId = trackId, details = "add clip")
    }

    override fun removeClip(timeline: Timeline, trackId: String, clipId: String): MediaResult<Timeline> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        ensureTrackEditable(track)?.let { return it }
        if (track.clips.none { it.id == clipId }) {
            return failure("Clip $clipId not found on track $trackId")
        }
        val updated = timeline.copy(
            tracks = timeline.tracks.map { existing ->
                if (existing.id == trackId) existing.copy(clips = existing.clips.filterNot { it.id == clipId }) else existing
            },
            selectedClipIds = timeline.selectedClipIds - clipId,
            clipGroups = timeline.clipGroups.mapNotNull { group ->
                val remaining = group.clipIds - clipId
                when {
                    remaining.size < 2 -> null
                    else -> group.copy(clipIds = remaining)
                }
            }
        )
        return validateAndRecord(timeline, updated, EditOperationType.Remove, clipId = clipId, trackId = trackId, details = "remove clip")
    }

    override fun selectClip(timeline: Timeline, clipId: String, selected: Boolean): MediaResult<Timeline> {
        if (!timeline.containsClipId(clipId)) {
            return failure("Clip $clipId not found")
        }
        val selection = if (selected) timeline.selectedClipIds + clipId else timeline.selectedClipIds - clipId
        return success(timeline.copy(selectedClipIds = selection))
    }

    override fun clearSelection(timeline: Timeline): MediaResult<Timeline> {
        return success(timeline.copy(selectedClipIds = emptySet()))
    }

    override fun groupClips(timeline: Timeline, groupId: String, clipIds: Set<String>): MediaResult<Timeline> {
        if (groupId.isBlank()) {
            return failure("Group id is required")
        }
        if (clipIds.size < 2) {
            return failure("At least two clips are required to create a group")
        }
        val missing = clipIds.filterNot { timeline.containsClipId(it) }
        if (missing.isNotEmpty()) {
            return failure("Clip group contains unknown clips: ${missing.joinToString()}")
        }
        val groups = timeline.clipGroups.filterNot { it.id == groupId } + ClipGroup(id = groupId, clipIds = clipIds)
        return success(timeline.copy(clipGroups = groups))
    }

    override fun ungroupClips(timeline: Timeline, groupId: String): MediaResult<Timeline> {
        if (timeline.clipGroups.none { it.id == groupId }) {
            return failure("Group $groupId not found")
        }
        return success(timeline.copy(clipGroups = timeline.clipGroups.filterNot { it.id == groupId }))
    }

    override fun validateClipPosition(timeline: Timeline, trackId: String, clip: TimelineClip): MediaResult<TimelineClip> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        val normalized = normalizeClip(clip, timeline.settings)
        return when (val result = validateClipPositionInternal(timeline, track, normalized, ignoreClipId = clip.id)) {
            is MediaResult.Success -> MediaResult.Success(normalized)
            is MediaResult.Failure -> result
        }
    }

    override fun splitClip(timeline: Timeline, trackId: String, clipId: String, playheadMs: Long): MediaResult<Timeline> {
        val located = findTrackAndClip(timeline, trackId, clipId) ?: return failure("Clip $clipId not found on track $trackId")
        ensureTrackEditable(located.track)?.let { return it }
        val clip = located.clip
        if (playheadMs <= clip.timelineStartMs || playheadMs >= clip.timelineEndMs) {
            return failure("Playhead must be inside clip bounds")
        }
        val splitOffsetMs = playheadMs - clip.timelineStartMs
        val splitSourceMs = clip.sourceStartMs + splitOffsetMs
        val left = clip.copy(
            sourceEndMs = splitSourceMs,
            metadata = clip.metadata.copy(createdFromSplit = true)
        )
        val right = clip.copy(
            id = generateSplitClipId(clip.id),
            timelineStartMs = playheadMs,
            sourceStartMs = splitSourceMs,
            metadata = clip.metadata.copy(createdFromSplit = true)
        )
        val updatedTrack = located.track.copy(
            clips = located.track.clips.flatMap { existing ->
                if (existing.id == clipId) listOf(left, right) else listOf(existing)
            }.sortedBy { it.timelineStartMs }
        )
        val updated = timeline
            .replaceTrack(updatedTrack)
            .copy(
                selectedClipIds = timeline.selectedClipIds - clipId + clip.id + right.id,
                clipGroups = timeline.clipGroups.map { group ->
                    if (clipId in group.clipIds) group.copy(clipIds = group.clipIds + right.id) else group
                }
            )
        return validateAndRecord(timeline, updated, EditOperationType.Split, clipId = clipId, trackId = trackId, details = "split clip")
    }

    override fun splitAllAt(timeline: Timeline, playheadMs: Long): MediaResult<Timeline> {
        var current = timeline
        var splitPerformed = false
        for (track in current.tracks) {
            val touching = track.clips.filter { playheadMs > it.timelineStartMs && playheadMs < it.timelineEndMs }
            for (clip in touching) {
                when (val result = splitClip(current, track.id, clip.id, playheadMs)) {
                    is MediaResult.Success -> {
                        current = result.value
                        splitPerformed = true
                    }
                    is MediaResult.Failure -> return result
                }
            }
        }
        return if (splitPerformed) success(current) else success(timeline)
    }

    override fun trimClip(timeline: Timeline, trackId: String, clipId: String, startMs: Long, endMs: Long): MediaResult<Timeline> {
        var current = timeline
        when (val startResult = trimClipStart(current, trackId, clipId, startMs, ripple = false)) {
            is MediaResult.Success -> current = startResult.value
            is MediaResult.Failure -> return startResult
        }
        return trimClipEnd(current, trackId, clipId, endMs, ripple = false)
    }

    override fun trimClipStart(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        newSourceStartMs: Long,
        ripple: Boolean
    ): MediaResult<Timeline> {
        val located = findTrackAndClip(timeline, trackId, clipId) ?: return failure("Clip $clipId not found on track $trackId")
        ensureTrackEditable(located.track)?.let { return it }
        val clip = located.clip
        if (newSourceStartMs < 0L) {
            return failure("Clip source start cannot be negative")
        }
        if (newSourceStartMs >= clip.sourceEndMs) {
            return failure("Clip duration must stay greater than zero")
        }
        val updatedClip = clip.copy(
            sourceStartMs = newSourceStartMs,
            timelineStartMs = if (ripple) clip.timelineStartMs else clip.timelineStartMs + (newSourceStartMs - clip.sourceStartMs)
        )
        return replaceClipAndRecord(
            timeline = timeline,
            track = located.track,
            updatedClip = updatedClip,
            operationType = EditOperationType.Trim,
            trackId = trackId,
            clipId = clipId,
            details = if (ripple) "ripple trim start" else "trim start"
        )
    }

    override fun trimClipEnd(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        newSourceEndMs: Long,
        ripple: Boolean
    ): MediaResult<Timeline> {
        val located = findTrackAndClip(timeline, trackId, clipId) ?: return failure("Clip $clipId not found on track $trackId")
        ensureTrackEditable(located.track)?.let { return it }
        val clip = located.clip
        if (newSourceEndMs > clip.resolvedSourceDurationMs(timeline.settings)) {
            return failure("Clip end exceeds source duration")
        }
        if (newSourceEndMs <= clip.sourceStartMs) {
            return failure("Clip duration must stay greater than zero")
        }
        val updatedClip = clip.copy(sourceEndMs = newSourceEndMs)
        val baseTrack = located.track.copy(
            clips = located.track.clips.map { existing -> if (existing.id == clipId) updatedClip else existing }
        )
        val adjustedTrack = if (ripple) rippleFollowingClips(baseTrack, clipId) else baseTrack
        val updatedTimeline = timeline.replaceTrack(adjustedTrack)
        return validateAndRecord(timeline, updatedTimeline, EditOperationType.Trim, clipId = clipId, trackId = trackId, details = if (ripple) "ripple trim end" else "trim end")
    }

    override fun moveClip(
        timeline: Timeline,
        fromTrackId: String,
        toTrackId: String,
        clipId: String,
        newStartMs: Long,
        newIndex: Int?
    ): MediaResult<Timeline> {
        val sourceTrack = timeline.findTrack(fromTrackId) ?: return failure("Track $fromTrackId not found")
        val targetTrack = timeline.findTrack(toTrackId) ?: return failure("Track $toTrackId not found")
        ensureTrackEditable(sourceTrack)?.let { return it }
        ensureTrackEditable(targetTrack)?.let { return it }
        val clip = sourceTrack.clips.find { it.id == clipId } ?: return failure("Clip $clipId not found on track $fromTrackId")
        val movedClip = clip.copy(timelineStartMs = newStartMs)
        val strippedSource = sourceTrack.copy(clips = sourceTrack.clips.filterNot { it.id == clipId })
        val insertionTarget = if (fromTrackId == toTrackId) strippedSource else targetTrack
        val placedTrack = insertClipAtIndex(insertionTarget, movedClip, newIndex)
        val candidateTimeline = timeline.copy(
            tracks = timeline.tracks.map { track ->
                when (track.id) {
                    fromTrackId -> if (fromTrackId == toTrackId) placedTrack else strippedSource
                    toTrackId -> if (fromTrackId == toTrackId) placedTrack else placedTrack
                    else -> track
                }
            }
        )
        return validateAndRecord(timeline, candidateTimeline, EditOperationType.Move, clipId = clipId, trackId = toTrackId, details = "move clip")
    }

    override fun duplicateClip(timeline: Timeline, trackId: String, clipId: String, newClipId: String): MediaResult<Timeline> {
        val located = findTrackAndClip(timeline, trackId, clipId) ?: return failure("Clip $clipId not found on track $trackId")
        ensureTrackEditable(located.track)?.let { return it }
        if (timeline.containsClipId(newClipId)) {
            return failure("Clip $newClipId already exists")
        }
        val duplicated = located.clip.copy(
            id = newClipId,
            timelineStartMs = located.clip.timelineEndMs,
            metadata = located.clip.metadata.copy(createdFromSplit = false)
        )
        return addClip(timeline, trackId, duplicated).mapRecordedOperation(EditOperationType.Duplicate, timeline, clipId = newClipId, trackId = trackId, details = "duplicate clip")
    }

    override fun snapClip(
        timeline: Timeline,
        trackId: String,
        clipId: String,
        desiredStartMs: Long,
        playheadMs: Long?,
        thresholdMs: Long,
        enabled: Boolean
    ): MediaResult<SnapResult> {
        val located = findTrackAndClip(timeline, trackId, clipId) ?: return failure("Clip $clipId not found on track $trackId")
        if (!enabled) {
            return MediaResult.Success(SnapResult(desiredStartMs, desiredStartMs, didSnap = false))
        }
        val candidates = mutableListOf<SnapTarget>()
        if (desiredStartMs in 0L..thresholdMs) {
            candidates += SnapTarget(SnapTargetType.TimelineStart, 0L)
        }
        playheadMs?.let { candidates += SnapTarget(SnapTargetType.Playhead, it) }
        timeline.tracks.forEach { track ->
            track.clips.filterNot { it.id == clipId }.forEach { other ->
                candidates += SnapTarget(SnapTargetType.ClipStart, other.timelineStartMs, track.id, other.id)
                candidates += SnapTarget(SnapTargetType.ClipEnd, other.timelineEndMs, track.id, other.id)
            }
        }
        located.track.clips.filterNot { it.id == clipId }.forEach { neighbor ->
            candidates += SnapTarget(SnapTargetType.TrackNeighborStart, neighbor.timelineStartMs, located.track.id, neighbor.id)
            candidates += SnapTarget(SnapTargetType.TrackNeighborEnd, neighbor.timelineEndMs, located.track.id, neighbor.id)
        }
        val best = candidates
            .map { target -> target to kotlin.math.abs(target.positionMs - desiredStartMs) }
            .filter { (_, distance) -> distance <= thresholdMs }
            .minByOrNull { (_, distance) -> distance }

        return if (best == null) {
            MediaResult.Success(SnapResult(desiredStartMs, desiredStartMs, didSnap = false))
        } else {
            MediaResult.Success(
                SnapResult(
                    originalPositionMs = desiredStartMs,
                    snappedPositionMs = best.first.positionMs,
                    didSnap = true,
                    target = best.first
                )
            )
        }
    }

    override fun calculateDuration(timeline: Timeline): MediaResult<Long> {
        return MediaResult.Success(TimelineCalculations.calculateTimelineDuration(timeline))
    }

    override fun calculateClipVisibleRange(timeline: Timeline, clipId: String): MediaResult<ClipRange> {
        val clip = timeline.findClip(clipId) ?: return failure("Clip $clipId not found")
        return MediaResult.Success(
            ClipRange(
                timelineStartMs = clip.timelineStartMs,
                timelineEndMs = clip.timelineEndMs,
                durationMs = clip.visibleDurationMs
            )
        )
    }

    override fun calculateSourceRange(timeline: Timeline, clipId: String): MediaResult<SourceRange> {
        val clip = timeline.findClip(clipId) ?: return failure("Clip $clipId not found")
        return MediaResult.Success(
            SourceRange(
                sourceStartMs = clip.sourceStartMs,
                sourceEndMs = clip.sourceEndMs,
                durationMs = clip.visibleDurationMs
            )
        )
    }

    override fun validateTimeline(timeline: Timeline): MediaResult<Timeline> {
        if (timeline.tracks.isEmpty()) {
            return failure("Timeline must contain at least one track")
        }
        val trackIds = mutableSetOf<String>()
        val clipIds = mutableSetOf<String>()
        for (track in timeline.tracks) {
            if (track.id.isBlank()) {
                return failure("Track id is required")
            }
            if (!trackIds.add(track.id)) {
                return failure("Duplicate track id ${track.id}")
            }
            var previousEnd: Long? = null
            for (clip in track.clips.sortedBy { it.timelineStartMs }) {
                validateClip(track, clip, timeline.settings)?.let { return it }
                if (!clipIds.add(clip.id)) {
                    return failure("Duplicate clip id ${clip.id}")
                }
                if (!track.allowOverlap && previousEnd != null && clip.timelineStartMs < previousEnd) {
                    return failure("Track ${track.id} contains overlapping clips")
                }
                previousEnd = maxOf(previousEnd ?: Long.MIN_VALUE, clip.timelineEndMs)
            }
        }
        val allClipIds = timeline.tracks.flatMap { track -> track.clips.map { clip -> clip.id } }.toSet()
        val unknownSelected = timeline.selectedClipIds - allClipIds
        if (unknownSelected.isNotEmpty()) {
            return failure("Selection contains unknown clips: ${unknownSelected.joinToString()}")
        }
        for (group in timeline.clipGroups) {
            if (group.id.isBlank()) {
                return failure("Clip group id is required")
            }
            if (group.clipIds.size < 2) {
                return failure("Clip group ${group.id} must contain at least two clips")
            }
            val missing = group.clipIds - allClipIds
            if (missing.isNotEmpty()) {
                return failure("Clip group ${group.id} references unknown clips: ${missing.joinToString()}")
            }
        }
        if (timeline.settings.snapThresholdMs < 0L) {
            return failure("Snap threshold cannot be negative")
        }
        if (timeline.settings.defaultImageDurationMs <= 0L) {
            return failure("Default image duration must be greater than zero")
        }
        return success(timeline)
    }

    override fun undo(timeline: Timeline): MediaResult<Timeline> {
        val operation = timeline.history.undoStack.lastOrNull() ?: return failure("Nothing to undo")
        val previous = operation.before.copy(
            history = EditHistory(
                undoStack = timeline.history.undoStack.dropLast(1),
                redoStack = timeline.history.redoStack + operation
            )
        )
        return success(previous)
    }

    override fun redo(timeline: Timeline): MediaResult<Timeline> {
        val operation = timeline.history.redoStack.lastOrNull() ?: return failure("Nothing to redo")
        val restored = operation.after.copy(
            history = EditHistory(
                undoStack = timeline.history.undoStack + operation,
                redoStack = timeline.history.redoStack.dropLast(1)
            )
        )
        return success(restored)
    }

    private fun replaceClipAndRecord(
        timeline: Timeline,
        track: TimelineTrack,
        updatedClip: TimelineClip,
        operationType: EditOperationType,
        trackId: String,
        clipId: String,
        details: String
    ): MediaResult<Timeline> {
        val updatedTrack = track.copy(
            clips = track.clips.map { existing -> if (existing.id == clipId) updatedClip else existing }.sortedBy { it.timelineStartMs }
        )
        return validateAndRecord(timeline, timeline.replaceTrack(updatedTrack), operationType, clipId = clipId, trackId = trackId, details = details)
    }

    private fun rippleFollowingClips(track: TimelineTrack, clipId: String): TimelineTrack {
        val clips = track.clips.sortedBy { it.timelineStartMs }
        val pivotIndex = clips.indexOfFirst { it.id == clipId }
        if (pivotIndex == -1) {
            return track
        }
        val adjusted = clips.toMutableList()
        for (index in (pivotIndex + 1) until adjusted.size) {
            val previous = adjusted[index - 1]
            val current = adjusted[index]
            adjusted[index] = current.copy(timelineStartMs = previous.timelineEndMs)
        }
        return track.copy(clips = adjusted)
    }

    private fun insertClipAtIndex(track: TimelineTrack, clip: TimelineClip, index: Int?): TimelineTrack {
        val clips = track.clips.filterNot { it.id == clip.id }.toMutableList()
        if (index == null || index !in 0..clips.size) {
            clips += clip
            return track.copy(clips = clips.sortedBy { it.timelineStartMs })
        }
        clips.add(index, clip)
        return track.copy(clips = clips)
    }

    private fun updateTrackFlag(
        timeline: Timeline,
        trackId: String,
        transform: (TimelineTrack) -> TimelineTrack
    ): MediaResult<Timeline> {
        val track = timeline.findTrack(trackId) ?: return failure("Track $trackId not found")
        return success(timeline.replaceTrack(transform(track)))
    }

    private fun validateAndRecord(
        before: Timeline,
        afterCandidate: Timeline,
        operationType: EditOperationType,
        clipId: String? = null,
        trackId: String? = null,
        details: String? = null
    ): MediaResult<Timeline> {
        return when (val validation = validateTimeline(afterCandidate.copy(history = before.history))) {
            is MediaResult.Success -> {
                val sanitizedBefore = before.withoutHistory()
                val sanitizedAfter = validation.value.withoutHistory()
                val operation = EditOperation(
                    type = operationType,
                    before = sanitizedBefore,
                    after = sanitizedAfter,
                    clipId = clipId,
                    trackId = trackId,
                    details = details
                )
                val recorded = validation.value.copy(
                    history = EditHistory(
                        undoStack = before.history.undoStack + operation,
                        redoStack = emptyList()
                    )
                )
                success(recorded)
            }
            is MediaResult.Failure -> validation
        }
    }

    private fun validateClipPositionInternal(
        timeline: Timeline,
        track: TimelineTrack,
        clip: TimelineClip,
        ignoreClipId: String?
    ): MediaResult<Unit> {
        validateClip(track, clip, timeline.settings)?.let { return it }
        if (!track.allowOverlap) {
            val overlapping = track.clips
                .filterNot { it.id == ignoreClipId }
                .any { other -> clip.timelineStartMs < other.timelineEndMs && clip.timelineEndMs > other.timelineStartMs }
            if (overlapping) {
                return failure("Clip ${clip.id} overlaps another clip on track ${track.id}")
            }
        }
        return MediaResult.Success(Unit)
    }

    private fun validateClip(track: TimelineTrack, clip: TimelineClip, settings: TimelineSettings): MediaResult.Failure? {
        if (clip.id.isBlank()) {
            return failure("Clip id is required") as MediaResult.Failure
        }
        if (clip.assetId.isBlank()) {
            return failure("Clip ${clip.id} is missing an asset id") as MediaResult.Failure
        }
        if (clip.timelineStartMs < 0L) {
            return failure("Clip ${clip.id} cannot start before 0ms") as MediaResult.Failure
        }
        if (clip.sourceStartMs < 0L) {
            return failure("Clip ${clip.id} has a negative source start") as MediaResult.Failure
        }
        if (clip.sourceEndMs <= clip.sourceStartMs) {
            return failure("Clip ${clip.id} must have positive duration") as MediaResult.Failure
        }
        val sourceDuration = clip.resolvedSourceDurationMs(settings)
        if (sourceDuration <= 0L) {
            return failure("Clip ${clip.id} has invalid source duration") as MediaResult.Failure
        }
        if (clip.sourceEndMs > sourceDuration) {
            return failure("Clip ${clip.id} trim exceeds source duration") as MediaResult.Failure
        }
        if (track.type != TrackType.Audio && track.isMuted && clip.audio.isMuted.not()) {
            // Track mute is allowed independently from clip mute; no validation needed.
        }
        return null
    }

    private fun normalizeClip(clip: TimelineClip, settings: TimelineSettings): TimelineClip {
        val resolvedSourceDurationMs = when {
            clip.sourceDurationMs > 0L -> clip.sourceDurationMs
            clip.assetType == AssetType.Image -> clip.metadata.defaultImageDurationMs ?: settings.defaultImageDurationMs
            else -> clip.sourceEndMs
        }
        val normalizedEndMs = when {
            clip.sourceEndMs > clip.sourceStartMs -> clip.sourceEndMs
            clip.assetType == AssetType.Image -> clip.sourceStartMs + resolvedSourceDurationMs
            else -> clip.sourceEndMs
        }
        return clip.copy(
            sourceDurationMs = resolvedSourceDurationMs,
            sourceEndMs = normalizedEndMs
        )
    }

    private fun findTrackAndClip(timeline: Timeline, trackId: String, clipId: String): LocatedClip? {
        val track = timeline.findTrack(trackId) ?: return null
        val clip = track.clips.find { it.id == clipId } ?: return null
        return LocatedClip(track, clip)
    }

    private fun ensureTrackEditable(track: TimelineTrack): MediaResult.Failure? {
        return if (track.isLocked) failure("Track ${track.id} is locked") as MediaResult.Failure else null
    }

    private fun success(timeline: Timeline): MediaResult<Timeline> = MediaResult.Success(timeline)

    private fun failure(message: String): MediaResult.Failure = MediaResult.Failure(MediaError.Validation(message))

    private fun generateSplitClipId(originalId: String): String {
        return "$originalId-split-${System.nanoTime()}"
    }

    private data class LocatedClip(
        val track: TimelineTrack,
        val clip: TimelineClip
    )
}

private fun Timeline.findTrack(trackId: String): TimelineTrack? = tracks.find { it.id == trackId }

private fun Timeline.findClip(clipId: String): TimelineClip? = tracks.asSequence().flatMap { it.clips.asSequence() }.firstOrNull { it.id == clipId }

private fun Timeline.containsClipId(clipId: String): Boolean = findClip(clipId) != null

private fun Timeline.replaceTrack(track: TimelineTrack): Timeline {
    return copy(tracks = tracks.map { existing -> if (existing.id == track.id) track else existing })
}

private fun Timeline.withoutHistory(): Timeline = copy(history = EditHistory())

private fun TimelineClip.resolvedSourceDurationMs(settings: TimelineSettings): Long {
    return when {
        sourceDurationMs > 0L -> sourceDurationMs
        assetType == AssetType.Image -> metadata.defaultImageDurationMs ?: settings.defaultImageDurationMs
        else -> sourceEndMs
    }
}

private fun MediaResult<Timeline>.mapRecordedOperation(
    expectedType: EditOperationType,
    before: Timeline,
    clipId: String?,
    trackId: String?,
    details: String?
): MediaResult<Timeline> {
    return when (this) {
        is MediaResult.Success -> {
            val operation = value.history.undoStack.lastOrNull()
            if (operation != null && operation.before == before.withoutHistory()) {
                MediaResult.Success(
                    value.copy(
                        history = value.history.copy(
                            undoStack = value.history.undoStack.dropLast(1) + operation.copy(
                                type = expectedType,
                                clipId = clipId,
                                trackId = trackId,
                                details = details
                            )
                        )
                    )
                )
            } else {
                this
            }
        }
        is MediaResult.Failure -> this
    }
}
