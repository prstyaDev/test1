package com.prstyadev.wibufy.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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

    var dragOffset by remember { mutableFloatStateOf(0f) }

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
            .height(68.dp)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragOffset += delta
                },
                onDragStopped = {
                    if (kotlin.math.abs(dragOffset) > 200) {
                        onClose()
                    }
                    dragOffset = 0f
                }
            )
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
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = viewModel.exoPlayer
                                useController = false
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
                        },
                        onRelease = { playerView ->
                            playerView.player = null
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

                Spacer(modifier = Modifier.width(12.dp))

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
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayEpisodeName,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Play/Pause Action
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Close (X) Action
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup Pemutar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
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
