// screens/UserSettingsScreen.kt
package com.example.tesis.ui.screens.drawer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.data.model.UserSettingsViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(
    navController: NavController,
    viewModel: UserSettingsViewModel = viewModel()
) {
    val settings by viewModel.notificationSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var showTimePickerFor by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var isInitialLoad by remember { mutableStateOf(true) }

    val hasChanges = remember(settings) {
        true
    }

    LaunchedEffect(Unit) {
        isInitialLoad = false
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess && !isInitialLoad) {
            showSaveDialog = true
            viewModel.resetSaveSuccess()
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("✅ Guardado") },
            text = { Text("Tu configuración se guardó correctamente.") },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("⚠️ Cerrar Sesión") },
            text = { Text("¿Estás seguro de que quieres cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                ) {
                    Text("Sí, cerrar sesión", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuración",
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFF8F0))
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Banner de estado
                    item {
                        StatusBanner(
                            enabled = settings.enabled,
                            onToggle = {
                                viewModel.toggleAllNotifications(it)
                                // 🔥 Si se apaga el switch principal, apaga todos los demás
                                if (!it) {
                                    viewModel.updateBreakfastEnabled(false)
                                    viewModel.updateLunchEnabled(false)
                                    viewModel.updateSnackEnabled(false)
                                    viewModel.updatePlayEnabled(false)
                                }
                            }
                        )
                    }

                    // Sección: Notificaciones de Comidas
                    item {
                        SectionHeader(
                            icon = "🍽️",
                            title = "Recordatorios de Comidas"
                        )
                    }

                    // Desayuno
                    item {
                        NotificationCard(
                            emoji = "☀️",
                            title = "Desayuno",
                            subtitle = "Te recordaremos escribir qué desayunaste",
                            time = settings.breakfastTime,
                            enabled = settings.breakfastEnabled && settings.enabled, // 🔥 Depende del switch principal
                            onEnabledChange = { viewModel.updateBreakfastEnabled(it) },
                            onTimeClick = { showTimePickerFor = "breakfast" },
                            isDisabled = !settings.enabled // 🔥 Deshabilita si el principal está OFF
                        )
                    }

                    // Almuerzo
                    item {
                        NotificationCard(
                            emoji = "🌞",
                            title = "Almuerzo",
                            subtitle = "Te recordaremos escribir qué almorzaste",
                            time = settings.lunchTime,
                            enabled = settings.lunchEnabled && settings.enabled,
                            onEnabledChange = { viewModel.updateLunchEnabled(it) },
                            onTimeClick = { showTimePickerFor = "lunch" },
                            isDisabled = !settings.enabled
                        )
                    }

                    // Merienda/Cena
                    item {
                        NotificationCard(
                            emoji = "🌙",
                            title = "Merienda",
                            subtitle = "Te recordaremos escribir qué cenaste",
                            time = settings.snackTime,
                            enabled = settings.snackEnabled && settings.enabled,
                            onEnabledChange = { viewModel.updateSnackEnabled(it) },
                            onTimeClick = { showTimePickerFor = "snack" },
                            isDisabled = !settings.enabled
                        )
                    }

                    // Sección: Juegos
                    item {
                        SectionHeader(
                            icon = "🎮",
                            title = "Recordatorios de Juegos"
                        )
                    }

                    // Jugar
                    item {
                        NotificationCard(
                            emoji = "🎮",
                            title = "Hora de Jugar",
                            subtitle = "Te recordaremos jugar y aprender",
                            time = settings.playTime,
                            enabled = settings.playEnabled && settings.enabled,
                            onEnabledChange = { viewModel.updatePlayEnabled(it) },
                            onTimeClick = { showTimePickerFor = "play" },
                            isDisabled = !settings.enabled
                        )
                    }

                    // Sección: Cuenta
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            icon = "👤",
                            title = "Cuenta"
                        )
                    }

                    // Cambiar contraseña
                    item {
                        ActionCard(
                            icon = Icons.Default.Lock,
                            title = "Cambiar Contraseña",
                            onClick = { navController.navigate("change_password") }
                        )
                    }
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
                        onClick = { viewModel.saveSettings(settings) },
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
                                text = "Guardar Configuración",
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

    // Time Picker Dialog
    showTimePickerFor?.let { type ->
        TimePickerDialog(
            initialTime = when (type) {
                "breakfast" -> settings.breakfastTime
                "lunch" -> settings.lunchTime
                "snack" -> settings.snackTime
                "play" -> settings.playTime
                else -> "12:00"
            },
            onDismiss = { showTimePickerFor = null },
            onTimeSelected = { time ->
                when (type) {
                    "breakfast" -> viewModel.updateBreakfastTime(time)
                    "lunch" -> viewModel.updateLunchTime(time)
                    "snack" -> viewModel.updateSnackTime(time)
                    "play" -> viewModel.updatePlayTime(time)
                }
                showTimePickerFor = null
            }
        )
    }
}

@Composable
private fun StatusBanner(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (enabled) "🔔" else "🔕",
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (enabled) "Notificaciones Activas" else "Notificaciones Desactivadas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        text = if (enabled) "Recibirás recordatorios" else "No recibirás recordatorios",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
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

@Composable
private fun SectionHeader(
    icon: String,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037)
        )
    }
}

@Composable
private fun NotificationCard(
    emoji: String,
    title: String,
    subtitle: String,
    time: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
    isDisabled: Boolean = false // 🔥 Nuevo parámetro
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) Color(0xFFF5F5F5) else Color.White
        ),
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        color = if (isDisabled) Color.Gray.copy(alpha = 0.5f) else Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDisabled) Color.Gray else Color.Unspecified
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = !isDisabled, // 🔥 Deshabilita el switch
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4CAF50),
                        disabledCheckedThumbColor = Color.Gray,
                        disabledUncheckedThumbColor = Color.Gray
                    )
                )
            }

            if (enabled && !isDisabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onTimeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hora: $time")
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    textColor: Color = Color(0xFF5D4037),
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val (hour, minute) = initialTime.split(":").map { it.toInt() }
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Hora") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = String.format(
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}