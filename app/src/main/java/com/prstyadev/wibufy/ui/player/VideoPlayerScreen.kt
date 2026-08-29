package com.prstyadev.wibufy.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

val overlayTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.85f),
    offset = Offset(2f, 2f),
    blurRadius = 4f
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    episodeSlug: String? = null,
    animeTitle: String? = null,
    episodeName: String? = null,
    posterUrl: String? = null,
    onMinimize: () -> Unit = {},
    onNavigateBack: () -> Unit = onMinimize,
    onNavigateToEpisode: ((newSlug: String, animeTitle: String?, episodeName: String?, posterUrl: String?) -> Unit)? = null,
    viewModel: GlobalPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var showQualityBottomSheet by remember { mutableStateOf(false) }
    var showSpeedBottomSheet by remember { mutableStateOf(false) }

    // If a new episodeSlug was provided that is not currently playing, start it
    LaunchedEffect(episodeSlug) {
        if (!episodeSlug.isNullOrBlank() && episodeSlug != uiState.episodeSlug) {
            viewModel.playEpisode(episodeSlug, animeTitle, episodeName, posterUrl)
        }
    }

    // Manage Fullscreen Orientation and System Bars
    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Restore orientation on exit
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back button: if in fullscreen exit fullscreen; else minimize player
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onMinimize()
        }
    }

    val resolvedAnimeTitle = remember(uiState.streamData?.title, uiState.animeTitle, animeTitle) {
        uiState.animeTitle ?: animeTitle ?: uiState.streamData?.title ?: "Anime"
    }

    val currentEpNum = remember(uiState.streamData?.title, uiState.episodeName, uiState.episodeSlug) {
        val titleText = uiState.streamData?.title ?: uiState.episodeName ?: uiState.episodeSlug
        val match = Regex("(?i)Episode\\s*(\\d+)").find(titleText)
            ?: Regex("(?i)ep[-_]?(\\d+)").find(titleText)
            ?: Regex("(?i)episode[-_]?(\\d+)").find(uiState.episodeSlug)
        match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
    }

    val hasPrevEpisode = (currentEpNum > 1) && (uiState.hasPreviousEpisode || !uiState.previousEpisodeSlug.isNullOrBlank())
    val prevEpLabel = uiState.previousEpisodeName ?: "Episode ${(currentEpNum - 1).coerceAtLeast(1)}"
    val prevEpSlug = uiState.previousEpisodeSlug ?: run {
        val prevEpInt = (currentEpNum - 1).coerceAtLeast(1)
        val slugRegex = Regex("(?i)(episode[-_]?)(\\d+)")
        if (slugRegex.containsMatchIn(uiState.episodeSlug)) {
            slugRegex.replace(uiState.episodeSlug) { m -> "${m.groupValues[1]}$prevEpInt" }
        } else {
            val altRegex = Regex("(?i)(ep[-_]?)(\\d+)")
            if (altRegex.containsMatchIn(uiState.episodeSlug)) {
                altRegex.replace(uiState.episodeSlug) { m -> "${m.groupValues[1]}$prevEpInt" }
            } else {
                "${uiState.episodeSlug}-$prevEpInt"
            }
        }
    }

    val hasNextEpisode = uiState.hasNextEpisode && !uiState.nextEpisodeSlug.isNullOrBlank()
    val nextEpLabel = uiState.nextEpisodeName ?: "Episode ${currentEpNum + 1}"
    val nextEpSlug = uiState.nextEpisodeSlug ?: run {
        val nextEpInt = currentEpNum + 1
        val slugRegex = Regex("(?i)(episode[-_]?)(\\d+)")
        if (slugRegex.containsMatchIn(uiState.episodeSlug)) {
            slugRegex.replace(uiState.episodeSlug) { m -> "${m.groupValues[1]}$nextEpInt" }
        } else {
            val altRegex = Regex("(?i)(ep[-_]?)(\\d+)")
            if (altRegex.containsMatchIn(uiState.episodeSlug)) {
                altRegex.replace(uiState.episodeSlug) { m -> "${m.groupValues[1]}$nextEpInt" }
            } else {
                "${uiState.episodeSlug}-$nextEpInt"
            }
        }
    }

    val displayTitle = remember(uiState.streamData?.title, resolvedAnimeTitle, uiState.episodeName) {
        uiState.streamData?.title ?: if (!uiState.episodeName.isNullOrBlank()) "$resolvedAnimeTitle ${uiState.episodeName}" else resolvedAnimeTitle
    }

    val handleNavigateToEpisode: (String, String) -> Unit = { targetSlug, targetLabel ->
        if (onNavigateToEpisode != null) {
            onNavigateToEpisode(targetSlug, resolvedAnimeTitle, targetLabel, uiState.posterUrl)
        } else {
            viewModel.playEpisode(targetSlug, resolvedAnimeTitle, targetLabel, uiState.posterUrl)
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context.findActivity())?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.parseColor("#161719")
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF161719),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF161719))
                .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                            .background(Color(0xFF161719)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFDD734),
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Memuat Stream Video...",
                                color = Color(0xFFCACACA),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                            .background(Color(0xFF161719))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.error ?: "Terjadi kesalahan",
                                color = Color(0xFFEF5350),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.playEpisode(uiState.episodeSlug, uiState.animeTitle, uiState.episodeName, uiState.posterUrl) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2D30))
                            ) {
                                Text("Coba Lagi", color = Color.White)
                            }
                        }
                    }
                }
                else -> {
                    val url = uiState.currentQualityUrl
                    if (url != null) {
                        CustomVideoPlayer(
                            exoPlayer = viewModel.exoPlayer,
                            hasPrevEpisode = hasPrevEpisode,
                            prevEpisodeLabel = prevEpLabel,
                            hasNextEpisode = hasNextEpisode,
                            nextEpisodeLabel = nextEpLabel,
                            currentQuality = uiState.currentQuality ?: "480p",
                            playbackSpeed = uiState.playbackSpeed,
                            isFullscreen = isFullscreen,
                            isAutonextEnabled = uiState.isAutonextEnabled,
                            isBuffering = uiState.isBuffering,
                            isPlaying = uiState.isPlaying,
                            currentPositionMs = uiState.currentPositionMs,
                            totalDurationMs = uiState.totalDurationMs,
                            bufferedPositionMs = uiState.bufferedPositionMs,
                            onToggleAutonext = { viewModel.toggleAutonext() },
                            onOpenQualityPicker = { showQualityBottomSheet = true },
                            onOpenSpeedPicker = { showSpeedBottomSheet = true },
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            onNavigateBack = {
                                if (isFullscreen) {
                                    isFullscreen = false
                                } else {
                                    onMinimize()
                                }
                            },
                            onNavigatePrevious = {
                                if (hasPrevEpisode) {
                                    handleNavigateToEpisode(prevEpSlug, prevEpLabel)
                                }
                            },
                            onNavigateNext = {
                                if (hasNextEpisode) {
                                    handleNavigateToEpisode(nextEpSlug, nextEpLabel)
                                }
                            },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeekTo = { pos -> viewModel.seekTo(pos) },
                            onSeekBy = { delta -> viewModel.seekBy(delta) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                                .background(Color(0xFF161719)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "URL video tidak ditemukan.",
                                color = Color(0xFFCACACA),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Resolution BottomSheet Picker
    if (showQualityBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQualityBottomSheet = false },
            containerColor = Color(0xFF1E1F23)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Pilih Resolusi Video",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                uiState.streamData?.qualities?.forEach { quality ->
                    val isSelected = quality.quality.equals(uiState.currentQuality, ignoreCase = true) ||
                            (quality.url != null && quality.url == uiState.currentQualityUrl)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = quality.quality ?: "Unknown",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFDD734) else Color.White
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Quality",
                                    tint = Color(0xFFFDD734)
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.changeQuality(quality)
                                showQualityBottomSheet = false
                            }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Playback Speed BottomSheet Picker
    if (showSpeedBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedBottomSheet = false },
            containerColor = Color(0xFF1E1F23)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Kecepatan Pemutaran",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                speeds.forEach { speed ->
                    val isSelected = uiState.playbackSpeed == speed
                    val label = if (speed == 1.0f) "Normal (1x)" else "${speed}x"
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFDD734) else Color.White
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Speed",
                                    tint = Color(0xFFFDD734)
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setPlaybackSpeed(speed)
                                showSpeedBottomSheet = false
                            }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    exoPlayer: ExoPlayer,
    hasPrevEpisode: Boolean,
    prevEpisodeLabel: String,
    hasNextEpisode: Boolean,
    nextEpisodeLabel: String,
    currentQuality: String,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    isAutonextEnabled: Boolean,
    isBuffering: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    bufferedPositionMs: Long,
    onToggleAutonext: () -> Unit,
    onOpenQualityPicker: () -> Unit,
    onOpenSpeedPicker: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onSeekBy: (deltaMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showControls by remember { mutableStateOf(true) }
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableLongStateOf(0L) }
    var showDoubleTapRewind by remember { mutableStateOf(false) }
    var showDoubleTapForward by remember { mutableStateOf(false) }
    var doubleTapRewindCount by remember { mutableIntStateOf(0) }
    var doubleTapForwardCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(doubleTapRewindCount) {
        if (doubleTapRewindCount > 0) {
            showDoubleTapRewind = true
            delay(650L)
            showDoubleTapRewind = false
        }
    }

    LaunchedEffect(doubleTapForwardCount) {
        if (doubleTapForwardCount > 0) {
            showDoubleTapForward = true
            delay(650L)
            showDoubleTapForward = false
        }
    }

    // Auto-hide controls timer (4 seconds of inactivity while playing and not dragging)
    LaunchedEffect(showControls, isPlaying, isDraggingScrubber) {
        if (showControls && isPlaying && !isDraggingScrubber) {
            delay(4000L)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.45f) {
                            onSeekBy(-10000L)
                            doubleTapRewindCount++
                        } else if (offset.x > width * 0.55f) {
                            onSeekBy(10000L)
                            doubleTapForwardCount++
                        } else {
                            onTogglePlayPause()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Android ExoPlayer View
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        // Double-Tap Rewind 10s Feedback
        AnimatedVisibility(
            visible = showDoubleTapRewind,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 64.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "Mundur 10 Detik",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-10 Detik",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = overlayTextShadow
                        )
                    )
                }
            }
        }

        // Double-Tap Forward 10s Feedback
        AnimatedVisibility(
            visible = showDoubleTapForward,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 64.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "Maju 10 Detik",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+10 Detik",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = overlayTextShadow
                        )
                    )
                }
            }
        }

        // Buffering Indicator
        AnimatedVisibility(
            visible = isBuffering,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFDD734),
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        // Custom Overlay UI
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF161719).copy(alpha = 0.50f),
                                    Color(0xFF161719).copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Center Controls Row
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Skip Previous
                    if (hasPrevEpisode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .widthIn(min = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onNavigatePrevious()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous Episode",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = prevEpisodeLabel,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // 2. Replay 10 Seconds
                    IconButton(
                        onClick = { onSeekBy(-10000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Replay 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // 3. Main Big White Play / Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onTogglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF1E1F24),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // 4. Forward 10 Seconds
                    IconButton(
                        onClick = { onSeekBy(10000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // 5. Skip Next
                    if (hasNextEpisode) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .widthIn(min = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onNavigateNext()
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next Episode",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nextEpisodeLabel,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(64.dp))
                    }
                }

                // Bottom Row Controls (Time, Autonext, Resolution, Speed, Fullscreen) & Scrubber
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayPosMs = if (isDraggingScrubber) dragProgressMs else currentPositionMs
                        Text(
                            text = "${formatDurationMs(displayPosMs)} / ${formatDurationMs(totalDurationMs)}",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                shadow = overlayTextShadow
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onToggleAutonext() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isAutonextEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Autonext",
                                    style = TextStyle(
                                        color = if (isAutonextEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        shadow = overlayTextShadow
                                    )
                                )
                            }

                            Text(
                                text = currentQuality,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenQualityPicker() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            val speedText = if (playbackSpeed == 1f) "1x" else "${playbackSpeed}x"
                            Text(
                                text = speedText,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenSpeedPicker() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Scrubber Bar
                    val progressFraction = if (totalDurationMs > 0) {
                        val pos = if (isDraggingScrubber) dragProgressMs else currentPositionMs
                        (pos.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val bufferedFraction = if (totalDurationMs > 0) {
                        (bufferedPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .pointerInput(totalDurationMs) {
                                detectTapGestures { offset ->
                                    if (totalDurationMs > 0) {
                                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                        val targetMs = (newFraction * totalDurationMs).toLong()
                                        onSeekTo(targetMs)
                                    }
                                }
                            }
                            .pointerInput(totalDurationMs) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        if (totalDurationMs > 0) {
                                            isDraggingScrubber = true
                                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            dragProgressMs = (fraction * totalDurationMs).toLong()
                                        }
                                    },
                                    onDragEnd = {
                                        if (totalDurationMs > 0) {
                                            onSeekTo(dragProgressMs)
                                            isDraggingScrubber = false
                                        }
                                    },
                                    onDragCancel = {
                                        isDraggingScrubber = false
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        if (totalDurationMs > 0) {
                                            val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            dragProgressMs = (fraction * totalDurationMs).toLong()
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val availableWidth = maxWidth

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bufferedFraction)
                                .height(3.5.dp)
                                .background(Color.White.copy(alpha = 0.5f))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(3.5.dp)
                                .background(Color(0xFFE50914))
                        )

                        val thumbOffset = availableWidth * progressFraction - 6.dp
                        Box(
                            modifier = Modifier
                                .offset(x = thumbOffset.coerceAtLeast(0.dp))
                                .size(12.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914))
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, min, sec)
    } else {
        String.format("%02d:%02d", min, sec)
    }
}
