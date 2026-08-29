package com.prstyadev.wibufy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prstyadev.wibufy.data.AnimeItem
import com.prstyadev.wibufy.data.WatchHistoryEntity
import com.prstyadev.wibufy.ui.components.AnimeGridItem
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import com.prstyadev.wibufy.ui.theme.WibufyPrimary
import com.prstyadev.wibufy.ui.theme.WibufySecondary
import com.prstyadev.wibufy.ui.theme.WibufySurface
import com.prstyadev.wibufy.utils.singleClick
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBookmark: () -> Unit,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToPlayer: (episodeSlug: String, animeTitle: String?, episodeName: String?, posterUrl: String?) -> Unit = { _, _, _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val refreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshHomeData() },
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
        if (uiState.isLoading && uiState.page1Items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WibufyPrimary)
            }
        } else if (uiState.error != null && uiState.page1Items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.error ?: "Unknown error", 
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.refreshHomeData() },
                        colors = ButtonDefaults.buttonColors(containerColor = WibufyPrimary)
                    ) {
                        Text("Coba Lagi", color = Color.White)
                    }
                }
            }
        } else {
            val page1Anime = uiState.page1Items
            val page2Anime = uiState.page2Items
            
            var isExpanded by remember { mutableStateOf(false) }
            val displayedAnime = if (isExpanded) (page1Anime + page2Anime).take(24) else page1Anime.take(12)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Fixed Search Bar at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SearchBarUI(onClick = onNavigateToSearch)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = 8.dp, 
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // SECTION: Terakhir Ditonton (Shown above New Update Anime if history exists)
                    if (uiState.watchHistory.isNotEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            WatchHistorySection(
                                historyList = uiState.watchHistory,
                                subscribedAnimeIds = uiState.subscribedAnimeIds,
                                onNavigateToHistory = onNavigateToHistory,
                                onNavigateToDetail = onNavigateToDetail
                            )
                        }
                    }

                    // Section Header Span: New Update Anime
                    item(span = { GridItemSpan(3) }) {
                        SectionHeader(onNavigateToSchedule = onNavigateToSchedule)
                    }

                    // Grid Items
                    itemsIndexed(displayedAnime) { index, anime ->
                        val isPage1 = index < page1Anime.size
                        val isSubscribed = uiState.subscribedAnimeIds.contains(anime.animeId ?: "")
                        AnimeGridItem(
                            anime = anime, 
                            showBadge = isPage1,
                            isSubscribed = isSubscribed,
                            onClick = { onNavigateToDetail(anime.animeId ?: "") }
                        )
                    }

                    // Show More / Show Less Button
                    item(span = { GridItemSpan(3) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isExpanded) "Show Less" else "Show More",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.singleClick(debounceTime = 400L) { 
                                    isExpanded = !isExpanded 
                                    if (isExpanded) {
                                        viewModel.loadPage2()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchHistorySection(
    historyList: List<WatchHistoryEntity>,
    subscribedAnimeIds: Set<String>,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetail: (animeId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append("Terakhir")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = Color.White)) {
                        append(" Ditonton")
                    }
                },
                fontSize = 24.sp,
                lineHeight = 28.sp
            )
            Text(
                text = "Lihat Lainnya >",
                color = WibufySecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .singleClick(debounceTime = 600L, onClick = onNavigateToHistory)
                    .padding(end = 4.dp)
                    .testTag("home_see_history_button")
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(historyList, key = { it.episodeSlug }) { item ->
                val isSubscribed = isHistoryItemSubscribed(item, subscribedAnimeIds)
                val derivedAnimeId = item.episodeSlug.replace(Regex("-episode-\\d+.*"), "").trim()
                WatchHistoryCard(
                    item = item,
                    isSubscribed = isSubscribed,
                    onClick = {
                        onNavigateToDetail(derivedAnimeId)
                    }
                )
            }
        }
    }
}

private fun isHistoryItemSubscribed(item: WatchHistoryEntity, subscribedAnimeIds: Set<String>): Boolean {
    val derivedSlug = item.episodeSlug.replace(Regex("-episode-\\d+.*"), "").trim()
    if (derivedSlug.isNotEmpty() && subscribedAnimeIds.any { it.equals(derivedSlug, ignoreCase = true) }) {
        return true
    }
    val titleSlug = item.animeTitle?.lowercase()?.replace(Regex("[^a-z0-9]+"), "-")?.trim('-')
    if (!titleSlug.isNullOrEmpty() && subscribedAnimeIds.any { 
        it.contains(titleSlug, ignoreCase = true) || titleSlug.contains(it, ignoreCase = true) 
    }) {
        return true
    }
    return false
}

@Composable
fun WatchHistoryCard(
    item: WatchHistoryEntity,
    isSubscribed: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .singleClick(debounceTime = 600L, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222327))
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.animeTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient scrim for bottom text clarity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Episode Label
            Text(
                text = formatEpisodeLabel(item.episodeName, item.episodeSlug),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 6.dp)
            )

            // Subscribed Badge (Only rendered if user has subscribed to the anime)
            if (isSubscribed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A93E6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Subscriptions,
                        contentDescription = "Subscribed",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Red Progress Bar at the bottom
            val progress = if (item.totalDurationMs > 0) {
                (item.lastPositionMs.toFloat() / item.totalDurationMs.toFloat()).coerceIn(0.06f, 1f)
            } else {
                0.25f
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF383838))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFFE50914))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.animeTitle ?: item.episodeName ?: "Anime",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatEpisodeLabel(episodeName: String?, episodeSlug: String?): String {
    val name = episodeName?.trim()
    if (!name.isNullOrEmpty()) {
        val lower = name.lowercase()
        if (lower.startsWith("episode")) {
            val num = name.substring(7).trim()
            return "Eps $num"
        }
        if (lower.startsWith("eps")) {
            return name
        }
        val match = Regex("""\b(?:ep|eps|episode)?\s*(\d+)\b""", RegexOption.IGNORE_CASE).find(name)
        if (match != null) {
            return "Eps ${match.groupValues[1]}"
        }
        return name
    }
    val slug = episodeSlug ?: ""
    val slugMatch = Regex("""episode-(\d+)""").find(slug)
    if (slugMatch != null) {
        return "Eps ${slugMatch.groupValues[1]}"
    }
    return "Eps 1"
}

@Composable
fun SearchBarUI(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(Color(0xFF222327))
            .singleClick(debounceTime = 600L, onClick = onClick)
            .testTag("home_search_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFFCACACA),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Cari Anime Di Sini",
                color = Color(0xFFCACACA),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(onNavigateToSchedule: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    append("New")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = Color.White)) {
                    append(" Update\nAnime")
                }
            },
            fontSize = 24.sp,
            lineHeight = 28.sp
        )
        Text(
            text = "Lihat Jadwal >",
            color = WibufySecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .singleClick(debounceTime = 600L, onClick = onNavigateToSchedule)
                .padding(end = 16.dp)
                .testTag("home_see_schedule_button")
        )
    }
}

@Composable
fun AnimeListItem(anime: AnimeItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .singleClick(debounceTime = 600L, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier
                .width(100.dp)
                .height(140.dp)
        ) {
            AsyncImage(
                model = anime.poster,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = anime.title ?: "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Episodes: ${anime.episodes ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
