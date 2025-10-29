package com.example.tesis.admin.estadisticas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.admin.ia.AIRecommendationLog
import com.example.tesis.ui.screens.achievements.AchievementData
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserStatsScreen(
    navController: NavController,
    userId: String,
    viewModel: UserStatsViewModel = viewModel()
) {
    val userStats by viewModel.userStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserStats(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Estadísticas del Usuario",
                        color = Color(0xFF8B5E3C),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF8B5E3C)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE67E22))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    UserHeaderCard(userStats)
                }

                // Resumen de juegos
                if (userStats.totalGamesPlayed > 0) {
                    item {
                        GamesOverviewCard(userStats)
                    }

                    item {
                        Text(
                            text = "🎮 Rendimiento por Juego",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                    }

                    items(userStats.gameStats.filter { it.gamesPlayed > 0 }) { gameStat ->
                        GameStatItemCard(gameStat)
                    }

                    item {
                        AchievementsCard(userStats.achievements)
                    }
                }

                // Diario
                item {
                    DiaryOverviewCard(userStats)
                }

                if (userStats.lastDiaryContent != null) {
                    item {
                        LastDiaryEntryCard(userStats)
                    }
                }

                // ⭐ NUEVO: Sección de IA
                if (userStats.totalAIInteractions > 0) {
                    item {
                        AIInteractionsCard(userStats)
                    }

                    item {
                        Text(
                            text = "🤖 Historial de Recomendaciones IA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                    }

                    items(userStats.aiLogs) { log ->
                        AILogCard(log)
                    }
                }

                item {
                    ActivityCard(userStats)
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// ⭐ NUEVO: Card de resumen de interacciones IA
@Composable
private fun AIInteractionsCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Interacciones con IA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem(
                    "Total",
                    stats.totalAIInteractions.toString(),
                    Color(0xFF9C27B0)
                )
                StatsItem(
                    "Última",
                    stats.lastAIInteraction.split(" ").firstOrNull() ?: "N/A",
                    Color(0xFF7B1FA2)
                )
            }
        }
    }
}

// ⭐ NUEVO: Card individual de log de IA
@Composable
private fun AILogCard(log: AIRecommendationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recomendación IA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5E3C)
                    )
                }

                Text(
                    text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        .format(log.timestamp.toDate()),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entrada del usuario
            Text(
                text = "📝 Lo que comió:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8F0)
                )
            ) {
                Text(
                    text = log.userInput,
                    fontSize = 13.sp,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Respuesta de la IA
            Text(
                text = "🤖 Recomendación:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Text(
                    text = log.aiResponse,
                    fontSize = 13.sp,
                    color = Color(0xFF4A148C),
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun UserHeaderCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stats.user?.name ?: "Usuario",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5E3C)
                    )
                    Text(
                        text = stats.user?.email ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = if (stats.user?.isChild() == true) "👶 Niño/a" else "👨 Adulto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.user?.isChild() == true) Color(0xFFFF8C7A) else Color(0xFF4CAF50)
                        )
                        Text(
                            text = " • ${stats.user?.age ?: 0} años",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // ⭐ NUEVO: Nivel del usuario
            if (stats.totalGamesPlayed > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getLevelEmoji(stats.level),
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nivel ${stats.level}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE67E22)
                        )
                        Text(
                            text = getLevelTitle(stats.level),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ⭐ NUEVO: Resumen de juegos
@Composable
private fun GamesOverviewCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF9B59B6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Resumen de Juegos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem("Total Partidas", stats.totalGamesPlayed.toString(), Color(0xFF9B59B6))
                StatsItem("Puntos Totales", stats.totalScore.toString(), Color(0xFFF39C12))
                StatsItem("Promedio", stats.averageScore.toString(), Color(0xFF3498DB))
            }
        }
    }
}

