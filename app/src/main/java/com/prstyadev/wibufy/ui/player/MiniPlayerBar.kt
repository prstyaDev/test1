package com.prstyadev.wibufy.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.R
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(
    viewModel: GlobalPlayerViewModel,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-pause ExoPlayer when app is paused or stopped
    DisposableEffect(lifecycleOwner, viewModel.exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val progress = if (uiState.totalDurationMs > 0) {
        (uiState.currentPositionMs.toFloat() / uiState.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // YouTube-like Gestures:
    // 1. Vertical Swipe Up (dragUpOffsetY) -> Expands to full screen video player
    // 2. Horizontal Swipe (dragOffsetX) -> Dismiss/Closes player
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Resolve robust anime title & episode name
    val displayAnimeTitle = remember(uiState.animeTitle, uiState.streamData?.title, uiState.episodeSlug) {
        resolveCleanAnimeTitle(uiState.animeTitle, uiState.streamData?.title, uiState.episodeSlug)
    }

    val displayEpisodeName = remember(uiState.episodeName, uiState.streamData?.title, uiState.episodeSlug) {
        resolveCleanEpisodeName(uiState.episodeName, uiState.streamData?.title, uiState.episodeSlug)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .offset { IntOffset(dragOffsetX.value.toInt(), dragOffsetY.value.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            // Check vertical swipe up first
                            if (dragOffsetY.value < -80f) {
                                onExpand()
                                dragOffsetY.snapTo(0f)
                                dragOffsetX.snapTo(0f)
                            } else if (kotlin.math.abs(dragOffsetX.value) > 180f) {
                                // Horizontal swipe dismiss
                                onClose()
                                dragOffsetX.snapTo(0f)
                                dragOffsetY.snapTo(0f)
                            } else {
                                // Snap back with spring
                                launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                                launch {
                                    dragOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.snapTo(0f)
                            dragOffsetX.snapTo(0f)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            // If dragging more vertically (upwards)
                            if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) || dragOffsetY.value != 0f) {
                                val newY = (dragOffsetY.value + dragAmount.y).coerceAtMost(0f)
                                dragOffsetY.snapTo(newY)
                            } else {
                                val newX = dragOffsetX.value + dragAmount.x
                                dragOffsetX.snapTo(newX)
                            }
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            ),
        shape = RectangleShape,
        color = Color(0xFF161719),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Video Player Thumbnail: Sharp rectangle flush to top, bottom, and left edge (0dp padding)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
                        .clip(RectangleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            (LayoutInflater.from(ctx).inflate(R.layout.view_mini_player, null) as PlayerView).apply {
                                player = viewModel.exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setPadding(0, 0, 0, 0)
                            }
                        },
                        update = { playerView ->
                            if (playerView.player != viewModel.exoPlayer) {
                                playerView.player = viewModel.exoPlayer
                            }
                            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        },
                        onRelease = { playerView ->
                            if (playerView.player == viewModel.exoPlayer) {
                                playerView.player = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(0.dp)
                    )

                    if (uiState.isLoading || uiState.isBuffering) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFDD734),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Center Anime Title & Current Episode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayAnimeTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = displayEpisodeName,
                        color = Color(0xFFA0A0A0),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Play/Pause Action
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Close (X) Action
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup Pemutar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Thin Red Progress Bar (YouTube Style) - sits right at the bottom edge
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter),
                color = Color(0xFFE50914),
                trackColor = Color(0xFF2C2D30)
            )
        }
    }
}
