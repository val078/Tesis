// ui/screens/game/NutriPlateGameScreen.kt
package com.example.tesis.ui.screens.games

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.viewmodel.AuthViewModel
import com.example.tesis.data.model.GameResult
import com.example.tesis.data.viewmodel.GameProgressViewModel
import com.example.tesis.utils.TutorialManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeNutritionGame(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // ⭐ NUEVO: Estados para cargar desde Firestore
    var isLoadingGame by remember { mutableStateOf(true) }
    var gameRounds by remember { mutableStateOf<List<GameRound>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Estados para el tutorial
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    var tutorialChecked by remember { mutableStateOf(false) }

    // Estados para la reflexión final
    var showReflection by remember { mutableStateOf(false) }
    var finalScore by remember { mutableIntStateOf(0) }
    var finalCorrectRounds by remember { mutableIntStateOf(0) }
    var finalTotalRounds by remember { mutableIntStateOf(0) }

    // Estado para la cuenta regresiva
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }

    // Estados del juego
    var score by remember { mutableIntStateOf(0) }
    var currentRoundIndex by remember { mutableIntStateOf(0) }
    var selectedItems by remember { mutableStateOf<List<PlateFood>>(emptyList()) }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackType by remember { mutableStateOf(PlateFeedbackType.CORRECT) }
    var feedbackMessage by remember { mutableStateOf("") }
    var gameStatus by remember { mutableStateOf(PlateGameStatus.PLAYING) }
    var totalCorrectRounds by remember { mutableIntStateOf(0) }

    // Estados para el quiz de porciones
    var showPortionQuiz by remember { mutableStateOf(false) }
    var selectedQuizAnswer by remember { mutableIntStateOf(-1) }
    var showQuizFeedback by remember { mutableStateOf(false) }
    val portionQuizzes = remember { getPortionQuizzes() }

    // Cargar rondas desde Firestore
    LaunchedEffect(Unit) {
        try {
            Log.d("NutriPlateGame", "🔄 Cargando rondas desde Firestore...")
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("games")
                .document("nutriPlate")
                .get()
                .await()

            if (doc.exists()) {
                val roundsList = doc.get("rounds") as? List<Map<String, Any>> ?: emptyList()

                gameRounds = roundsList.map { roundMap ->
                    val correctList = roundMap["correctItems"] as? List<Map<String, Any>> ?: emptyList()
                    val wrongList = roundMap["wrongItems"] as? List<Map<String, Any>> ?: emptyList()

                    val correctItems = correctList.map { item ->
                        PlateFood(
                            emoji = item["emoji"] as? String ?: "❓",
                            name = item["name"] as? String ?: "Desconocido",
                            isHealthy = item["isHealthy"] as? Boolean ?: true
                        )
                    }

                    val wrongItems = wrongList.map { item ->
                        PlateFood(
                            emoji = item["emoji"] as? String ?: "❓",
                            name = item["name"] as? String ?: "Desconocido",
                            isHealthy = item["isHealthy"] as? Boolean ?: false
                        )
                    }

                    GameRound(
                        question = roundMap["question"] as? String ?: "",
                        correctItems = correctItems,
                        allOptions = (correctItems + wrongItems).shuffled()
                    )
                }

                Log.d("NutriPlateGame", "✅ ${gameRounds.size} rondas cargadas desde Firestore")
            } else {
                loadError = "No se encontraron rondas configuradas"
                Log.e("NutriPlateGame", "❌ Documento no existe")
            }
        } catch (e: Exception) {
            loadError = "Error al cargar el juego: ${e.message}"
            Log.e("NutriPlateGame", "❌ Error al cargar desde Firestore", e)
        } finally {
            isLoadingGame = false
        }
    }

    val currentRound = gameRounds.getOrNull(currentRoundIndex)
    val isLastRound = currentRoundIndex == gameRounds.size - 1

    // VERIFICAR SI MOSTRAR TUTORIAL
    LaunchedEffect(isLoadingGame) {
        if (!isLoadingGame && loadError == null) {
            val hasSeen = TutorialManager.hasSeen("nutri_plate")
            if (!hasSeen) {
                showTutorial = true
                Log.d("NutriPlateGame", "✅ Mostrando tutorial (primera vez)")
            } else {
                showCountdown = true
                Log.d("NutriPlateGame", "✅ Tutorial ya visto, iniciando juego")
            }
            tutorialChecked = true
        }
    }

    // COUNTDOWN
    LaunchedEffect(showCountdown, showTutorial) {
        if (showCountdown && !showTutorial && gameRounds.isNotEmpty()) {
            countdownValue = 3
            gameStarted = false
            while (countdownValue > 0) {
                delay(1000L)
                countdownValue--
            }
            gameStarted = true
            showCountdown = false
        }
    }

    // Verificar respuesta - SOLO funciona si el juego ha comenzado
    fun verifyAnswer() {
        if (!gameStarted || currentRound == null) return

        val correctSet = currentRound.correctItems.toSet()
        val selectedSet = selectedItems.toSet()

        val extraWrong = selectedSet.filter { !it.isHealthy }
        val missingCorrect = correctSet.filter { it !in selectedSet }
        val correctlySelected = selectedSet.filter { it.isHealthy }

        var roundScore = 0

        // ✔️ +10 por alimento saludable
        val correctCount = selectedSet.count { it.isHealthy }
        roundScore += correctCount * 10

        // ✔️ -5 por chatarra
        roundScore -= extraWrong.size * 5

        // Detectar si es perfecto
        val isPerfect = selectedSet.size == correctSet.size &&
                missingCorrect.isEmpty() &&
                extraWrong.isEmpty()

        // 🔥 NUEVO: Generar mensaje detallado
        if (isPerfect) {
            roundScore += 50
            feedbackMessage = "¡Perfecto! +50 bonus\n✅ Plato completamente balanceado"
            feedbackType = PlateFeedbackType.CORRECT
            totalCorrectRounds++
        } else if (extraWrong.isEmpty() && correctCount > 0) {
            val feedbackParts = mutableListOf<String>()

            if (missingCorrect.isNotEmpty()) {
                val missingNames = missingCorrect.joinToString(", ") { it.name }
                feedbackParts.add("Te faltó: $missingNames")
            }

            if (correctlySelected.isNotEmpty()) {
                feedbackParts.add("✅ Bien: ${correctlySelected.joinToString(", ") { it.emoji }}")
            }

            feedbackMessage = if (feedbackParts.isNotEmpty()) {
                "¡Buen intento!\n${feedbackParts.joinToString("\n")}"
            } else {
                "¡Muy bien!"
            }
            feedbackType = PlateFeedbackType.CORRECT
            totalCorrectRounds++
        } else if (extraWrong.isNotEmpty()) {
            val wrongNames = extraWrong.joinToString(", ") { "${it.emoji} ${it.name}" }
            val correctNames = if (correctlySelected.isNotEmpty()) {
                "\n✅ Bien: ${correctlySelected.joinToString(", ") { it.emoji }}"
            } else ""

            feedbackMessage = "⚠️ Alimentos poco saludables:\n$wrongNames$correctNames\n\nIntenta elegir opciones más nutritivas"
            feedbackType = PlateFeedbackType.INCORRECT
        } else {
            feedbackMessage = "¡Intenta de nuevo!"
            feedbackType = PlateFeedbackType.INCORRECT
        }

        // Nunca bajar de 0 puntos
        score = maxOf(0, score + roundScore)

        showFeedback = true
    }

    // Avanzar ronda
    LaunchedEffect(showFeedback) {
        if (showFeedback && gameRounds.isNotEmpty()) {
            delay(4000L)
            showFeedback = false
            val currentQuiz = portionQuizzes.getOrNull(currentRoundIndex)
            if (currentQuiz != null && !isLastRound) {
                showPortionQuiz = true
            } else if (isLastRound) {
                // Si es la última ronda, mostrar reflexión
                val result = GameResult(
                    gameId = "nutri_plate",
                    score = score,
                    correctAnswers = totalCorrectRounds,
                    totalQuestions = gameRounds.size,
                    timeLeft = 0,
                    streak = totalCorrectRounds,
                    extraData = mapOf(
                        "totalRounds" to gameRounds.size,
                        "correctRounds" to totalCorrectRounds
                    )
                )
                gameProgressViewModel.saveGameResult("nutri_plate", result)
                finalScore = score
                finalCorrectRounds = totalCorrectRounds
                finalTotalRounds = gameRounds.size
                showReflection = true
            } else {
                currentRoundIndex++
                selectedItems = emptyList()
            }
        }
    }

    LaunchedEffect(showQuizFeedback) {
        if (showQuizFeedback) {
            delay(4000L) // 3 segundos para leer la explicación
            showQuizFeedback = false
            showPortionQuiz = false
            selectedQuizAnswer = -1
            currentRoundIndex++
            selectedItems = emptyList()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F0),
            Color(0xFFFFE4CC),
            Color(0xFFFF9AA2).copy(alpha = 0.3f)
        )
    )

    // Guardar resultado al finalizar el juego
    LaunchedEffect(gameStatus) {
        if (gameStatus == PlateGameStatus.FINISHED && gameStarted && currentUser != null && gameRounds.isNotEmpty()) {
            val result = GameResult(
                gameId = "nutri_plate",
                score = score,
                correctAnswers = totalCorrectRounds,
                totalQuestions = gameRounds.size,
                timeLeft = 0,
                streak = totalCorrectRounds,
                extraData = mapOf(
                    "roundsCompleted" to currentRoundIndex + 1,
                    "totalRounds" to gameRounds.size,
                    "gameStatus" to gameStatus.name
                )
            )
            gameProgressViewModel.saveGameResult("nutri_plate", result)
            Log.d("NutriPlateGame", "✅ Resultado guardado: $result")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NutriChef",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            if (isLoadingGame) {
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
                            text = "Cargando juego...",
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
                            Text(text = "❌", fontSize = 48.sp)
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
            // ⭐ Juego solo se muestra cuando cargó exitosamente
            else if (currentRound != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "⭐", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$score pts",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                }
                            }
                            Text(
                                text = "Ronda ${currentRoundIndex + 1}/${gameRounds.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ConchodeVino
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentRound.question,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ConchodeVino,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        val plateScale by animateFloatAsState(
                            targetValue = if (selectedItems.isNotEmpty()) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "PlateScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(180.dp * plateScale)
                                .shadow(12.dp, CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFF8F0),
                                            Color(0xFFF9F3E9)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .border(6.dp, PrimaryOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🍽️", fontSize = 60.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (selectedItems.isNotEmpty()) {
                                    Text(
                                        text = selectedItems.joinToString(" ") { it.emoji },
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "${selectedItems.size} de 3 alimentos",
                                    fontSize = 12.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Toca los alimentos para añadirlos al plato",
                            fontSize = 14.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.alpha(if (selectedItems.isEmpty()) 1f else 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(currentRound.allOptions) { food ->
                                SelectableFoodOption(
                                    food = food,
                                    isSelected = food in selectedItems,
                                    onClick = {
                                        if (!gameStarted) return@SelectableFoodOption
                                        if (food in selectedItems) {
                                            selectedItems = selectedItems - food
                                        } else if (selectedItems.size < 3) {
                                            selectedItems = selectedItems + food
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (!showFeedback) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (selectedItems.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { selectedItems = emptyList() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    Text("Limpiar", fontWeight = FontWeight.Medium)
                                }
                            }
                            Button(
                                onClick = { verifyAnswer() },
                                modifier = Modifier
                                    .weight(if (selectedItems.isNotEmpty()) 1f else 1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                enabled = selectedItems.isNotEmpty() && gameStarted
                            ) {
                                Text(
                                    "¡Mandar!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quiz de porciones
            if (showPortionQuiz) {
                val currentQuiz = portionQuizzes.getOrNull(currentRoundIndex)
                if (currentQuiz != null) {
                    PortionQuizModal(
                        quiz = currentQuiz,
                        selectedAnswer = selectedQuizAnswer,
                        showFeedback = showQuizFeedback,
                        onAnswerSelected = { index ->
                            selectedQuizAnswer = index
                        },
                        onConfirm = {
                            showQuizFeedback = true
                        }
                    )
                }
            }

            // TUTORIAL
            if (showTutorial && !isLoadingGame) {
                NutriPlateTutorialScreen(
                    step = tutorialStep,
                    onNext = {
                        if (tutorialStep < 3) {
                            tutorialStep++
                        } else {
                            coroutineScope.launch {
                                TutorialManager.markAsSeen("nutri_plate")
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

            // COUNTDOWN
            AnimatedVisibility(
                visible = showFeedback,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(32.dp)
                            .widthIn(max = 350.dp)
                            .heightIn(max = 500.dp), // 🔥 NUEVO: altura máxima
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp) // 🔥 Reducido para dar más espacio al texto
                        ) {
                            Text(
                                text = if (feedbackType == PlateFeedbackType.CORRECT) "✅" else "🤔",
                                fontSize = 56.sp // 🔥 Reducido un poco
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = feedbackMessage,
                                fontSize = 16.sp, // 🔥 Tamaño más legible
                                fontWeight = FontWeight.Medium,
                                color = if (feedbackType == PlateFeedbackType.CORRECT)
                                    Color(0xFF4CAF50) else Color(0xFFFF9800),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp // 🔥 Mejor espaciado entre líneas
                            )
                        }
                    }
                }
            }

            // Fin del juego
            if (gameStatus == PlateGameStatus.FINISHED) {
                GameFinishedModal(
                    score = score,
                    totalCorrectRounds = totalCorrectRounds,
                    totalRounds = gameRounds.size,
                    onRestart = {
                        score = 0
                        currentRoundIndex = 0
                        totalCorrectRounds = 0
                        selectedItems = emptyList()
                        gameStatus = PlateGameStatus.PLAYING
                        tutorialStep = 0
                        showTutorial = false
                        showCountdown = true
                        countdownValue = 3
                    },
                    onExit = { navController.popBackStack() }
                )
            }

            if (showReflection) {
                NutriPlateReflectionScreen(
                    score = finalScore,
                    correctRounds = finalCorrectRounds,
                    totalRounds = finalTotalRounds,
                    onExit = { navController.popBackStack() },
                    onRestart = {
                        score = 0
                        currentRoundIndex = 0
                        totalCorrectRounds = 0
                        selectedItems = emptyList()
                        gameStatus = PlateGameStatus.PLAYING
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

@Composable
fun NutriPlateTutorialScreen(
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
                Text(
                    text = "¿Cómo jugar?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                when (step) {
                    0 -> {
                        Text(
                            text = "¡Bienvenido a NutriChef!",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Aprende a armar platos nutritivos usando alimentos reales",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text("🍽️🥗", fontSize = 56.sp)
                    }
                    1 -> {
                        Text(
                            text = "Antes de jugar:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Tu plato debe tener 3 tipos de alimentos:",
                            fontSize = 15.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text("🍗 Constructores → Para crecer y tener músculos",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ConchodeVino)
                        Text("🥦 Reguladores → Frutas y verduras que te mantienen sano",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ConchodeVino)
                        Text("🍚 Energéticos → Te dan fuerza para jugar y estudiar",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ConchodeVino,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Solo puedes elegir 3 alimentos: uno de cada grupo.",
                            fontSize = 15.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text(
                            text = "¿Cómo elegir bien?",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text("🖐️ Palma = Proteína (constructores)", fontSize = 15.sp, color = TextGray)
                        Text("👊 Puño = Carbohidratos (energéticos)", fontSize = 15.sp, color = TextGray)
                        Text("🤲 Dos manos = Frutas y verduras (reguladores)",
                            fontSize = 15.sp, color = TextGray, modifier = Modifier.padding(bottom = 16.dp))

                        Text("Puntuación:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ConchodeVino)

                        Text("✔️ +10 puntos por alimento saludable", fontSize = 15.sp, color = Color(0xFF4CAF50))
                        Text("❌ -5 puntos por comida chatarra", fontSize = 15.sp, color = Color(0xFFF44336))
                        Text("🌟 +50 si armas un plato perfecto",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    }
                    3 -> {
                        Text(
                            text = "Ejemplos de platos correctos:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text("🍗 + 🥦 + 🍚 = ✔️ Plato equilibrado", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("🍎 + 🥕 + 🍞 = ✔️ Variado y nutritivo", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("🍟 + 🍫 + 🧁 = ❌ No es saludable", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp))

                        Text(
                            text = "¡Listo! Ahora arma tus platos y demuestra qué tan buen chef saludable eres 🍽️✨",
                            fontSize = 15.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
fun CountdownScreenPlate(countdownValue: Int) {
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
private fun SelectableFoodOption(
    food: PlateFood,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "FoodScale"
    )

    val backgroundColor = when {
        isSelected -> Color(0xFFE8F5E8)
        else -> Color.White
    }

    val borderColor = when {
        food.isHealthy -> Color(0xFFFF8251)
        else -> Color(0xFFFF8251)
    }

    Card(
        modifier = modifier
            .size(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 2.dp,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = food.emoji,
                fontSize = (36.sp.value * scale).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = food.name,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = if (food.isHealthy) Color(0xFF2A2727) else Color(0xFF2A2727),
                fontWeight = FontWeight.Medium,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun NutriPlateReflectionScreen(
    score: Int,
    correctRounds: Int,
    totalRounds: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
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
                    text = "🎉 ¡Excelente trabajo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aprendiste a armar platos nutritivos y balanceados",
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
                        containerColor = Color(0xFFFFF8F0)
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estadísticas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        emoji = "🎯",
                        label = "Rondas",
                        value = "$correctRounds/$totalRounds"
                    )
                    StatItem(
                        emoji = "⭐",
                        label = "Precisión",
                        value = "${if (totalRounds > 0) (correctRounds * 100 / totalRounds) else 0}%"
                    )
                    StatItem(
                        emoji = "🏆",
                        label = "Puntos",
                        value = score.toString()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje personalizado basado en el desempeño
                val performanceMessage = when {
                    correctRounds == totalRounds -> "🏆 ¡Maestro de la Nutrición! ¡Perfecto equilibrio!"
                    correctRounds >= totalRounds * 0.8 -> "⭐ ¡Excelente! Dominas la alimentación saludable."
                    correctRounds >= totalRounds * 0.6 -> "👍 ¡Muy bien! Vas por buen camino."
                    correctRounds >= totalRounds * 0.4 -> "🙂 ¡Buen esfuerzo! Practica más para mejorar."
                    else -> "📚 ¡No te rindas! Cada intento te enseña algo nuevo."
                }

                Text(
                    text = performanceMessage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Reflexión sobre la importancia de la nutrición
                Text(
                    text = "Recuerda que armar platos balanceados con alimentos saludables te da energía para jugar, estudiar y crecer fuerte. ¡Intenta aplicar lo aprendido en tus comidas diarias!",
                    fontSize = 15.sp,
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
private fun GameFinishedModal(
    score: Int,
    totalCorrectRounds: Int,
    totalRounds: Int,
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
                .padding(24.dp)
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF8F0),
                                Color.White
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¡Felicidades!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completaste las 4 rondas",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryOrange.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puntuación Final",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        Text(
                            text = "$score",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange
                        )
                        Text(
                            text = "puntos",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("🔄 Jugar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PortionQuizModal(
    quiz: PortionQuiz,
    selectedAnswer: Int,
    showFeedback: Boolean,
    onAnswerSelected: (Int) -> Unit,
    onConfirm: () -> Unit
) {
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
                // Título
                Text(
                    text = "¡Aprende las porciones!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pregunta
                Text(
                    text = quiz.question,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Opciones
                quiz.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val isCorrect = index == quiz.correctAnswerIndex
                    val showResult = showFeedback

                    val backgroundColor = when {
                        showResult && isCorrect -> Color(0xFFE8F5E9)
                        showResult && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        isSelected -> Color(0xFFFFF8F0)
                        else -> Color.White
                    }

                    val borderColor = when {
                        showResult && isCorrect -> Color(0xFF4CAF50)
                        showResult && isSelected && !isCorrect -> Color(0xFFF44336)
                        isSelected -> PrimaryOrange
                        else -> Color.LightGray
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !showFeedback) {
                                onAnswerSelected(index)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = BorderStroke(2.dp, borderColor),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 8.dp else 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.emoji,
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ConchodeVino
                            )

                            if (showResult && isCorrect) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "✅", fontSize = 24.sp)
                            }
                            if (showResult && isSelected && !isCorrect) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "❌", fontSize = 24.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Explicación (solo cuando se muestra feedback)
                if (showFeedback) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💡", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quiz.explanation,
                                fontSize = 14.sp,
                                color = Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Botón
                if (!showFeedback) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onConfirm,
                        enabled = selectedAnswer != -1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange
                        )
                    ) {
                        Text(
                            "Confirmar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// Data classes y enums para NutriPlate Game
data class GameRound(
    val question: String,
    val correctItems: List<PlateFood>,
    val allOptions: List<PlateFood>
)

data class PlateFood(
    val emoji: String,
    val name: String,
    val isHealthy: Boolean
) {
    // Override equals y hashCode para comparación correcta
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlateFood) return false
        return emoji == other.emoji && name == other.name
    }

    override fun hashCode(): Int {
        var result = emoji.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

enum class PlateGameStatus {
    PLAYING, FINISHED
}

enum class PlateFeedbackType {
    CORRECT, INCORRECT
}

// Data class para el quiz de porciones
data class PortionQuiz(
    val question: String,
    val options: List<PortionOption>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class PortionOption(
    val emoji: String,
    val text: String
)

// Preguntas del quiz basadas en el PDF
fun getPortionQuizzes(): List<PortionQuiz?> {
    return listOf(
        // Quiz después de Ronda 1 (Constructores - Proteínas)
        PortionQuiz(
            question = "¿Cómo se miden las proteínas (pollo, pescado, huevos)?",
            options = listOf(
                PortionOption("🖐️", "Palma de la mano"),
                PortionOption("✊", "Puño cerrado"),
                PortionOption("🤲", "Dos manos")
            ),
            correctAnswerIndex = 0, // Palma
            explanation = "Las proteínas se miden con la palma de tu mano. Así sabes cuánto pollo, pescado o carne comer."
        ),

        // Quiz después de Ronda 2 (Reguladores - Frutas y verduras)
        PortionQuiz(
            question = "¿Cómo se miden las frutas y verduras?",
            options = listOf(
                PortionOption("🖐️", "Palma de la mano"),
                PortionOption("✊", "Puño cerrado"),
                PortionOption("🤲", "Dos manos juntas")
            ),
            correctAnswerIndex = 2, // Dos manos
            explanation = "Las frutas y verduras se miden con dos manos juntas. ¡Debes comer muchas para estar sano!"
        ),

        // Quiz después de Ronda 3 (Energéticos - Carbohidratos)
        PortionQuiz(
            question = "¿Cómo se miden los carbohidratos (arroz, papa, pan)?",
            options = listOf(
                PortionOption("🖐️", "Palma de la mano"),
                PortionOption("✊", "Puño cerrado"),
                PortionOption("🤲", "Dos manos")
            ),
            correctAnswerIndex = 1, // Puño
            explanation = "Los carbohidratos se miden con tu puño cerrado. Así sabes cuánto arroz o papa comer."
        ),

        // null para Ronda 4 (no hay quiz)
        null
    )
}