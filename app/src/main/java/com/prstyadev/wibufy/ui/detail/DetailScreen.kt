package com.prstyadev.wibufy.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prstyadev.wibufy.data.EpisodeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    animeId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (episodeSlug: String, animeTitle: String?, episodeName: String?, posterUrl: String?) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animeId) {
        viewModel.loadAnimeDetail(animeId)
    }

    val anime = uiState.detailData?.anime

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(anime?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Unknown error")
            }
        } else if (anime != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    AsyncImage(
                        model = anime.poster,
                        contentDescription = anime.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = anime.title ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Text(text = "Rating: ${anime.score?.value ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "Status: ${anime.status ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val genres = anime.genreList?.joinToString { it.title ?: "" } ?: "N/A"
                        Text(text = "Genres: $genres", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row (Mulai Tonton / Lanjut Eps & Subscribe)
                        val lastHistory = uiState.lastWatchedHistory
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left Column: Watch Button + Timestamp progress indicator centered underneath
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF3EA5F4), Color(0xFF1E8AE5))
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            if (lastHistory != null && !lastHistory.episodeSlug.isBlank()) {
                                                onNavigateToPlayer(
                                                    lastHistory.episodeSlug,
                                                    anime.title,
                                                    lastHistory.episodeName ?: "Episode 1",
                                                    anime.poster
                                                )
                                            } else {
                                                val firstEp = findFirstEpisode(anime.episodeList)
                                                if (firstEp?.episodeId != null) {
                                                    val epNum = firstEp.title.toString().toDoubleOrNull()?.toInt()?.toString() ?: firstEp.title.toString()
                                                    val epTitle = "Episode $epNum"
                                                    onNavigateToPlayer(firstEp.episodeId, anime.title, epTitle, anime.poster)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF1E8AE5),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val buttonText = if (lastHistory != null) {
                                            val epName = lastHistory.episodeName ?: "Eps 1"
                                            if (epName.contains("Episode", ignoreCase = true)) {
                                                epName.replace(Regex("Episode\\s*", RegexOption.IGNORE_CASE), "Lanjut Eps ")
                                            } else if (epName.startsWith("Eps", ignoreCase = true)) {
                                                "Lanjut $epName"
                                            } else {
                                                "Lanjut Eps $epName"
                                            }
                                        } else {
                                            "Mulai Tonton"
                                        }
                                        Text(
                                            text = buttonText,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Timestamp progress detail if history exists (Centered directly under Lanjut Eps button)
                                if (lastHistory != null && (lastHistory.lastPositionMs > 0 || lastHistory.totalDurationMs > 0)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "· ${formatDurationMs(lastHistory.lastPositionMs)} / ${formatDurationMs(lastHistory.totalDurationMs)} ·",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.75f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Right Column: Subscribe Button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val isSubscribed = uiState.isBookmarked
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (isSubscribed) {
                                                Modifier
                                                    .background(Color.White)
                                                    .border(1.dp, Color.White, CircleShape)
                                            } else {
                                                Modifier
                                                    .background(
                                                        brush = Brush.horizontalGradient(
                                                            colors = listOf(Color(0xFF393A3E), Color(0xFF2C2D31))
                                                        )
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = Color.White.copy(alpha = 0.15f),
                                                        shape = CircleShape
                                                    )
                                            }
                                        )
                                        .clickable {
                                            viewModel.toggleBookmark(animeId)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSubscribed) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                            contentDescription = if (isSubscribed) "Subscribed" else "Subscribe",
                                            tint = if (isSubscribed) Color(0xFF1E1F24) else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                                            color = if (isSubscribed) Color(0xFF1E1F24) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Synopsis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val synopsisText = anime.synopsis?.paragraphs?.joinToString("\n\n") ?: "No synopsis available."
                        Text(text = synopsisText, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                val episodes = anime.episodeList ?: emptyList()
                if (episodes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Episodes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(episodes) { episode ->
                        val epNumber = episode.title.toString().toDoubleOrNull()?.toInt()?.toString() ?: episode.title.toString()
                        val epTitle = "Episode $epNumber"
                        ListItem(
                            headlineContent = { Text(epTitle) },
                            modifier = Modifier.clickable {
                                episode.episodeId?.let { slug ->
                                    onNavigateToPlayer(slug, anime.title, epTitle, anime.poster)
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

private fun findFirstEpisode(episodes: List<EpisodeItem>?): EpisodeItem? {
    if (episodes.isNullOrEmpty()) return null
    val ep1 = episodes.find { ep ->
        val t = ep.title.toString().trim()
        t == "1" || t == "Episode 1" || t.endsWith(" 1") || t == "1.0"
    }
    if (ep1 != null) return ep1
    return episodes.lastOrNull() ?: episodes.firstOrNull()
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
