package com.prstyadev.wibufy.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prstyadev.wibufy.ui.home.HomeScreen
import com.prstyadev.wibufy.ui.detail.DetailScreen
import com.prstyadev.wibufy.ui.search.SearchScreen
import com.prstyadev.wibufy.ui.bookmark.BookmarkScreen
import com.prstyadev.wibufy.ui.subscribed.SubscribedScreen
import com.prstyadev.wibufy.ui.player.VideoPlayerScreen
import com.prstyadev.wibufy.ui.schedule.ScheduleScreen
import com.prstyadev.wibufy.ui.components.ComingSoonScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
    
    val bottomBarRoutes = listOf("home", "schedule", "history", "subscribed", "profile")

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                NavigationBar(
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
                    
                    val selectedItemIndex = when (currentRoute) {
                        "home" -> 0
                        "schedule" -> 1
                        "history" -> 2
                        "subscribed" -> 3
                        "profile" -> 4
                        else -> 0
                    }

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
                            selected = selectedItemIndex == index,
                            onClick = { 
                                val route = when(index) {
                                    0 -> "home"
                                    1 -> "schedule"
                                    2 -> "history"
                                    3 -> "subscribed"
                                    4 -> "profile"
                                    else -> "home"
                                }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
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
        NavHost(
            navController = navController, 
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToDetail = { animeId ->
                        navController.navigate("detail/$animeId")
                    },
                    onNavigateToSearch = {
                        navController.navigate("search")
                    },
                    onNavigateToBookmark = {
                        // In HomeScreen, if they click bookmarks icon on top bar, they could go to subscribed.
                        navController.navigate("subscribed")
                    },
                    onNavigateToSchedule = {
                        navController.navigate("schedule") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable("schedule") {
                ScheduleScreen(
                    onNavigateToDetail = { animeId ->
                        navController.navigate("detail/$animeId")
                    }
                )
            }
            
            composable("history") {
                ComingSoonScreen(title = "History Coming Soon")
            }
            
            composable("subscribed") {
                SubscribedScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { animeId ->
                        navController.navigate("detail/$animeId")
                    }
                )
            }
            
            composable("profile") {
                ComingSoonScreen(title = "Profile Coming Soon")
            }
            
            composable(
                route = "detail/{animeId}",
                arguments = listOf(navArgument("animeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
                DetailScreen(
                    animeId = animeId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { episodeSlug ->
                        navController.navigate("player/$episodeSlug")
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
                route = "player/{episodeSlug}",
                arguments = listOf(navArgument("episodeSlug") { type = NavType.StringType })
            ) { backStackEntry ->
                val episodeSlug = backStackEntry.arguments?.getString("episodeSlug") ?: ""
                VideoPlayerScreen(
                    episodeSlug = episodeSlug,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
