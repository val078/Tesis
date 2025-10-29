// ui/components/NextAchievementWidget.kt
package com.example.tesis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tesis.data.model.PolloState // ✅ AGREGADO
import com.example.tesis.ui.screens.achievements.AchievementData
import com.example.tesis.ui.screens.achievements.NextAchievementInfo
import com.example.tesis.ui.screens.achievements.calculateAllAchievements
import com.example.tesis.ui.screens.stats.getGamesPlayedToday
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray

@Composable
fun NextAchievementWidget(
    allResults: List<GameResult>,
    polloState: PolloState,
    navController: NavController
) {
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

    val level = (totalScore / 500).coerceAtLeast(1)

    // Obtener todos los logros
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

    // Encontrar el próximo logro más cercano
    val nextAchievement = findNextAchievement(
        achievements = achievements,
        totalScore = totalScore,
        totalGames = totalGames,
        currentStreak = polloState.currentStreak,
        happinessLevel = polloState.happinessLevel,
        level = level,
        perfectGames = perfectGames
    )

    if (nextAchievement != null) {
        NextAchievementCard(
            achievement = nextAchievement.achievement,
            progress = nextAchievement.progress,
            progressText = nextAchievement.progressText,
            onClick = { navController.navigate("achievements") }
        )
    }
}

@Composable
private fun NextAchievementCard(
    achievement: AchievementData,
    progress: Float,
    progressText: String,
    onClick: () -> Unit
) {
    // Animación de escala al aparecer
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )

    // 🔥 OPCIÓN 1: Animación MUY sutil (casi imperceptible)
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.05f,  // Cambiado: empieza en 0.05 en vez de 0
        targetValue = 0.15f,   // Cambiado: termina en 0.15 en vez de 1
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,  // Más lento (era 2000)
                easing = FastOutSlowInEasing  // Transición más suave
            ),
            repeatMode = RepeatMode.Reverse  // 🔥 CLAVE: Reverse en vez de Restart
        ),
        label = "shimmer"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            // Fondo con gradiente sutil
            if (progress >= 0.8f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    achievement.color.copy(alpha = shimmer),  // Usa el shimmer suave
                                    achievement.color.copy(alpha = shimmer * 0.7f)
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ... resto del código sin cambios

                // Icono del logro
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = achievement.color.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = achievement.color.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = achievement.emoji,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información del logro
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 Próximo Logro",
                            fontSize = 11.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = achievement.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = progressText,
                        fontSize = 12.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de progreso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            achievement.color.copy(alpha = 0.8f),
                                            achievement.color
                                        )
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Porcentaje
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = achievement.color
                    )
                    Text(
                        text = "completo",
                        fontSize = 9.sp,
                        color = TextGray
                    )
                }
            }
        }
    }
}

// Encontrar el próximo logro más cercano a completar
private fun findNextAchievement(
    achievements: List<AchievementData>,
    totalScore: Int,
    totalGames: Int,
    currentStreak: Int,
    happinessLevel: Int,
    level: Int,
    perfectGames: Int
): NextAchievementInfo? {
    // Filtrar solo logros no desbloqueados
    val locked = achievements.filter { !it.unlocked }

    if (locked.isEmpty()) {
        // Si todos están desbloqueados, mostrar el último como referencia
        return null
    }

    // Calcular progreso para cada logro bloqueado
    val achievementsWithProgress = locked.mapNotNull { achievement ->
        val progressInfo = calculateAchievementProgress(
            achievement = achievement,
            totalScore = totalScore,
            totalGames = totalGames,
            currentStreak = currentStreak,
            happinessLevel = happinessLevel,
            level = level,
            perfectGames = perfectGames
        )

        if (progressInfo != null) {
            NextAchievementInfo(
                achievement = achievement,
                progress = progressInfo.first,
                progressText = progressInfo.second
            )
        } else {
            null
        }
    }

    // Devolver el logro con mayor progreso (más cercano a completarse)
    return achievementsWithProgress.maxByOrNull { it.progress }
}

