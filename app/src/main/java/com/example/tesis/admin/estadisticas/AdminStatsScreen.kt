package com.example.tesis.admin.estadisticas

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(
    navController: NavController,
    viewModel: AdminStatsViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Estadísticas Generales",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()) // ⭐ Cambio aquí
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE67E22))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp, // ⭐ Espacio adicional arriba
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PeriodSelector(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { viewModel.setPeriod(it) }
                        )
                    }

                    item {
                        PeriodActivityCard(stats, selectedPeriod)
                    }

                    item {
                        StatsHeaderCard(stats)
                    }

                    item {
                        UserStatsCard(stats)
                    }

                    item {
                        ActivityChartCard(stats)
                    }

                    item {
                        TopUsersCard(stats)
                    }

                    item {
                        DiaryStatsCard(stats)
                    }

                    item {
                        FoodStatsCard(stats)
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
        targetValue = if (isSelected) Color(0xFFE67E22) else Color.Transparent,
        label = "button_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Gray,
        label = "button_text"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .wrapContentHeight() // ✅ deja que crezca si necesita más espacio
            .defaultMinSize(minHeight = 56.dp), // ✅ mantiene altura mínima visual
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp) // ✅ padding más pequeño
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = period.emoji,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = period.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PeriodActivityCard(stats: StatsData, period: StatsPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = period.emoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Actividad - ${period.label}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActivityStat(
                    "👥",
                    stats.newUsersInPeriod.toString(),
                    "Nuevos",
                    Color(0xFF3498DB)
                )
                ActivityStat(
                    "📝",
                    stats.diaryEntriesInPeriod.toString(),
                    "Diarios",
                    Color(0xFF1ABC9C)
                )
                // ⭐ ELIMINADO: ya no mostrar comidas
                // ActivityStat("🍽️", stats.mealsInPeriod.toString(), "Comidas", Color(0xFFE67E22))
                ActivityStat(
                    "🎮",
                    stats.gamesPlayedInPeriod.toString(),
                    "Juegos",
                    Color(0xFF9B59B6)
                )
            }
        }
    }
}

@Composable
private fun ActivityStat(emoji: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun ActivityChartCard(stats: StatsData) {
    if (stats.activityByDay.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📈 Actividad de los últimos 7 días",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxValue = stats.activityByDay.values.maxOrNull() ?: 1

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.activityByDay.forEach { (day, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height((100 * (count.toFloat() / maxValue)).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(Color(0xFFE67E22))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopUsersCard(stats: StatsData) {
    if (stats.topUsers.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                    text = "Usuarios Más Activos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            stats.topUsers.forEachIndexed { index, user ->
                TopUserItem(
                    rank = index + 1,
                    user = user
                )
                if (index < stats.topUsers.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TopUserItem(rank: Int, user: TopUserData) {
    val rankEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rankEmoji,
            fontSize = 24.sp,
            modifier = Modifier.width(40.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C)
            )
            Row {
                Text(
                    text = "🍽️${user.mealsCount}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📝${user.diaryCount}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🎮${user.gamesCount}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        val totalActivity = user.mealsCount + user.diaryCount + user.gamesCount
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFFFD700).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = totalActivity.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE67E22)
            )
        }
    }
}

// ... (mantén las demás funciones: StatsHeaderCard, UserStatsCard, etc.)

@Composable
private fun StatsHeaderCard(stats: StatsData) {
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
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Resumen de Usuarios",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem("Total", stats.totalUsers.toString(), Color(0xFF3498DB))
                StatsItem("Activos", stats.activeUsers.toString(), Color(0xFF27AE60))
                StatsItem("Inactivos", stats.inactiveUsers.toString(), Color(0xFFE74C3C))
            }
        }
    }
}

@Composable
private fun UserStatsCard(stats: StatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Por Categoría",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem("👶 Niños", stats.childUsers.toString(), Color(0xFFFF8C7A))
                StatsItem("👨 Adultos", stats.adultUsers.toString(), Color(0xFF9B59B6))
            }
        }
    }
}

@Composable
private fun DiaryStatsCard(stats: StatsData) {
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
                imageVector = Icons.Default.DateRange,
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
            }
        }
    }
}

@Composable
private fun FoodStatsCard(stats: StatsData) {
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
                    imageVector = Icons.Default.AccountBox, // ⭐ Cambio de ícono
                    contentDescription = null,
                    tint = Color(0xFF1ABC9C), // ⭐ Cambio de color
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Entradas del Diario", // ⭐ Cambio de título
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatsItem(
                    "Total",
                    stats.diaryEntries.toString(), // ⭐ Usar diaryEntries
                    Color(0xFF1ABC9C)
                )
                StatsItem(
                    "Promedio",
                    String.format("%.1f",
                        if (stats.totalUsers > 0)
                            stats.diaryEntries.toDouble() / stats.totalUsers
                        else 0.0
                    ),
                    Color(0xFF16A085)
                )
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