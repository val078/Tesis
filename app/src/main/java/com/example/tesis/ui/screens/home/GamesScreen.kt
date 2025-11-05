package com.example.tesis.ui.screens.home

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.viewmodel.GameProgressViewModel
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.screens.stats.getGamesPlayedToday
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.data.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

data class GameCard(
    val gameId: String, // ✅ Agregado para identificar el juego
    val title: String,
    val emoji: String,
    val description: String,
    val gradientColors: List<Color>,
    val textColor: Color = Color.White,
    val route: String
)

@Composable
fun GamesScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    val allResults by gameProgressViewModel.allGameResults.collectAsState()
    val gamesPlayedToday = remember(allResults) {
        getGamesPlayedToday(allResults)
    }

    var isLoading by remember { mutableStateOf(true) }

    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showLockedMessage by remember { mutableStateOf(false) }
    var lockedGameName by remember { mutableStateOf("") }

    /*LaunchedEffect(Unit) {
        authViewModel.getCurrentUser()
    }*/

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            isLoading = true
            gameProgressViewModel.loadAllGameResults()
            delay(800)
            isLoading = false
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
                        "settings" -> navController.navigate("user_settings")
                        "achievements" -> navController.navigate("achievements")
                        "food_history" -> navController.navigate("food_history")
                        "statistics" -> navController.navigate("statistics")
                        "help" -> navController.navigate("help")
                        "logout" -> {
                            Log.d("HomeScreen", "🚪 Iniciando logout...")
                            authViewModel.logout()
                            // ✅ El Flow automático se encarga de navegar
                        }
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
                    currentRoute = "games",
                    navController = navController
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = remember { SnackbarHostState() }
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
                                Color(0xFFFF9AA2).copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    GamesHeader(
                        onMenuClick = {
                            showDrawer = true
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        GamesBannerShimmer()
                    } else {
                        GamesBanner(
                            gamesPlayed = gamesPlayedToday.size,
                            totalGames = 4
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (isLoading) {
                        GamesGridShimmer()
                    } else {
                        GamesGrid(
                            navController = navController,
                            gamesPlayedToday = gamesPlayedToday,
                            onLockedGameClick = { gameName ->
                                lockedGameName = gameName
                                showLockedMessage = true
                                coroutineScope.launch {
                                    delay(3000)
                                    showLockedMessage = false
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        DailyReminderCardShimmer()
                    } else {
                        DailyReminderCard(gamesLeft = 4 - gamesPlayedToday.size)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                AnimatedVisibility(
                    visible = showLockedMessage,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                ) {
                    LockedGameMessage(gameName = lockedGameName)
                }
            }
        }
    }
}

@Composable
private fun LockedGameMessage(gameName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji pollito triste
            Text(
                text = "🔒",
                fontSize = 36.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¡Ya jugaste $gameName hoy!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vuelve mañana para jugar de nuevo 🌅",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun GamesHeader(onMenuClick: () -> Unit) {
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
            text = "Juegos",
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
private fun GamesBanner(
    gamesPlayed: Int,
    totalGames: Int
) {
    val progress = gamesPlayed.toFloat() / totalGames.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4D6)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF4D6),
                            Color(0xFFFFE9A8)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Juegos de hoy",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B4513)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$gamesPlayed de $totalGames completados",
                            fontSize = 15.sp,
                            color = Color(0xFF8B4513).copy(alpha = 0.8f)
                        )
                    }

                    // Emoji pollito
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(30.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (gamesPlayed == totalGames) "🎉" else "🐣",
                            fontSize = 36.sp
                        )
                    }
                }

                // Barra de progreso
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (gamesPlayed == totalGames) "¡Completado!" else "${totalGames - gamesPlayed} por jugar",
                            fontSize = 12.sp,
                            color = Color(0xFF8B4513).copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .shadow(2.dp, RoundedCornerShape(4.dp)),
                        color = PrimaryOrange,
                        trackColor = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GamesGrid(
    navController: NavController,
    gamesPlayedToday: Set<String>,
    onLockedGameClick: (String) -> Unit
) {
    val games = listOf(
        GameCard(
            gameId = "drip_and_drop",
            title = "Drag & Drop",
            emoji = "🍓",
            description = "Arrastra los alimentos a su lado correcto",
            gradientColors = listOf(Color(0xFFFF9AA2), Color(0xFFFFB6C1)),
            route = "game_drip_go"
        ),
        GameCard(
            gameId = "nutri_plate",
            title = "NutriChef",
            emoji = "🍎",
            description = "Completa un plato nutritivo",
            gradientColors = listOf(Color(0xFFFF8C42), Color(0xFFFFA500)),
            textColor = Color.White,
            route = "game_nutriswipe"
        ),
        GameCard(
            gameId = "pregunton",
            title = "Preguntón",
            emoji = "🥐",
            description = "Responde preguntas sobre alimentación saludable",
            gradientColors = listOf(Color(0xFFC17B5A), Color(0xFFD4A574)),
            route = "pregunton"
        ),
        GameCard(
            gameId = "memory_game",
            title = "Memoria",
            emoji = "🐥",
            description = "Encuentra pares de alimentos saludables",
            gradientColors = listOf(Color(0xFFA8E6CF), Color(0xFF88D3C5)),
            textColor = Color(0xFF5D4037),
            route = "memory_game"
        )
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fila 1: 2 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            games.take(2).forEach { game ->
                Box(modifier = Modifier.weight(1f)) {
                    val isLocked = game.gameId in gamesPlayedToday
                    GameCardItem(
                        game = game,
                        isLocked = isLocked,
                        onClick = {
                            if (isLocked) {
                                onLockedGameClick(game.title)
                            } else {
                                navController.navigate(game.route)
                            }
                        }
                    )
                }
            }
        }

        // Fila 2: 2 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            games.drop(2).take(2).forEach { game ->
                Box(modifier = Modifier.weight(1f)) {
                    val isLocked = game.gameId in gamesPlayedToday
                    GameCardItem(
                        game = game,
                        isLocked = isLocked,
                        onClick = {
                            if (isLocked) {
                                onLockedGameClick(game.title)
                            } else {
                                navController.navigate(game.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardItem(
    game: GameCard,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    // Animación del candado
    val infiniteTransition = rememberInfiniteTransition(label = "lock")
    val lockScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(
                elevation = if (isLocked) 4.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            )
            .clickable(enabled = !isLocked || true) { onClick() }, // Siempre clickeable para mostrar mensaje
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fondo con gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = game.gradientColors
                        )
                    )
                    .then(
                        if (isLocked) {
                            Modifier.alpha(0.4f) // Desaturar cuando está bloqueado
                        } else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Contenido superior
                    Column {
                        Text(
                            text = game.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = game.textColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = game.description,
                            fontSize = 13.sp,
                            color = game.textColor.copy(alpha = 0.9f),
                            lineHeight = 16.sp,
                            maxLines = 2
                        )
                    }

                    // Emoji inferior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = game.textColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = game.emoji,
                                fontSize = 28.sp,
                                color = game.textColor
                            )
                        }
                    }
                }

                // Efecto de brillo sutil
                if (!isLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // ✅ Overlay de bloqueo
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Candado animado
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(28.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = (32 * lockScale).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "¡Ya jugaste hoy!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyReminderCard(gamesLeft: Int) {
    val reminderText = when (gamesLeft) {
        0 -> "¡Felicidades! Completaste todos los juegos de hoy 🎉"
        1 -> "¡Solo falta 1 juego! ¡Tú puedes! 💪"
        else -> "¡Juega los $gamesLeft juegos restantes y mantén feliz a Señor Pollo! 🐥"
    }

    val backgroundColor = if (gamesLeft == 0) {
        Color(0xFFB2F5B2) // Verde claro para completado
    } else {
        Color(0xFFFFCDD2) // Rosa pastel
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (gamesLeft == 0) "🏆" else "❤️",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Texto de recordatorio
            Text(
                text = reminderText,
                fontSize = 16.sp,
                color = Color(0xFF5D4037),
                lineHeight = 24.sp,
                fontWeight = if (gamesLeft == 0) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
        }
    }
}

@Composable
private fun GamesBannerShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4D6)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

@Composable
private fun GamesGridShimmer() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fila 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                Box(modifier = Modifier.weight(1f)) {
                    GameCardShimmer()
                }
            }
        }

        // Fila 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                Box(modifier = Modifier.weight(1f)) {
                    GameCardShimmer()
                }
            }
        }
    }
}

@Composable
private fun GameCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffectsGame()
        )
    }
}

@Composable
private fun DailyReminderCardShimmer() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFCDD2)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffectsGame()
        )
    }
}

@Composable
fun Modifier.shimmerEffectsGame(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE0E0E0).copy(alpha = 0.6f),
                Color(0xFFF5F5F5).copy(alpha = 0.3f),
                Color(0xFFE0E0E0).copy(alpha = 0.6f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}