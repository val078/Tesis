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
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FoodItemAdmin(
    val emoji: String = "",
    val type: String = "HEALTHY", // "HEALTHY" o "JUNK"
    val name: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragDropAdminScreen(navController: NavController) {
    var foods by remember { mutableStateOf<List<FoodItemAdmin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<Pair<Int, FoodItemAdmin>?>(null) } // ⭐ Eliminado showAddDialog
    var hasChanges by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

    // Cargar alimentos al inicio
    LaunchedEffect(Unit) {
        try {
            Log.d("DragDropAdmin", "🔄 Cargando alimentos...")
            val doc = firestore.collection("games")
                .document("dragAndDrop")
                .get()
                .await()

            if (doc.exists()) {
                val foodsList = doc.get("foods") as? List<Map<String, Any>> ?: emptyList()
                foods = foodsList.map { foodMap ->
                    FoodItemAdmin(
                        emoji = foodMap["emoji"] as? String ?: "❓",
                        type = foodMap["type"] as? String ?: "HEALTHY",
                        name = foodMap["name"] as? String ?: "Desconocido"
                    )
                }
                Log.d("DragDropAdmin", "✅ ${foods.size} alimentos cargados")
            }
        } catch (e: Exception) {
            Log.e("DragDropAdmin", "❌ Error al cargar", e)
        } finally {
            isLoading = false
        }
    }

    // Diálogo de éxito
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
                        "Arrastra y Suelta - Alimentos",
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
                                    text = "${foods.size} alimentos configurados",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "Saludables: ${foods.count { it.type == "HEALTHY" }} | Chatarra: ${foods.count { it.type == "JUNK" }}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Lista de alimentos
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(foods) { index, food ->
                            FoodItemCard(
                                food = food,
                                onEdit = {
                                    editingFood = Pair(index, food)
                                }
                                // ⭐ Eliminado onDelete
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // ⭐ ELIMINADO: Botón flotante de agregar

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
                                    Log.d("DragDropAdmin", "💾 Guardando ${foods.size} alimentos...")

                                    val foodsMap = foods.map { food ->
                                        mapOf(
                                            "emoji" to food.emoji,
                                            "type" to food.type,
                                            "name" to food.name
                                        )
                                    }

                                    firestore.collection("games")
                                        .document("dragAndDrop")
                                        .set(mapOf(
                                            "foods" to foodsMap,
                                            "updatedAt" to Timestamp.now(),
                                            "updatedBy" to "admin"
                                        ))
                                        .await()

                                    Log.d("DragDropAdmin", "✅ Guardado exitoso")
                                    hasChanges = false
                                    showSuccessDialog = true
                                } catch (e: Exception) {
                                    Log.e("DragDropAdmin", "❌ Error al guardar", e)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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

        // ⭐ Diálogo SOLO para editar (no agregar)
        editingFood?.let { (index, food) ->
            FoodEditDialog(
                food = food,
                onDismiss = {
                    editingFood = null
                },
                onSave = { newFood ->
                    foods = foods.toMutableList().apply {
                        set(index, newFood)
                    }
                    hasChanges = true
                    editingFood = null
                }
            )
        }
    }
}

@Composable
private fun FoodItemCard(
    food: FoodItemAdmin,
    onEdit: () -> Unit
    // ⭐ Eliminado parámetro onDelete
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            // Emoji
            Text(
                text = food.emoji,
                fontSize = 32.sp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = if (food.type == "HEALTHY")
                                Color(0xFFE8F5E9)
                            else
                                Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (food.type == "HEALTHY") "Saludable" else "Chatarra",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (food.type == "HEALTHY")
                            Color(0xFF2E7D32)
                        else
                            Color(0xFFC62828)
                    )
                }
            }

            // ⭐ SOLO botón editar
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFFE67E22)
                )
            }

            // ⭐ ELIMINADO: Botón eliminar
        }
    }
}

@Composable
private fun FoodEditDialog(
    food: FoodItemAdmin?,
    onDismiss: () -> Unit,
    onSave: (FoodItemAdmin) -> Unit
) {
    var emoji by remember { mutableStateOf(food?.emoji ?: "") }
    var name by remember { mutableStateOf(food?.name ?: "") }
    var type by remember { mutableStateOf(food?.type ?: "HEALTHY") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Editar Alimento") // ⭐ Solo "Editar", no "Agregar"
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

                // Selector de tipo
                Column {
                    Text("Tipo:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = type == "HEALTHY",
                            onClick = { type = "HEALTHY" },
                            label = { Text("Saludable") },
                            leadingIcon = { Text("✅") }
                        )
                        FilterChip(
                            selected = type == "JUNK",
                            onClick = { type = "JUNK" },
                            label = { Text("Chatarra") },
                            leadingIcon = { Text("❌") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (emoji.isNotEmpty() && name.isNotEmpty()) {
                        onSave(FoodItemAdmin(emoji, type, name))
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