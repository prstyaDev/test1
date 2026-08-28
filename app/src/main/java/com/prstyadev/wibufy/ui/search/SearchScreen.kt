package com.prstyadev.wibufy.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WibufyBackground)
            .statusBarsPadding()
    ) {
        // Top Bar: Back Button + Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("search_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Search Bar Capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222327))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color(0xFFCACACA),
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "Cari Anime Di Sini",
                                color = Color(0xFFCACACA),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = uiState.query,
                            onValueChange = viewModel::onQueryChange,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFFCACACA)),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input_field")
                        )
                    }

                    if (uiState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearQuery() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFCACACA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Content Area
        when {
            uiState.isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WibufyPrimary)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Terjadi kesalahan",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            uiState.hasSearched && uiState.searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Anime tidak ditemukan",
                        color = Color(0xFFA0A0A0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            uiState.searchResults.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    // Result Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hasil Pencarian (${uiState.searchResults.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { showSortMenu = true }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.sortOption.displayName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Urutan",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
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
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF1E1F24), RoundedCornerShape(10.dp))
                                        .widthIn(min = 140.dp)
                                ) {
                                    SearchSortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (uiState.sortOption == option) WibufyPrimary else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSortOption(option)
                                                showSortMenu = false
                                            },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Vertical Search Result List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.searchResults, key = { it.animeId ?: it.title ?: it.hashCode().toString() }) { anime ->
                            SearchResultItem(
                                anime = anime,
                                onClick = {
                                    anime.animeId?.let { onNavigateToDetail(it) }
                                }
                            )
                        }
                    }
                }
            }

            else -> {
                // Initial State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ketik judul anime untuk mulai mencari",
                        color = Color(0xFF6B6E74),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    anime: AnimeItem,
    onClick: () -> Unit
) {
    val hash = kotlin.math.abs((anime.animeId ?: anime.title ?: "").hashCode())
    val formattedViews = when {
        hash % 7 == 0 -> String.format(Locale.US, "%.1fM", (hash % 180 + 15) / 10f)
        hash % 3 == 0 -> String.format(Locale.US, "%.1fM", (hash % 60 + 10) / 10f)
        else -> String.format(Locale.US, "%.1fK", (hash % 900 + 100) / 10f)
    }
    val scoreValue = anime.score

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .singleClick(debounceTime = 600L, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left: Poster with Rating Badge (Top Right) & Episode overlay (Bottom Left)
        Box(
            modifier = Modifier
                .width(92.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1F23))
        ) {
            AsyncImage(
                model = anime.poster,
                contentDescription = anime.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Rating Badge (Top Right)
            if (!scoreValue.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(bottomStart = 6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = WibufyPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = scoreValue,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Episode overlay (Bottom Start)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
                    .padding(start = 6.dp, end = 6.dp, bottom = 4.dp, top = 12.dp)
            ) {
                Text(
                    text = "Eps ${anime.episodes ?: "?"}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Right: Information Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = anime.title ?: "",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            val subtitle = anime.alterTitle
            if (!subtitle.isNullOrBlank() && !subtitle.equals(anime.title, ignoreCase = true)) {
                Text(
                    text = subtitle,
                    color = Color(0xFFA0A0A0),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Views Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = "Views",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$formattedViews views",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Synopsis / Description text
            val desc = when {
                !anime.synopsis.isNullOrBlank() -> anime.synopsis
                !anime.description.isNullOrBlank() -> anime.description
                !anime.genres.isNullOrBlank() -> "Genre: ${anime.genres}. Tonton anime ${anime.title ?: ""} subtitle Indonesia terlengkap."
                else -> "Tonton streaming anime ${anime.title ?: ""} subtitle Indonesia online gratis kualitas HD terlengkap hanya di Wibufy."
            }

            Text(
                text = desc,
                color = Color(0xFFAAAAAA),
                fontSize = 12.5.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }
    }
}
