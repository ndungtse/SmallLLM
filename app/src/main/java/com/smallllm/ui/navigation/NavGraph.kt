package com.smallllm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smallllm.ui.chat.ChatScreen
import com.smallllm.ui.detail.ModelDetailScreen
import com.smallllm.ui.gallery.GalleryScreen

/** Wires the three screens together. */
@Composable
fun SmallLlmNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.GALLERY) {
        composable(Destinations.GALLERY) {
            GalleryScreen(
                onModelClick = { name -> navController.navigate(Destinations.detail(name)) },
            )
        }
        composable(
            route = Destinations.DETAIL,
            arguments = listOf(navArgument(Destinations.MODEL_NAME_ARG) { type = NavType.StringType }),
        ) { entry ->
            val name = entry.arguments?.getString(Destinations.MODEL_NAME_ARG).orEmpty()
            ModelDetailScreen(
                modelName = name,
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Destinations.chat(it)) },
            )
        }
        composable(
            route = Destinations.CHAT,
            arguments = listOf(navArgument(Destinations.MODEL_NAME_ARG) { type = NavType.StringType }),
        ) { entry ->
            val name = entry.arguments?.getString(Destinations.MODEL_NAME_ARG).orEmpty()
            ChatScreen(
                modelName = name,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
