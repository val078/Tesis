// ui/screens/mrpollo/SrPolloScreen.kt
package com.example.tesis.ui.screens.srpollo

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.example.tesis.data.viewmodel.MrPolloViewModel
import com.example.tesis.data.viewmodel.NutritionArticle
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.screens.stats.getGamesPlayedToday
import com.example.tesis.ui.theme.*
import com.example.tesis.data.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MrPolloScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val mrPolloViewModel: MrPolloViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val allResults by gameProgressViewModel.allGameResults.collectAsState()
    val polloState by mrPolloViewModel.polloState.collectAsState()

    // Calcular juegos completados hoy
    val gamesPlayedToday = remember(allResults) {
        getGamesPlayedToday(allResults)
    }
    val gamesCompleted = gamesPlayedToday.size
    val canFeed = gamesCompleted >= 4 && !polloState.fedToday

    var showDrawer by remember { mutableStateOf(false) }
    var showArticle by remember { mutableStateOf(false) }
    var showFeedAnimation by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    /*LaunchedEffect(Unit) {
        authViewModel.getCurrentUser()
    }*/

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
        // ✅ Scaffold DENTRO del drawer
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "mr_pollo",
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
                                Color(0xFFFFD4A8).copy(alpha = 0.5f)
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

                    MrPolloHeader(
                        onMenuClick = {
                            showDrawer = true
                            coroutineScope.launch { drawerState.open() }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    StreakCard(
                        currentStreak = polloState.currentStreak,
                        longestStreak = polloState.longestStreak
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HappinessMeter(
                        happinessLevel = polloState.happinessLevel,
                        gamesCompleted = gamesCompleted
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    MrPolloCharacter(
                        isHappy = polloState.fedToday,
                        showFeedAnimation = showFeedAnimation,
                        happinessLevel = polloState.happinessLevel
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PolloStatusMessage(
                        gamesCompleted = gamesCompleted,
                        fedToday = polloState.fedToday
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    FeedButton(
                        enabled = canFeed,
                        gamesCompleted = gamesCompleted,
                        onClick = {
                            Log.d("MrPollo", "🔘 Botón clickeado - Felicidad ANTES: ${polloState.happinessLevel}")

                            showFeedAnimation = true
                            coroutineScope.launch {
                                delay(1500)
                                showFeedAnimation = false
                                Log.d("MrPollo", "🍽️ Llamando a feedPollo()...")
                                mrPolloViewModel.feedPollo()

                                delay(500)
                                Log.d("MrPollo", "📚 Felicidad DESPUÉS: ${polloState.happinessLevel}")
                                showArticle = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    DailyProgressCard(gamesCompleted = gamesCompleted)

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Modal de artículo educativo
    if (showArticle) {
        NutritionArticleDialog(
            article = polloState.todayArticle,
            onDismiss = { showArticle = false }
        )
    }
}

@Composable
private fun MrPolloHeader(onMenuClick: () -> Unit) {
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
            text = "Señor Pollo",
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
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Racha actual
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currentStreak días",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                Text(
                    text = "Racha actual",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            // Separador
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(80.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            // Mejor racha
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$longestStreak días",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
                Text(
                    text = "Mejor racha",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        }
    }
}

// 💖 COMPONENTE ACTUALIZADO con corazones flotantes alrededor
@Composable
private fun MrPolloCharacter(
    isHappy: Boolean,
    showFeedAnimation: Boolean,
    happinessLevel: Int
) {
    // Animación de rebote
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (!isHappy) 10f else 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (!isHappy) 1000 else 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Animación de escala al alimentar
    val scale by animateFloatAsState(
        targetValue = if (showFeedAnimation) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "feed_scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 💖 Corazones flotantes alrededor del pollo (si felicidad >= 80)
        if (happinessLevel >= 80) {
            repeat(6) { index ->
                FloatingHeart(index = index)
            }
        }

        Card(
            modifier = Modifier
                .size(280.dp)
                .shadow(12.dp, CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isHappy) Color(0xFFFFF9C4) else Color(0xFFFFE0B2)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = bounceOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHappy) "🐥" else "🐣",
                    fontSize = (140 * scale).sp
                )
            }
        }

        // Partículas de comida al alimentar
        if (showFeedAnimation) {
            repeat(6) { index ->
                FoodParticle(index = index)
            }
        }
    }
}

// 💖 NUEVO: Corazón flotante alrededor del pollo
@Composable
private fun FloatingHeart(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_heart_$index")

    // Movimiento vertical (arriba y abajo)
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000 + index * 300,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "heart_offset_y"
    )

    // Fade in/out
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000 + index * 300
                0f at 0
                1f at 300
                1f at 1700
                0f at 2000 + index * 300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heart_alpha"
    )

    // Posición circular alrededor del pollo
    val angle = (index * 60).toFloat()
    val radius = 160f
    val offsetX = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * radius

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .alpha(alpha)
    ) {
        Text(
            text = "❤️",
            fontSize = 20.sp
        )
    }
}

@Composable
private fun FoodParticle(index: Int) {
    val offsetY by rememberInfiniteTransition(label = "particle_$index").animateFloat(
        initialValue = 0f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_offset"
    )

    val angle = (index * 60).toFloat()
    val offsetX = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * 40

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
    ) {
        Text(
            text = listOf("🌾", "🌽", "🥕", "🥦", "🍎", "🥬")[index],
            fontSize = 24.sp
        )
    }
}

@Composable
private fun PolloStatusMessage(
    gamesCompleted: Int,
    fedToday: Boolean
) {
    val (emoji, title, subtitle, backgroundColor) = when {
        fedToday -> Quadruple(
            "💚",
            "¡Señor Pollo está feliz!",
            "Ya lo alimentaste hoy. ¡Vuelve mañana!",
            Color(0xFFE8F5E9)
        )
        gamesCompleted >= 4 -> Quadruple(
            "🍽️",
            "¡Es hora de comer!",
            "Completaste los 4 juegos. ¡Alimenta a Señor Pollo!",
            Color(0xFFFFF3E0)
        )
        else -> Quadruple(
            "😢",
            "Señor Pollo tiene hambre",
            "Completa los 4 juegos para poder alimentarlo",
            Color(0xFFFFEBEE)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 36.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = TextGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun FeedButton(
    enabled: Boolean,
    gamesCompleted: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) PrimaryOrange else Color.Gray.copy(alpha = 0.3f),
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🍽️",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (enabled) "Alimentar a Señor Pollo" else "Completa más juegos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else TextGray
                )
                if (gamesCompleted < 4) {
                    Text(
                        text = "$gamesCompleted/4 juegos completados",
                        fontSize = 12.sp,
                        color = if (enabled) Color.White.copy(alpha = 0.8f) else TextGray
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyProgressCard(gamesCompleted: Int) {
    val progress = gamesCompleted / 4f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progreso de hoy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )
                Text(
                    text = "$gamesCompleted/4",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = PrimaryOrange,
                trackColor = PrimaryOrange.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) { index ->
                    GameBadge(
                        completed = index < gamesCompleted,
                        gameEmoji = listOf("🎯", "📝", "❓", "🧠")[index]
                    )
                }
            }
        }
    }
}

@Composable
private fun GameBadge(completed: Boolean, gameEmoji: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (completed) Color(0xFFE8F5E9) else Color.Gray.copy(alpha = 0.1f),
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (completed) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (completed) gameEmoji else "🔒",
            fontSize = 24.sp
        )
    }
}

