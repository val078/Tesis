// ui/screens/auth/RegisterScreen.kt
package com.example.tesis.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import com.example.tesis.ui.components.CalendarPicker
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tesis.R
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.CoralRed
import com.example.tesis.ui.theme.PinkOrange
import com.example.tesis.ui.theme.BackgroundLight
import com.example.tesis.ui.theme.BackgroundWhite
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextDark
import com.example.tesis.ui.theme.TextGray
import java.text.SimpleDateFormat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.tesis.ui.theme.LightOrange
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tesis.viewmodel.AuthViewModel
import androidx.compose.runtime.collectAsState
import java.util.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.VisualTransformation
import com.example.tesis.utils.KidFriendlyMessages
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(navController: NavController) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } // ⭐ NUEVO
    var confirmPasswordVisible by remember { mutableStateOf(false) } // ⭐ NUEVO
    var birthDate by remember { mutableStateOf<Date?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var parentEmailError by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // ⭐ NUEVO: Estado de registros
    var registrationsAllowed by remember { mutableStateOf(true) }
    var isCheckingRegistrations by remember { mutableStateOf(true) }

    val age = birthDate?.let { calculateAge(it) } ?: 0
    val scope = rememberCoroutineScope()

    // ⭐ NUEVO: Verificar si permite registros
    LaunchedEffect(Unit) {
        registrationsAllowed = viewModel.checkRegistrationsAllowed()
        isCheckingRegistrations = false
    }

    LaunchedEffect(authState) {
        authState?.let { result ->
            if (result.success) {
                showSuccessMessage = true
                errorMessage = "¡Cuenta creada exitosamente!"
                scope.launch {
                    delay(2000)
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            } else {
                errorMessage = result.message
                showSuccessMessage = false
            }
        }
    }

    // ⭐ NUEVO: Si está verificando, mostrar loading
    if (isCheckingRegistrations) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryOrange)
        }
        return
    }

    // ⭐ NUEVO: Si no permite registros, mostrar pantalla especial
    if (!registrationsAllowed) {
        RegistrationsClosedScreen(navController)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundLight,
                        BackgroundWhite
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        if (showCalendar) {
            CalendarPicker(
                onDateSelected = { date ->
                    birthDate = date
                    showCalendar = false
                },
                onDismiss = { showCalendar = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            NutriProLogoPlaceholder()
            Spacer(modifier = Modifier.height(20.dp))
            RegisterTitleSection()
            Spacer(modifier = Modifier.height(24.dp))

            SuccessMessage(showSuccessMessage = showSuccessMessage)

            validationError?.let { message ->
                if (!showSuccessMessage) {
                    ErrorMessage(text = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            errorMessage?.let { message ->
                if (!showSuccessMessage) {
                    ErrorMessage(text = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            parentEmailError?.let { errorKey ->
                if (!showSuccessMessage) {
                    Text(
                        text = KidFriendlyMessages.getValidationErrors(errorKey),
                        color = CoralRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (!showSuccessMessage) {
                RegisterForm(
                    name = name,
                    email = email,
                    parentEmail = parentEmail,
                    password = password,
                    confirmPassword = confirmPassword,
                    passwordVisible = passwordVisible, // ⭐ NUEVO
                    confirmPasswordVisible = confirmPasswordVisible, // ⭐ NUEVO
                    birthDate = birthDate,
                    age = age,
                    onNameChange = { name = it },
                    onEmailChange = { email = it },
                    onParentEmailChange = { parentEmail = it },
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }, // ⭐ NUEVO
                    onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible }, // ⭐ NUEVO
                    onShowCalendar = { showCalendar = true },
                    isLoading = isLoading,
                    onValidationError = { validationError = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                RegisterButton(
                    enabled = name.isNotEmpty() &&
                            email.isNotEmpty() &&
                            password.isNotEmpty() &&
                            confirmPassword.isNotEmpty() &&
                            birthDate != null &&
                            password == confirmPassword &&
                            (age > 15 || (age <= 15 && parentEmail.isNotEmpty())) &&
                            !isLoading,
                    loading = isLoading,
                    onClick = {
                        if (password != confirmPassword) {
                            validationError = KidFriendlyMessages.getValidationErrors("passwords_dont_match")
                            return@RegisterButton
                        }
                        validationError = null
                        errorMessage = null
                        viewModel.registerUser(
                            name = name,
                            email = email,
                            password = password,
                            parentEmail = if (age <= 15) parentEmail else null,
                            birthDate = birthDate
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                RegisterOptions(navController)
            }
        }
    }
}

// ⭐ NUEVO: Pantalla de registros cerrados
@Composable
fun RegistrationsClosedScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundLight,
                        BackgroundWhite
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔒",
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Registros Cerrados",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ConchodeVino,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Lo sentimos, no estamos aceptando nuevos registros en este momento. ¡Vuelve pronto!",
                    fontSize = 16.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryOrange
                ),
                border = BorderStroke(3.dp, PrimaryOrange)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Login icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Ir a Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterForm(
    name: String,
    email: String,
    parentEmail: String,
    password: String,
    confirmPassword: String,
    passwordVisible: Boolean, // ⭐ NUEVO
    confirmPasswordVisible: Boolean, // ⭐ NUEVO
    birthDate: Date?,
    age: Int,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onParentEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit, // ⭐ NUEVO
    onConfirmPasswordVisibilityToggle: () -> Unit, // ⭐ NUEVO
    onShowCalendar: () -> Unit,
    isLoading: Boolean,
    onValidationError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = {
                Text("Nombre de usuario")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Person icon",
                    tint = if (name.isNotEmpty()) DarkOrange else TextGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        DateFieldButton(
            selectedDate = birthDate,
            onShowCalendar = onShowCalendar,
            isLoading = isLoading
        )

        if (birthDate != null && age <= 15) {
            OutlinedTextField(
                value = parentEmail,
                onValueChange = onParentEmailChange,
                label = {
                    Text("Correo electrónico parental")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Email icon",
                        tint = if (parentEmail.isNotEmpty()) DarkOrange else TextGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                    cursorColor = PrimaryOrange,
                    focusedLabelColor = DarkOrange,
                    unfocusedLabelColor = TextGray
                )
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = {
                Text("Tu correo electrónico")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Email icon",
                    tint = if (email.isNotEmpty()) DarkOrange else TextGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        // ⭐ NUEVO: Contraseña con ojito
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
                    tint = if (password.isNotEmpty()) DarkOrange else TextGray
                )
            },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Outlined.Visibility
                        else
                            Icons.Outlined.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = DarkOrange
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
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        // ⭐ NUEVO: Confirmar contraseña con ojito
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = {
                Text("Confirmar contraseña")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Check icon",
                    tint = if (confirmPassword.isNotEmpty()) DarkOrange else TextGray
                )
            },
            trailingIcon = {
                IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (confirmPasswordVisible)
                            Icons.Outlined.Visibility
                        else
                            Icons.Outlined.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = DarkOrange
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "La contraseña necesita más caracteres. ¡Al menos 6!",
                color = CoralRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (password.isNotEmpty() && confirmPassword.isNotEmpty() &&
            !KidFriendlyMessages.doPasswordsMatch(password, confirmPassword)) {
            Text(
                text = KidFriendlyMessages.getValidationErrors("passwords_dont_match"),
                color = CoralRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (birthDate != null && age <= 15 && parentEmail.isEmpty()) {
            InfoMessage(text = "El correo parental es requerido")
        }
    }
}


@Composable
private fun SuccessMessage(showSuccessMessage: Boolean) {
    if (showSuccessMessage) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightOrange.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ✅ Check grande y bonito
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(PrimaryOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Éxito",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Mensaje de éxito
                    Text(
                        text = "¡Cuenta creada exitosamente!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkOrange,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Redirigiendo...",
                        fontSize = 14.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ✅ Barra de progreso sencilla
                    LinearProgressIndicator(
                        color = PrimaryOrange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightOrange.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = "Warning icon",
                modifier = Modifier.size(20.dp),
                tint = DarkOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = TextGray,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RegisterForm(
    name: String,
    email: String,
    parentEmail: String,
    password: String,
    confirmPassword: String,
    birthDate: Date?,
    age: Int,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onParentEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onShowCalendar: () -> Unit,
    isLoading: Boolean,
    onValidationError: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nombre completo
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = {
                Text("Nombre de usuario")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Person icon",
                    tint = if (name.isNotEmpty()) DarkOrange else TextGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        DateFieldButton(
            selectedDate = birthDate,
            onShowCalendar = onShowCalendar,
            isLoading = isLoading
        )

        // Campo de correo parental (solo para menores de 16 años)
        if (birthDate != null && age <= 15) {
            OutlinedTextField(
                value = parentEmail,
                onValueChange = onParentEmailChange,
                label = {
                    Text("Correo electrónico parental")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = "Email icon",
                        tint = if (parentEmail.isNotEmpty()) DarkOrange else TextGray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                    cursorColor = PrimaryOrange,
                    focusedLabelColor = DarkOrange,
                    unfocusedLabelColor = TextGray
                )
            )
        }

        // Email personal
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = {
                Text("Tu correo electrónico")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Email icon",
                    tint = if (email.isNotEmpty()) DarkOrange else TextGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        // Contraseña
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
                    tint = if (password.isNotEmpty()) DarkOrange else TextGray
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        // Confirmar contraseña
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = {
                Text("Confirmar contraseña")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Check icon",
                    tint = if (confirmPassword.isNotEmpty()) DarkOrange else TextGray
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray
            )
        )

        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "La contraseña necesita más caracteres. ¡Al menos 6!",
                color = CoralRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Validaciones
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() &&
            !KidFriendlyMessages.doPasswordsMatch(password, confirmPassword)) {
            Text(
                text = KidFriendlyMessages.getValidationErrors("passwords_dont_match"),
                color = CoralRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (birthDate != null && age <= 15 && parentEmail.isEmpty()) {
            InfoMessage(text = "El correo parental es requerido")
        }
    }
}

// ✅ Componente para mensajes informativos
@Composable
private fun InfoMessage(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LightOrange.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info icon",
                modifier = Modifier.size(20.dp),
                tint = DarkOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                color = TextGray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DateFieldButton(
    selectedDate: Date?,
    onShowCalendar: () -> Unit,
    isLoading: Boolean
) {
    val backgroundAlpha = if (isLoading) 0.5f else 1f
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()            // Box ocupa todo el ancho
            .alpha(backgroundAlpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading
            ) {
                onShowCalendar()
            }
    ) {
        OutlinedTextField(
            value = selectedDate?.let { formatDate(it) } ?: "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(), // ✅ Asegura que el TextField ocupe todo el ancho
            label = { Text("Fecha de nacimiento") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = "Calendario",
                    tint = if (selectedDate != null) DarkOrange else TextGray
                )
            },
            readOnly = true,
            enabled = false,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = PinkOrange.copy(alpha = 0.7f),
                disabledBorderColor = PinkOrange.copy(alpha = 0.7f),
                cursorColor = PrimaryOrange,
                focusedLabelColor = DarkOrange,
                unfocusedLabelColor = TextGray,
                disabledLabelColor = TextGray,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun RegisterButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(60.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) PrimaryOrange else PrimaryOrange.copy(alpha = 0.5f),
            contentColor = Color.White,
            disabledContainerColor = PrimaryOrange.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Creando tu cuenta...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add person icon",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "¡Crear mi cuenta!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RegisterOptions(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Separador
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = PinkOrange.copy(alpha = 0.3f),
                thickness = 2.dp
            )
            Text(
                " o ",
                color = PinkOrange,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = PinkOrange.copy(alpha = 0.3f),
                thickness = 2.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ¿Ya tienes cuenta?
        Text(
            "¿Ya tienes cuenta?",
            fontSize = 16.sp,
            color = TextDark,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de login
        OutlinedButton(
            onClick = { navController.navigate("login") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryOrange
            ),
            border = BorderStroke(3.dp, PrimaryOrange)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Login icon",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "¡Inicia sesión!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ✅ Función de validación con mensajes amigables
private fun validateForm(
    password: String,
    confirmPassword: String,
    parentEmail: String,
    age: Int,
    onValidationError: (String) -> Unit
): Boolean {
    return when {
        password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword -> {
            onValidationError(KidFriendlyMessages.getValidationErrors("passwords_dont_match"))
            false
        }
        age <= 15 && parentEmail.isEmpty() -> {
            onValidationError("El correo parental es requerido para menores de 16 años")
            false
        }
        else -> true
    }
}

// Funciones auxiliares
private fun calculateAge(birthDate: Date): Int {
    val calendar = Calendar.getInstance()
    calendar.time = birthDate
    val birthYear = calendar.get(Calendar.YEAR)
    val birthMonth = calendar.get(Calendar.MONTH)
    val birthDay = calendar.get(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val currentYear = today.get(Calendar.YEAR)
    val currentMonth = today.get(Calendar.MONTH)
    val currentDay = today.get(Calendar.DAY_OF_MONTH)

    var age = currentYear - birthYear
    if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
        age--
    }

    return age
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(date)
}

@Composable
private fun RegisterTitleSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            "¡Crea tu cuenta NutriPro!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Únete a la aventura nutricional",
            fontSize = 16.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun NutriProLogoPlaceholder() {
    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryOrange,
                        DarkOrange
                    )
                )
            )
            .border(4.dp, CoralRed, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🍊", fontSize = 56.sp)
            Text(
                "NutriPro",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}