// Calcular el progreso específico de un logro
private fun calculateAchievementProgress(
    achievement: AchievementData,
    totalScore: Int,
    totalGames: Int,
    currentStreak: Int,
    happinessLevel: Int,
    level: Int,
    perfectGames: Int
): Pair<Float, String>? {
    return when {
        // Logros de puntos
        achievement.name == "100 Puntos" -> {
            val progress = (totalScore.toFloat() / 100f).coerceIn(0f, 1f)
            Pair(progress, "$totalScore / 100 puntos")
        }
        achievement.name == "500 Puntos" -> {
            val progress = (totalScore.toFloat() / 500f).coerceIn(0f, 1f)
            Pair(progress, "$totalScore / 500 puntos")
        }
        achievement.name == "1000 Puntos" -> {
            val progress = (totalScore.toFloat() / 1000f).coerceIn(0f, 1f)
            Pair(progress, "$totalScore / 1000 puntos")
        }

        // Logros de juegos completados
        achievement.name == "Primera Victoria" -> {
            val progress = (totalGames.toFloat() / 1f).coerceIn(0f, 1f)
            Pair(progress, "$totalGames / 1 juego")
        }
        achievement.name == "Novato" -> {
            val progress = (totalGames.toFloat() / 5f).coerceIn(0f, 1f)
            Pair(progress, "$totalGames / 5 juegos")
        }
        achievement.name == "Explorador" -> {
            val progress = (totalGames.toFloat() / 10f).coerceIn(0f, 1f)
            Pair(progress, "$totalGames / 10 juegos")
        }

        // Logros de racha
        achievement.name == "Racha 3" -> {
            val progress = (currentStreak.toFloat() / 3f).coerceIn(0f, 1f)
            Pair(progress, "$currentStreak / 3 días")
        }
        achievement.name == "Racha 7" -> {
            val progress = (currentStreak.toFloat() / 7f).coerceIn(0f, 1f)
            Pair(progress, "$currentStreak / 7 días")
        }
        achievement.name == "Racha 14" -> {
            val progress = (currentStreak.toFloat() / 14f).coerceIn(0f, 1f)
            Pair(progress, "$currentStreak / 14 días")
        }
        achievement.name == "Imparable" -> {
            val progress = (currentStreak.toFloat() / 30f).coerceIn(0f, 1f)
            Pair(progress, "$currentStreak / 30 días")
        }

        // Logros de felicidad
        achievement.name == "Pollito Feliz" -> {
            val progress = (happinessLevel.toFloat() / 50f).coerceIn(0f, 1f)
            Pair(progress, "$happinessLevel / 50%")
        }
        achievement.name == "Pollito Súper Feliz" -> {
            val progress = (happinessLevel.toFloat() / 80f).coerceIn(0f, 1f)
            Pair(progress, "$happinessLevel / 80%")
        }
        achievement.name == "Amor Total" -> {
            val progress = (happinessLevel.toFloat() / 100f).coerceIn(0f, 1f)
            Pair(progress, "$happinessLevel / 100%")
        }

        // Logros de nivel
        achievement.name == "Nivel 2" -> {
            val progress = (level.toFloat() / 2f).coerceIn(0f, 1f)
            Pair(progress, "Nivel $level / 2")
        }
        achievement.name == "Nivel 3" -> {
            val progress = (level.toFloat() / 3f).coerceIn(0f, 1f)
            Pair(progress, "Nivel $level / 3")
        }
        achievement.name == "Nivel 5" -> {
            val progress = (level.toFloat() / 5f).coerceIn(0f, 1f)
            Pair(progress, "Nivel $level / 5")
        }
        achievement.name == "Nivel 7" -> {
            val progress = (level.toFloat() / 7f).coerceIn(0f, 1f)
            Pair(progress, "Nivel $level / 7")
        }
        achievement.name == "Leyenda" -> {
            val progress = (level.toFloat() / 10f).coerceIn(0f, 1f)
            Pair(progress, "Nivel $level / 10")
        }

        // Logros de juegos perfectos
        achievement.name == "Perfeccionista" -> {
            val progress = (perfectGames.toFloat() / 1f).coerceIn(0f, 1f)
            Pair(progress, "$perfectGames / 1 perfecto")
        }
        achievement.name == "Experto" -> {
            val progress = (perfectGames.toFloat() / 5f).coerceIn(0f, 1f)
            Pair(progress, "$perfectGames / 5 perfectos")
        }

        // Logros de consistencia
        achievement.name == "Dedicado" -> {
            Pair(0f, "Completa 4 juegos hoy")
        }

        else -> null
    }
}