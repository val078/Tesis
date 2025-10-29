package com.example.tesis.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    var emailOrUsername by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var maintenanceMessage by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState?.success == true) {
            kotlinx.coroutines.delay(200)
            val user = authViewModel.currentUser.value
            if (user?.role == "admin") {
                navController.navigate("admin_home") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        } else if (authState != null) {
            errorMessage = authState?.message
        }
    }

    DisposableEffect(emailOrUsername, password) {
        errorMessage = null
        onDispose { }
    }

    if (showMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceDialog = false },
            icon = {
                Text("🔧", fontSize = 48.sp)
            },
            title = {
                Text(
                    "Modo Mantenimiento",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        maintenanceMessage,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Solo los administradores pueden acceder durante el mantenimiento.",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMaintenanceDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
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
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                LoginHeader()
                Spacer(modifier = Modifier.height(40.dp))
                LoginFormCard(
                    emailOrUsername = emailOrUsername,
                    password = password,
                    passwordVisible = passwordVisible,
                    onEmailOrUsernameChange = { emailOrUsername = it },
                    onPasswordChange = { password = it },
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    isLoading = isLoading,
                    onLoginClick = {
                        // ⭐ NUEVO: Verificar mantenimiento antes de hacer login
                        kotlinx.coroutines.GlobalScope.launch {
                            val isMaintenanceMode = authViewModel.checkMaintenanceMode()

                            if (isMaintenanceMode) {
                                // ⭐ Intentar hacer login primero para verificar si es admin
                                authViewModel.loginUser(emailOrUsername, password)

                                // Esperar un poco a que se complete el login
                                kotlinx.coroutines.delay(2000)

                                val user = authViewModel.currentUser.value

                                // Si NO es admin, cerrar sesión y mostrar mensaje
                                if (user != null && user.role != "admin") {
                                    authViewModel.logout()
                                    maintenanceMessage = authViewModel.getMaintenanceMessage()
                                    showMaintenanceDialog = true
                                }
                                // Si es admin, el LaunchedEffect lo redirigirá automáticamente
                            } else {
                                // Sin mantenimiento, login normal
                                authViewModel.loginUser(emailOrUsername, password)
                            }
                        }
                    },
                    onForgotPasswordClick = { navController.navigate("forgot_password") },
                    onRegisterClick = { navController.navigate("register") },
                    errorMessage = errorMessage
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0xFFFF8C42).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B35),
                            Color(0xFFFF8C42)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🍊",
                    fontSize = 48.sp
                )
                Text(
                    text = "NutriPro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Bienvenido a NutriPro!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B4513),
            textAlign = TextAlign.Center,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Diviértete mientras te alimentas bien",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoginFormCard(
    emailOrUsername: String,
    password: String,
    passwordVisible: Boolean,
    onEmailOrUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    errorMessage: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = true
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFF8F0).copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFCDD2)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Error",
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = Color(0xFFF44336),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = emailOrUsername,
                    onValueChange = onEmailOrUsernameChange,
                    label = {
                        Text("Usuario o correo electrónico")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Person icon",
                            tint = if (emailOrUsername.isNotEmpty()) Color(0xFFFF6B35) else Color(0xFFBBBBBB)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        cursorColor = Color(0xFFFF6B35),
                        focusedLabelColor = Color(0xFFFF6B35),
                        unfocusedLabelColor = Color(0xFF999999)
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = {
                        Text("Contraseña")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Lock icon",
                            tint = if (password.isNotEmpty()) Color(0xFFFF6B35) else Color(0xFFBBBBBB)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onPasswordVisibilityToggle) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        cursorColor = Color(0xFFFF6B35),
                        focusedLabelColor = Color(0xFFFF6B35),
                        unfocusedLabelColor = Color(0xFF999999)
                    )
                )

                MainLoginButton(
                    enabled = emailOrUsername.isNotEmpty() && password.isNotEmpty() && !isLoading,
                    loading = isLoading,
                    onClick = onLoginClick
                )

                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        color = Color(0xFFFF6B35),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )
                    Text(
                        text = "o",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = Color(0xFFFF8C42),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Divider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFE0E0E0),
                        thickness = 1.dp
                    )
                }

                Text(
                    text = "¿Aún no tienes cuenta?",
                    color = Color(0xFF666666),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                CreateAccountButton(
                    onClick = onRegisterClick
                )
            }
        }
    }
}

@Composable
private fun MainLoginButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0xFFFF6B35).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFFFF8C7A) else Color(0xFFFFBBB3),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFFFBBB3),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Iniciando...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "¡Vamos a aprender!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun CreateAccountButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFFF6B35)
        ),
        border = BorderStroke(2.dp, Color(0xFFFF6B35))
    ) {
        Text(
            text = "¡Crea tu cuenta!",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}