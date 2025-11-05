package com.example.tesis.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tesis.admin.config.AppConfigScreen
import com.example.tesis.admin.edit.AdminEditUserScreen
import com.example.tesis.admin.edit.AdminFoodHistoryScreen
import com.example.tesis.admin.estadisticas.AdminStatsScreen
import com.example.tesis.admin.estadisticas.UserStatsScreen
import com.example.tesis.admin.games.NutriPlateAdminScreen
import com.example.tesis.admin.gamesAdmin.DragDropAdminScreen
import com.example.tesis.admin.gamesAdmin.GamesManagementScreen
import com.example.tesis.admin.gamesAdmin.MemoryGameAdminScreen
import com.example.tesis.admin.gamesAdmin.PreguntonAdminScreen
import com.example.tesis.admin.home.AdminHomeScreen
import com.example.tesis.admin.ia.AIConfigScreen
import com.example.tesis.admin.reportes.AdminReportsScreen
import com.example.tesis.admin.user.AdminUserScreen
import com.example.tesis.ui.screens.achievements.AchievementsScreen
import com.example.tesis.ui.screens.auth.*
import com.example.tesis.ui.screens.drawer.EditProfileScreen
import com.example.tesis.ui.screens.drawer.UserSettingsScreen
import com.example.tesis.ui.screens.foodhistory.FoodHistoryScreen
import com.example.tesis.ui.screens.home.*
import com.example.tesis.ui.screens.games.*
import com.example.tesis.ui.screens.srpollo.MrPolloScreen
import com.example.tesis.ui.screens.stats.StatsScreen
import com.example.tesis.data.viewmodel.AuthViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel() // ⭐ UNA SOLA instancia

    val currentUser by authViewModel.currentUser.collectAsState()

    Log.d("AppNavigation", "🔐 Estado Auth: ${currentUser?.email ?: "NULL"}")

    LaunchedEffect(currentUser) {
        val user = currentUser
        val currentRoute = navController.currentDestination?.route

        Log.d("AppNavigation", "⚡ LaunchedEffect - Usuario: ${user?.email ?: "NULL"}, Ruta: $currentRoute")

        kotlinx.coroutines.delay(100)

        when {
            user == null && currentRoute !in listOf("login", "register", "forgot_password") -> {
                Log.d("AppNavigation", "🔴 No autenticado → Navegando a LOGIN")
                navController.navigate("login") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            user?.role == "admin" && !currentRoute.orEmpty().startsWith("admin") -> {
                Log.d("AppNavigation", "👑 Admin → Navegando a ADMIN_HOME")
                navController.navigate("admin_home") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            user != null && user.role != "admin" && currentRoute == "login" -> {
                Log.d("AppNavigation", "🔵 Usuario normal → Navegando a HOME")
                navController.navigate("home") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable("login") {
            // ⭐ Pasar el authViewModel compartido
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
        composable("forgot_password") {
            ForgotPasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("home") {
            // ⭐ Pasar el authViewModel compartido
            HomeScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("diary") {
            DiaryScreen(navController = navController)
        }
        composable("add_food/{selectedDate}") { backStackEntry ->
            val selectedDate = backStackEntry.arguments?.getString("selectedDate") ?: ""
            SelectionScreen(navController = navController, selectedDate = selectedDate)
        }
        composable("food_description/{selectedDate}/{selectedMoment}/{selectedSticker}") { backStackEntry ->
            val selectedDate = backStackEntry.arguments?.getString("selectedDate")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            val selectedMoment = backStackEntry.arguments?.getString("selectedMoment")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            val selectedSticker = backStackEntry.arguments?.getString("selectedSticker")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""
            WritingScreen(navController, selectedDate, selectedMoment, selectedSticker)
        }
        composable("games") {
            GamesScreen(navController = navController)
        }
        composable("game_drip_go") {
            DripGoGameScreen(navController = navController)
        }
        composable("pregunton") {
            PreguntonScreen(navController = navController)
        }
        composable("memory_game") {
            MemoryGameScreen(navController = navController)
        }
        composable("game_nutriswipe") {
            SwipeNutritionGame(navController = navController)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("mr_pollo") {
            MrPolloScreen(navController = navController)
        }
        composable("achievements") {
            AchievementsScreen(navController = navController)
        }
        composable("food_history") {
            FoodHistoryScreen(navController = navController)
        }
        composable("admin_home") {
            AdminHomeScreen(
                navController = navController,
                onLogout = { authViewModel.logout() }
            )
        }
        composable("admin_users") {
            AdminUserScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("admin_edit_user/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            AdminEditUserScreen(navController = navController, userId = userId)
        }
        composable("admin_food_history/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            AdminFoodHistoryScreen(navController = navController, userId = userId)
        }
        composable("admin_stats") {
            AdminStatsScreen(navController = navController)
        }
        composable("user_stats/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserStatsScreen(
                navController = navController,
                userId = userId
            )
        }
        composable("admin_ai_config") {
            AIConfigScreen(navController)
        }
        composable("admin_app_config") {
            AppConfigScreen(navController)
        }
        composable("admin_games") {
            GamesManagementScreen(navController)
        }
        composable("admin_game_dragdrop") {
            DragDropAdminScreen(navController)
        }
        composable("admin_game_memory") {
            MemoryGameAdminScreen(navController)
        }
        composable("admin_game_pregunton") {
            PreguntonAdminScreen(navController)
        }
        composable("admin_game_nutriplate") {
            NutriPlateAdminScreen(navController)
        }
        composable("user_settings") {
            UserSettingsScreen(navController)
        }
        composable(
            route = "edit_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            EditProfileScreen(
                navController = navController,
                userId = userId,
                onProfileUpdated = {
                    // ⭐ Callback que se ejecuta después de guardar
                    // Esto fuerza la actualización del Drawer
                }
            )
        }
        composable("admin_reports") {
            AdminReportsScreen(navController = navController)
        }
        composable("change_password") {
            ChangePasswordScreen(
                navController = navController,
                authViewModel = authViewModel // o viewModel() si no usas Hilt
            )
        }
    }
}