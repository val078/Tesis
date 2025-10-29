package com.example.tesis.admin.config

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    navController: NavController,
    viewModel: AppConfigViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var appVersion by remember(config.appVersion) {
        mutableStateOf(config.appVersion)
    }
    var maintenanceMode by remember(config.maintenanceMode) {
        mutableStateOf(config.maintenanceMode)
    }
    var maintenanceMessage by remember(config.maintenanceMessage) {
        mutableStateOf(config.maintenanceMessage)
    }
    var allowNewRegistrations by remember(config.allowNewRegistrations) {
        mutableStateOf(config.allowNewRegistrations)
    }

    var showSaveDialog by remember { mutableStateOf(false) }

    // Detectar cambios
    val hasChanges = remember(appVersion, maintenanceMode, maintenanceMessage, allowNewRegistrations, config) {
        appVersion != config.appVersion ||
                maintenanceMode != config.maintenanceMode ||
                maintenanceMessage != config.maintenanceMessage ||
                allowNewRegistrations != config.allowNewRegistrations
    }

    LaunchedEffect(Unit) {
        viewModel.loadConfig()
    }

    LaunchedEffect(config) {
        appVersion = config.appVersion
        maintenanceMode = config.maintenanceMode
        maintenanceMessage = config.maintenanceMessage
        allowNewRegistrations = config.allowNewRegistrations
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
            text = { Text("La configuración se guardó correctamente.") },
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
                        "Configuración General",
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
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Banner de estado
                    StatusBanner(
                        maintenanceMode = maintenanceMode,
                        allowNewRegistrations = allowNewRegistrations
                    )

                    // Versión de la app
                    VersionCard(
                        version = appVersion,
                        onVersionChange = { appVersion = it }
                    )

                    // Modo de mantenimiento
                    MaintenanceModeCard(
                        enabled = maintenanceMode,
                        onToggle = { maintenanceMode = it },
                        message = maintenanceMessage,
                        onMessageChange = { maintenanceMessage = it }
                    )

                    // Registros nuevos
                    RegistrationsCard(
                        enabled = allowNewRegistrations,
                        onToggle = { allowNewRegistrations = it }
                    )

                    // Info de última actualización
                    LastUpdateCard(
                        lastUpdated = config.lastUpdated,
                        updatedBy = config.updatedBy
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Botón flotante de guardar
                AnimatedVisibility(
                    visible = hasChanges,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Button(
                        onClick = {
                            val newConfig = config.copy(
                                appVersion = appVersion,
                                maintenanceMode = maintenanceMode,
                                maintenanceMessage = maintenanceMessage,
                                allowNewRegistrations = allowNewRegistrations
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
private fun StatusBanner(
    maintenanceMode: Boolean,
    allowNewRegistrations: Boolean
) {
    val backgroundColor = when {
        maintenanceMode -> Color(0xFFFFEBEE)
        !allowNewRegistrations -> Color(0xFFFFF3E0)
        else -> Color(0xFFE8F5E9)
    }

    val iconColor = when {
        maintenanceMode -> Color(0xFFC62828)
        !allowNewRegistrations -> Color(0xFFE67E22)
        else -> Color(0xFF2E7D32)
    }

    val statusText = when {
        maintenanceMode -> "⚠️ Modo Mantenimiento Activo"
        !allowNewRegistrations -> "🔒 Registros Cerrados"
        else -> "✅ App Funcionando Normal"
    }

    val description = when {
        maintenanceMode -> "Los usuarios no pueden acceder a la app"
        !allowNewRegistrations -> "No se permiten nuevos registros"
        else -> "Todos los servicios operativos"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (maintenanceMode || !allowNewRegistrations)
                    Icons.Default.Warning
                else
                    Icons.Default.CheckCircle,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun VersionCard(
    version: String,
    onVersionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Versión de la App",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Versión actual mostrada a los usuarios",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = version,
                onValueChange = onVersionChange,
                label = { Text("Versión") },
                placeholder = { Text("ej: 1.0.0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun MaintenanceModeCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // ⭐ CAMBIO: peso para no recortar
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) { // ⭐ CAMBIO
                        Text(
                            text = "Modo Mantenimiento",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1 // ⭐ CAMBIO: limitar a 1 línea
                        )
                        Text(
                            text = "Bloquea el acceso a la app",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2 // ⭐ CAMBIO: máximo 2 líneas
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp)) // ⭐ NUEVO: espacio
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFF9800),
                        checkedTrackColor = Color(0xFFFFB74D)
                    )
                )
            }
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text("Mensaje de Mantenimiento") },
                    placeholder = { Text("Escribe el mensaje que verán los usuarios...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${message.length} caracteres",
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RegistrationsCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // ⭐ CAMBIO: peso para no recortar
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) { // ⭐ CAMBIO
                        Text(
                            text = "Permitir Registros Nuevos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2 // ⭐ CAMBIO: hasta 2 líneas si es necesario
                        )
                        Text(
                            text = if (enabled)
                                "Los usuarios pueden crear cuentas"
                            else
                                "No se permiten nuevos registros",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 2 // ⭐ CAMBIO: máximo 2 líneas
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp)) // ⭐ NUEVO: espacio
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        checkedTrackColor = Color(0xFF81C784)
                    )
                )
            }
        }
    }
}

@Composable
private fun LastUpdateCard(
    lastUpdated: com.google.firebase.Timestamp,
    updatedBy: String
) {
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