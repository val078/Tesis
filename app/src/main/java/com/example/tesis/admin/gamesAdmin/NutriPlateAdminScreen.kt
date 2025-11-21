package com.example.tesis.admin.games

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

data class PlateFoodItemAdmin(
    val emoji: String = "",
    val name: String = "",
    val isHealthy: Boolean = true
)

data class PlateRoundAdmin(
    val question: String = "",
    val correctItems: List<PlateFoodItemAdmin> = emptyList(),
    val wrongItems: List<PlateFoodItemAdmin> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriPlateAdminScreen(navController: NavController) {
    var rounds by remember { mutableStateOf<List<PlateRoundAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var editingRoundIndex by remember { mutableStateOf<Int?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Cargar rondas al inicio
    LaunchedEffect(Unit) {
        try {
            Log.d("NutriPlateAdmin", "🔄 Cargando rondas...")
            val doc = firestore.collection("games")
                .document("nutriPlate")
                .get()
                .await()

            if (doc.exists()) {
                val roundsList = doc.get("rounds") as? List<Map<String, Any>> ?: emptyList()
                rounds = roundsList.map { roundMap ->
                    val correctList = roundMap["correctItems"] as? List<Map<String, Any>> ?: emptyList()
                    val wrongList = roundMap["wrongItems"] as? List<Map<String, Any>> ?: emptyList()

                    PlateRoundAdmin(
                        question = roundMap["question"] as? String ?: "",
                        correctItems = correctList.map { item ->
                            PlateFoodItemAdmin(
                                emoji = item["emoji"] as? String ?: "",
                                name = item["name"] as? String ?: "",
                                isHealthy = item["isHealthy"] as? Boolean ?: true
                            )
                        },
                        wrongItems = wrongList.map { item ->
                            PlateFoodItemAdmin(
                                emoji = item["emoji"] as? String ?: "",
                                name = item["name"] as? String ?: "",
                                isHealthy = item["isHealthy"] as? Boolean ?: false
                            )
                        }
                    )
                }
                Log.d("NutriPlateAdmin", "✅ ${rounds.size} rondas cargadas")
            }
        } catch (e: Exception) {
            Log.e("NutriPlateAdmin", "❌ Error al cargar", e)
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
                        "NutriChef - Rondas",
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
                                    text = "${rounds.size} rondas configuradas",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "Total: ${rounds.sumOf { it.correctItems.size + it.wrongItems.size }} alimentos",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Lista de rondas
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(rounds) { index, round ->
                            PlateRoundCard(
                                round = round,
                                index = index + 1,
                                onEdit = { editingRoundIndex = index }
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
                                    Log.d("NutriPlateAdmin", "💾 Guardando rondas...")

                                    val roundsMap = rounds.map { round ->
                                        mapOf(
                                            "question" to round.question,
                                            "correctItems" to round.correctItems.map { item ->
                                                mapOf(
                                                    "emoji" to item.emoji,
                                                    "name" to item.name,
                                                    "isHealthy" to item.isHealthy
                                                )
                                            },
                                            "wrongItems" to round.wrongItems.map { item ->
                                                mapOf(
                                                    "emoji" to item.emoji,
                                                    "name" to item.name,
                                                    "isHealthy" to item.isHealthy
                                                )
                                            }
                                        )
                                    }

                                    firestore.collection("games")
                                        .document("nutriPlate")
                                        .set(mapOf(
                                            "rounds" to roundsMap,
                                            "updatedAt" to Timestamp.now(),
                                            "updatedBy" to "admin"
                                        ))
                                        .await()

                                    Log.d("NutriPlateAdmin", "✅ Guardado exitoso")
                                    hasChanges = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    Log.e("NutriPlateAdmin", "❌ Error al guardar", e)
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

        // Diálogo para editar ronda
        editingRoundIndex?.let { index ->
            val round = rounds[index]
            PlateRoundEditDialog(
                round = round,
                roundNumber = index + 1,
                onDismiss = { editingRoundIndex = null },
                onSave = { updatedRound ->
                    rounds = rounds.toMutableList().apply {
                        set(index, updatedRound)
                    }
                    hasChanges = true
                    editingRoundIndex = null
                }
            )
        }
    }
}

@Composable
private fun PlateRoundCard(
    round: PlateRoundAdmin,
    index: Int,
    onEdit: () -> Unit
) {
    val roundColor = when (index) {
        1 -> Color(0xFFFF6B6B)
        2 -> Color(0xFF4ECDC4)
        3 -> Color(0xFFFFE66D)
        4 -> Color(0xFF95E1D3)
        else -> Color(0xFF9E9E9E)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = roundColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = roundColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ronda $index",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
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
                text = round.question,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5D4037)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Alimentos correctos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✅ Correctos:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = round.correctItems.joinToString(" ") { it.emoji },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Alimentos incorrectos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❌ Incorrectos:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = round.wrongItems.joinToString(" ") { it.emoji },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChipPlate("🍽️ ${round.correctItems.size} saludables")
                InfoChipPlate("🚫 ${round.wrongItems.size} no saludables")
            }
        }
    }
}

@Composable
private fun InfoChipPlate(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color(0xFFFFF3E0),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF5D4037)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlateRoundEditDialog(
    round: PlateRoundAdmin,
    roundNumber: Int,
    onDismiss: () -> Unit,
    onSave: (PlateRoundAdmin) -> Unit
) {
    var editedQuestion by remember { mutableStateOf(round.question) }
    var editedCorrectItems by remember { mutableStateOf(round.correctItems) }
    var editedWrongItems by remember { mutableStateOf(round.wrongItems) }
    var editingItemIndex by remember { mutableStateOf<Pair<Boolean, Int>?>(null) } // (isCorrect, index)

    val roundColor = when (roundNumber) {
        1 -> Color(0xFFFF6B6B)
        2 -> Color(0xFF4ECDC4)
        3 -> Color(0xFFFFE66D)
        4 -> Color(0xFF95E1D3)
        else -> Color(0xFF9E9E9E)
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
                        .background(roundColor.copy(alpha = 0.1f))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ronda $roundNumber",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = roundColor
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
                                tint = roundColor
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
                                placeholder = { Text("Escribe la pregunta...") }
                            )
                        }
                    }

                    // Alimentos Correctos
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Alimentos Correctos (${editedCorrectItems.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    itemsIndexed(editedCorrectItems) { index, item ->
                        FoodItemCardEditable(
                            item = item,
                            onEdit = { editingItemIndex = Pair(true, index) }
                        )
                    }

                    // Alimentos Incorrectos
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❌ Alimentos Incorrectos (${editedWrongItems.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF44336)
                            )
                        }
                    }

                    itemsIndexed(editedWrongItems) { index, item ->
                        FoodItemCardEditable(
                            item = item,
                            onEdit = { editingItemIndex = Pair(false, index) }
                        )
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
                                PlateRoundAdmin(
                                    question = editedQuestion,
                                    correctItems = editedCorrectItems,
                                    wrongItems = editedWrongItems
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = roundColor
                        ),
                        enabled = editedQuestion.isNotEmpty() &&
                                editedCorrectItems.size == 3 &&
                                editedWrongItems.size == 3
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    // Diálogo para editar item individual
    editingItemIndex?.let { (isCorrect, index) ->
        val item = if (isCorrect) editedCorrectItems[index] else editedWrongItems[index]
        FoodItemEditDialog(
            item = item,
            isCorrect = isCorrect,
            onDismiss = { editingItemIndex = null },
            onSave = { updatedItem ->
                if (isCorrect) {
                    editedCorrectItems = editedCorrectItems.toMutableList().apply {
                        set(index, updatedItem)
                    }
                } else {
                    editedWrongItems = editedWrongItems.toMutableList().apply {
                        set(index, updatedItem)
                    }
                }
                editingItemIndex = null
            }
        )
    }
}

@Composable
private fun FoodItemCardEditable(
    item: PlateFoodItemAdmin,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isHealthy)
                Color(0xFFE8F5E9)
            else
                Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.emoji,
                fontSize = 28.sp,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF5D4037),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FoodItemEditDialog(
    item: PlateFoodItemAdmin,
    isCorrect: Boolean,
    onDismiss: () -> Unit,
    onSave: (PlateFoodItemAdmin) -> Unit
) {
    var emoji by remember { mutableStateOf(item.emoji) }
    var name by remember { mutableStateOf(item.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isCorrect) "Editar Alimento Saludable" else "Editar Alimento No Saludable",
                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji") },
                    placeholder = { Text("🍎") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Manzana") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (emoji.isNotEmpty() && name.isNotEmpty()) {
                        onSave(PlateFoodItemAdmin(emoji, name, isCorrect))
                    }
                },
                enabled = emoji.isNotEmpty() && name.isNotEmpty()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}