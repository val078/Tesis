// ui/screens/stats/StatsScreen.kt
package com.example.tesis.ui.screens.stats

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.data.model.GameResult
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.viewmodel.GameProgressViewModel
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private val ALL_GAMES = listOf(
    GameMetadata("drip_and_drop", "Arrastra y Suelta", "🎯", Color(0xFFFF6B35)),
    GameMetadata("memory_game", "Memoria", "🧠", Color(0xFF4CAF50)),
    GameMetadata("pregunton", "Preguntón", "❓", Color(0xFF2196F3)),
    GameMetadata("nutri_plate", "NutriChef", "📝", Color(0xFF9C27B0))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    val isLoading by gameProgressViewModel.isLoading.collectAsState()
    val allResults by gameProgressViewModel.allGameResults.collectAsState()
    val error by gameProgressViewModel.error.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf(StatsPeriod.ALL_TIME) }

    val filteredResults by remember(allResults, selectedPeriod) {
        derivedStateOf {
            filterResultsByPeriod(allResults, selectedPeriod)
        }
    }

    val gameStats by remember(filteredResults) {
        derivedStateOf {
            computeGameStats(filteredResults)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            gameProgressViewModel.loadAllGameResults()
        }
    }

    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F0),
            Color(0xFFFFE4CC),
            Color(0xFFFF9AA2).copy(alpha = 0.2f)
        )
    )

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
        // ✅ Scaffold DENTRO del drawer
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "statistics",
                    navController = navController
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(backgroundGradient)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    StatsHeader(
                        onMenuClick = {
                            showDrawer = true
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        LoadingState()
                    } else {
                        PeriodSelector(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { selectedPeriod = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        OverallSummaryCard(stats = gameStats)
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "🎮 Rendimiento por Juego",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        gameStats.forEach { stat ->
                            GameStatCard(stat = stat)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        StatsAchievementsSection(stats = gameStats)
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}


private fun filterResultsByPeriod(
    allResults: List<GameResult>,
    period: StatsPeriod
): List<GameResult> {
    if (period == StatsPeriod.ALL_TIME) return allResults

    val calendar = Calendar.getInstance()
    val nowMillis = calendar.timeInMillis

    return when (period) {
        StatsPeriod.TODAY -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDayMillis = calendar.timeInMillis

            allResults.filter { result ->
                val timestampMillis = result.date.toDate().time
                timestampMillis >= startOfDayMillis && timestampMillis <= nowMillis
            }
        }

        StatsPeriod.WEEK -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgoMillis = calendar.timeInMillis

            allResults.filter { result ->
                val timestampMillis = result.date.toDate().time
                timestampMillis >= weekAgoMillis
            }
        }

        StatsPeriod.ALL_TIME -> allResults
    }
}

fun isGamePlayedToday(gameId: String, allResults: List<GameResult>): Boolean {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDayMillis = calendar.timeInMillis
    val nowMillis = System.currentTimeMillis()

    return allResults.any { result ->
        val timestampMillis = result.date.toDate().time
        result.gameId == gameId &&
                timestampMillis >= startOfDayMillis &&
                timestampMillis <= nowMillis
    }
}

fun getGamesPlayedToday(allResults: List<GameResult>): Set<String> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfDayMillis = calendar.timeInMillis
    val nowMillis = System.currentTimeMillis()

    return allResults
        .filter {
            val timestampMillis = it.date.toDate().time
            timestampMillis >= startOfDayMillis && timestampMillis <= nowMillis
        }
        .map { it.gameId }
        .toSet()
}

@Composable
private fun StatsHeader(
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
            text = "Mi Progreso",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PrimaryOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando tus estadísticas...",
                fontSize = 14.sp,
                color = TextGray
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.values().forEach { period ->
                PeriodButton(
                    period = period,
                    isSelected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PeriodButton(
    period: StatsPeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryOrange else Color.Transparent,
        label = "button_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextGray,
        label = "button_text"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .wrapContentHeight()
            .defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = period.emoji,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = period.label,
                fontSize = 11.sp, // 🔥 un poquito más legible en pantallas reales
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OverallSummaryCard(stats: List<GameStat>) {
    val totalScore = stats.sumOf { it.bestScore }
    val totalGamesPlayed = stats.sumOf { it.gamesPlayed }
    val averageAccuracy = if (stats.isEmpty() || stats.all { it.gamesPlayed == 0 }) {
        0
    } else {
        stats.filter { it.gamesPlayed > 0 }
            .map { it.accuracy }
            .average()
            .roundToInt()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PrimaryOrange.copy(alpha = 0.1f),
                                Color(0xFFFFE4CC).copy(alpha = 0.3f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mi Progreso Total",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        emoji = "⭐",
                        value = totalScore.toString(),
                        label = "Puntos Totales",
                        color = Color(0xFFFFD700)
                    )

                    SummaryItem(
                        emoji = "🎮",
                        value = totalGamesPlayed.toString(),
                        label = "Partidas",
                        color = Color(0xFF4CAF50)
                    )

                    SummaryItem(
                        emoji = "🎯",
                        value = "$averageAccuracy%",
                        label = "Precisión",
                        color = Color(0xFF2196F3)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PlayerLevelBadge(totalScore = totalScore)
            }
        }
    }
}

@Composable
private fun SummaryItem(
    emoji: String,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlayerLevelBadge(totalScore: Int) {
    val level = calculateLevel(totalScore)
    val nextLevelScore = (level + 1) * 500
    val currentLevelScore = level * 500
    val progress = ((totalScore - currentLevelScore).toFloat() / (nextLevelScore - currentLevelScore).toFloat())
        .coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = getLevelEmoji(level),
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Nivel $level",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Text(
                        text = getLevelTitle(level),
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso al Nivel ${level + 1}",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                    Text(
                        text = "$totalScore / $nextLevelScore",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryOrange,
                    trackColor = PrimaryOrange.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun GameStatCard(stat: GameStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(stat.color.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stat.emoji, fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stat.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = "${stat.gamesPlayed} partidas jugadas",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                PerformanceBadge(stat.performance)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    label = "Mejor",
                    value = stat.bestScore.toString(),
                    icon = "🏆",
                    color = Color(0xFFFFD700)
                )

                MiniStat(
                    label = "Promedio",
                    value = stat.averageScore.toString(),
                    icon = "📊",
                    color = Color(0xFF4CAF50)
                )

                MiniStat(
                    label = "Precisión",
                    value = "${stat.accuracy}%",
                    icon = "🎯",
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { stat.accuracy / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = stat.color,
                trackColor = stat.color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun PerformanceBadge(performance: Performance) {
    val (emoji, text, color) = when (performance) {
        Performance.EXCELLENT -> Triple("🌟", "Excelente", Color(0xFFFFD700))
        Performance.GOOD -> Triple("⭐", "Muy Bien", Color(0xFF4CAF50))
        Performance.IMPROVING -> Triple("📈", "Mejorando", Color(0xFF2196F3))
        Performance.NEEDS_PRACTICE -> Triple("💪", "Práctica", Color(0xFFFF9800))
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    icon: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextGray
        )
    }
}

@Composable
private fun StatsAchievementsSection(stats: List<GameStat>) {
    val achievements = calculateStatsAchievements(stats)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🏅 Tus Logros",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(16.dp))

            achievements.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { achievement ->
                        StatsAchievementBadge(achievement)
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
private fun StatsAchievementBadge(achievement: StatsAchievement) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.unlocked) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "achievement_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = if (achievement.unlocked) achievement.color.copy(alpha = 0.2f)
                    else Color.Gray.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (achievement.unlocked) achievement.color else Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.emoji,
                fontSize = (24 * scale).sp,
                modifier = Modifier.graphicsLayer {
                    alpha = if (achievement.unlocked) 1f else 0.3f
                }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = achievement.name,
            fontSize = 10.sp,
            fontWeight = if (achievement.unlocked) FontWeight.Bold else FontWeight.Normal,
            color = if (achievement.unlocked) ConchodeVino else TextGray.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun calculateLevel(score: Int): Int = (score / 500).coerceAtLeast(1)

private fun getLevelEmoji(level: Int): String = when (level) {
    1 -> "🥉"
    2, 3 -> "🥈"
    4, 5 -> "🥇"
    6, 7 -> "💎"
    else -> "👑"
}

private fun getLevelTitle(level: Int): String = when (level) {
    1 -> "Aprendiz"
    2, 3 -> "Explorador"
    4, 5 -> "Experto"
    6, 7 -> "Maestro"
    else -> "Leyenda"
}

private fun calculateStatsAchievements(stats: List<GameStat>): List<StatsAchievement> {
    val totalScore = stats.sumOf { it.bestScore }
    val totalGames = stats.sumOf { it.gamesPlayed }
    val perfectGames = stats.count { it.accuracy == 100 }

    return listOf(
        StatsAchievement("🎮", "Primera Victoria", totalGames >= 1, Color(0xFF4CAF50)),
        StatsAchievement("🔥", "Racha 5", totalGames >= 5, Color(0xFFFF5722)),
        StatsAchievement("⭐", "100 Puntos", totalScore >= 100, Color(0xFFFFD700)),
        StatsAchievement("💯", "Perfecto", perfectGames >= 1, Color(0xFF2196F3)),
        StatsAchievement("🏆", "Campeón", totalScore >= 500, Color(0xFFFF9800)),
        StatsAchievement("🌟", "Super Estrella", totalGames >= 20, Color(0xFF9C27B0))
    )
}

private fun computeGameStats(results: List<GameResult>): List<GameStat> {
    val resultsByGame = results.groupBy { it.gameId }

    return ALL_GAMES.map { gameMeta ->
        val gameResults = resultsByGame[gameMeta.gameId] ?: emptyList()

        if (gameResults.isEmpty()) {
            GameStat(
                id = gameMeta.gameId,
                name = gameMeta.name,
                emoji = gameMeta.emoji,
                color = gameMeta.color,
                gamesPlayed = 0,
                bestScore = 0,
                averageScore = 0,
                accuracy = 0,
                performance = Performance.NEEDS_PRACTICE
            )
        } else {
            val gamesPlayed = gameResults.size
            val bestScore = gameResults.maxOf { it.score }
            val averageScore = (gameResults.sumOf { it.score }.toDouble() / gamesPlayed).toInt()
            val totalCorrect = gameResults.sumOf { it.correctAnswers }
            val totalQuestions = gameResults.sumOf { it.totalQuestions }
            val accuracy = if (totalQuestions > 0) {
                ((totalCorrect.toDouble() / totalQuestions) * 100).toInt()
            } else 0

            val performance = when {
                accuracy >= 90 -> Performance.EXCELLENT
                accuracy >= 75 -> Performance.GOOD
                accuracy >= 60 -> Performance.IMPROVING
                else -> Performance.NEEDS_PRACTICE
            }

            GameStat(
                id = gameMeta.gameId,
                name = gameMeta.name,
                emoji = gameMeta.emoji,
                color = gameMeta.color,
                gamesPlayed = gamesPlayed,
                bestScore = bestScore,
                averageScore = averageScore,
                accuracy = accuracy,
                performance = performance
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class GameMetadata(
    val gameId: String,
    val name: String,
    val emoji: String,
    val color: Color
)

data class GameStat(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
    val gamesPlayed: Int,
    val bestScore: Int,
    val averageScore: Int,
    val accuracy: Int,
    val performance: Performance
)

data class StatsAchievement(
    val emoji: String,
    val name: String,
    val unlocked: Boolean,
    val color: Color
)

enum class StatsPeriod(val label: String, val emoji: String) {
    TODAY("Hoy", "📅"),
    WEEK("Semana", "📆"),
    ALL_TIME("Todo", "🌟")
}

enum class Performance {
    EXCELLENT, GOOD, IMPROVING, NEEDS_PRACTICE
}