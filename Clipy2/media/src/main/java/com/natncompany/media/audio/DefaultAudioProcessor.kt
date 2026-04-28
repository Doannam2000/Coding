package com.natncompany.media.audio

import android.content.Context
import com.natncompany.media.Asset
import com.natncompany.media.AudioInfo
import com.natncompany.media.AudioMixItem
import com.natncompany.media.AudioMixPlan
import com.natncompany.media.AudioProcessor
import com.natncompany.media.AudioSettings
import com.natncompany.media.MediaError
import com.natncompany.media.MediaResult
import com.natncompany.media.MetadataReader
import com.natncompany.media.Timeline
import com.natncompany.media.TimelineClip
import com.natncompany.media.WaveformConfig
import com.natncompany.media.WaveformPlaceholder
import com.natncompany.media.WaveformProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sin

class DefaultAudioProcessor(
    context: Context,
    private val metadataReader: MetadataReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AudioProcessor {
    private val appContext = context.applicationContext

    override suspend fun analyzeAudio(asset: Asset): MediaResult<AudioInfo> = withContext(ioDispatcher) {
        when (val result = metadataReader.read(asset)) {
            is MediaResult.Success -> {
                val metadata = result.value.metadata
                MediaResult.Success(
                    AudioInfo(
                        durationMs = metadata.durationMs,
                        sampleRateHz = metadata.audioSampleRate,
                        channels = null,
                        bitrate = metadata.bitrate.takeIf { it > 0 },
                        mimeType = metadata.mimeType
                    )
                )
            }
            is MediaResult.Failure -> result
        }
    }

    override fun applyAudioSettings(clip: TimelineClip, settings: AudioSettings): MediaResult<TimelineClip> {
        val fadeInMs = settings.fadeInMs ?: clip.audio.fadeInMs
        val fadeOutMs = settings.fadeOutMs ?: clip.audio.fadeOutMs
        if (fadeInMs < 0L || fadeOutMs < 0L) {
            return MediaResult.Failure(MediaError.Validation("Fade duration cannot be negative"))
        }
        return MediaResult.Success(
            clip.copy(
                audio = clip.audio.copy(
                    volume = settings.volume?.coerceIn(0f, 2f) ?: clip.audio.volume,
                    isMuted = settings.muted ?: clip.audio.isMuted,
                    fadeInMs = fadeInMs.coerceAtMost(clip.visibleDurationMs),
                    fadeOutMs = fadeOutMs.coerceAtMost(clip.visibleDurationMs),
                    offsetMs = settings.offsetMs ?: clip.audio.offsetMs
                )
            )
        )
    }

    override fun createMixPlan(timeline: Timeline): MediaResult<AudioMixPlan> {
        val items = timeline.tracks.flatMap { track ->
            if (track.isMuted || !track.isEnabled) {
                emptyList()
            } else {
                track.clips.filterNot { it.audio.isMuted }.map { clip ->
                    AudioMixItem(
                        clipId = clip.id,
                        assetId = clip.assetId,
                        trackId = track.id,
                        startMs = clip.timelineStartMs,
                        durationMs = clip.visibleDurationMs,
                        sourceStartMs = (clip.sourceStartMs + clip.audio.offsetMs).coerceAtLeast(0L),
                        volume = clip.audio.volume,
                        muted = clip.audio.isMuted,
                        fadeInMs = clip.audio.fadeInMs.coerceAtMost(clip.visibleDurationMs),
                        fadeOutMs = clip.audio.fadeOutMs.coerceAtMost(clip.visibleDurationMs),
                        audioOffsetMs = clip.audio.offsetMs
                    )
                }
            }
        }.sortedBy { it.startMs }
        return MediaResult.Success(AudioMixPlan(items = items, timelineDurationMs = timeline.durationMs))
    }

    override fun mapTimelineToSourceTime(clip: TimelineClip, timelineTimeMs: Long): MediaResult<Long> {
        if (timelineTimeMs !in clip.timelineStartMs..clip.timelineEndMs) {
            return MediaResult.Failure(MediaError.Validation("Timeline time is outside clip bounds"))
        }
        val offset = timelineTimeMs - clip.timelineStartMs
        return MediaResult.Success((clip.sourceStartMs + offset + clip.audio.offsetMs).coerceAtLeast(0L))
    }

    override fun extractWaveform(asset: Asset, config: WaveformConfig): Flow<WaveformProgress> = flow {
        emit(WaveformProgress(progressPercent = 0))
        delay(16)
        val waveform = when (val placeholder = createWaveformPlaceholder(asset, config.samples)) {
            is MediaResult.Success -> placeholder.value
            is MediaResult.Failure -> {
                emit(WaveformProgress(progressPercent = 100, completed = true, error = placeholder.error))
                return@flow
            }
        }
        emit(WaveformProgress(progressPercent = 100, waveform = waveform, completed = true))
    }.flowOn(ioDispatcher)

    override fun createWaveformPlaceholder(asset: Asset, samples: Int): MediaResult<WaveformPlaceholder> {
        val safeSamples = samples.coerceIn(8, 512)
        val seed = abs(asset.id.hashCode()).coerceAtLeast(1)
        val bars = List(safeSamples) { index ->
            val wave = sin((index + 1) * (seed % 17 + 3) * 0.31).toFloat()
            (0.25f + abs(wave) * 0.75f).coerceIn(0f, 1f)
        }
        return MediaResult.Success(
            WaveformPlaceholder(
                bars = bars,
                durationMs = asset.durationMs ?: 0L
            )
        )
    }
}
