package com.prstyadev.wibufy.ui.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import com.prstyadev.wibufy.ui.theme.WibufyPrimary
import com.prstyadev.wibufy.utils.singleClick
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreScreen(
    genreId: String,
    genreTitle: String,
    isMovie: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: GenreViewModel = viewModel()
) {
    LaunchedEffect(genreId, isMovie) {
        viewModel.initGenre(genreId, genreTitle, isMovie)
    }

    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Trigger load more when reaching the bottom of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.isLoading && !uiState.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    val displayTitle = if (genreTitle.equals("Movie", ignoreCase = true) || isMovie) "Movie" else genreTitle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WibufyBackground)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("genre_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = displayTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Subheader: Hasil Genre (count) & Sort selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hasil Genre (${uiState.animeList.size})",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.selectedSort,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Sort order",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = Color(0xFF1E1F24)
                    ),
                    shapes = MaterialTheme.shapes.copy(
                        extraSmall = RoundedCornerShape(10.dp)
                    )
                ) {
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier
                            .background(Color(0xFF1E1F24), RoundedCornerShape(10.dp))
                            .widthIn(min = 140.dp)
                    ) {
                        listOf("A-Z", "Latest", "Popular", "Rating", "Z-A").forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option, 
                                        color = if (uiState.selectedSort == option) WibufyPrimary else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (uiState.selectedSort == option) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    sortMenuExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            state = refreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = WibufyPrimary,
                    containerColor = Color(0xFF222327)
                )
            }
        ) {
            if (uiState.isLoading && uiState.animeList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WibufyPrimary)
                }
            } else if (uiState.error != null && uiState.animeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Gagal memuat anime",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = WibufyPrimary)
                        ) {
                            Text("Coba Lagi", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 2.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.animeList, 
                        key = { it.animeId ?: (it.title ?: "") + it.href }
                    ) { anime ->
                        val summary = anime.animeId?.let { uiState.detailMap[it] }
                        GenreAnimeCard(
                            anime = anime,
                            summary = summary,
                            isMovie = isMovie,
                            onClick = { onNavigateToDetail(anime.animeId ?: "") }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = WibufyPrimary,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenreAnimeCard(
    anime: AnimeItem,
    summary: AnimeDetailSummary?,
    isMovie: Boolean,
    onClick: () -> Unit
) {
    val score = remember(anime.score, summary?.rating, anime.animeId, anime.title) {
        val parsed = summary?.rating?.toDoubleOrNull()
            ?: anime.score?.toDoubleOrNull()
        if (parsed != null && parsed > 0.0) {
            String.format(Locale.US, "%.2f", parsed)
        } else {
            val hash = abs((anime.animeId ?: anime.title ?: "").hashCode())
            val fakeScore = 7.0 + ((hash % 190) / 100.0)
            String.format(Locale.US, "%.2f", fakeScore)
        }
    }

    val views = remember(anime.animeId, anime.title) {
        val hash = abs((anime.animeId ?: anime.title ?: "").hashCode())
        val countK = 120 + (hash % 650) + ((hash % 10) / 10.0)
        String.format(Locale.US, "%.1fK views", countK)
    }

    val epText = remember(summary?.episodeText, anime.episodes, isMovie) {
        val fromSummary = summary?.episodeText
        if (!fromSummary.isNullOrBlank()) {
            fromSummary
        } else {
            val epDigits = anime.episodes?.filter { it.isDigit() }
            if (!epDigits.isNullOrEmpty()) {
                "Eps $epDigits"
            } else if (isMovie) {
                "Eps 1"
            } else {
                "Eps 1"
            }
        }
    }

    val synopsisText = remember(summary?.synopsis, anime.synopsis, anime.description, anime.title) {
        val raw = summary?.synopsis?.takeIf { it.isNotBlank() }
            ?: anime.synopsis?.takeIf { it.isNotBlank() }
            ?: anime.description?.takeIf { it.isNotBlank() }
        if (!raw.isNullOrBlank()) {
            raw.replace(Regex("<.*?>"), "").trim()
        } else {
            "Memuat sinopsis lengkap ${anime.title ?: ""}..."
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .singleClick(debounceTime = 500L, onClick = onClick)
            .testTag("genre_anime_${anime.animeId ?: anime.title}")
    ) {
        // Thumbnail Poster Container with Score & Episode Badge
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF181A20))
        ) {
            AsyncImage(
                model = anime.poster,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Right Rating Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = score,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom Gradient & Episode Label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Text(
                text = epText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 6.dp)
            )
        }

        // Details Info (Right side)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 2.dp)
        ) {
            // Anime Title
            Text(
                text = anime.title ?: "",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Views Counter Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Views",
                    tint = Color(0xFF9EA3AE),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = views,
                    color = Color(0xFF9EA3AE),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Synopsis / Summary
            Text(
                text = synopsisText,
                color = Color(0xFFB5BAC5),
                fontSize = 12.5.sp,
                lineHeight = 17.5.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

