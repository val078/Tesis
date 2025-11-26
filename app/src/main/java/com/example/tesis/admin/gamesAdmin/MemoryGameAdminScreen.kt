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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MemoryPairAdmin(
    val emoji: String = "",
    val emojiDesc: String = "",
    val benefitText: String = "",
    val benefitEmoji: String = ""
)

data class MemoryRoundAdmin(
    val number: Int = 1,
    val name: String = "",
    val description: String = "",
    val timeLimit: Int = 60,
    val pairs: List<MemoryPairAdmin> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryGameAdminScreen(navController: NavController) {
    var rounds by remember { mutableStateOf<List<MemoryRoundAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedRound by remember { mutableStateOf<Int?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Cargar rounds al inicio
    LaunchedEffect(Unit) {
        try {
            Log.d("MemoryAdmin", "🔄 Cargando rounds...")
            val doc = firestore.collection("games")
                .document("memoryGame")
                .get()
                .await()

            if (doc.exists()) {
                val roundsList = doc.get("rounds") as? List<Map<String, Any>> ?: emptyList()
                rounds = roundsList.map { roundMap ->
                    val pairsList = roundMap["pairs"] as? List<Map<String, Any>> ?: emptyList()
                    MemoryRoundAdmin(
                        number = (roundMap["number"] as? Long)?.toInt() ?: 1,
                        name = roundMap["name"] as? String ?: "",
                        description = roundMap["description"] as? String ?: "",
                        timeLimit = (roundMap["timeLimit"] as? Long)?.toInt() ?: 60,
                        pairs = pairsList.map { pairMap ->
                            MemoryPairAdmin(
                                emoji = pairMap["emoji"] as? String ?: "",
                                emojiDesc = pairMap["emojiDesc"] as? String ?: "",
                                benefitText = pairMap["benefitText"] as? String ?: "",
                                benefitEmoji = pairMap["benefitEmoji"] as? String ?: ""
                            )
                        }
                    )
                }
                Log.d("MemoryAdmin", "✅ ${rounds.size} rounds cargados")
            }
        } catch (e: Exception) {
            Log.e("MemoryAdmin", "❌ Error al cargar", e)
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
                        "Memory Game - Rondas",
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
                                    text = "Total: ${rounds.sumOf { it.pairs.size }} pares",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Lista de rounds
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(rounds) { index, round ->
                            RoundCard(
                                round = round,
                                onClick = { selectedRound = round.number }
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
                                    Log.d("MemoryAdmin", "💾 Guardando rounds...")

                                    val roundsMap = rounds.map { round ->
                                        mapOf(
                                            "number" to round.number,
                                            "name" to round.name,
                                            "description" to round.description,
                                            "timeLimit" to round.timeLimit,
                                            "pairs" to round.pairs.map { pair ->
                                                mapOf(
                                                    "emoji" to pair.emoji,
                                                    "emojiDesc" to pair.emojiDesc,
                                                    "benefitText" to pair.benefitText,
                                                    "benefitEmoji" to pair.benefitEmoji
                                                )
                                            }
                                        )
                                    }

                                    firestore.collection("games")
                                        .document("memoryGame")
                                        .set(mapOf(
                                            "rounds" to roundsMap,
                                            "updatedAt" to Timestamp.now(),
                                            "updatedBy" to "admin"
                                        ))
                                        .await()

                                    Log.d("MemoryAdmin", "✅ Guardado exitoso")
                                    hasChanges = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    Log.e("MemoryAdmin", "❌ Error al guardar", e)
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

        // Pantalla de detalle de ronda
        selectedRound?.let { roundNumber ->
            val round = rounds.find { it.number == roundNumber }
            if (round != null) {
                RoundDetailScreen(
                    round = round,
                    onDismiss = { selectedRound = null },
                    onSave = { updatedRound ->
                        rounds = rounds.map {
                            if (it.number == roundNumber) updatedRound else it
                        }
                        hasChanges = true
                        selectedRound = null
                    }
                )
            }
        }
    }
}

@Composable
private fun RoundCard(
    round: MemoryRoundAdmin,
    onClick: () -> Unit
) {
    val roundColor = when (round.number) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número de ronda
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = roundColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = round.number.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = roundColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = round.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = round.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip("🎴 ${round.pairs.size} pares")
                    InfoChip("⏱️ ${round.timeLimit}s")
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFE67E22)
            )
        }
    }
}

@Composable
private fun InfoChip(text: String) {
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
private fun RoundDetailScreen(
    round: MemoryRoundAdmin,
    onDismiss: () -> Unit,
    onSave: (MemoryRoundAdmin) -> Unit
) {
    var editedRound by remember { mutableStateOf(round) }
    var editingPairIndex by remember { mutableStateOf<Int?>(null) }

    // ⭐ NUEVO: Variable temporal para el tiempo como String
    var timeLimitText by remember { mutableStateOf(round.timeLimit.toString()) }

    val roundColor = when (round.number) {
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFF2196F3)
        3 -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
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
                                text = "Ronda ${round.number}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = roundColor
                            )
                            Text(
                                text = round.name,
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

                // Configuración de la ronda
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = editedRound.name,
                        onValueChange = { editedRound = editedRound.copy(name = it) },
                        label = { Text("Nombre de la ronda") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editedRound.description,
                        onValueChange = { editedRound = editedRound.copy(description = it) },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ⭐ CAMBIO: Usar variable temporal de String
                    OutlinedTextField(
                        value = timeLimitText,
                        onValueChange = { newValue ->
                            // Permitir solo números y campo vacío
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                timeLimitText = newValue
                                // Actualizar el estado solo si es un número válido
                                newValue.toIntOrNull()?.let { time ->
                                    editedRound = editedRound.copy(timeLimit = time)
                                }
                            }
                        },
                        label = { Text("Tiempo límite (segundos)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("60") },
                        supportingText = {
                            if (timeLimitText.isEmpty()) {
                                Text(
                                    "Ingresa un tiempo en segundos",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE67E22)
                                )
                            }
                        }
                    )
                }

                Divider()

                // Lista de pares
                Text(
                    text = "Pares (${editedRound.pairs.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    itemsIndexed(editedRound.pairs) { index, pair ->
                        PairItemCard(
                            pair = pair,
                            onEdit = { editingPairIndex = index },
                            onDelete = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                        onClick = { onSave(editedRound) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = roundColor
                        ),
                        // ⭐ NUEVO: Deshabilitar si el tiempo está vacío
                        enabled = timeLimitText.isNotEmpty() &&
                                timeLimitText.toIntOrNull() != null &&
                                editedRound.name.isNotEmpty()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    // Diálogo para editar par
    editingPairIndex?.let { index ->
        val pair = editedRound.pairs[index]
        PairEditDialog(
            pair = pair,
            onDismiss = { editingPairIndex = null },
            onSave = { newPair ->
                editedRound = editedRound.copy(
                    pairs = editedRound.pairs.toMutableList().apply {
                        set(index, newPair)
                    }
                )
                editingPairIndex = null
            }
        )
    }
}

@Composable
private fun PairItemCard(
    pair: MemoryPairAdmin,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji del alimento + descripción
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = pair.emoji,
                    fontSize = 32.sp
                )

                if (pair.emojiDesc.isNotEmpty()) {
                    Text(
                        text = pair.emojiDesc,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Beneficio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pair.benefitText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = pair.benefitEmoji,
                    fontSize = 24.sp
                )
            }

            // Botón editar
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFFE67E22),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Botón eliminar
            onDelete?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PairEditDialog(
    pair: MemoryPairAdmin?,
    onDismiss: () -> Unit,
    onSave: (MemoryPairAdmin) -> Unit
) {
    var emoji by remember { mutableStateOf(pair?.emoji ?: "") }
    var emojiDesc by remember { mutableStateOf(pair?.emojiDesc ?: "") }  // 🔥 NUEVO
    var benefitText by remember { mutableStateOf(pair?.benefitText ?: "") }
    var benefitEmoji by remember { mutableStateOf(pair?.benefitEmoji ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (pair == null) "Agregar Par" else "Editar Par")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),  // 🔥 Hacer scrolleable
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sección: Alimento
                Text(
                    text = "🍎 Alimento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it },
                    label = { Text("Emoji") },
                    placeholder = { Text("🍎") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 🔥 NUEVO: Campo para descripción del emoji
                OutlinedTextField(
                    value = emojiDesc,
                    onValueChange = { emojiDesc = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Manzana") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            "Nombre del alimento que aparecerá en la carta",
                            fontSize = 11.sp
                        )
                    }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Sección: Beneficio
                Text(
                    text = "✨ Beneficio",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                OutlinedTextField(
                    value = benefitText,
                    onValueChange = { benefitText = it },
                    label = { Text("Texto del beneficio") },
                    placeholder = { Text("Corazón") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = benefitEmoji,
                    onValueChange = { benefitEmoji = it },
                    label = { Text("Emoji del beneficio") },
                    placeholder = { Text("❤️") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (emoji.isNotEmpty() &&
                        emojiDesc.isNotEmpty() &&
                        benefitText.isNotEmpty() &&
                        benefitEmoji.isNotEmpty()) {
                        onSave(MemoryPairAdmin(
                            emoji = emoji,
                            emojiDesc = emojiDesc,
                            benefitText = benefitText,
                            benefitEmoji = benefitEmoji
                        ))
                    }
                },
                enabled = emoji.isNotEmpty() &&
                        emojiDesc.isNotEmpty() &&
                        benefitText.isNotEmpty() &&
                        benefitEmoji.isNotEmpty()
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