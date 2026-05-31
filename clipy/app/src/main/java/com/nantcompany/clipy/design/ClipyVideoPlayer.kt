package com.nantcompany.clipy.design

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun ClipyVideoPlayer(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    previewColorMatrix: ColorMatrix? = null
) {
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var showOverlay by remember { mutableStateOf(false) }
    val togglePlayback = remember(player) {
        {
            if (player.isPlaying || player.playWhenReady) {
                player.pause()
                isPlaying = false
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                }
                player.play()
                isPlaying = true
            }
            showOverlay = true
            Unit
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) showOverlay = false
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.clearVideoSurface()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> TextureView(context) },
            update = { textureView ->
                if (textureView.tag !== player) {
                    player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    player.setVideoTextureView(textureView)
                    textureView.tag = player
                }
                textureView.applyPreviewColorMatrix(previewColorMatrix)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(player) {
                    detectTapGestures(onTap = { togglePlayback() })
                }
        )

        if (showOverlay || !isPlaying) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .clickable { togglePlayback() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 28.dp)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 7.dp, height = 28.dp)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            LaunchedEffect(showOverlay) {
                if (showOverlay && isPlaying) {
                    delay(1200)
                    showOverlay = false
                }
            }
        }
    }
}

private fun TextureView.applyPreviewColorMatrix(colorMatrix: ColorMatrix?) {
    if (colorMatrix == null) {
        setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
    invalidate()
}
