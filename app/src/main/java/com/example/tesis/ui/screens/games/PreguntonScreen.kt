// ui/screens/games/PreguntonScreen.kt
package com.example.tesis.ui.screens.games

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import com.example.tesis.utils.TutorialManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreguntonScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // ⭐ NUEVO: Estados para cargar desde Firestore
    var isLoadingGame by remember { mutableStateOf(true) }
    var questions by remember { mutableStateOf<List<TriviaQuestion>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Estados para el tutorial
    var showTutorial by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    var tutorialChecked by remember { mutableStateOf(false) }

    // Estado para la cuenta regresiva
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var gameStarted by remember { mutableStateOf(false) }

    // Estados para la reflexión final
    var showReflection by remember { mutableStateOf(false) }
    var finalScore by remember { mutableIntStateOf(0) }
    var finalCorrectAnswers by remember { mutableIntStateOf(0) }
    var finalTotalQuestions by remember { mutableIntStateOf(0) }
    var finalLives by remember { mutableIntStateOf(3) }

    // Estados del juego
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var gameStatus by remember { mutableStateOf(TriviaGameStatus.PLAYING) }
    var lives by remember { mutableIntStateOf(3) }
    var totalCorrectAnswers by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(15) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // ⭐ NUEVO: Cargar preguntas desde Firestore
    LaunchedEffect(Unit) {
        try {
            Log.d("PreguntonGame", "🔄 Cargando preguntas desde Firestore...")
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("games")
                .document("pregunton")
                .get()
                .await()

            if (doc.exists()) {
                val questionsList = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()

                questions = questionsList.map { qMap ->
                    val optionsList = qMap["options"] as? List<String> ?: listOf("", "", "", "")
                    TriviaQuestion(
                        question = qMap["question"] as? String ?: "",
                        options = optionsList,
                        correctAnswer = (qMap["correctAnswer"] as? Long)?.toInt() ?: 0,
                        difficulty = (qMap["difficulty"] as? Long)?.toInt() ?: 1,
                        points = (qMap["points"] as? Long)?.toInt() ?: 10
                    )
                }

                Log.d("PreguntonGame", "✅ ${questions.size} preguntas cargadas desde Firestore")
            } else {
                loadError = "No se encontraron preguntas configuradas"
                Log.e("PreguntonGame", "❌ Documento no existe")
            }
        } catch (e: Exception) {
            loadError = "Error al cargar el juego: ${e.message}"
            Log.e("PreguntonGame", "❌ Error al cargar desde Firestore", e)
        } finally {
            isLoadingGame = false
        }
    }

    val currentQuestion = if (currentQuestionIndex < questions.size) {
        questions[currentQuestionIndex]
    } else null

    // VERIFICAR SI MOSTRAR TUTORIAL
    LaunchedEffect(isLoadingGame) {
        if (!isLoadingGame && loadError == null) {
            val hasSeen = TutorialManager.hasSeen("pregunton")
            if (!hasSeen) {
                showTutorial = true
                Log.d("PreguntonGame", "✅ Mostrando tutorial (primera vez)")
            } else {
                showCountdown = true
                Log.d("PreguntonGame", "✅ Tutorial ya visto, iniciando juego")
            }
            tutorialChecked = true
        }
    }

    // COUNTDOWN
    LaunchedEffect(showCountdown, showTutorial) {
        if (showCountdown && !showTutorial && questions.isNotEmpty()) {
            countdownValue = 3
            gameStarted = false
            while (countdownValue > 0) {
                delay(1000L)
                countdownValue--
            }
            gameStarted = true
            showCountdown = false
            isTimerRunning = true
        }
    }

    // Reiniciar temporizador al cambiar de pregunta
    LaunchedEffect(currentQuestionIndex, gameStarted) {
        if (gameStatus == TriviaGameStatus.PLAYING && currentQuestion != null && gameStarted) {
            timeLeft = 15
            isTimerRunning = true
        }
    }

    // Lógica del temporizador - SOLO corre cuando gameStarted es true
    LaunchedEffect(isTimerRunning, gameStarted) {
        if (isTimerRunning && gameStatus == TriviaGameStatus.PLAYING && gameStarted && !showCountdown && !showTutorial) {
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft--
            }
            if (selectedAnswer == null) {
                lives--
                selectedAnswer = -1
                showFeedback = true
                isTimerRunning = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isTimerRunning = false
        }
    }

    // Manejar selección de respuesta
    fun handleAnswer(answerIndex: Int) {
        if (!gameStarted || showFeedback || selectedAnswer != null) return
        selectedAnswer = answerIndex
        showFeedback = true
        isTimerRunning = false
        if (answerIndex == currentQuestion?.correctAnswer) {
            score += currentQuestion.points
            totalCorrectAnswers++
        } else {
            lives--
            if (lives <= 0) {
                gameStatus = TriviaGameStatus.GAME_OVER
            }
        }
    }

    // Auto-avanzar después del feedback
    LaunchedEffect(showFeedback) {
        if (showFeedback) {
            delay(2000L)
            showFeedback = false
            selectedAnswer = null
            if (lives > 0) {
                currentQuestionIndex++
                if (currentQuestionIndex >= questions.size) {
                    gameStatus = TriviaGameStatus.COMPLETED
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

    // Guardar resultado al finalizar el juego
    LaunchedEffect(gameStatus) {
        if ((gameStatus == TriviaGameStatus.GAME_OVER || gameStatus == TriviaGameStatus.COMPLETED) &&
            gameStarted && currentUser != null && questions.isNotEmpty()) {
            val result = GameResult(
                gameId = "pregunton",
                score = score,
                correctAnswers = totalCorrectAnswers,
                totalQuestions = questions.size,
                timeLeft = timeLeft,
                streak = currentQuestionIndex,
                extraData = mapOf(
                    "lives" to lives,
                    "gameStatus" to gameStatus.name
                )
            )
            gameProgressViewModel.saveGameResult("pregunton", result)
            Log.d("PreguntonGame", "✅ Resultado guardado: $result")
            finalScore = score
            finalCorrectAnswers = totalCorrectAnswers
            finalTotalQuestions = questions.size
            finalLives = lives
            showReflection = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trivia Nutricional",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino
                    )
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
                            text = "Cargando preguntas...",
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
            else if (gameStatus == TriviaGameStatus.PLAYING && currentQuestion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    TriviaHeader(
                        score = score,
                        lives = lives,
                        currentQuestion = currentQuestionIndex + 1,
                        totalQuestions = questions.size,
                        difficulty = currentQuestion.difficulty,
                        timeLeft = timeLeft,
                        isTimerRunning = isTimerRunning
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ProgressIndicator(
                        current = currentQuestionIndex,
                        total = questions.size
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    QuestionCard(
                        question = currentQuestion.question,
                        difficulty = currentQuestion.difficulty,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentQuestion.options.forEachIndexed { index, option ->
                            AnswerOption(
                                text = option,
                                index = index,
                                isSelected = selectedAnswer == index,
                                isCorrect = index == currentQuestion.correctAnswer,
                                showFeedback = showFeedback,
                                onClick = { handleAnswer(index) }
                            )
                        }
                    }
                }
            }

            // TUTORIAL
            if (showTutorial && !isLoadingGame) {
                PreguntonTutorialScreen(
                    step = tutorialStep,
                    onNext = {
                        if (tutorialStep < 3) {
                            tutorialStep++
                        } else {
                            coroutineScope.launch {
                                TutorialManager.markAsSeen("pregunton")
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
                visible = showCountdown && !showTutorial && !isLoadingGame,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                CountdownScreenTrivia(countdownValue = countdownValue)
            }

            // Modales de fin de juego
            when (gameStatus) {
                TriviaGameStatus.GAME_OVER -> {
                    GameOverModal(
                        score = score,
                        totalQuestions = questions.size,
                        answeredQuestions = currentQuestionIndex,
                        totalCorrectAnswers = totalCorrectAnswers,
                        onRestart = {
                            currentQuestionIndex = 0
                            score = 0
                            lives = 3
                            totalCorrectAnswers = 0
                            selectedAnswer = null
                            showFeedback = false
                            gameStatus = TriviaGameStatus.PLAYING
                            tutorialStep = 0
                            showTutorial = false
                            showCountdown = true
                            countdownValue = 3
                        },
                        onExit = { navController.popBackStack() }
                    )
                }
                TriviaGameStatus.COMPLETED -> {
                    VictoryModal(
                        score = score,
                        totalQuestions = questions.size,
                        totalCorrectAnswers = totalCorrectAnswers,
                        onRestart = {
                            currentQuestionIndex = 0
                            score = 0
                            lives = 3
                            totalCorrectAnswers = 0
                            selectedAnswer = null
                            showFeedback = false
                            gameStatus = TriviaGameStatus.PLAYING
                            tutorialStep = 0
                            showTutorial = false
                            showCountdown = true
                            countdownValue = 3
                        },
                        onExit = { navController.popBackStack() }
                    )
                }
                else -> {}
            }

            if (showReflection) {
                TriviaReflectionScreen(
                    score = finalScore,
                    correctAnswers = finalCorrectAnswers,
                    totalQuestions = finalTotalQuestions,
                    lives = finalLives,
                    onExit = { navController.popBackStack() },
                    onRestart = {
                        currentQuestionIndex = 0
                        score = 0
                        lives = 3
                        totalCorrectAnswers = 0
                        selectedAnswer = null
                        showFeedback = false
                        gameStatus = TriviaGameStatus.PLAYING
                        timeLeft = 15
                        isTimerRunning = false
                        tutorialStep = 0
                        showTutorial = false
                        showCountdown = true
                        countdownValue = 3
                        showReflection = false
                    }
                )
            }
        }
    }
}

@Composable
fun PreguntonTutorialScreen(
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
                            text = "¡Bienvenido a la Trivia Nutricional!",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Responde preguntas sobre alimentación saludable",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🤔❓",
                            fontSize = 56.sp
                        )
                    }
                    1 -> {
                        Text(
                            text = "Antes de empezar…",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = "🍽️ Principios para comer bien:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "• Come 5 veces al día 🍏\n" +
                                    "• Varía tus comidas (de muchos colores) 🌈\n" +
                                    "• Sirve porciones adecuadas ✋\n" +
                                    "• Mastica despacio 🐢\n" +
                                    "• Come sin pantallas 📵",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Start,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Estos tips te ayudarán a responder mejor la trivia 🎯✨",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text(
                            text = "Mecánica del juego:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "• Tienes ❤️❤️❤️ 3 vidas\n• 15 segundos por pregunta ⏱️\n• Responde correctamente para sumar puntos 🌟\n• Si fallas, pierdes una vida ❌",
                            fontSize = 15.sp,
                            color = ConchodeVino,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 16.dp),
                            lineHeight = 22.sp
                        )
                    }
                    3 -> {
                        Text(
                            text = "Puntuación:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🟢 Fácil: 10 puntos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "🔵 Media: 20 puntos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "🟡 Difícil: 30 puntos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFC107),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "🔴 Experto: 40 puntos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                    4 -> {
                        Text(
                            text = "¡Listo para jugar!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "¡Responde correctamente y demuestra cuánto sabes sobre nutrición!",
                            fontSize = 15.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🏆✨",
                            fontSize = 48.sp
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
fun CountdownScreenTrivia(countdownValue: Int) {
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
private fun TriviaHeader(
    score: Int,
    lives: Int,
    currentQuestion: Int,
    totalQuestions: Int,
    difficulty: Int,
    timeLeft: Int,
    isTimerRunning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Puntuación
                Column(horizontalAlignment = Alignment.Start) {
                    Text(text = "PUNTOS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                score >= 100 -> "🥑"
                                score >= 50 -> "🍎"
                                else -> "🥕"
                            },
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(score.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    }
                }

                // Temporizador (centrado)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "TIEMPO", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (timeLeft <= 5 && isTimerRunning) Color(0xFFFF6B6B) else Color(0xFFFFE4CC),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$timeLeft",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (timeLeft <= 5 && isTimerRunning) Color.White else ConchodeVino
                        )
                    }
                }

                // Vidas
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "VIDAS", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.SemiBold)
                    Row {
                        repeat(3) { index ->
                            Text(
                                text = if (index < lives) "❤️" else "🖤",
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator(current: Int, total: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Pregunta ${current + 1} de $total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ConchodeVino
            )
            Text(
                text = "${((current.toFloat() / total) * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { current.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = PrimaryOrange,
            trackColor = Color(0xFFFFE4CC)
        )
    }
}

@Composable
private fun QuestionCard(
    question: String,
    difficulty: Int,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(20.dp)), // menos sombra
        shape = RoundedCornerShape(20.dp), // menos radio
        colors = CardDefaults.cardColors(
            containerColor = when (difficulty) {
                1 -> Color(0xFFE8F5E8)
                2 -> Color(0xFFE3F2FD)
                3 -> Color(0xFFFFF9C4)
                else -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp) // padding reducido
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "🤔",
                    fontSize = 36.sp // más pequeño
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question,
                    fontSize = 17.sp, // más pequeño
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp // menos espacio entre líneas
                )
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    showFeedback: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            showFeedback && isSelected && isCorrect -> Color(0xFF4CAF50)
            showFeedback && isSelected && !isCorrect -> Color(0xFFF44336)
            showFeedback && !isSelected && isCorrect -> Color(0xFF4CAF50)
            isSelected -> Color(0xFFFFE4CC)
            else -> Color.White
        },
        animationSpec = tween(durationMillis = 300), label = ""
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            showFeedback && isSelected && isCorrect -> Color(0xFF2E7D32)
            showFeedback && isSelected && !isCorrect -> Color(0xFFC62828)
            showFeedback && !isSelected && isCorrect -> Color(0xFF2E7D32)
            else -> PrimaryOrange.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 300), label = ""
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected && showFeedback) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = ""
    )

    val shadowElevation by animateDpAsState(
        targetValue = if (showFeedback && isCorrect) 16.dp else 4.dp,
        animationSpec = tween(durationMillis = 300), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(shadowElevation, RoundedCornerShape(20.dp))
            .clickable(enabled = !showFeedback) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 2.dp, // menos grosor
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp) // menos radio
                )
                .padding(horizontal = 16.dp, vertical = 12.dp) // padding reducido
        ){
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de letra
                Box(
                    modifier = Modifier
                        .size(32.dp) // era 40.dp
                        .background(
                            color = when {
                                showFeedback && isCorrect -> Color.White.copy(alpha = 0.3f)
                                else -> PrimaryOrange.copy(alpha = 0.2f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        fontSize = 16.sp, // era 18.sp
                        fontWeight = FontWeight.Bold,
                        color = when {
                            showFeedback && isCorrect -> Color.White
                            showFeedback && isSelected -> Color.White
                            else -> ConchodeVino
                        }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        showFeedback && (isCorrect || isSelected) -> Color.White
                        else -> ConchodeVino
                    },
                    modifier = Modifier.weight(1f)
                )

                // Icono de feedback
                AnimatedVisibility(
                    visible = showFeedback && (isSelected || isCorrect),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Text(
                        text = if (isCorrect) "✅" else if (isSelected) "❌" else "",
                        fontSize = 28.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TriviaReflectionScreen(
    score: Int,
    correctAnswers: Int,
    totalQuestions: Int,
    lives: Int,
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
                    text = "🎉 ¡Buen trabajo!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aprendiste mucho sobre nutrición y alimentación saludable",
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
                        value = "${if (totalQuestions > 0) (correctAnswers * 100 / totalQuestions) else 0}%"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje personalizado basado en el desempeño
                val performanceMessage = when {
                    score >= totalQuestions * 25 -> "🏆 ¡Experto en Nutrición! Dominas el tema."
                    score >= totalQuestions * 20 -> "⭐ ¡Excelente trabajo! Sigue así."
                    score >= totalQuestions * 15 -> "👍 ¡Muy bien! Vas mejorando."
                    score >= totalQuestions * 10 -> "🙂 ¡Buen esfuerzo! Practica más."
                    else -> "📚 ¡No te rindas! Cada intento te hace más fuerte."
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
                    text = "Recuerda que una buena alimentación te da energía para jugar, estudiar y crecer fuerte. ¡Intenta comer más frutas y verduras!",
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
                            color = ConchodeVino, // color directo del texto
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
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
private fun GameOverModal(
    score: Int,
    totalQuestions: Int,
    answeredQuestions: Int,
    totalCorrectAnswers: Int,
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
                Text(text = "😔", fontSize = 80.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Juego Terminado!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Te quedaste sin vidas",
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            text = "Tu Puntuación",
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
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📝", fontSize = 24.sp)
                                Text(
                                    text = "$answeredQuestions/$totalQuestions",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConchodeVino
                                )
                                Text(
                                    text = "Preguntas",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "¡No te rindas! Intenta de nuevo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.Bold)
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
                        Text("Reintentar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryModal(
    score: Int,
    totalQuestions: Int,
    totalCorrectAnswers: Int,
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
                Text(text = "🏆", fontSize = 80.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Felicitaciones!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Completaste toda la trivia",
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🥑", fontSize = 40.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = score.toString(),
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "✅", fontSize = 24.sp)
                                Text(
                                    text = "$totalQuestions/$totalQuestions",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConchodeVino
                                )
                                Text(
                                    text = "Completadas",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "⭐", fontSize = 24.sp)
                                Text(
                                    text = "100%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConchodeVino
                                )
                                Text(
                                    text = "Progreso",
                                    fontSize = 12.sp,
                                    color = TextGray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when {
                        score >= 200 -> "¡Eres un experto en nutrición!"
                        score >= 150 -> "¡Excelente conocimiento!"
                        else -> "¡Muy bien! Sigue aprendiendo"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Salir", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Jugar de Nuevo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Data classes
data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val difficulty: Int,
    val points: Int
)

enum class TriviaGameStatus {
    PLAYING, GAME_OVER, COMPLETED
}