// ⭐ NUEVO: Card individual por juego
@Composable
private fun GameStatItemCard(gameStat: UserGameStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji del juego
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        getGameColor(gameStat.gameId).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = gameStat.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info del juego
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameStat.gameName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
                Text(
                    text = "${gameStat.gamesPlayed} partidas jugadas",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Estadísticas
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Mejor: ${gameStat.bestScore}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = "Promedio: ${gameStat.averageScore}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Precisión: ${gameStat.accuracy}%",
                    fontSize = 12.sp,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun DiaryOverviewCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = null,
                tint = Color(0xFF1ABC9C),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Entradas del Diario",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = stats.diaryEntries.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1ABC9C)
                )
                Row {
                    Text(
                        text = "Esta semana: ${stats.entriesThisWeek} • ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Este mes: ${stats.entriesThisMonth}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}


@Composable
private fun StatsItem(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LastDiaryEntryCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = Color(0xFFF39C12),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Última Entrada del Diario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Emoción
            if (stats.lastDiaryEmotion != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getEmotionEmoji(stats.lastDiaryEmotion),
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stats.lastDiaryEmotion.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = getEmotionColor(stats.lastDiaryEmotion)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Contenido
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = stats.lastDiaryContent ?: "",
                    fontSize = 14.sp,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stats.lastDiaryDate,
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun AchievementsCard(achievements: List<AchievementData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Logros",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B5E3C)
                    )
                }

                val unlockedCount = achievements.count { it.unlocked }
                Text(
                    text = "$unlockedCount/${achievements.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid de logros (3 columnas)
            achievements.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { achievement ->
                        AchievementBadge(
                            achievement = achievement,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Relleno si la fila tiene menos de 3
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
private fun AchievementBadge(
    achievement: AchievementData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = if (achievement.unlocked)
                        Color(0xFFFFD700).copy(alpha = 0.2f)
                    else
                        Color.Gray.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (achievement.unlocked)
                        Color(0xFFFFD700)
                    else
                        Color.Gray.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.emoji,
                fontSize = 28.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = if (achievement.unlocked) 1f else 0.3f
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = achievement.name,
            fontSize = 11.sp,
            fontWeight = if (achievement.unlocked) FontWeight.Bold else FontWeight.Normal,
            color = if (achievement.unlocked) Color(0xFF8B5E3C) else Color.Gray.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2
        )

        if (!achievement.unlocked) {
            Text(
                text = achievement.requirement,
                fontSize = 9.sp,
                color = Color.Gray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ActivityCard(stats: UserStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color(0xFF3498DB),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Actividad Reciente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.totalGamesPlayed > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎮 Última partida:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = stats.lastGameDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9B59B6)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📖 Última entrada:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = stats.lastDiaryDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1ABC9C)
                )
            }

            // ⭐ NUEVO: Última interacción IA
            if (stats.totalAIInteractions > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🤖 Última recomendación IA:",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = stats.lastAIInteraction,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

// Helper functions
private fun getEmotionEmoji(emotion: String): String = when (emotion.lowercase()) {
    "feliz", "happy" -> "😊"
    "triste", "sad" -> "😢"
    "enojado", "angry" -> "😠"
    "ansioso", "anxious" -> "😰"
    "calmado", "calm" -> "😌"
    "emocionado", "excited" -> "🤩"
    else -> "😐"
}

private fun getEmotionColor(emotion: String): Color = when (emotion.lowercase()) {
    "feliz", "happy" -> Color(0xFF4CAF50)
    "triste", "sad" -> Color(0xFF2196F3)
    "enojado", "angry" -> Color(0xFFF44336)
    "ansioso", "anxious" -> Color(0xFFFF9800)
    "calmado", "calm" -> Color(0xFF9C27B0)
    "emocionado", "excited" -> Color(0xFFFFD700)
    else -> Color.Gray
}


// Helper functions
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

private fun getGameColor(gameId: String): Color = when (gameId) {
    "drip_and_drop" -> Color(0xFFFF6B35)
    "memory_game" -> Color(0xFF4CAF50)
    "pregunton" -> Color(0xFF2196F3)
    "nutri_plate" -> Color(0xFF9C27B0)
    else -> Color.Gray
}
