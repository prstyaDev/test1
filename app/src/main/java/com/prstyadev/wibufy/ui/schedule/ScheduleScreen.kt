package com.prstyadev.wibufy.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.prstyadev.wibufy.data.ScheduleAnimeItem
import com.prstyadev.wibufy.utils.singleClick
import java.util.Locale

// Exact Wibufy Colors from Mockup
private val ColorBackground = Color(0xFF161719)
private val ColorCardSurface = Color(0xFF1E1F23)
private val ColorAccentActive = Color(0xFF2A93E6)
private val ColorStatusAired = Color(0xFFFDD734)
private val ColorStatusWaiting = Color(0xFF8E9096)
private val ColorStripeWaiting = Color(0xFF45474D)
private val ColorTextPrimary = Color(0xFFFFFFFF)
private val ColorTextMuted = Color(0xFF9A9BA0)
private val ColorDateInactive = Color(0xFF727375)
private val ColorDivider = Color(0xFF26272B)
private val ColorPillButton = Color(0xFF141517)
private val ColorPillBorder = Color(0xFF2E3035)

@Composable
fun ScheduleScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val selectedDay = uiState.days.getOrNull(uiState.selectedDayIndex)
    val prevIndex = if (uiState.selectedDayIndex == 0) 6 else uiState.selectedDayIndex - 1
    val nextIndex = if (uiState.selectedDayIndex == 6) 0 else uiState.selectedDayIndex + 1
    val prevDay = uiState.days.getOrNull(prevIndex)
    val nextDay = uiState.days.getOrNull(nextIndex)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Bar: Centered "Jadwal Tayang"
            Text(
                text = "Jadwal Tayang",
                color = ColorTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 14.dp),
                textAlign = TextAlign.Center
            )

            // Thin Horizontal Divider under Jadwal Tayang header
            HorizontalDivider(
                color = ColorDivider,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Horizontal Date Selector (Min, Sen, Sel, Rab, Kam, Jum, Sab)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                uiState.days.forEach { day ->
                    val isSelected = day.dayIndex == uiState.selectedDayIndex
                    DaySelectorItem(
                        day = day,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectDay(day.dayIndex) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Thin Horizontal Divider under date selector
            HorizontalDivider(
                color = ColorDivider,
                thickness = 1.dp
            )

            // Content Area
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorAccentActive)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error ?: "Gagal memuat jadwal",
                                color = Color(0xFFEF5350),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchSchedule() },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorAccentActive)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Coba Lagi", color = Color.White)
                            }
                        }
                    }
                }

                uiState.currentDayAnimeList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada jadwal rilis untuk hari ${selectedDay?.fullName ?: ""}.",
                            color = ColorDateInactive,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 14.dp,
                            bottom = 90.dp // Extra bottom padding for floating buttons
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.currentDayAnimeList,
                            key = { it.anime.animeId ?: it.anime.title ?: it.hashCode().toString() }
                        ) { itemModel ->
                            ScheduleCardItem(
                                model = itemModel,
                                onClick = {
                                    itemModel.anime.animeId?.let { id ->
                                        onNavigateToDetail(id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Day Navigation Buttons at the bottom (Left: ← Senin, Right: Rabu →)
        if (prevDay != null && nextDay != null && !uiState.isLoading) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Floating Pill Button
                Surface(
                    shape = CircleShape,
                    color = ColorPillButton,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .border(width = 1.dp, color = ColorPillBorder, shape = CircleShape)
                        .singleClick(debounceTime = 400L) {
                            viewModel.selectPreviousDay()
                        }
                        .testTag("schedule_prev_day_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Day",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = prevDay.fullName,
                            color = ColorTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right Floating Pill Button
                Surface(
                    shape = CircleShape,
                    color = ColorPillButton,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .border(width = 1.dp, color = ColorPillBorder, shape = CircleShape)
                        .singleClick(debounceTime = 400L) {
                            viewModel.selectNextDay()
                        }
                        .testTag("schedule_next_day_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = nextDay.fullName,
                            color = ColorTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Day",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DaySelectorItem(
    day: DayInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(44.dp)
            .singleClick(debounceTime = 300L, onClick = onSelect)
            .testTag("day_selector_${day.shortName}")
    ) {
        // Day Name on Top
        Text(
            text = day.shortName,
            color = if (isSelected) ColorAccentActive else ColorDateInactive,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Date Number (In Blue Capsule if selected)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(ColorAccentActive),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.dayOfMonth}",
                    color = ColorTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Blue Dot Indicator
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ColorAccentActive)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.dayOfMonth}",
                    color = ColorDateInactive,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(9.dp)) // To match height of dot + space
        }
    }
}

@Composable
fun ScheduleCardItem(
    model: ScheduleItemUiModel,
    onClick: () -> Unit
) {
    val anime = model.anime
    val isAlreadyAired = model.isAlreadyAired
    val stripeColor = if (isAlreadyAired) ColorStatusAired else ColorStripeWaiting
    val statusColor = if (isAlreadyAired) ColorStatusAired else ColorStatusWaiting
    val statusText = if (isAlreadyAired) "Sudah Tayang" else "Menunggu Update Baru"

    val pseudoViews = kotlin.math.abs((anime.animeId ?: anime.title ?: "").hashCode() % 1000) / 10f + 12.5f
    val formattedViews = String.format(Locale.US, "%.1fK", pseudoViews)
    val scoreValue = anime.score?.takeIf { it.isNotBlank() } ?: "N/A"

    // Episode text display
    val episodeText = if (!anime.episodes.isNullOrBlank()) {
        "Episode ${anime.episodes}"
    } else {
        val pseudoEps = kotlin.math.abs((anime.animeId ?: anime.title ?: "").hashCode() % 12) + 1
        "Episode $pseudoEps"
    }

    Surface(
        shape = RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp
        ),
        color = ColorCardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp
                )
            )
            .singleClick(debounceTime = 600L, onClick = onClick)
            .testTag("schedule_card_${anime.animeId ?: anime.title}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Vertical Stripe Far-Left (Sharp 90-degree edge)
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripeColor)
            )

            // 2. Air Time Column
            Box(
                modifier = Modifier
                    .width(66.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = model.airTime,
                    color = ColorTextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // 3. Poster Anime (Full height with rounded corners)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF161719))
            ) {
                AsyncImage(
                    model = anime.poster,
                    contentDescription = anime.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 4. Right Information Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Judul Anime
                Text(
                    text = anime.title ?: "Anime",
                    color = ColorTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Episode
                Text(
                    text = episodeText,
                    color = ColorTextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )

                // Views + Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Views",
                            tint = ColorTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedViews,
                            color = ColorTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = ColorStatusAired,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = scoreValue,
                            color = ColorTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Status Dot + Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
