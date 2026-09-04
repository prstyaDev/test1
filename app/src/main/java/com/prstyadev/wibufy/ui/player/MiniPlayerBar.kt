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
import androidx.compose.foundation.gestures.detectTapGestures
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

@Composable
fun MiniPlayerKerangka(
    viewModel: GlobalPlayerViewModel,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = if (uiState.totalDurationMs > 0) {
        (uiState.currentPositionMs.toFloat() / uiState.totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onExpand() }
                )
            },
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
                // Left Video Player Slot placeholder: 80.dp height, 16:9 aspect ratio
                // The shared ExoPlayer surface sits directly on top of this slot without being re-created!
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
                        .clip(RectangleShape)
                        .background(Color.Black)
                )

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

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(
    viewModel: GlobalPlayerViewModel,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiniPlayerKerangka(
        viewModel = viewModel,
        onExpand = onExpand,
        onClose = onClose,
        modifier = modifier
    )
}
