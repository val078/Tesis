package com.example.tesis.admin.gamesAdmin

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TriviaQuestionAdmin(
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctAnswer: Int = 0,
    val difficulty: Int = 1,
    val points: Int = 10
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreguntonAdminScreen(navController: NavController) {
    var questions by remember { mutableStateOf<List<TriviaQuestionAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Cargar preguntas al inicio
    LaunchedEffect(Unit) {
        try {
            Log.d("PreguntonAdmin", "🔄 Cargando preguntas...")
            val doc = firestore.collection("games")
                .document("pregunton")
                .get()
                .await()

            if (doc.exists()) {
                val questionsList = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
                questions = questionsList.map { qMap ->
                    val optionsList = qMap["options"] as? List<String> ?: listOf("", "", "", "")
                    TriviaQuestionAdmin(
                        question = qMap["question"] as? String ?: "",
                        options = optionsList,
                        correctAnswer = (qMap["correctAnswer"] as? Long)?.toInt() ?: 0,
                        difficulty = (qMap["difficulty"] as? Long)?.toInt() ?: 1,
                        points = (qMap["points"] as? Long)?.toInt() ?: 10
                    )
                }
                Log.d("PreguntonAdmin", "✅ ${questions.size} preguntas cargadas")
            }
        } catch (e: Exception) {
            Log.e("PreguntonAdmin", "❌ Error al cargar", e)
        } finally {
            isLoading = false
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("✅ Guardado") },
            text = { Text("Los cambios se guardaron correctamente.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Preguntón - Preguntas",
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
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE67E22))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFF8F0))
                        .padding(bottom = 80.dp)
                ) {
                    // Banner informativo
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${questions.size} preguntas configuradas",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "Total puntos: ${questions.sumOf { it.points }}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Lista de preguntas
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(questions) { index, question ->
                            QuestionCard(
                                question = question,
                                index = index + 1,
                                onEdit = { editingQuestionIndex = index }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Botón de guardar cambios
                AnimatedVisibility(
                    visible = hasChanges,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                try {
                                    Log.d("PreguntonAdmin", "💾 Guardando preguntas...")

                                    val questionsMap = questions.map { q ->
                                        mapOf(
                                            "question" to q.question,
                                            "options" to q.options,
                                            "correctAnswer" to q.correctAnswer,
                                            "difficulty" to q.difficulty,
                                            "points" to q.points
                                        )
                                    }

                                    firestore.collection("games")
                                        .document("pregunton")
                                        .set(mapOf(
                                            "questions" to questionsMap,
                                            "updatedAt" to Timestamp.now(),
                                            "updatedBy" to "admin"
                                        ))
                                        .await()

                                    Log.d("PreguntonAdmin", "✅ Guardado exitoso")
                                    hasChanges = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    Log.e("PreguntonAdmin", "❌ Error al guardar", e)
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Guardar Cambios",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diálogo para editar pregunta
        editingQuestionIndex?.let { index ->
            val question = questions[index]
            QuestionEditDialog(
                question = question,
                questionNumber = index + 1,
                onDismiss = { editingQuestionIndex = null },
                onSave = { updatedQuestion ->
                    questions = questions.toMutableList().apply {
                        set(index, updatedQuestion)
                    }
                    hasChanges = true
                    editingQuestionIndex = null
                }
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: TriviaQuestionAdmin,
    index: Int,
    onEdit: () -> Unit
) {
    val difficultyColor = when (question.difficulty) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con número y dificultad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = difficultyColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pregunta $index",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyBadge(question.difficulty)
                            PointsBadge(question.points)
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color(0xFFE67E22)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pregunta
            Text(
                text = question.question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Opciones
            question.options.forEachIndexed { optionIndex, option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (optionIndex == question.correctAnswer)
                            Icons.Default.CheckCircle
                        else
                            Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (optionIndex == question.correctAnswer)
                            Color(0xFF4CAF50)
                        else
                            Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        color = if (optionIndex == question.correctAnswer)
                            Color(0xFF4CAF50)
                        else
                            Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Int) {
    val color = when (difficulty) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFFF44336)
        else -> Color.Gray
    }

    val label = when (difficulty) {
        1 -> "Fácil"
        2 -> "Media"
        3 -> "Difícil"
        4 -> "Experto"
        else -> "N/A"
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun PointsBadge(points: Int) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$points pts",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE67E22)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionEditDialog(
    question: TriviaQuestionAdmin,
    questionNumber: Int,
    onDismiss: () -> Unit,
    onSave: (TriviaQuestionAdmin) -> Unit
) {
    var editedQuestion by remember { mutableStateOf(question.question) }
    var editedOptions by remember { mutableStateOf(question.options) }
    var editedCorrectAnswer by remember { mutableStateOf(question.correctAnswer) }
    var editedDifficulty by remember { mutableStateOf(question.difficulty) }
    var editedPoints by remember { mutableStateOf(question.points) }

    // ⭐ NUEVO: Variable temporal para puntos como String
    var pointsText by remember { mutableStateOf(question.points.toString()) }

    val difficultyColor = when (editedDifficulty) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFFFF9800)
        4 -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(difficultyColor.copy(alpha = 0.1f))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pregunta $questionNumber",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = difficultyColor
                            )
                            Text(
                                text = "Editar contenido",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = difficultyColor
                            )
                        }
                    }
                }

                // Contenido con scroll
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pregunta
                    item {
                        Column {
                            Text(
                                text = "Pregunta",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editedQuestion,
                                onValueChange = { editedQuestion = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                placeholder = { Text("Escribe la pregunta aquí...") }
                            )
                        }
                    }

                    // Opciones
                    item {
                        Column {
                            Text(
                                text = "Opciones de respuesta",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Marca la opción correcta:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    itemsIndexed(editedOptions) { index, option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = editedCorrectAnswer == index,
                                onClick = { editedCorrectAnswer = index },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF4CAF50)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newValue ->
                                    editedOptions = editedOptions.toMutableList().apply {
                                        set(index, newValue)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Opción ${index + 1}") },
                                singleLine = true
                            )
                        }
                    }

                    // Dificultad
                    item {
                        Column {
                            Text(
                                text = "Dificultad",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    1 to "Fácil",
                                    2 to "Media",
                                    3 to "Difícil",
                                    4 to "Experto"
                                ).forEach { (level, label) ->
                                    FilterChip(
                                        selected = editedDifficulty == level,
                                        onClick = { editedDifficulty = level },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (level) {
                                                1 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                2 -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                                3 -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                                4 -> Color(0xFFF44336).copy(alpha = 0.2f)
                                                else -> Color.Gray.copy(alpha = 0.2f)
                                            }
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // ⭐ CAMBIO: Puntos con variable temporal String
                    item {
                        Column {
                            Text(
                                text = "Puntos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pointsText,
                                onValueChange = { newValue ->
                                    // Permitir solo números y campo vacío
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        pointsText = newValue
                                        // Actualizar el estado solo si es un número válido
                                        newValue.toIntOrNull()?.let { points ->
                                            editedPoints = points
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Puntos por acierto") },
                                singleLine = true,
                                placeholder = { Text("10, 20, 30, 40...") },
                                supportingText = {
                                    if (pointsText.isEmpty()) {
                                        Text(
                                            "Ingresa la puntuación",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE67E22)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            onSave(
                                TriviaQuestionAdmin(
                                    question = editedQuestion,
                                    options = editedOptions,
                                    correctAnswer = editedCorrectAnswer,
                                    difficulty = editedDifficulty,
                                    points = editedPoints
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = difficultyColor
                        ),
                        // ⭐ NUEVO: Validar que pointsText no esté vacío
                        enabled = editedQuestion.isNotEmpty() &&
                                editedOptions.all { it.isNotEmpty() } &&
                                pointsText.isNotEmpty() &&
                                pointsText.toIntOrNull() != null
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}