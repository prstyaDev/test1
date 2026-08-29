package com.prstyadev.wibufy.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prstyadev.wibufy.data.BookmarkEntity
import com.prstyadev.wibufy.data.WatchHistoryEntity
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import com.prstyadev.wibufy.ui.theme.WibufyPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (animeId: String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = WibufyBackground,
        contentColor = Color.White,
        topBar = {
            if (uiState.isSelectionMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1D24))
                        .statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${uiState.selectedSlugs.size} Dipilih",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Batal",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (uiState.selectedSlugs.size == uiState.rawList.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Pilih Semua",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (uiState.selectedSlugs.isNotEmpty()) {
                                        showDeleteConfirmDialog = true
                                    }
                                },
                                enabled = uiState.selectedSlugs.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = if (uiState.selectedSlugs.isNotEmpty()) Color(0xFFEF5350) else Color.Gray
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1A1D24),
                            titleContentColor = Color.White
                        )
                    )
                    HorizontalDivider(
                        color = Color(0xFF26272B),
                        thickness = 1.dp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WibufyBackground)
                        .statusBarsPadding()
                ) {
                    Text(
                        text = "Riwayat Menonton",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 14.dp),
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(
                        color = Color(0xFF26272B),
                        thickness = 1.dp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WibufyBackground)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1E88E5))
                    }
                }
                uiState.rawList.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Riwayat Kosong",
                            tint = Color(0xFF494A54),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Belum Ada Riwayat Menonton",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Anime yang kamu tonton akan tersimpan otomatis dan muncul di sini.",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        uiState.groups.forEach { group ->
                            // Date Header Pill Chip
                            item(key = "header_${group.dateHeader}") {
                                TimelineDateHeader(
                                    dateText = group.dateHeader,
                                    isToday = group.dateHeader.equals("Hari ini", ignoreCase = true)
                                )
                            }

                            // Items under this date
                            items(
                                items = group.items,
                                key = { it.episodeSlug }
                            ) { historyItem ->
                                val isSelected = uiState.selectedSlugs.contains(historyItem.episodeSlug)
                                val isSubscribed = isHistoryItemSubscribed(historyItem, uiState.subscribedAnimeList)

                                TimelineHistoryCard(
                                    item = historyItem,
                                    isSubscribed = isSubscribed,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            viewModel.toggleSelection(historyItem.episodeSlug)
                                        } else {
                                            val derivedAnimeId = historyItem.episodeSlug.replace(Regex("-episode-\\d+.*"), "").trim()
                                            onNavigateToDetail(derivedAnimeId)
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionMode) {
                                            viewModel.startSelectionMode(historyItem.episodeSlug)
                                        } else {
                                            viewModel.toggleSelection(historyItem.episodeSlug)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Delete Confirmation Dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text("Hapus Riwayat", fontWeight = FontWeight.Bold, color = Color.White)
                    },
                    text = {
                        Text(
                            "Apakah Anda yakin ingin menghapus ${uiState.selectedSlugs.size} riwayat menonton yang dipilih?",
                            color = Color(0xFFCCCCCC)
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteSelected()
                                showDeleteConfirmDialog = false
                            }
                        ) {
                            Text("Hapus", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Batal", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF1E1F24),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun TimelineDateHeader(
    dateText: String,
    isToday: Boolean
) {
    val pillBgColor = if (isToday) Color(0xFF1E88E5) else Color(0xFF2B3844)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = pillBgColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = dateText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}

private fun isHistoryItemSubscribed(item: WatchHistoryEntity, subscribedList: List<BookmarkEntity>): Boolean {
    val derivedSlug = item.episodeSlug.replace(Regex("-episode-\\d+.*"), "").trim()
    if (derivedSlug.isNotEmpty() && subscribedList.any { it.animeId.equals(derivedSlug, ignoreCase = true) }) {
        return true
    }
    val titleSlug = item.animeTitle?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')
    if (!titleSlug.isNullOrEmpty() && subscribedList.any { 
        it.animeId.contains(titleSlug, ignoreCase = true) || titleSlug.contains(it.animeId, ignoreCase = true) 
    }) {
        return true
    }
    return false
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineHistoryCard(
    item: WatchHistoryEntity,
    isSubscribed: Boolean = false,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val watchTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val progress = remember(item.lastPositionMs, item.totalDurationMs) {
        if (item.totalDurationMs > 0) {
            (item.lastPositionMs.toFloat() / item.totalDurationMs.toFloat()).coerceIn(0.01f, 1f)
        } else {
            0.05f
        }
    }

    val formattedDurationText = remember(item.lastPositionMs, item.totalDurationMs) {
        val posStr = formatDuration(item.lastPositionMs)
        val durStr = if (item.totalDurationMs > 0) formatDuration(item.totalDurationMs) else "23:50"
        "$posStr / $durStr"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF1E88E5) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF222630) else Color(0xFF1A1C22),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Standard 2:3 Anime Poster Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141518))
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.animeTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Subscribed Badge at Bottom-Right (Only shown if isSubscribed is true)
                if (isSubscribed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A93E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Subscriptions,
                            contentDescription = "Subscribed",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Top Row: Title (up to 2 lines) & Watch Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.animeTitle ?: "Anime",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = watchTime,
                        color = Color(0xFFA0A3AC),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Episode Subtitle
                Text(
                    text = item.episodeName ?: "Episode",
                    color = Color(0xFF9E9FA6),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Red Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = Color(0xFFE50914),
                    trackColor = Color(0xFF383B44)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Duration Text aligned to end
                Text(
                    text = formattedDurationText,
                    color = Color(0xFF9E9FA6),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

                // Selection check indicator if in selection mode
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFF2C2D34)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
