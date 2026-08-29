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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

// Text shadow style for high-contrast video overlay readability
val overlayTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.85f),
    offset = Offset(2f, 2f),
    blurRadius = 4f
)

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    episodeSlug: String,
    animeTitle: String? = null,
    episodeName: String? = null,
    posterUrl: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isFullscreen by remember { mutableStateOf(false) }
    var showQualityBottomSheet by remember { mutableStateOf(false) }
    var showSpeedBottomSheet by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isAutonextEnabled by remember { mutableStateOf(true) }

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
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Restore orientation on exit
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                val window = act.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Handle back button when in fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    LaunchedEffect(episodeSlug) {
        viewModel.loadStream(episodeSlug)
    }

    val resolvedAnimeTitle = remember(uiState.streamData?.title, animeTitle) {
        animeTitle ?: uiState.streamData?.title ?: "Anime"
    }

    // Compute previous and next episode labels dynamically from title or episodeName
    val (currentEpNum, prevEpLabel, nextEpLabel) = remember(uiState.streamData?.title, episodeName, episodeSlug) {
        val titleText = uiState.streamData?.title ?: episodeName ?: episodeSlug
        val match = Regex("(?i)Episode\\s*(\\d+)").find(titleText)
            ?: Regex("(?i)ep[-_]?(\\d+)").find(episodeSlug)
        val epInt = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 2
        val prev = "Episode ${(epInt - 1).coerceAtLeast(1)}"
        val next = "Episode ${epInt + 1}"
        Triple(epInt, prev, next)
    }

    val displayTitle = remember(uiState.streamData?.title, resolvedAnimeTitle, episodeName) {
        uiState.streamData?.title ?: if (!episodeName.isNullOrBlank()) "$resolvedAnimeTitle $episodeName" else resolvedAnimeTitle
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { 
                        Text(
                            text = displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF141518),
                        titleContentColor = Color.White
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
                .background(Color.Black)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                            .background(Color(0xFF15161A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFE50914),
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
                            .fillMaxWidth()
                            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                            .background(Color(0xFF15161A))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Terjadi kesalahan",
                            color = Color(0xFFEF5350),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    val url = uiState.currentQualityUrl
                    if (url != null) {
                        CustomVideoPlayer(
                            url = url,
                            videoTitle = displayTitle,
                            prevEpisodeLabel = prevEpLabel,
                            nextEpisodeLabel = nextEpLabel,
                            currentQuality = uiState.currentQuality ?: "480p",
                            playbackSpeed = playbackSpeed,
                            isFullscreen = isFullscreen,
                            isAutonextEnabled = isAutonextEnabled,
                            initialPositionMs = uiState.initialPositionMs,
                            onToggleAutonext = { isAutonextEnabled = !isAutonextEnabled },
                            onOpenQualityPicker = { showQualityBottomSheet = true },
                            onOpenSpeedPicker = { showSpeedBottomSheet = true },
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            onNavigateBack = {
                                if (isFullscreen) {
                                    isFullscreen = false
                                } else {
                                    onNavigateBack()
                                }
                            },
                            onSaveProgress = { pos, dur ->
                                viewModel.saveProgress(
                                    episodeSlug = episodeSlug,
                                    animeTitle = resolvedAnimeTitle,
                                    episodeName = episodeName ?: "Episode $currentEpNum",
                                    posterUrl = posterUrl,
                                    lastPositionMs = pos,
                                    totalDurationMs = dur
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                                .background(Color(0xFF15161A)),
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
                                color = if (isSelected) Color(0xFFE50914) else Color.White
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Quality",
                                    tint = Color(0xFFE50914)
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
                    val isSelected = playbackSpeed == speed
                    val label = if (speed == 1.0f) "Normal (1x)" else "${speed}x"
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFE50914) else Color.White
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Speed",
                                    tint = Color(0xFFE50914)
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                playbackSpeed = speed
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
    url: String,
    videoTitle: String,
    prevEpisodeLabel: String,
    nextEpisodeLabel: String,
    currentQuality: String,
    playbackSpeed: Float,
    isFullscreen: Boolean,
    isAutonextEnabled: Boolean,
    initialPositionMs: Long = 0L,
    onToggleAutonext: () -> Unit,
    onOpenQualityPicker: () -> Unit,
    onOpenSpeedPicker: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onNavigateBack: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            setAudioAttributes(audioAttributes, true)
            playWhenReady = true
        }
    }

    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableLongStateOf(0L) }
    var hasAppliedInitialSeek by remember { mutableStateOf(false) }
    var hasRetriedWithFallback by remember(url) { mutableStateOf(false) }
    var showDoubleTapRewind by remember { mutableStateOf(false) }
    var showDoubleTapForward by remember { mutableStateOf(false) }
    var doubleTapRewindCount by remember { mutableIntStateOf(0) }
    var doubleTapForwardCount by remember { mutableIntStateOf(0) }

    // Auto-hide double tap animations after short delay
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

    // Update playback speed dynamically
    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Auto-hide controls timer (4 seconds of inactivity while playing and not dragging)
    LaunchedEffect(showControls, isPlaying, isDraggingScrubber) {
        if (showControls && isPlaying && !isDraggingScrubber) {
            delay(4000L)
            showControls = false
        }
    }

    // Periodic position update loop
    LaunchedEffect(exoPlayer, isDraggingScrubber) {
        while (true) {
            if (!isDraggingScrubber) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            }
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
            delay(200L)
        }
    }

    // ExoPlayer Listener
    DisposableEffect(exoPlayer, url) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY && !hasAppliedInitialSeek) {
                    if (initialPositionMs > 0 && (exoPlayer.duration <= 0 || initialPositionMs < exoPlayer.duration - 5000)) {
                        exoPlayer.seekTo(initialPositionMs)
                    }
                    hasAppliedInitialSeek = true
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!hasRetriedWithFallback) {
                    hasRetriedWithFallback = true
                    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setAllowCrossProtocolRedirects(true)
                        .setUserAgent(userAgent)
                        .setConnectTimeoutMs(20000)
                        .setReadTimeoutMs(20000)
                        .setDefaultRequestProperties(
                            mapOf(
                                "Referer" to "https://www.blogger.com/",
                                "Origin" to "https://www.blogger.com",
                                "User-Agent" to userAgent
                            )
                        )
                    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
                    val fallbackMediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setMimeType(MimeTypes.APPLICATION_M3U8)
                        .build()
                    val fallbackSource = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(fallbackMediaItem)
                    exoPlayer.setMediaSource(fallbackSource)
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Progress Auto-Save Tracker (Every 5 seconds)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(5000L)
            val currentPos = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (currentPos > 0 && duration > 0) {
                onSaveProgress(currentPos, duration)
            }
        }
    }

    // Lifecycle Auto-Pause and Resume
    DisposableEffect(lifecycleOwner, exoPlayer) {
        var wasPlayingBeforePause = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    wasPlayingBeforePause = exoPlayer.isPlaying || exoPlayer.playWhenReady
                    val currentPos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    if (currentPos > 0 && duration > 0) {
                        onSaveProgress(currentPos, duration)
                    }
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> {
                    if (wasPlayingBeforePause) {
                        exoPlayer.play()
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    val currentPos = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    if (currentPos > 0 && duration > 0) {
                        onSaveProgress(currentPos, duration)
                    }
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            val currentPos = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (currentPos > 0 && duration > 0) {
                onSaveProgress(currentPos, duration)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Load MediaSource when URL changes
    LaunchedEffect(url) {
        val currentPos = exoPlayer.currentPosition
        val uri = android.net.Uri.parse(url)
        val host = uri.host?.lowercase() ?: ""
        val isBloggerDomain = host.contains("blogger") || host.contains("blogspot") || host.contains("googleusercontent")
        val referer = if (isBloggerDomain || host.isEmpty()) "https://www.blogger.com/" else "${uri.scheme ?: "https"}://$host/"
        val origin = if (isBloggerDomain || host.isEmpty()) "https://www.blogger.com" else "${uri.scheme ?: "https"}://$host"
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to referer,
                    "Origin" to origin,
                    "User-Agent" to userAgent
                )
            )

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val defaultMediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        val isHls = url.contains("m3u8", ignoreCase = true) || url.contains("hls", ignoreCase = true)
        val isDash = url.contains("mpd", ignoreCase = true) || url.contains("dash", ignoreCase = true)

        val mediaItem = when {
            isHls -> MediaItem.Builder()
                .setUri(url)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            isDash -> MediaItem.Builder()
                .setUri(url)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build()
            else -> MediaItem.fromUri(url)
        }

        val mediaSource = defaultMediaSourceFactory.createMediaSource(mediaItem)

        exoPlayer.stop()
        exoPlayer.setMediaSource(mediaSource)
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()
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
                            // Tap kiri 2x -> Mundur 10 detik
                            val target = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            doubleTapRewindCount++
                        } else if (offset.x > width * 0.55f) {
                            // Tap kanan 2x -> Maju 10 detik
                            val maxDur = exoPlayer.duration.coerceAtLeast(0L)
                            val target = if (maxDur > 0) {
                                (exoPlayer.currentPosition + 10000L).coerceAtMost(maxDur)
                            } else {
                                exoPlayer.currentPosition + 10000L
                            }
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            doubleTapForwardCount++
                        } else {
                            // Tap tengah 2x -> Play / Pause
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
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
            modifier = Modifier.fillMaxSize()
        )

        // Double-Tap Rewind 10s Visual Feedback (Layar Kiri)
        AnimatedVisibility(
            visible = showDoubleTapRewind,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.1f),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-10 Detik",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = overlayTextShadow
                        )
                    )
                }
            }
        }

        // Double-Tap Fast-Forward 10s Visual Feedback (Layar Kanan)
        AnimatedVisibility(
            visible = showDoubleTapForward,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.1f),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+10 Detik",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
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
                    color = Color(0xFFE50914),
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 4.dp
                )
            }
        }

        // Custom Overlay UI (Exact Mockup Match)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            ) {
                // Top Bar in Overlay (Visible in fullscreen & standard)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = videoTitle,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            shadow = overlayTextShadow
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center Controls Row: Skip Previous (with label), Replay 10s, Big White Circle Play/Pause, Forward 10s, Skip Next (with label)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Skip Previous + Episode Name underneath (e.g., Episode 1)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                exoPlayer.seekTo(0L)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Episode",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = prevEpisodeLabel,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = overlayTextShadow
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Replay 10 Seconds
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Replay 10s",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // 3. Main Big White Play / Pause Circle Button (68.dp)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF1E1F24),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // 4. Forward 10 Seconds
                    IconButton(
                        onClick = {
                            val maxDur = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = if (maxDur > 0) {
                                (exoPlayer.currentPosition + 10000L).coerceAtMost(maxDur)
                            } else {
                                exoPlayer.currentPosition + 10000L
                            }
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // 5. Skip Next + Next Episode Name underneath (e.g., Episode 3)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val dur = exoPlayer.duration.coerceAtLeast(0L)
                                if (dur > 0) {
                                    exoPlayer.seekTo((dur - 2000L).coerceAtLeast(0L))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Episode",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextEpisodeLabel,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = overlayTextShadow
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                        // Left: Current time / Total time (e.g. 00:02 / 23:50)
                        val displayPosMs = if (isDraggingScrubber) dragProgressMs else currentPositionMs
                        Text(
                            text = "${formatDurationMs(displayPosMs)} / ${formatDurationMs(totalDurationMs)}",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                shadow = overlayTextShadow
                            )
                        )

                        // Right: Autonext, 480p, 1x, Fullscreen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            // Autonext toggle (▶| Autonext)
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Autonext",
                                    style = TextStyle(
                                        color = if (isAutonextEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        shadow = overlayTextShadow
                                    )
                                )
                            }

                            // Resolution badge (e.g. 480p) - Prominent bold font matching mockup
                            Text(
                                text = currentQuality,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenQualityPicker() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Speed indicator (e.g. 1x) - Prominent bold font matching mockup
                            val speedText = if (playbackSpeed == 1f) "1x" else "${playbackSpeed}x"
                            Text(
                                text = speedText,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = overlayTextShadow
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenSpeedPicker() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Fullscreen icon button (sharp brackets/fullscreen icon)
                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Full-width Slim Red Progress Slider / Scrubber Bar with Red Circle Thumb
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
                                        exoPlayer.seekTo(targetMs)
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
                                            exoPlayer.seekTo(dragProgressMs)
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

                        // Track background (dark gray / semi-transparent)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.5.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        // Buffered progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(bufferedFraction)
                                .height(3.5.dp)
                                .background(Color.White.copy(alpha = 0.5f))
                        )

                        // Active watched red bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(3.5.dp)
                                .background(Color(0xFFE50914))
                        )

                        // Red thumb circle at current scrubber position (matching mockup)
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

