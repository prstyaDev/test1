package com.prstyadev.wibufy.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import com.prstyadev.wibufy.ui.theme.WibufyPrimary

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(episodeSlug) {
        viewModel.loadStream(episodeSlug)
    }

    var showQualityBottomSheet by remember { mutableStateOf(false) }

    val resolvedAnimeTitle = remember(uiState.streamData?.title, animeTitle) {
        animeTitle ?: uiState.streamData?.title ?: "Anime"
    }
    val resolvedEpisodeName = remember(uiState.streamData?.title, episodeName) {
        episodeName ?: run {
            val title = uiState.streamData?.title ?: ""
            if (title.contains("Episode", ignoreCase = true)) {
                val match = Regex("(?i)Episode\\s*\\d+").find(title)
                match?.value ?: "Episode"
            } else {
                "Episode"
            }
        }
    }

    Scaffold(
        containerColor = WibufyBackground,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.streamData?.title ?: resolvedAnimeTitle,
                        maxLines = 1,
                        fontSize = 17.sp,
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
                actions = {
                    if (uiState.streamData?.qualities?.isNotEmpty() == true) {
                        IconButton(onClick = { showQualityBottomSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings, 
                                contentDescription = "Quality Settings",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161719),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // Smooth Loading Skeleton / Shimmer Card for Stream Data
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color(0xFF1B1C20)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2A93E6),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
                            .aspectRatio(16f / 9f)
                            .background(Color(0xFF1B1C20))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Terjadi kesalahan",
                            color = Color(0xFFEF5350),
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    val url = uiState.currentQualityUrl
                    if (url != null) {
                        ExoPlayerView(
                            url = url,
                            initialPositionMs = uiState.initialPositionMs,
                            onSaveProgress = { pos, dur ->
                                viewModel.saveProgress(
                                    episodeSlug = episodeSlug,
                                    animeTitle = resolvedAnimeTitle,
                                    episodeName = resolvedEpisodeName,
                                    posterUrl = posterUrl,
                                    lastPositionMs = pos,
                                    totalDurationMs = dur
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color(0xFF1B1C20)),
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

            // Quality indicator info bar under player
            if (uiState.currentQuality != null && !uiState.isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kualitas Aktif: ${uiState.currentQuality}",
                        color = Color(0xFFA0A0A0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF222327),
                        modifier = Modifier.clickable { showQualityBottomSheet = true }
                    ) {
                        Text(
                            text = "Ganti Kualitas",
                            color = Color(0xFF2A93E6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

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
                                color = if (isSelected) Color(0xFF2A93E6) else Color.White
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected Quality",
                                    tint = Color(0xFF2A93E6)
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
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    url: String,
    initialPositionMs: Long = 0L,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
        }
    }

    var isBuffering by remember { mutableStateOf(false) }
    var hasAppliedInitialSeek by remember { mutableStateOf(false) }

    // ExoPlayer Listener for Buffering State & Seek on Ready
    DisposableEffect(exoPlayer) {
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
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Periodic Progress Auto-Save Tracker (Every 5 seconds)
    LaunchedEffect(exoPlayer) {
        while (true) {
            kotlinx.coroutines.delay(5000L)
            val currentPos = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            if (currentPos > 0 && duration > 0) {
                onSaveProgress(currentPos, duration)
            }
        }
    }

    // Lifecycle Auto-Pause and Auto-Resume + Progress Saving
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

    // Load URL and preserve playback position when changing quality
    LaunchedEffect(url) {
        val currentPos = exoPlayer.currentPosition
        val shouldPlay = exoPlayer.isPlaying || exoPlayer.playWhenReady
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = shouldPlay
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Smooth Buffering Overlay UI
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
                    color = Color(0xFF2A93E6),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
            }
        }
    }
}
