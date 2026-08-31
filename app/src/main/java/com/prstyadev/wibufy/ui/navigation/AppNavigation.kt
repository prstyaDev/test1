package com.prstyadev.wibufy.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prstyadev.wibufy.ui.bookmark.BookmarkScreen
import com.prstyadev.wibufy.ui.components.ComingSoonScreen
import com.prstyadev.wibufy.ui.detail.DetailScreen
import com.prstyadev.wibufy.ui.history.HistoryScreen
import com.prstyadev.wibufy.ui.home.HomeScreen
import com.prstyadev.wibufy.ui.player.GlobalPlayerViewModel
import com.prstyadev.wibufy.ui.player.MiniPlayerBar
import com.prstyadev.wibufy.ui.player.VideoPlayerScreen
import com.prstyadev.wibufy.ui.schedule.ScheduleScreen
import com.prstyadev.wibufy.ui.search.SearchScreen
import com.prstyadev.wibufy.ui.subscribed.SubscribedScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    globalPlayerViewModel: GlobalPlayerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
    val playerUiState by globalPlayerViewModel.uiState.collectAsState()

    val pagerState = rememberPagerState { 5 }
    val coroutineScope = rememberCoroutineScope()

    val isMainScreen = currentRoute == "main" || currentRoute == null

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isMainScreen) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        containerColor = Color(0xFF161719),
                        contentColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        val navItems = listOf(
                            "Home" to Icons.Rounded.Home,
                            "Jadwal" to Icons.Rounded.CalendarToday,
                            "History" to Icons.Outlined.Schedule,
                            "Subscribed" to Icons.Filled.Subscriptions,
                            "Profile" to Icons.Rounded.AccountCircle
                        )

                        navItems.forEachIndexed { index, pair ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = pair.second,
                                        contentDescription = pair.first,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = { Text(pair.first, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                alwaysShowLabel = false,
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    unselectedIconColor = Color(0xFF8E8E93),
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color(0xFF8E8E93),
                                    indicatorColor = Color(0xFF494458)
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "main",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("main") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    onNavigateToDetail = { animeId ->
                                        navController.navigate("detail/$animeId")
                                    },
                                    onNavigateToSearch = {
                                        navController.navigate("search")
                                    },
                                    onNavigateToBookmark = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(3)
                                        }
                                    },
                                    onNavigateToSchedule = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                    onNavigateToHistory = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(2)
                                        }
                                    },
                                    onNavigateToPlayer = { episodeSlug, animeTitle, episodeName, posterUrl ->
                                        globalPlayerViewModel.playEpisode(episodeSlug, animeTitle, episodeName, posterUrl)
                                    }
                                )
                                1 -> ScheduleScreen(
                                    onNavigateToDetail = { animeId ->
                                        navController.navigate("detail/$animeId")
                                    }
                                )
                                2 -> HistoryScreen(
                                    onNavigateToDetail = { animeId ->
                                        navController.navigate("detail/$animeId")
                                    }
                                )
                                3 -> SubscribedScreen(
                                    onNavigateBack = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    },
                                    onNavigateToDetail = { animeId ->
                                        navController.navigate("detail/$animeId")
                                    }
                                )
                                4 -> ComingSoonScreen(title = "Profile Coming Soon")
                            }
                        }
                    }

                    composable(
                        route = "detail/{animeId}",
                        arguments = listOf(navArgument("animeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                        DetailScreen(
                            animeId = animeId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPlayer = { episodeSlug, animeTitle, episodeName, posterUrl ->
                                globalPlayerViewModel.playEpisode(episodeSlug, animeTitle, episodeName, posterUrl)
                            }
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { animeId ->
                                navController.navigate("detail/$animeId")
                            }
                        )
                    }

                    composable(
                        route = "player/{episodeSlug}?animeTitle={animeTitle}&episodeName={episodeName}&posterUrl={posterUrl}",
                        arguments = listOf(
                            navArgument("episodeSlug") { type = NavType.StringType },
                            navArgument("animeTitle") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("episodeName") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("posterUrl") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val episodeSlug = backStackEntry.arguments?.getString("episodeSlug") ?: ""
                        val rawAnimeTitle = backStackEntry.arguments?.getString("animeTitle")
                        val rawEpisodeName = backStackEntry.arguments?.getString("episodeName")
                        val rawPosterUrl = backStackEntry.arguments?.getString("posterUrl")

                        val animeTitle = rawAnimeTitle?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }?.takeIf { it.isNotEmpty() }
                        val episodeName = rawEpisodeName?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }?.takeIf { it.isNotEmpty() }
                        val posterUrl = rawPosterUrl?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }?.takeIf { it.isNotEmpty() }

                        // Start playing via GlobalPlayerViewModel and pop back so that the full overlay handles it
                        globalPlayerViewModel.playEpisode(episodeSlug, animeTitle, episodeName, posterUrl)
                        navController.popBackStack()
                    }
                }
            }
        }

        // Root MiniPlayerBar Overlay floating at the highest Z-index covering Bottom Navigation Bar uniformly across all pages
        AnimatedVisibility(
            visible = playerUiState.isActive && playerUiState.isMinimized,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            MiniPlayerBar(
                viewModel = globalPlayerViewModel,
                onExpand = { globalPlayerViewModel.expand() },
                onClose = { globalPlayerViewModel.stopAndClose() }
            )
        }

        // Full Screen Video Player Overlay when Active & Expanded
        AnimatedVisibility(
            visible = playerUiState.isActive && !playerUiState.isMinimized,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            VideoPlayerScreen(
                viewModel = globalPlayerViewModel,
                onMinimize = { globalPlayerViewModel.minimize() },
                onNavigateToEpisode = { nextSlug, nextTitle, nextEp, nextPoster ->
                    globalPlayerViewModel.playEpisode(nextSlug, nextTitle, nextEp, nextPoster)
                }
            )
        }
    }
}

