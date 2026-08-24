package com.prstyadev.wibufy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prstyadev.wibufy.ui.home.HomeScreen
import com.prstyadev.wibufy.ui.detail.DetailScreen
import com.prstyadev.wibufy.ui.search.SearchScreen
import com.prstyadev.wibufy.ui.bookmark.BookmarkScreen
import com.prstyadev.wibufy.ui.player.VideoPlayerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { animeId ->
                    navController.navigate("detail/$animeId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToBookmark = {
                    navController.navigate("bookmark")
                }
            )
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
        
        composable("bookmark") {
            BookmarkScreen(
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
