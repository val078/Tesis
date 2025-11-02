package com.example.tesis.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tesis.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val coroutineScope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }  // NUEVO
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }  // NUEVO
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    var showDialogSuccess by remember { mutableStateOf(false) }
    var showDialogError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }  // NUEVO

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,  // Respeta barras del sistema
        topBar = {
            TopAppBar(
                title = { Text("Cambiar Contraseña", color = Color(0xFF8B5E3C), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color(0xFF8B5E3C))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8F0),
                            Color(0xFFFFF0E6),
                            Color(0xFFFFE4CC)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    "Por tu seguridad, verifica tu identidad y crea una nueva contraseña.",
                    color = Color(0xFF5D4037),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                // NUEVO: Campo de contraseña actual
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Contraseña actual") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFFF6B35)) },
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible)
                                    androidx.compose.material.icons.Icons.Filled.Visibility
                                else
                                    androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    },
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Campo de nueva contraseña
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva contraseña") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFFF6B35)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    androidx.compose.material.icons.Icons.Filled.Visibility
                                else
                                    androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                // Confirmar contraseña
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar nueva contraseña") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFFFF6B35)) },
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible)
                                    androidx.compose.material.icons.Icons.Filled.Visibility
                                else
                                    androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        when {
                            currentPassword.isBlank() -> {
                                showDialogError = "Por favor, ingresa tu contraseña actual."
                            }
                            newPassword.isBlank() || confirmPassword.isBlank() -> {
                                showDialogError = "Por favor, completa ambos campos de la nueva contraseña."
                            }
                            newPassword.length < 6 -> {
                                showDialogError = "La contraseña debe tener al menos 6 caracteres."
                            }
                            newPassword != confirmPassword -> {
                                showDialogError = "Las contraseñas no coinciden."
                            }
                            newPassword == currentPassword -> {
                                showDialogError = "La nueva contraseña debe ser diferente a la actual."
                            }
                            else -> {
                                isLoading = true
                                coroutineScope.launch {
                                    authViewModel.changePasswordWithReauth(
                                        currentPassword = currentPassword,
                                        newPassword = newPassword
                                    ) { success, error ->
                                        isLoading = false
                                        if (success) {
                                            showDialogSuccess = true
                                        } else {
                                            showDialogError = error ?: "Ocurrió un error al cambiar la contraseña."
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Guardar nueva contraseña",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (showDialogSuccess) {
                AlertDialog(
                    onDismissRequest = {
                        showDialogSuccess = false
                        navController.popBackStack()
                    },
                    title = { Text("Contraseña actualizada", fontWeight = FontWeight.Bold) },
                    text = { Text("Tu contraseña se cambió correctamente.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDialogSuccess = false
                            navController.popBackStack()
                        }) {
                            Text("OK", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    },
                    icon = { Text("✅", fontSize = 48.sp) }
                )
            }

            showDialogError?.let { msg ->
                AlertDialog(
                    onDismissRequest = { showDialogError = null },
                    title = { Text("Error", fontWeight = FontWeight.Bold) },
                    text = { Text(msg) },
                    confirmButton = {
                        TextButton(onClick = { showDialogError = null }) {
                            Text("OK", color = Color(0xFFFF6B35), fontWeight = FontWeight.Bold)
                        }
                    },
                    icon = { Text("⚠️", fontSize = 48.sp) }
                )
            }
        }
    }
}