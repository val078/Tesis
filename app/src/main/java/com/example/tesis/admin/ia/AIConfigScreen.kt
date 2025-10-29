package com.example.tesis.admin.ia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.admin.ia.AIConfigViewModel
import java.text.SimpleDateFormat
import androidx.compose.animation.*
import androidx.compose.foundation.background
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigScreen(
    navController: NavController,
    viewModel: AIConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var editedPrompt by remember(config.systemPrompt) {
        mutableStateOf(config.systemPrompt)
    }
    var isEnabled by remember(config.enabled) {
        mutableStateOf(config.enabled)
    }
    var maxLength by remember(config.maxResponseLength) {
        mutableStateOf(config.maxResponseLength.toString())
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // ⭐ NUEVO: Detectar si hay cambios
    val hasChanges = remember(editedPrompt, isEnabled, maxLength, config) {
        editedPrompt != config.systemPrompt ||
                isEnabled != config.enabled ||
                maxLength != config.maxResponseLength.toString()
    }

    LaunchedEffect(Unit) {
        viewModel.loadConfig()
        viewModel.loadLogs()
    }

    LaunchedEffect(config) {
        editedPrompt = config.systemPrompt
        isEnabled = config.enabled
        maxLength = config.maxResponseLength.toString()
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showSaveDialog = true
            viewModel.resetSaveSuccess()
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("✅ Guardado") },
            text = { Text("La configuración de IA se guardó correctamente.") },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
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
                        "Configuración de IA",
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE67E22))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFF8F0))
                ) {
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("⚙️ Configuración") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("📊 Historial") }
                        )
                    }

                    when (selectedTab) {
                        0 -> ConfigTab(
                            editedPrompt = editedPrompt,
                            onPromptChange = { editedPrompt = it },
                            isEnabled = isEnabled,
                            onEnabledChange = { isEnabled = it },
                            maxLength = maxLength,
                            onMaxLengthChange = { maxLength = it },
                            lastUpdated = config.lastUpdated,
                            updatedBy = config.updatedBy
                        )
                        1 -> LogsTab(logs = logs)
                    }
                }

                // ⭐ NUEVO: Botón flotante de guardar (solo visible si hay cambios)
                AnimatedVisibility(
                    visible = hasChanges && selectedTab == 0,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Button(
                        onClick = {
                            val newConfig = config.copy(
                                systemPrompt = editedPrompt,
                                enabled = isEnabled,
                                maxResponseLength = maxLength.toIntOrNull() ?: 200
                            )
                            viewModel.saveConfig(newConfig)
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Guardar Cambios",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigTab(
    editedPrompt: String,
    onPromptChange: (String) -> Unit,
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    maxLength: String,
    onMaxLengthChange: (String) -> Unit,
    lastUpdated: com.google.firebase.Timestamp,
    updatedBy: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp // ⭐ Espacio extra para el botón flotante
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ⭐ NUEVO: Banner de estado
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled)
                        Color(0xFFE8F5E9)
                    else
                        Color(0xFFFFEBEE)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnabled) "✅" else "⚠️",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEnabled)
                                "IA Activa"
                            else
                                "IA Desactivada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled)
                                Color(0xFF2E7D32)
                            else
                                Color(0xFFC62828)
                        )
                        Text(
                            text = if (isEnabled)
                                "Los usuarios están recibiendo recomendaciones"
                            else
                                "Los usuarios no recibirán recomendaciones",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estado de la IA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Activar o desactivar las recomendaciones",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF81C784)
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Longitud Máxima de Respuesta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cantidad máxima de caracteres en la respuesta",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = maxLength,
                        onValueChange = onMaxLengthChange,
                        label = { Text("Caracteres") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = "Recomendado: 150-300 caracteres",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prompt del Sistema",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Instrucciones base que guían el comportamiento de la IA",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedPrompt,
                        onValueChange = onPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        label = { Text("Instrucciones para la IA") },
                        placeholder = { Text("Escribe las instrucciones aquí...") },
                        supportingText = {
                            Text(
                                text = "${editedPrompt.length} caracteres",
                                fontSize = 11.sp,
                                color = if (editedPrompt.length > 1000)
                                    Color(0xFFE67E22)
                                else
                                    Color.Gray
                            )
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Última Actualización",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    .format(lastUpdated.toDate()),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (updatedBy.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Actualizado por:",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = updatedBy,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTab(logs: List<com.example.tesis.admin.ia.AIRecommendationLog>) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📊",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay registros aún",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Los registros aparecerán cuando los usuarios usen la IA",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { log ->
                LogItem(log)
            }
        }
    }
}

@Composable
private fun LogItem(log: com.example.tesis.admin.ia.AIRecommendationLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF8B5E3C)
                )
                Text(
                    text = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        .format(log.timestamp.toDate()),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "📝 Entrada del usuario:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.userInput,
                fontSize = 13.sp,
                color = Color(0xFF5D4037)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🤖 Respuesta de la IA:",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3E5F5)
                )
            ) {
                Text(
                    text = log.aiResponse,
                    fontSize = 13.sp,
                    color = Color(0xFF4A148C),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}