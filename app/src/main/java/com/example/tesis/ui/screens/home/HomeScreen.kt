package com.example.tesis.ui.screens.home

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.model.GameProgressViewModel
import com.example.tesis.data.model.MrPolloViewModel
import com.example.tesis.ui.components.NextAchievementWidget
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.example.tesis.data.model.AIState
import com.example.tesis.data.model.HomeViewModel // ✅ NUEVO IMPORT
import com.example.tesis.data.model.DiaryViewModel
import com.google.firebase.auth.FirebaseAuth // ✅ NUEVO IMPORT
import kotlinx.coroutines.delay

// Data classes para las cards
data class QuickActionCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val textColor: Color = Color.White,
    val route: String
)

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel // ⭐ Recibir en lugar de crear
) {

    val currentUser by authViewModel.currentUser.collectAsState()

    // ✅ Guard clause
    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val mrPolloViewModel: MrPolloViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val diaryViewModel: DiaryViewModel = viewModel()

    // ✅ CORREGIDO: Observar currentUser del Flow automático
    val isLoading by authViewModel.isLoading.collectAsState()
    val allResults by gameProgressViewModel.allGameResults.collectAsState()
    val polloState by mrPolloViewModel.polloState.collectAsState()
    val aiState by homeViewModel.aiRecommendation.collectAsState()
    val entrySaved by diaryViewModel.entrySavedEvent.collectAsState()

    val isDataLoading = currentUser == null

    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ✅ MEJORADO: El Flow se encarga automáticamente, no necesitas llamar nada
    LaunchedEffect(currentUser) {
        val user = currentUser // ✅ Capturar en variable local

        if (user != null) {
            Log.d("HomeScreen", "👤 Usuario disponible: ${user.email}, cargando datos...")
            gameProgressViewModel.loadAllGameResults()
            mrPolloViewModel.loadPolloState()

            // Cargar recomendación AI
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                Log.d("HomeScreen", "📱 Cargando recomendación inicial...")
                homeViewModel.loadAIRecommendation(userId)
            }
        }
    }

        // ✅ Observar cuando se guarda una entrada en el diario
    LaunchedEffect(entrySaved) {
        if (entrySaved) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                Log.d("HomeScreen", "🔄 Detectada nueva entrada, refrescando recomendación...")
                homeViewModel.refreshRecommendation(userId)
                diaryViewModel.resetEntrySavedEvent()
            }
        }
    }
    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

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
                            diaryViewModel.stopListening()
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
                    currentRoute = "home",
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
                                Color(0xFFFFF0E6),
                                Color(0xFFFFE4CC)
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

                    ImprovedHomeHeader(
                        userName = currentUser?.name ?: "Usuario",
                        onMenuClick = {
                            showDrawer = true
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        isLoading = isDataLoading
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✅ PASAMOS aiState al banner
                    ImprovedWelcomeBanner(
                        userName = currentUser?.name ?: "Usuario",
                        isLoading = isDataLoading,
                        aiState = aiState // ✅ Ahora sí se pasa correctamente
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isDataLoading && allResults.isNotEmpty()) {
                        NextAchievementWidget(
                            allResults = allResults,
                            polloState = polloState,
                            navController = navController
                        )
                    } else if (!isDataLoading) {
                        AchievementPlaceholder()
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Módulos Principales",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    MainModulesGrid(navController = navController)

                    Spacer(modifier = Modifier.height(28.dp))

                    QuickActionsGrid(navController = navController)

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ImprovedHomeHeader(
    userName: String,
    onMenuClick: () -> Unit,
    isLoading: Boolean = false
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

        if (isLoading) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(20.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .shimmerEffect()
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¡Bienvenido, $userName!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.End
                )
                Text(
                    text = "¿Listo para aprender y divertirte?",
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ImprovedWelcomeBanner(
    userName: String,
    isLoading: Boolean = false,
    aiState: AIState = AIState.Idle
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 270.dp) // ✅ Altura dinámica
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF0E0)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFF0E0),
                            Color(0xFFFFE4CC)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(20.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(14.dp)
                                .background(
                                    Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .shimmerEffect()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color.Gray.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(40.dp)
                            )
                            .shimmerEffect()
                    )
                }
            } else {
                // ✅ Contenido real con IA MEJORADO
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.Top // ✅ Cambio: de CenterVertically a Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(), // ✅ Ocupa toda la altura
                        verticalArrangement = Arrangement.spacedBy(8.dp) // ✅ Espaciado entre elementos
                    ) {
                        Text(
                            text = "¡Hola $userName!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B4513)
                        )

                        // 🤖 SECCIÓN DE IA MEJORADA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // ✅ Toma el espacio restante
                                .verticalScroll(rememberScrollState()) // ✅ Scroll si es muy largo
                        ) {
                            when (aiState) {
                                is AIState.Idle -> {
                                    Text(
                                        text = "Leyendo tu diario...",
                                        fontSize = 14.sp, // ✅ Más grande: de 13sp a 14sp
                                        color = Color(0xFF8B4513).copy(alpha = 0.8f),
                                        lineHeight = 18.sp // ✅ Más alto: de 16sp a 18sp
                                    )
                                }

                                is AIState.Loading -> {
                                    ThinkingAnimation()
                                }

                                is AIState.Success -> {
                                    Text(
                                        text = aiState.recommendation,
                                        fontSize = 14.sp, // ✅ Más grande: de 13sp a 14sp
                                        color = Color(0xFF8B4513).copy(alpha = 0.9f), // ✅ Más opaco
                                        lineHeight = 20.sp, // ✅ Más alto: de 16sp a 20sp
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                is AIState.Error -> {
                                    Text(
                                        text = "No pude cargar recomendaciones. 😅",
                                        fontSize = 14.sp, // ✅ Más grande
                                        color = Color(0xFF8B4513).copy(alpha = 0.8f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp)) // ✅ Espacio entre texto y emoji

                    // Emoji del pollito
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(40.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🐥",
                            fontSize = 44.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainModulesGrid(
    navController: NavController
) {
    val mainModules = listOf(
        QuickActionCard(
            title = "Diario",
            subtitle = "¡Anota tus momentos!",
            icon = Icons.Outlined.Book,
            gradientColors = listOf(Color(0xFFFF9AA2), Color(0xFFFFB6C1)),
            route = "diary"
        ),
        QuickActionCard(
            title = "Señor Pollo",
            subtitle = "¡Dale cariño!",
            icon = Icons.Outlined.Favorite,
            gradientColors = listOf(Color(0xFFC17B5A), Color(0xFFD4A574)),
            route = "mr_pollo"
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        mainModules.forEach { module ->
            Box(modifier = Modifier.weight(1f)) {
                ImprovedQuickActionCard(
                    card = module,
                    onClick = { navController.navigate(module.route) },
                    height = 160.dp
                )
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    navController: NavController
) {
    val quickActions = listOf(
        QuickActionCard(
            title = "Progreso",
            subtitle = "¡Mira tu avance!",
            icon = Icons.Outlined.BarChart,
            gradientColors = listOf(Color(0xFFFF8C42), Color(0xFFFFA500)),
            route = "stats"
        ),
        QuickActionCard(
            title = "Juegos",
            subtitle = "¡Ven a divertirte!",
            icon = Icons.Outlined.Games,
            gradientColors = listOf(Color(0xFFFFD93D), Color(0xFFFFE55C)),
            textColor = Color(0xFF8B4513),
            route = "games"
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        quickActions.forEach { action ->
            Box(modifier = Modifier.weight(1f)) {
                ImprovedQuickActionCard(
                    card = action,
                    onClick = { navController.navigate(action.route) },
                    height = 160.dp
                )
            }
        }
    }
}

@Composable
private fun ImprovedQuickActionCard(
    card: QuickActionCard,
    onClick: () -> Unit,
    height: Dp = 140.dp
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = card.gradientColors
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = card.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = card.textColor,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = card.subtitle,
                        fontSize = 13.sp,
                        color = card.textColor.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = card.textColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = card.title,
                            tint = card.textColor.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

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
}

@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
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

@Composable
private fun AchievementPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF5E6)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎯",
                    fontSize = 32.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¡Completa juegos para desbloquear logros!",
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp // ✅ agrega aire entre líneas
                )
            }
        }
    }
}

@Composable
fun ThinkingAnimation() {
    var dots by remember { mutableStateOf("") }
    val emojis = listOf("🤔", "💭", "🧠", "💡")
    var currentEmoji by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            // Animar los puntos
            dots = when (dots) {
                "" -> "."
                "." -> ".."
                ".." -> "..."
                else -> ""
            }
            delay(400)

            // Cambiar emoji cada 1.2 segundos
            if (dots == "") {
                currentEmoji = (currentEmoji + 1) % emojis.size
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = emojis[currentEmoji],
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = "Analizando tu alimentación$dots",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B4513)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Esto tomará unos segundos",
                fontSize = 12.sp,
                color = Color(0xFF8B4513).copy(alpha = 0.7f)
            )
        }
    }
}