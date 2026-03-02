package com.ovais.panorama

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
@Composable
fun NavGraph() {

    val navController = rememberNavController()

    NavHost(navController, startDestination = "capture") {

        composable("capture") {
            CaptureScreen { uri ->
                val encodedUri = Uri.encode(uri.toString())
                navController.navigate("viewer/$encodedUri")
            }
        }

        composable(
            "viewer/{uri}",
            arguments = listOf(navArgument("uri") {
                type = NavType.StringType
            })
        ) { backStackEntry ->

            val uriString = backStackEntry.arguments?.getString("uri")
            // 🔹 Decode back to Uri
            val uri = Uri.parse(Uri.decode(uriString))

            ViewerScreen(uri)
        }
    }
}