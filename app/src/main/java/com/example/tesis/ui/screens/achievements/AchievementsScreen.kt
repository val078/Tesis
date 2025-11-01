package com.example.tesis.ui.screens.achievements

// ui/screens/achievements/AchievementsScreen.kt

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.model.GameProgressViewModel
import com.example.tesis.data.model.MrPolloViewModel
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.screens.stats.getGamesPlayedToday
import com.example.tesis.ui.theme.*
import com.example.tesis.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val mrPolloViewModel: MrPolloViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val allResults by gameProgressViewModel.allGameResults.collectAsState()
    val polloState by mrPolloViewModel.polloState.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Calcular estadísticas
    val totalScore = remember(allResults) {
        allResults.sumOf { it.score }
    }

    val totalGames = remember(allResults) {
        allResults.size
    }

    val gamesPlayedToday = remember(allResults) {
        getGamesPlayedToday(allResults).size
    }

    val perfectGames = remember(allResults) {
        allResults.count {
            it.totalQuestions > 0 && it.correctAnswers == it.totalQuestions
        }
    }

    val level = calculateLevel(totalScore)

    // Calcular logros
    val achievements = remember(totalScore, totalGames, gamesPlayedToday, perfectGames, polloState) {
        calculateAllAchievements(
            totalScore = totalScore,
            totalGames = totalGames,
            gamesPlayedToday = gamesPlayedToday,
            perfectGames = perfectGames,
            currentStreak = polloState.currentStreak,
            longestStreak = polloState.longestStreak,
            happinessLevel = polloState.happinessLevel,
            level = level
        )
    }

    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            gameProgressViewModel.loadAllGameResults()
            mrPolloViewModel.loadPolloState()
        }
    }

    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // ✅ SOLUCIÓN: ModalNavigationDrawer FUERA del Scaffold
    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        drawerContent = {
            DrawerMenu(
                currentUser = currentUser,
                onOptionSelected = { option ->
                    when (option) {
                        "profile" -> {
                            val userId = currentUser?.userId ?: ""
                            navController.navigate("edit_profile/$userId")
                        }
                        "settings" -> navController.navigate("settings")
                        "achievements" -> navController.navigate("achievements")
                        "food_history" -> navController.navigate("food_history")
                        "statistics" -> navController.navigate("statistics")
                        "help" -> navController.navigate("help")
                        "logout" -> {
                            authViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                    coroutineScope.launch {
                        drawerState.close()
                    }
                },
                onClose = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "achievements",
                    navController = navController
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF8F0),
                                Color(0xFFFFE4CC),
                                Color(0xFFFFD4A8).copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        AchievementsHeader(
                            onMenuClick = {
                                showDrawer = true
                                coroutineScope.launch { drawerState.open() }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Resumen de progreso
                    item {
                        AchievementsSummaryCard(
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            level = level
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Categorías de logros
                    item {
                        Text(
                            text = "🏆 Logros por Categoría",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Agrupar por categoría
                    val groupedAchievements = achievements.groupBy { it.category }

                    groupedAchievements.forEach { (category, categoryAchievements) ->
                        item {
                            CategorySection(
                                category = category,
                                achievements = categoryAchievements
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsHeader(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Menú",
                tint = DarkOrange,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Mis Logros",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun AchievementsSummaryCard(
    unlockedCount: Int,
    totalCount: Int,
    level: Int
) {
    val progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Fondo decorativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.15f),
                                Color(0xFFFF9800).copy(alpha = 0.15f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trofeo animado
                AnimatedTrophy(progress = progress)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "$unlockedCount de $totalCount Logros",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Nivel $level • ${getLevelTitle(level)}",
                    fontSize = 14.sp,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de progreso
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progreso Total",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color(0xFFFFD700),
                        trackColor = Color(0xFFFFD700).copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedTrophy(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophy_rotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (progress > 0.5f) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trophy_scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
    ) {
        Text(
            text = "🏆",
            fontSize = 64.sp
        )
    }
}

@Composable
private fun CategorySection(
    category: AchievementCategory,
    achievements: List<AchievementData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )
            }

            achievements.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { achievement ->
                        AchievementItem(achievement = achievement)
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AchievementItem(achievement: AchievementData) {
    var showDetails by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (achievement.unlocked) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "achievement_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(90.dp)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .background(
                    color = if (achievement.unlocked) achievement.color.copy(alpha = 0.2f)
                    else Color.Gray.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 3.dp,
                    color = if (achievement.unlocked) achievement.color
                    else Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 32.sp,
                modifier = Modifier.alpha(if (achievement.unlocked) 1f else 0.3f)
            )

            // Candado si está bloqueado
            if (!achievement.unlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🔒", fontSize = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = achievement.name,
            fontSize = 11.sp,
            fontWeight = if (achievement.unlocked) FontWeight.Bold else FontWeight.Normal,
            color = if (achievement.unlocked) ConchodeVino else TextGray.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )

        if (!achievement.unlocked) {
            Text(
                text = achievement.requirement,
                fontSize = 9.sp,
                color = TextGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 11.sp
            )
        }
    }
}

// Función para calcular todos los logros
fun calculateAllAchievements(
    totalScore: Int,
    totalGames: Int,
    gamesPlayedToday: Int,
    perfectGames: Int,
    currentStreak: Int,
    longestStreak: Int,
    happinessLevel: Int,
    level: Int
): List<AchievementData> {
    return listOf(
        // 🎮 CATEGORÍA: PRIMEROS PASOS
        AchievementData(
            "🎮", "Primera Victoria", "Completa tu primer juego",
            totalGames >= 1, Color(0xFF4CAF50), AchievementCategory.BEGINNER
        ),
        AchievementData(
            "🌟", "Novato", "Completa 5 juegos",
            totalGames >= 5, Color(0xFFFF9800), AchievementCategory.BEGINNER
        ),
        AchievementData(
            "🎯", "Explorador", "Completa 10 juegos",
            totalGames >= 10, Color(0xFF2196F3), AchievementCategory.BEGINNER
        ),

        // 🏆 CATEGORÍA: MAESTRÍA
        AchievementData(
            "⭐", "100 Puntos", "Alcanza 100 puntos totales",
            totalScore >= 100, Color(0xFFFFD700), AchievementCategory.MASTERY
        ),
        AchievementData(
            "💎", "500 Puntos", "Alcanza 500 puntos totales",
            totalScore >= 500, Color(0xFF00BCD4), AchievementCategory.MASTERY
        ),
        AchievementData(
            "👑", "1000 Puntos", "Alcanza 1000 puntos totales",
            totalScore >= 1000, Color(0xFF9C27B0), AchievementCategory.MASTERY
        ),
        AchievementData(
            "💯", "Perfeccionista", "Consigue un juego perfecto",
            perfectGames >= 1, Color(0xFF2196F3), AchievementCategory.MASTERY
        ),
        AchievementData(
            "🎖️", "Experto", "Consigue 5 juegos perfectos",
            perfectGames >= 5, Color(0xFFE91E63), AchievementCategory.MASTERY
        ),

        // 🔥 CATEGORÍA: RACHA
        AchievementData(
            "🔥", "Racha 3", "Alimenta a Señor Pollo 3 días seguidos",
            currentStreak >= 3, Color(0xFFFF5722), AchievementCategory.STREAK
        ),
        AchievementData(
            "⚡", "Racha 7", "Alimenta a Señor Pollo 7 días seguidos",
            currentStreak >= 7, Color(0xFFFF9800), AchievementCategory.STREAK
        ),
        AchievementData(
            "💥", "Racha 14", "Alimenta a Señor Pollo 14 días seguidos",
            currentStreak >= 14, Color(0xFFFFD700), AchievementCategory.STREAK
        ),
        AchievementData(
            "🌋", "Imparable", "Racha de 30 días",
            longestStreak >= 30, Color(0xFFE91E63), AchievementCategory.STREAK
        ),

        // 💖 CATEGORÍA: MR. POLLO
        AchievementData(
            "🐣", "Pollito Feliz", "Felicidad al 50%",
            happinessLevel >= 50, Color(0xFFFFEB3B), AchievementCategory.MRPOLLO
        ),
        AchievementData(
            "🐥", "Pollito Súper Feliz", "Felicidad al 80%",
            happinessLevel >= 80, Color(0xFF4CAF50), AchievementCategory.MRPOLLO
        ),
        AchievementData(
            "💕", "Amor Total", "Felicidad al 100%",
            happinessLevel >= 100, Color(0xFFE91E63), AchievementCategory.MRPOLLO
        ),

        // 📈 CATEGORÍA: NIVEL
        AchievementData(
            "🥉", "Nivel 2", "Alcanza el nivel 2",
            level >= 2, Color(0xFFCD7F32), AchievementCategory.LEVEL
        ),
        AchievementData(
            "🥈", "Nivel 3", "Alcanza el nivel 3",
            level >= 3, Color(0xFFC0C0C0), AchievementCategory.LEVEL
        ),
        AchievementData(
            "🥇", "Nivel 5", "Alcanza el nivel 5",
            level >= 5, Color(0xFFFFD700), AchievementCategory.LEVEL
        ),
        AchievementData(
            "🏆", "Nivel 7", "Alcanza el nivel 7",
            level >= 7, Color(0xFF9C27B0), AchievementCategory.LEVEL
        ),
        AchievementData(
            "👑", "Leyenda", "Alcanza el nivel 10",
            level >= 10, Color(0xFFFF1744), AchievementCategory.LEVEL
        ),

        // 📅 CATEGORÍA: CONSISTENCIA
        AchievementData(
            "📅", "Dedicado", "Completa 4 juegos en un día",
            gamesPlayedToday >= 4, Color(0xFF4CAF50), AchievementCategory.CONSISTENCY
        ),
        AchievementData(
            "⏰", "Madrugador", "Completa un juego antes de las 9 AM",
            false, Color(0xFFFF9800), AchievementCategory.CONSISTENCY // TODO: implementar
        ),
        AchievementData(
            "🌙", "Nocturno", "Completa un juego después de las 10 PM",
            false, Color(0xFF673AB7), AchievementCategory.CONSISTENCY // TODO: implementar
        )
    )
}

// Helper functions
private fun calculateLevel(score: Int): Int = (score / 500).coerceAtLeast(1)

private fun getLevelTitle(level: Int): String = when (level) {
    1 -> "Aprendiz"
    2, 3 -> "Explorador"
    4, 5 -> "Experto"
    6, 7 -> "Maestro"
    else -> "Leyenda"
}