package com.prstyadev.wibufy.ui.subscribed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prstyadev.wibufy.data.SubscribedAnimeEntity
import com.prstyadev.wibufy.ui.components.AnimeGridItem
import com.prstyadev.wibufy.ui.theme.WibufyBackground
import com.prstyadev.wibufy.ui.theme.WibufyPrimary
import com.prstyadev.wibufy.ui.theme.WibufySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribedScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit,
    viewModel: SubscribedViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortDropdownExpanded by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<SubscribedAnimeEntity?>(null) }

    // Safe navigation helper
    val safeNavigateToDetail: (String?) -> Unit = { animeId ->
        if (!animeId.isNullOrBlank()) {
            try {
                onNavigateToDetail(animeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WibufyBackground)
            .statusBarsPadding()
    ) {
        // Top Header: Centered "Subscribed Anime"
        Text(
            text = "Subscribed Anime",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 14.dp),
            textAlign = TextAlign.Center
        )

        // Thin Horizontal Divider under Subscribed Anime header
        HorizontalDivider(
            color = Color(0xFF26272B),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Sub-Header Row: "Total (X)" and Sort Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total (${uiState.totalCount})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Dropdown Selector
            Box {
                Surface(
                    color = Color(0xFF1E1F23),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3035)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sortDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.selectedSort.displayName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFFA0A3AC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortDropdownExpanded,
                    onDismissRequest = { sortDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1E1F23))
                ) {
                    SubscribedSortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.displayName,
                                    color = if (option == uiState.selectedSort) WibufyPrimary else Color.White,
                                    fontWeight = if (option == uiState.selectedSort) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.5.sp
                                )
                            },
                            onClick = {
                                viewModel.setSortOption(option)
                                sortDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Main Grid containing Ongoing & Completed sections
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("subscribed_grid")
        ) {
            // SECTION 1: Ongoing
            if (uiState.ongoingList.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "Ongoing (${uiState.ongoingList.size})",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(uiState.ongoingList, key = { "ongoing_${it.animeId}" }) { bookmark ->
                    AnimeGridItem(
                        bookmark = bookmark,
                        showBadge = false, // Sembunyikan badge "New"
                        onLongClick = {
                            itemToDelete = bookmark
                        },
                        onClick = { safeNavigateToDetail(bookmark.animeId) }
                    )
                }
            }

            // SECTION 2: Completed
            if (uiState.completedList.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "Completed (${uiState.completedList.size})",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(uiState.completedList, key = { "completed_${it.animeId}" }) { bookmark ->
                    AnimeGridItem(
                        bookmark = bookmark,
                        showBadge = false, // Sembunyikan badge "New"
                        onLongClick = {
                            itemToDelete = bookmark
                        },
                        onClick = { safeNavigateToDetail(bookmark.animeId) }
                    )
                }
            }

            // Empty state if both lists are empty
            if (uiState.ongoingList.isEmpty() && uiState.completedList.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada anime yang disubscribe",
                            color = Color(0xFF9E9FA4),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Delete / Unbookmark on Long-Press
    itemToDelete?.let { anime ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF5252)
                )
            },
            title = {
                Text(
                    text = "Hapus Langganan?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Apakah kamu yakin ingin menghapus \"${anime.title}\" dari daftar Subscribed Anime?",
                    color = Color(0xFFC0C2C8),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBookmark(anime.animeId)
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = "Hapus",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(
                        text = "Batal",
                        color = Color.White
                    )
                }
            },
            containerColor = WibufySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