@Composable
private fun NutritionArticleDialog(
    article: NutritionArticle,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(text = "📚", fontSize = 48.sp) },
        title = {
            Text(
                text = article.title,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = article.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextGray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange
                )
            ) {
                Text("¡Entendido!")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// Helper data class
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Medidor de felicidad (SIN corazones flotantes aquí)
@Composable
private fun HappinessMeter(
    happinessLevel: Int,
    gamesCompleted: Int
) {
    LaunchedEffect(happinessLevel) {
        Log.d("MrPollo", "📊 HappinessMeter recibió: happinessLevel=$happinessLevel")
    }

    val animatedProgress by animateFloatAsState(
        targetValue = happinessLevel / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "happiness_progress"
    )

    val meterColor = when {
        happinessLevel >= 80 -> Color(0xFF4CAF50)
        happinessLevel >= 50 -> Color(0xFFFFEB3B)
        happinessLevel >= 25 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val emoji = when {
        happinessLevel >= 80 -> "😊"
        happinessLevel >= 50 -> "😐"
        happinessLevel >= 25 -> "😟"
        else -> "😢"
    }

    val message = when {
        happinessLevel == 100 -> "¡Súper feliz!"
        happinessLevel >= 80 -> "¡Muy contento!"
        happinessLevel >= 50 && gamesCompleted >= 4 -> "Listo para comer"
        happinessLevel >= 50 -> "Esperando..."
        happinessLevel >= 25 -> "Un poco triste"
        else -> "Muy triste..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = emoji,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Nivel de amor",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                Text(
                    text = "$happinessLevel%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = meterColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de progreso limpia (sin corazones)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    meterColor.copy(alpha = 0.8f),
                                    meterColor
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }

            if (gamesCompleted < 4 && happinessLevel < 100) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💡 Completa los ${4 - gamesCompleted} juegos restantes para alimentarlo",
                        fontSize = 11.sp,
                        color = TextGray.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}