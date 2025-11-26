// ui/screens/game/DripGoGameScreen.kt
package com.example.tesis.ui.screens.games

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tesis.R
import com.example.tesis.data.viewmodel.GameProgressViewModel
import com.example.tesis.data.viewmodel.AuthViewModel
import com.example.tesis.data.model.GameResult
import kotlin.math.roundToInt
import com.example.tesis.utils.TutorialManager
import androidx.compose.runtime.collectAsState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DripGoGameScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // ⭐ NUEVO: Estados para cargar alimentos desde Firestore
    var allFoodItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoadingFoods by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Estados para el tutorial
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    var tutorialChecked by remember { mutableStateOf(false) } // Nuevo estado

    // Estado para la cuenta regresiva
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }

    // Estados del juego
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(50) }
    var gameStatus by remember { mutableStateOf(GameStatus.PLAYING) }
    var currentFoodIndex by remember { mutableIntStateOf(0) }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackType by remember { mutableStateOf(FeedbackType.CORRECT) }
    var streak by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var correctAnswersCount by remember { mutableIntStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }

    // Estados para la reflexión final
    var showReflection by remember { mutableStateOf(false) }
    var finalScore by remember { mutableIntStateOf(0) }
    var finalCorrectAnswers by remember { mutableIntStateOf(0) }

    // Posiciones de las zonas de drop
    var healthyZonePosition by remember { mutableStateOf(Offset.Zero) }
    var healthyZoneSize by remember { mutableStateOf(Offset.Zero) }
    var junkZonePosition by remember { mutableStateOf(Offset.Zero) }
    var junkZoneSize by remember { mutableStateOf(Offset.Zero) }

    // ⭐ NUEVO: Cargar alimentos desde Firestore
    LaunchedEffect(Unit) {
        try {
            Log.d("DripGoGame", "🔄 Cargando alimentos desde Firestore...")

            val doc = FirebaseFirestore.getInstance()
                .collection("games")
                .document("dragAndDrop")
                .get()
                .await()

            if (doc.exists()) {
                val foodsList = doc.get("foods") as? List<Map<String, Any>> ?: emptyList()

                allFoodItems = foodsList.map { foodMap ->
                    FoodItem(
                        emoji = foodMap["emoji"] as? String ?: "❓",
                        type = when (foodMap["type"] as? String) {
                            "HEALTHY" -> FoodType.HEALTHY
                            "JUNK" -> FoodType.JUNK
                            else -> FoodType.HEALTHY
                        },
                        name = foodMap["name"] as? String ?: "Desconocido"
                    )
                }.shuffled() // Mezclar aleatoriamente

                Log.d("DripGoGame", "✅ ${allFoodItems.size} alimentos cargados")
            } else {
                Log.e("DripGoGame", "❌ Documento no existe")
                loadError = "No se encontraron alimentos configurados"
            }
        } catch (e: Exception) {
            Log.e("DripGoGame", "❌ Error al cargar alimentos", e)
            loadError = "Error al cargar el juego: ${e.message}"
        } finally {
            isLoadingFoods = false
        }

        // Verificar tutorial
        val hasSeen = TutorialManager.hasSeen("drip_and_drop")
        if (!hasSeen) {
            showTutorial = true
            Log.d("DripGoGame", "✅ Mostrando tutorial (primera vez)")
        } else {
            showCountdown = true
            Log.d("DripGoGame", "✅ Tutorial ya visto, iniciando juego")
        }
        tutorialChecked = true
    }

    val currentFood = if (currentFoodIndex < allFoodItems.size) {
        allFoodItems[currentFoodIndex]
    } else null

    // CORRECCIÓN 2: Countdown SOLO se ejecuta cuando se activa y tutorial está cerrado
    LaunchedEffect(showCountdown, showTutorial) {
        if (showCountdown && !showTutorial) {
            // Reiniciar estados
            timeLeft = 50
            countdownValue = 3
            gameStarted = false

            while (countdownValue > 0) {
                delay(1000L)
                countdownValue--
            }

            // Marcar que el juego ha comenzado
            gameStarted = true
            showCountdown = false
        }
    }


    // CORRECCIÓN 3: Timer del juego SOLO corre cuando gameStarted es true
    LaunchedEffect(key1 = timeLeft, key2 = gameStatus, key3 = gameStarted) {
        if (gameStatus == GameStatus.PLAYING && timeLeft > 0 && gameStarted && !showCountdown && !showTutorial) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft <= 0 || currentFood == null) {
            if (gameStarted && !showTutorial && !showCountdown) {
                gameStatus = GameStatus.FINISHED
            }
        }
    }

    // Lógica para manejar el resultado
    fun handleFoodDrop(droppedZone: FoodType) {
        if (currentFood != null && gameStatus == GameStatus.PLAYING && gameStarted) {
            val isCorrect = currentFood.type == droppedZone

            if (isCorrect) {
                val multiplier = 1 + (streak * 0.5f).toInt()
                score += 10 * multiplier
                streak++
                feedbackType = FeedbackType.CORRECT
                correctAnswersCount++
            } else {
                score = maxOf(0, score - 5)
                streak = 0
                feedbackType = FeedbackType.INCORRECT
            }

            totalAnswered++
            showFeedback = true
        }
    }

    // Función para detectar colisión
    fun checkCollision(itemPosition: Offset): FoodType? {
        if (!gameStarted) return null

        if (itemPosition.x >= healthyZonePosition.x &&
            itemPosition.x <= healthyZonePosition.x + healthyZoneSize.x &&
            itemPosition.y >= healthyZonePosition.y &&
            itemPosition.y <= healthyZonePosition.y + healthyZoneSize.y
        ) {
            return FoodType.HEALTHY
        }

        if (itemPosition.x >= junkZonePosition.x &&
            itemPosition.x <= junkZonePosition.x + junkZoneSize.x &&
            itemPosition.y >= junkZonePosition.y &&
            itemPosition.y <= junkZonePosition.y + junkZoneSize.y
        ) {
            return FoodType.JUNK
        }

        return null
    }

    // Auto-avanzar después del feedback
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(1200L)
            showFeedback = false
            currentFoodIndex++
            if (currentFoodIndex >= allFoodItems.size) {
                gameStatus = GameStatus.FINISHED
            }
        }
    }

    // Finalizar juego y mostrar reflexión
    LaunchedEffect(gameStatus) {
        if (gameStatus == GameStatus.FINISHED && gameStarted) {
            val current = authViewModel.currentUser.value
            if (current != null && current.userId.isNotEmpty()) {
                finalScore = score
                finalCorrectAnswers = correctAnswersCount
                showReflection = true

                try {
                    val result = GameResult(
                        gameId = "drip_and_drop",
                        score = score,
                        correctAnswers = correctAnswersCount,
                        totalQuestions = allFoodItems.size,
                        timeLeft = timeLeft,
                        streak = streak
                    )
                    gameProgressViewModel.saveGameResult("drip_and_drop", result)
                } catch (e: Exception) {
                    Log.e("DripGoGame", "EXCEPCIÓN al guardar resultado", e)
                }
            }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F0),
            Color(0xFFFFE4CC),
            Color(0xFFFF9AA2).copy(alpha = 0.3f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Drag & Drop",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(backgroundGradient)
        ) {
            // ⭐ NUEVO: Mostrar loading mientras carga
            if (isLoadingFoods) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Cargando alimentos...",
                            fontSize = 16.sp,
                            color = ConchodeVino
                        )
                    }
                }
            }
            // ⭐ NUEVO: Mostrar error si falla
            else if (loadError != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFCDD2)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error al cargar",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loadError ?: "Error desconocido",
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { navController.popBackStack() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryOrange
                                )
                            ) {
                                Text("Volver")
                            }
                        }
                    }
                }
            }
            // ⭐ El juego normal solo se muestra cuando cargó exitosamente
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header mejorado
                    EnhancedGameHeader(
                        score = score,
                        timeLeft = timeLeft,
                        streak = streak,
                        totalAnswered = totalAnswered,
                        totalQuestions = allFoodItems.size
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // Instrucciones animadas
                    AnimatedInstructions(currentFood != null && gameStarted)
                    Spacer(modifier = Modifier.height(24.dp))
                    // Área del juego con drag & drop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Zonas de destino
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                DropZone(
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            healthyZonePosition = coordinates.positionInRoot()
                                            healthyZoneSize = Offset(
                                                coordinates.size.width.toFloat(),
                                                coordinates.size.height.toFloat()
                                            )
                                        },
                                    zoneType = FoodType.HEALTHY,
                                    enabled = !showFeedback && currentFood != null && gameStarted
                                )
                                DropZone(
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coordinates ->
                                            junkZonePosition = coordinates.positionInRoot()
                                            junkZoneSize = Offset(
                                                coordinates.size.width.toFloat(),
                                                coordinates.size.height.toFloat()
                                            )
                                        },
                                    zoneType = FoodType.JUNK,
                                    enabled = !showFeedback && currentFood != null && gameStarted
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            // Alimento arrastrable
                            if (currentFood != null && !showFeedback && gameStarted) {
                                DraggableFoodItem(
                                    food = currentFood,
                                    onDrop = { position ->
                                        checkCollision(position)?.let { zone -> handleFoodDrop(zone) }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // Feedback visual
                AnimatedVisibility(
                    visible = showFeedback,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (feedbackType == FeedbackType.CORRECT)
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else
                                    Color(0xFFF44336).copy(alpha = 0.2f)
                            )
                    )
                }

                AnimatedVisibility(
                    visible = showFeedback,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        FeedbackContent(feedbackType = feedbackType)
                    }
                }

                // Tutorial
                if (showTutorial) {
                    TutorialScreen(
                        step = tutorialStep,
                        onNext = {
                            if (tutorialStep < 3) {
                                tutorialStep++
                            } else {
                                coroutineScope.launch {
                                    TutorialManager.markAsSeen("drip_and_drop")
                                }
                                showTutorial = false
                                tutorialStep = 0
                                showCountdown = true
                            }
                        },
                        onPrevious = {
                            if (tutorialStep > 0) tutorialStep--
                        }
                    )
                }

                // Cuenta regresiva
                AnimatedVisibility(
                    visible = showCountdown && !showTutorial,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    CountdownScreen(countdownValue = countdownValue)
                }

                // Modal de fin de juego
                if (showReflection) {
                    GameReflectionScreen(
                        score = finalScore,
                        correctAnswers = finalCorrectAnswers,
                        totalQuestions = allFoodItems.size,
                        onExit = { navController.popBackStack() },
                        onRestart = {
                            score = 0
                            timeLeft = 50
                            gameStatus = GameStatus.PLAYING
                            currentFoodIndex = 0
                            streak = 0
                            totalAnswered = 0
                            correctAnswersCount = 0
                            showFeedback = false
                            gameStarted = false
                            showReflection = false
                            tutorialStep = 0
                            showTutorial = false
                            showCountdown = true
                            countdownValue = 3
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialScreen(
    step: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Text(
                    text = "¿Cómo jugar?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Contenido del paso
                when (step) {
                    0 -> {
                        Text(
                            text = "¡Hola! Vamos a aprender sobre alimentos saludables y chatarra.",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Arrastra los alimentos a la zona correcta:",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ej), // Reemplaza con tu drawable
                            contentDescription = "Ejemplo de alimentos",
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                        )
                    }
                    1 -> {
                        Text(
                            text = "Alimentos saludables como:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🍎 🥦 🥕 🥛 🍊",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Van a la zona de SALUDABLE",
                            fontSize = 16.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    2 -> {
                        Text(
                            text = "Alimentos chatarra como:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🍔 🍕 🍩 🥤 🍟",
                            fontSize = 36.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Van a la zona de CHATARRA",
                            fontSize = 16.sp,
                            color = Color(0xFFD24033),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    3 -> {
                        Text(
                            text = "¡Listo para comenzar?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Tienes 50 segundos para clasificar todos los alimentos",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de navegación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (step > 0) Arrangement.SpaceBetween else Arrangement.End
                ) {
                    if (step > 0) {
                        Button(
                            onClick = onPrevious,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray
                            )
                        ) {
                            Text("Anterior")
                        }
                    }
                    Button(onClick = onNext) {
                        Text(if (step < 3) "Siguiente" else "¡Jugar!")
                    }
                }

                // Indicador de progreso
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (index == step) ConchodeVino else Color.LightGray)
                                .padding(2.dp)
                        )
                        if (index < 3) Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CountdownScreen(countdownValue: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countdownValue.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun GameReflectionScreen(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
    // Determinar el comentario basado en la puntuación
    val reflectionMessage = when {
        score == 100 -> "¡Increíble! Reconociste TODOS los alimentos correctamente. ¡Eres un NutriChef experto!"
        score in 70..99 -> "Recuerda que comer alimentos saludables te garantiza jugar, estudiar y crecer fuerte. ¡Incluye más frutas y verduras en tu día a día!"
        score in 40..69 -> "¡Lo hiciste bien! Solo fallaste algunas opciones. ¡Sigue así!"
        score in 10..39 -> "¡Buen trabajo! Aún puedes mejorar. Algunas comidas parecían saludables, pero no lo eran."
        else -> "Continúa esforzándote, tienes un gran espíritu competitivo. ¡Esta información es variada y creemos que con la práctica la dominarás!"
    }

    // Determinar emoji basado en la puntuación
    val emoji = when {
        score == 100 -> "🏆"
        score >= 70 -> "🎉"
        score >= 40 -> "👍"
        score >= 10 -> "💪"
        else -> "🌟"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$emoji ¡Buen trabajo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aprendiste mucho sobre alimentos saludables y chatarra",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Puntuación
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFDE6D5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puntuación",
                            fontSize = 16.sp,
                            color = TextGray
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estadísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        emoji = "✅",
                        label = "Correctas",
                        value = correctAnswers.toString()
                    )
                    StatItem(
                        emoji = "📊",
                        label = "Total",
                        value = totalQuestions.toString()
                    )
                    StatItem(
                        emoji = "🎯",
                        label = "Precisión",
                        value = "${(correctAnswers * 100 / totalQuestions.coerceAtLeast(1))}%"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reflexión dinámica basada en la puntuación
                Text(
                    text = reflectionMessage,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, ConchodeVino)
                    ) {
                        Text(
                            text = "Salir",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = ConchodeVino
                            )
                        )
                    }
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange
                        )
                    ) {
                        Text(
                            "Jugar de Nuevo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedGameHeader(
    score: Int,
    timeLeft: Int,
    streak: Int,
    totalAnswered: Int,
    totalQuestions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Puntuación
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PUNTOS",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }

                // Tiempo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIEMPO",
                        fontSize = 10.sp,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (timeLeft <= 5) Color(0xFFC04C4C) else Color(0xFFEEC7A3),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$timeLeft",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeft <= 5) Color.White else ConchodeVino
                        )
                    }
                }

                // Racha
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RACHA",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔥",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = streak.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB6441D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progreso
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$totalAnswered/$totalQuestions",
                        fontSize = 12.sp,
                        color = ConchodeVino,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { totalAnswered.toFloat() / totalQuestions.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryOrange,
                    trackColor = Color(0xFFEECFB8)
                )
            }
        }
    }
}

@Composable
private fun AnimatedInstructions(showInstructions: Boolean) {
    AnimatedVisibility(
        visible = showInstructions,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFEECFB8).copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "¡Arrastra el alimento a la zona correcta!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConchodeVino,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DropZone(
    modifier: Modifier = Modifier,
    zoneType: FoodType,
    enabled: Boolean
) {
    val backgroundColor = when (zoneType) {
        FoodType.HEALTHY -> Color(0xFFC2DEC2)
        FoodType.JUNK -> Color(0xFFE7BCC3)
    }

    val borderColor = when (zoneType) {
        FoodType.HEALTHY -> Color(0xFF47A24B)
        FoodType.JUNK -> Color(0xFFD04A42)
    }

    val title = when (zoneType) {
        FoodType.HEALTHY -> "Saludable"
        FoodType.JUNK -> "Chatarra"
    }

    val emoji = when (zoneType) {
        FoodType.HEALTHY -> "✅"
        FoodType.JUNK -> "❌"
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .background(backgroundColor, RoundedCornerShape(24.dp))
            .border(
                width = 4.dp,
                color = borderColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .border(3.dp, borderColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = borderColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DraggableFoodItem(
    food: FoodItem,
    onDrop: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var initialCenter by remember { mutableStateOf<Offset?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DraggableFoodScale"
    )

    val itemSizeDp = 140.dp
    val itemSizePx = with(LocalDensity.current) { itemSizeDp.toPx() }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        dragOffset.x.roundToInt(),
                        dragOffset.y.roundToInt()
                    )
                }
                .size(itemSizeDp * scale)
                .shadow(
                    elevation = if (isDragging) 24.dp else 8.dp,
                    shape = RoundedCornerShape(28.dp)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFEECFB8))
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 4.dp,
                    color = PrimaryOrange.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(28.dp)
                )
                .onGloballyPositioned { coordinates ->
                    // Solo calculamos el centro inicial una vez
                    if (initialCenter == null) {
                        val xPos = coordinates.positionInRoot().x + coordinates.size.width / 2f
                        val yPos = coordinates.positionInRoot().y + coordinates.size.height / 2f
                        initialCenter = Offset(xPos, yPos)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val center = initialCenter?.let { center ->
                                Offset(
                                    x = center.x + dragOffset.x,
                                    y = center.y + dragOffset.y
                                )
                            } ?: dragOffset // fallback seguro (poco probable)

                            onDrop(center)
                            dragOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = food.emoji, fontSize = 56.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = food.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
private fun EnhancedGameOverModal(
    score: Int,
    totalAnswered: Int,
    correctAnswers: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Juego Terminado!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estadísticas
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEECFB8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puntuación Final",
                            fontSize = 16.sp,
                            color = TextGray
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                emoji = "✅",
                                label = "Correctas",
                                value = correctAnswers.toString()
                            )
                            StatItem(
                                emoji = "📊",
                                label = "Total",
                                value = totalAnswered.toString()
                            )
                            StatItem(
                                emoji = "⭐",
                                label = "Precisión",
                                value = "${(correctAnswers * 100 / totalAnswered.coerceAtLeast(1))}%"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val performance = when {
                    score >= 150 -> "🏆 ¡Excelente!"
                    score >= 100 -> "⭐ ¡Muy Bien!"
                    score >= 50 -> "¡Bien Hecho!"
                    else -> "¡Sigue Practicando!"
                }

                Text(
                    text = performance,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, ConchodeVino)
                    ) {
                        Text(
                            text = "Salir",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = ConchodeVino
                            )
                        )
                    }
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange
                        )
                    ) {
                        Text(
                            "Jugar de Nuevo",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextGray
        )
    }
}

@Composable
private fun FeedbackContent(feedbackType: FeedbackType) {
    Card(
        modifier = Modifier
            .size(200.dp)
            .shadow(24.dp, CircleShape),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (feedbackType == FeedbackType.CORRECT) "✅" else "❌",
                    fontSize = 72.sp
                )
                Text(
                    text = if (feedbackType == FeedbackType.CORRECT) "¡Correcto!" else "¡Ups!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (feedbackType == FeedbackType.CORRECT)
                        Color(0xFF3D8F40)
                    else
                        Color(0xFFB03126)
                )
            }
        }
    }
}


// Data classes y enums
data class FoodItem(
    val emoji: String,
    val type: FoodType,
    val name: String
)

enum class FoodType {
    HEALTHY, JUNK
}

enum class GameStatus {
    PLAYING, FINISHED
}

enum class FeedbackType {
    CORRECT, INCORRECT
}