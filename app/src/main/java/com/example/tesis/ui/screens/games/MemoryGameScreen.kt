// ui/screens/games/MemoryGameScreen.kt
package com.example.tesis.ui.screens.games

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.data.model.GameProgressViewModel
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.viewmodel.AuthViewModel
import com.example.tesis.ui.components.GameResult
import kotlinx.coroutines.delay
import com.example.tesis.utils.TutorialManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryGameScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val gameProgressViewModel: GameProgressViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // ⭐ NUEVO: Estados para cargar datos desde Firestore
    var isLoadingGame by remember { mutableStateOf(true) }
    var gameRounds by remember { mutableStateOf<List<MemoryGameRound>>(emptyList()) }
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
    var finalMoves by remember { mutableIntStateOf(0) }
    var finalTotalRounds by remember { mutableIntStateOf(0) }

    // Estados del juego - CON RONDAS
    var score by rememberSaveable { mutableIntStateOf(0) }
    var currentRound by rememberSaveable { mutableIntStateOf(1) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var matchedPairs by rememberSaveable { mutableIntStateOf(0) }
    var gameStatus by rememberSaveable { mutableStateOf(MemoryGameStatus.PLAYING) }
    var timeLeft by rememberSaveable { mutableIntStateOf(70) }

    // Estados de las cartas
    var cards by remember { mutableStateOf<List<MemoryCardData>>(emptyList()) }
    var flippedCards by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var matchedCards by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isProcessing by remember { mutableStateOf(false) }

    // ⭐ NUEVO: Cargar configuración del juego desde Firestore
    LaunchedEffect(Unit) {
        try {
            Log.d("MemoryGame", "🔄 Cargando configuración desde Firestore...")
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("games")
                .document("memoryGame")
                .get()
                .await()

            if (doc.exists()) {
                val roundsList = doc.get("rounds") as? List<Map<String, Any>> ?: emptyList()

                gameRounds = roundsList.map { roundMap ->
                    val pairsList = roundMap["pairs"] as? List<Map<String, Any>> ?: emptyList()

                    MemoryGameRound(
                        number = (roundMap["number"] as? Long)?.toInt() ?: 1,
                        name = roundMap["name"] as? String ?: "",
                        description = roundMap["description"] as? String ?: "",
                        timeLimit = (roundMap["timeLimit"] as? Long)?.toInt() ?: 60,
                        pairsCount = pairsList.size,
                        pairs = pairsList.map { pairMap ->
                            MemoryCardPair(
                                foodEmoji = pairMap["emoji"] as? String ?: "❓",
                                benefitName = pairMap["benefitText"] as? String ?: "",
                                benefitEmoji = pairMap["benefitEmoji"] as? String ?: "❓"
                            )
                        }
                    )
                }

                // Crear las cartas para la primera ronda
                if (gameRounds.isNotEmpty()) {
                    cards = createMemoryCardsFromRound(gameRounds[0])
                    timeLeft = gameRounds[0].timeLimit
                }

                Log.d("MemoryGame", "✅ ${gameRounds.size} rondas cargadas desde Firestore")
            } else {
                loadError = "No se encontró la configuración del juego"
                Log.e("MemoryGame", "❌ Documento no existe")
            }
        } catch (e: Exception) {
            loadError = "Error al cargar el juego: ${e.message}"
            Log.e("MemoryGame", "❌ Error al cargar desde Firestore", e)
        } finally {
            isLoadingGame = false
        }
    }

    val currentRoundData = gameRounds.find { it.number == currentRound }

    // VERIFICAR SI MOSTRAR TUTORIAL
    LaunchedEffect(isLoadingGame) {
        if (!isLoadingGame && loadError == null) {
            val hasSeen = TutorialManager.hasSeen("memory_game")
            if (!hasSeen) {
                showTutorial = true
                Log.d("MemoryGame", "✅ Mostrando tutorial (primera vez)")
            } else {
                showCountdown = true
                Log.d("MemoryGame", "✅ Tutorial ya visto, iniciando juego")
            }
            tutorialChecked = true
        }
    }

    // COUNTDOWN SOLO se ejecuta cuando se activa y tutorial está cerrado
    LaunchedEffect(showCountdown, showTutorial) {
        if (showCountdown && !showTutorial && gameRounds.isNotEmpty()) {
            timeLeft = gameRounds[0].timeLimit
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

    // Timer del juego - SOLO corre cuando gameStarted es true
    LaunchedEffect(gameStatus, currentRound, gameStarted) {
        if (gameStatus == MemoryGameStatus.PLAYING && gameStarted && !showCountdown && !showTutorial) {
            timeLeft = currentRoundData?.timeLimit ?: 60
            while (timeLeft > 0 && gameStatus == MemoryGameStatus.PLAYING) {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft <= 0) {
                gameStatus = MemoryGameStatus.TIME_UP
            }
        }
    }

    // Lógica de voltear cartas
    fun handleCardClick(index: Int) {
        if (!gameStarted) return
        if (isProcessing ||
            flippedCards.contains(index) ||
            matchedCards.contains(index) ||
            flippedCards.size >= 2) {
            return
        }
        val newFlipped = flippedCards + index
        flippedCards = newFlipped
        if (newFlipped.size == 2) {
            isProcessing = true
            moves++
            val card1 = cards[newFlipped.elementAt(0)]
            val card2 = cards[newFlipped.elementAt(1)]
            if (card1.pairId == card2.pairId) {
                score += getPointsForRound(currentRound)
                matchedPairs++
                matchedCards = matchedCards + newFlipped
                flippedCards = emptySet()
                isProcessing = false
                if (matchedPairs == currentRoundData?.pairsCount) {
                    if (currentRound == gameRounds.size) {
                        gameStatus = MemoryGameStatus.COMPLETED
                    } else {
                        gameStatus = MemoryGameStatus.ROUND_COMPLETED
                    }
                }
            } else {
                score = maxOf(0, score - 2)
            }
        }
    }

    // Avanzar a siguiente ronda
    fun advanceToNextRound() {
        if (currentRound < gameRounds.size) {
            currentRound++
            val nextRound = gameRounds.find { it.number == currentRound }
            if (nextRound != null) {
                cards = createMemoryCardsFromRound(nextRound)
                flippedCards = emptySet()
                matchedCards = emptySet()
                matchedPairs = 0
                moves = 0
                timeLeft = nextRound.timeLimit
                gameStatus = MemoryGameStatus.PLAYING
                isProcessing = false
            }
        }
    }

    // Reiniciar juego completo
    fun restartGame() {
        currentRound = 1
        if (gameRounds.isNotEmpty()) {
            cards = createMemoryCardsFromRound(gameRounds[0])
        }
        flippedCards = emptySet()
        matchedCards = emptySet()
        score = 0
        moves = 0
        matchedPairs = 0
        gameStatus = MemoryGameStatus.PLAYING
        isProcessing = false
        tutorialStep = 0
        showTutorial = false
        showCountdown = true
        countdownValue = 3
    }

    LaunchedEffect(flippedCards) {
        if (flippedCards.size == 2) {
            delay(1000L)
            flippedCards = emptySet()
            isProcessing = false
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
        if ((gameStatus == MemoryGameStatus.COMPLETED || gameStatus == MemoryGameStatus.TIME_UP) &&
            gameStarted && currentUser != null) {
            val result = GameResult(
                gameId = "memory_game",
                score = score,
                correctAnswers = matchedPairs,
                totalQuestions = gameRounds.sumOf { it.pairsCount },
                timeLeft = timeLeft,
                streak = currentRound,
                extraData = mapOf(
                    "moves" to moves,
                    "totalRounds" to gameRounds.size,
                    "currentRound" to currentRound,
                    "gameStatus" to gameStatus.name
                )
            )
            gameProgressViewModel.saveGameResult("memory_game", result)
            Log.d("MemoryGame", "✅ Resultado guardado: $result")
            finalScore = score
            finalMoves = moves
            finalTotalRounds = gameRounds.size
            showReflection = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🧠 Memoria Saludable",
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
            // ⭐ NUEVO: Mostrar loading mientras carga desde Firestore
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
            // ⭐ El juego normal solo se muestra cuando cargó exitosamente
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    MemoryGameHeader(
                        score = score,
                        moves = moves,
                        matchedPairs = matchedPairs,
                        totalPairs = currentRoundData?.pairsCount ?: 6,
                        timeLeft = timeLeft,
                        currentRound = currentRound,
                        totalRounds = gameRounds.size
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = getRoundColor(currentRound).copy(alpha = 0.2f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Ronda $currentRound: ${currentRoundData?.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = getRoundColor(currentRound),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentRoundData?.description ?: "",
                                fontSize = 11.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    val calculatedCardSize = remember(currentRound, screenHeight) {
                        calculateCardSize(currentRound, screenHeight)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cards.size, key = { cards[it].id }) { index ->
                                Box(
                                    modifier = Modifier.aspectRatio(1f)
                                ) {
                                    MemoryCard(
                                        card = cards[index],
                                        isFlipped = flippedCards.contains(index) || matchedCards.contains(index),
                                        isMatched = matchedCards.contains(index),
                                        onClick = { handleCardClick(index) },
                                        cardSize = calculatedCardSize
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // TUTORIAL
            if (showTutorial && !isLoadingGame) {
                MemoryTutorialScreen(
                    step = tutorialStep,
                    onNext = {
                        if (tutorialStep < 3) {
                            tutorialStep++
                        } else {
                            coroutineScope.launch {
                                TutorialManager.markAsSeen("memory_game")
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
                CountdownScreenM(countdownValue = countdownValue)
            }

            // Modales de fin de juego
            when (gameStatus) {
                MemoryGameStatus.ROUND_COMPLETED -> {
                    RoundCompletedModal(
                        round = currentRound,
                        score = score,
                        onNextRound = { advanceToNextRound() },
                        onExit = { navController.popBackStack() }
                    )
                }
                MemoryGameStatus.COMPLETED -> {
                    VictoryMemoryModal(
                        score = score,
                        moves = moves,
                        totalRounds = gameRounds.size,
                        onRestart = { restartGame() },
                        onExit = { navController.popBackStack() }
                    )
                }
                MemoryGameStatus.TIME_UP -> {
                    TimeUpModal(
                        score = score,
                        matchedPairs = matchedPairs,
                        totalPairs = currentRoundData?.pairsCount ?: 6,
                        currentRound = currentRound,
                        onRestart = { restartGame() },
                        onContinue = {
                            gameStatus = MemoryGameStatus.PLAYING
                            timeLeft = currentRoundData?.timeLimit ?: 60
                        },
                        onExit = { navController.popBackStack() }
                    )
                }
                else -> {}
            }
            if (showReflection) {
                MemoryGameReflectionScreen(
                    score = finalScore,
                    moves = finalMoves,
                    totalRounds = finalTotalRounds,
                    onExit = { navController.popBackStack() },
                    onRestart = {
                        score = 0
                        currentRound = 1
                        moves = 0
                        matchedPairs = 0
                        gameStatus = MemoryGameStatus.PLAYING
                        if (gameRounds.isNotEmpty()) {
                            timeLeft = gameRounds[0].timeLimit
                            cards = createMemoryCardsFromRound(gameRounds[0])
                        }
                        flippedCards = emptySet()
                        matchedCards = emptySet()
                        showReflection = false
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryTutorialScreen(
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
                            text = "¡Bienvenido al juego de memoria!",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Encuentra parejas de alimentos y sus beneficios",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🧠💚",
                            fontSize = 56.sp
                        )
                    }
                    1 -> {
                        Text(
                            text = "Así funciona:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "1️⃣ Toca dos cartas para voltearlas\n2️⃣ Si hacen pareja, ¡sumas puntos!\n3️⃣ Si no coinciden, se ocultan de nuevo",
                            fontSize = 15.sp,
                            color = ConchodeVino,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🃏🃏 → ✅",
                            fontSize = 42.sp
                        )
                    }
                    2 -> {
                        Text(
                            text = "Tipos de parejas:",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "🍎 + ❤️ = Corazón sano",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "🥛 + 🦴 = Huesos fuertes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Empareja cada alimento con su beneficio",
                            fontSize = 14.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                    3 -> {
                        Text(
                            text = "¡Listo para jugar!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Tienes 3 rondas con tiempo límite",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "¡Encuentra todas las parejas antes de que se acabe el tiempo!",
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
fun CountdownScreenM(countdownValue: Int) {
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
private fun MemoryGameHeader(
    score: Int,
    moves: Int,
    matchedPairs: Int,
    totalPairs: Int,
    timeLeft: Int,
    currentRound: Int,
    totalRounds: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),  // Más compacto
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)  // Menos elevación
    ) {
        Column(modifier = Modifier.padding(12.dp)) {  // Padding reducido
            // Información de ronda y tiempo en una sola línea
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ronda $currentRound/$totalRounds",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getRoundColor(currentRound)
                )

                // Tiempo más compacto
                Box(
                    modifier = Modifier
                        .size(40.dp)  // Más pequeño
                        .background(
                            color = when {
                                timeLeft <= 15 -> Color(0xFFFF6B6B)
                                timeLeft <= 30 -> Color(0xFFFFA726)
                                else -> Color(0xFFFFE4CC)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$timeLeft",
                        fontSize = 14.sp,  // Texto más pequeño
                        fontWeight = FontWeight.Bold,
                        color = when {
                            timeLeft <= 15 -> Color.White
                            else -> ConchodeVino
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))  // Menos espacio

            // Puntuación y movimientos en línea más compacta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PUNTOS",
                        fontSize = 9.sp,  // Más pequeño
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = score.toString(),
                        fontSize = 20.sp,  // Más pequeño
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MOVIMIENTOS",
                        fontSize = 9.sp,  // Más pequeño
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = moves.toString(),
                        fontSize = 20.sp,  // Más pequeño
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))  // Menos espacio

            // Progreso de parejas más compacto
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Parejas encontradas",
                        fontSize = 11.sp,  // Más pequeño
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$matchedPairs/$totalPairs",
                        fontSize = 11.sp,  // Más pequeño
                        color = getRoundColor(currentRound),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))  // Menos espacio
                LinearProgressIndicator(
                    progress = { matchedPairs.toFloat() / totalPairs.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)  // Más delgado
                        .clip(RoundedCornerShape(2.dp)),
                    color = getRoundColor(currentRound),
                    trackColor = getRoundColor(currentRound).copy(alpha = 0.2f)
                )
            }
        }
    }
}

private fun calculateCardSize(round: Int, screenHeight: Dp): Dp {
    // Altura aproximada que ocupan los otros componentes (header, info, etc.)
    val otherComponentsHeight = when (round) {
        1 -> 320.dp  // Ronda 1: componentes más grandes
        2 -> 300.dp  // Rondas 2-3: componentes más compactos
        3 -> 300.dp
        else -> 320.dp
    }

    // Altura disponible para el grid
    val availableHeight = screenHeight - otherComponentsHeight

    // Calcular tamaño basado en el número de filas necesarias
    return when (round) {
        1 -> {
            // Ronda 1: 6 pares = 12 cartas = 3 filas de 4
            val cardHeight = (availableHeight - 16.dp) / 3f  // 3 filas + espacios
            cardHeight.coerceIn(80.dp, 120.dp)
        }
        2 -> {
            // Ronda 2: 8 pares = 16 cartas = 4 filas de 4
            val cardHeight = (availableHeight - 24.dp) / 4f  // 4 filas + espacios
            cardHeight.coerceIn(70.dp, 100.dp)
        }
        3 -> {
            // Ronda 3: 8 pares = 16 cartas = 4 filas de 4 (igual que ronda 2)
            val cardHeight = (availableHeight - 24.dp) / 4f  // 4 filas + espacios
            cardHeight.coerceIn(70.dp, 100.dp)
        }
        else -> 80.dp
    }
}

@Composable
private fun RoundCompletedModal(
    round: Int,
    score: Int,
    onNextRound: () -> Unit,
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
                Text(text = "🎯", fontSize = 72.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Ronda $round Completada!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = getRoundColor(round),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Preparando siguiente nivel...",
                    fontSize = 15.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getRoundColor(round).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puntuación Acumulada",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = getRoundColor(round)
                        )
                        Text(
                            text = "puntos",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (round) {
                        1 -> "¡Excelente inicio! La siguiente ronda es más desafiante."
                        2 -> "¡Increíble! Prepárate para el nivel final."
                        else -> "¡Listo para el desafío final!"
                    },
                    fontSize = 15.sp,
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
                        onClick = onNextRound,
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getRoundColor(round + 1)
                        )
                    ) {
                        Text("Siguiente Ronda", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getRoundColor(round: Int): Color {
    return when (round) {
        1 -> Color(0xFF4CAF50) // Verde
        2 -> Color(0xFF2196F3) // Azul
        3 -> Color(0xFFFF9800) // Naranja
        else -> PrimaryOrange
    }
}

private fun getTimeForRound(round: Int): Int {
    return when (round) {
        1 -> 90  // 1.5 minutos
        2 -> 75  // 1.25 minutos
        3 -> 60  // 1 minuto
        else -> 60
    }
}

private fun getPointsForRound(round: Int): Int {
    return when (round) {
        1 -> 20  // Puntos base
        2 -> 25  // Más puntos
        3 -> 30  // Máximos puntos
        else -> 20
    }
}

private fun createMemoryCardsForRound(round: Int): List<MemoryCardData> {
    val pairs = when (round) {
        1 -> getRound1Pairs()  // 6 pares = 12 cartas
        2 -> getRound2Pairs()  // 8 pares = 16 cartas
        3 -> getRound3Pairs()  // 8 pares = 16 cartas (reducido de 10)
        else -> getRound1Pairs()
    }
    val cards = mutableListOf<MemoryCardData>()
    pairs.forEachIndexed { index, pair ->
        cards.add(MemoryCardData(id = index * 2, emoji = pair.foodEmoji, label = "", pairId = index))
        cards.add(MemoryCardData(id = index * 2 + 1, emoji = pair.benefitEmoji, label = pair.benefitName, pairId = index))
    }

    return cards.shuffled()
}

private fun getRound1Pairs(): List<MemoryCardPair> {
    return listOf(
        MemoryCardPair("🍎", "Corazón", "❤️"),       // manzana
        MemoryCardPair("🥛", "Huesos", "🦴"),         // leche
        MemoryCardPair("🥕", "Vista", "👁️"),          // zanahoria
        MemoryCardPair("🥦", "Fuerza", "💪"),         // brócoli
        MemoryCardPair("🍊", "Defensas", "🛡️"),      // naranja
        MemoryCardPair("🐟", "Cerebro", "🧠")         // pescado
    )
}

// Pares para Ronda 2 - Nutrientes Especiales
private fun getRound2Pairs(): List<MemoryCardPair> {
    return listOf(
        MemoryCardPair("🍌", "Energía", "⚡"),          // plátano
        MemoryCardPair("🥚", "Proteína", "💪"),         // huevo
        MemoryCardPair("🫐", "Memoria", "🧠"),          // arándano
        MemoryCardPair("🥑", "Grasas buenas", "💚"),    // aguacate
        MemoryCardPair("🍓", "Antioxidante", "🌟"),           // fresa
        MemoryCardPair("🌰", "Resistencia", "🔋"),      // nuez
        MemoryCardPair("🍅", "Piel", "✨"),              // tomate
        MemoryCardPair("🫘", "Vegetal", "🌱")           // frijoles
    )
}

// Pares para Ronda 3 - Super Alimentos
private fun getRound3Pairs(): List<MemoryCardPair> {
    return listOf(
        return listOf(
            MemoryCardPair("🥝", "Defensas", "🛡️"),       // kiwi → refuerza defensas
            MemoryCardPair("🍠", "Energía", "⚡"),          // camote
            MemoryCardPair("🧄", "Protección", "🌟"),      // ajo
            MemoryCardPair("🍯", "Natural", "🐝"),          // miel
            MemoryCardPair("🌻", "Corazón", "❤️"),          // semillas
            MemoryCardPair("🍋", "Cicatriza", "✨"),        // limón
            MemoryCardPair("🥬", "Vitaminas", "🌈"),       // espinaca
            MemoryCardPair("🥥", "Hidratación", "💧")      // coco
        )
    )
}

@Composable
private fun MemoryCard(
    card: MemoryCardData,
    isFlipped: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit,
    cardSize: Dp = 80.dp,  // Tamaño dinámico
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isMatched) 1.05f else 1f,
        animationSpec = if (isMatched) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            tween(durationMillis = 200)
        },
        label = "card_scale"
    )

    Box(
        modifier = modifier
            .size(cardSize)  // Usar tamaño dinámico
            .scale(animatedScale)
            .clickable(
                enabled = !isFlipped && !isMatched,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Cara trasera - se muestra cuando NO está volteada
        AnimatedVisibility(
            visible = !isFlipped,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CardBack(cardSize = cardSize)  // Pasar tamaño a CardBack
        }

        // Cara frontal - se muestra cuando SÍ está volteada
        AnimatedVisibility(
            visible = isFlipped,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CardFront(card = card, isMatched = isMatched, cardSize = cardSize)  // Pasar tamaño a CardFront
        }
    }
}


@Composable
private fun CardBack(cardSize: Dp = 80.dp) {
    val fontSize = when (cardSize) {
        60.dp -> 8.sp
        70.dp -> 9.sp
        else -> 10.sp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(12.dp))  // Sombra reducida
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryOrange,
                        PrimaryOrange.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,  // Borde más delgado
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "TOCA",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun CardFront(card: MemoryCardData, isMatched: Boolean, cardSize: Dp = 80.dp) {
    val isFoodCard = card.emoji != "💊"

    // Tamaños dinámicos basados en el tamaño de la carta
    val emojiSize = when (cardSize) {
        60.dp -> 20.sp
        70.dp -> 24.sp
        else -> 28.sp
    }

    val benefitBoxSize = when (cardSize) {
        60.dp -> 30.dp
        70.dp -> 32.dp
        else -> 36.dp
    }

    val labelFontSize = when (cardSize) {
        60.dp -> 8.sp
        70.dp -> 9.sp
        else -> 10.sp
    }

    val typeFontSize = when (cardSize) {
        60.dp -> 7.sp
        70.dp -> 7.sp
        else -> 8.sp
    }

    val backgroundColor = when {
        isMatched -> Color(0xFFE8F5E8)
        isFoodCard -> Color(0xFFFFF8F0)
        else -> Color(0xFFF0F8FF)
    }

    val borderColor = when {
        isMatched -> Color(0xFF4CAF50)
        isFoodCard -> Color(0xFFFFA726)
        else -> Color(0xFF42A5F5)
    }

    val textColor = when {
        isMatched -> Color(0xFF2E7D32)
        isFoodCard -> ConchodeVino
        else -> Color(0xFF1976D2)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(12.dp))  // Sombra reducida
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,  // Borde más delgado
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)  // Padding reducido
        ) {
            if (isFoodCard) {
                Text(
                    text = card.emoji,
                    fontSize = emojiSize
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(benefitBoxSize)
                        .background(
                            color = Color(0xFFE3F2FD),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,  // Borde reducido
                            color = Color(0xFF90CAF9),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.emoji,
                        fontSize = emojiSize.times(0.7f)  // Emoji más pequeño para beneficios
                    )
                }
            }

            if (card.label.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))  // Espacio reducido
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isFoodCard) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(vertical = 3.dp, horizontal = 4.dp)  // Padding reducido
                ) {
                    Text(
                        text = card.label,
                        fontSize = labelFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        lineHeight = labelFontSize.times(1.1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFoodCard) "Alimento" else "Nutriente",
                    fontSize = typeFontSize,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }

        if (isMatched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (-2).dp, x = (-2).dp)
                    .padding(2.dp)
                    .size(16.dp)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MemoryGameReflectionScreen(
    score: Int,
    moves: Int,
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
                    text = "🎉 ¡Felicidades!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aprendiste mucho sobre alimentos y sus beneficios",
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
                        value = totalRounds.toString()
                    )
                    StatItem(
                        emoji = "👆",
                        label = "Movimientos",
                        value = moves.toString()
                    )
                    StatItem(
                        emoji = "⭐",
                        label = "Eficiencia",
                        value = if (moves > 0) "${(score * 100 / moves).toInt()}/mov" else "0"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Reflexión
                Text(
                    text = "Recuerda que recordar los beneficios de los alimentos saludables te ayuda a tomar mejores decisiones. ¡Intenta comer más frutas y verduras!",
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
private fun VictoryMemoryModal(
    score: Int,
    moves: Int,
    totalRounds: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    // Calcular calificación final
    val finalGrade = when {
        score >= 500 -> "🏆 Maestro de la Memoria"
        score >= 400 -> "⭐ Experto en Nutrición"
        score >= 300 -> "🎯 Memoria Excelente"
        score >= 200 -> "👍 Muy Bueno"
        else -> "🙂 Buen Trabajo"
    }

    val gradeEmoji = when {
        score >= 500 -> "🏆"
        score >= 400 -> "⭐"
        score >= 300 -> "🎯"
        score >= 200 -> "👍"
        else -> "🙂"
    }

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
                // Celebración animada
                Text(text = "🎉", fontSize = 72.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Felicidades!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Completaste las $totalRounds rondas",
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tarjeta de puntuación final
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Puntuación Final",
                            fontSize = 14.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = score.toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )

                        Text(
                            text = "puntos",
                            fontSize = 14.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Calificación
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = gradeEmoji,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = finalGrade,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ConchodeVino
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Estadísticas detalladas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "🎯", fontSize = 28.sp)
                        Text(
                            text = moves.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = "Movimientos",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⏱️", fontSize = 28.sp)
                        Text(
                            text = "$totalRounds",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = "Rondas",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "⭐", fontSize = 28.sp)
                        Text(
                            text = "${score / totalRounds}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = "Promedio",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje final personalizado
                Text(
                    text = when {
                        score >= 500 -> "¡Eres un genio de la memoria nutricional! 🧠"
                        score >= 400 -> "¡Excelente trabajo! Tu memoria es impresionante 💪"
                        score >= 300 -> "¡Muy bien! Dominas los beneficios de los alimentos 🌟"
                        score >= 200 -> "¡Buen trabajo! Sigue practicando para mejorar 🚀"
                        else -> "¡Bien hecho! Cada intento te hace mejor 📚"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
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

                // Mensaje adicional
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "¡Sigue aprendiendo sobre alimentación saludable!",
                    fontSize = 12.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TimeUpModal(
    score: Int,
    matchedPairs: Int,
    totalPairs: Int,
    currentRound: Int,
    onRestart: () -> Unit,
    onContinue: () -> Unit,
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
                Text(text = "⏰", fontSize = 72.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Tiempo Agotado!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = getRoundColor(currentRound),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ronda $currentRound",
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estadísticas de la ronda
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getRoundColor(currentRound).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tu Progreso",
                            fontSize = 14.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Puntuación
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "⭐",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = score.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                                Text(
                                    text = "Puntos",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "🧩",
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "$matchedPairs/$totalPairs",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = getRoundColor(currentRound)
                                )
                                Text(
                                    text = "Parejas",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra de progreso
                        LinearProgressIndicator(
                            progress = { matchedPairs.toFloat() / totalPairs.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = getRoundColor(currentRound),
                            trackColor = getRoundColor(currentRound).copy(alpha = 0.2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mensaje motivacional basado en el progreso
                Text(
                    text = when {
                        matchedPairs == totalPairs -> "¡Perfecto! Completaste todas las parejas."
                        matchedPairs >= totalPairs * 0.7 -> "¡Muy bien! Casi lo logras."
                        matchedPairs >= totalPairs * 0.5 -> "Buen esfuerzo. ¡Sigue practicando!"
                        else -> "¡No te rindas! La práctica hace al maestro."
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConchodeVino,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones adaptativos
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

                    // Mostrar "Continuar" si no es la última ronda y se hizo buen progreso
                    if (currentRound < 3 && matchedPairs >= totalPairs * 0.5) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .height(56.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getRoundColor(currentRound)
                            )
                        ) {
                            Text("Continuar", fontWeight = FontWeight.Bold)
                        }
                    } else {
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
                                text = if (currentRound == 1) "Reintentar" else "Reiniciar Juego",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Información adicional para continuar
                if (currentRound < 3 && matchedPairs >= totalPairs * 0.5) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Puedes continuar con la siguiente ronda",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun createMemoryCardsFromRound(round: MemoryGameRound): List<MemoryCardData> {
    val cards = mutableListOf<MemoryCardData>()
    round.pairs.forEachIndexed { index, pair ->
        // Carta del alimento
        cards.add(
            MemoryCardData(
                id = index * 2,
                emoji = pair.foodEmoji,
                label = "",
                pairId = index
            )
        )
        // Carta del beneficio
        cards.add(
            MemoryCardData(
                id = index * 2 + 1,
                emoji = pair.benefitEmoji,
                label = pair.benefitName,
                pairId = index
            )
        )
    }
    return cards.shuffled()
}

// Data classes
data class MemoryCardData(
    val id: Int,
    val emoji: String,
    val label: String,
    val pairId: Int
)

data class MemoryCardPair(
    val foodEmoji: String,
    val benefitName: String,
    val benefitEmoji: String
)

data class MemoryGameRound(
    val number: Int,
    val name: String,
    val description: String,
    val timeLimit: Int,
    val pairsCount: Int,
    val pairs: List<MemoryCardPair> = emptyList() // ⭐ NUEVO: agregar lista de pares
)
enum class MemoryGameStatus {
    PLAYING, ROUND_COMPLETED, COMPLETED, TIME_UP
}