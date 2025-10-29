// ui/screens/auth/ForgotPasswordScreen.kt
package com.example.tesis.ui.screens.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tesis.R
import com.example.tesis.ui.components.VerificationCodeInput
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.LightOrange
import com.example.tesis.ui.theme.CoralRed
import com.example.tesis.ui.theme.PinkOrange
import com.example.tesis.ui.theme.BackgroundLight
import com.example.tesis.ui.theme.BackgroundWhite
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextDark
import com.example.tesis.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showCodeScreen by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            // Icono de recuperación
            RecoveryIcon()

            Spacer(modifier = Modifier.height(20.dp))

            // Mostrar la pantalla correspondiente
            if (showCodeScreen) {
                VerificationCodeScreen(
                    email = email,
                    verificationCode = verificationCode,
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onCodeChange = { verificationCode = it },
                    onNewPasswordChange = { newPassword = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onVerifyCode = { code ->
                        // Simular verificación del código
                        if (code == "1234567") { // Código de ejemplo
                            showSuccess = true
                        } else {
                            errorMessage = "Código incorrecto. Intenta de nuevo."
                        }
                    },
                    onResendCode = {
                        if (timeLeft == 0) {
                            timeLeft = 20
                            coroutineScope.launch {
                                while (timeLeft > 0) {
                                    delay(1000L)
                                    timeLeft--
                                }
                            }
                            errorMessage = "¡Código reenviado! Revisa tu correo."
                        }
                    },
                    timeLeft = timeLeft,
                    errorMessage = errorMessage,
                    isLoading = isLoading,
                    showSuccess = showSuccess,
                    onBackToLogin = { navController.navigate("login") }
                )
            } else {
                EmailInputScreen(
                    email = email,
                    onEmailChange = { email = it },
                    isLoading = isLoading,
                    onSendCode = {
                        if (email.isNotEmpty()) {
                            isLoading = true
                            isLoading = false
                            showCodeScreen = true
                            timeLeft = 20
                            coroutineScope.launch {
                                while (timeLeft > 0) {
                                    delay(1000L)
                                    timeLeft--
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecoveryIcon() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LightOrange,
                        PrimaryOrange
                    )
                )
            )
            .border(4.dp, CoralRed, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email icon",
            modifier = Modifier.size(60.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun EmailInputScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onSendCode: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "¿Olvidaste tu contraseña?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "No te preocupes, ¡te ayudamos a recuperarla!",
            fontSize = 16.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Ingresa tu correo electrónico y te enviaremos un código de verificación de 7 dígitos.",
            fontSize = 14.sp,
            color = TextDark,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = {
                Text("Correo electrónico")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = "Email icon",
                    tint = if (email.isNotEmpty()) DarkOrange else TextGray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSendCode,
            enabled = email.isNotEmpty() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (email.isNotEmpty() && !isLoading) PrimaryOrange else PrimaryOrange.copy(alpha = 0.5f),
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
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
                        "Enviando...",
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
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Enviar código",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
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
                    "El código será enviado a tu correo. Revisa tu bandeja de entrada o spam.",
                    fontSize = 12.sp,
                    color = TextGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun VerificationCodeScreen(
    email: String,
    verificationCode: String,
    newPassword: String,
    confirmPassword: String,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onVerifyCode: (String) -> Unit,
    onResendCode: () -> Unit,
    timeLeft: Int,
    errorMessage: String,
    isLoading: Boolean,
    showSuccess: Boolean,
    onBackToLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Verifica tu correo",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Te enviamos un código a:",
            fontSize = 14.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        Text(
            email,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DarkOrange,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (showSuccess) {
            SuccessScreen(onBackToLogin = onBackToLogin)
        } else {
            VerificationForm(
                verificationCode = verificationCode,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                onCodeChange = onCodeChange,
                onNewPasswordChange = onNewPasswordChange,
                onConfirmPasswordChange = onConfirmPasswordChange,
                onVerifyCode = onVerifyCode,
                onResendCode = onResendCode,
                timeLeft = timeLeft,
                errorMessage = errorMessage,
                isLoading = isLoading
            )
        }
    }
}

@Composable
private fun VerificationForm(
    verificationCode: String,
    newPassword: String,
    confirmPassword: String,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onVerifyCode: (String) -> Unit,
    onResendCode: () -> Unit,
    timeLeft: Int,
    errorMessage: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo de código de verificación personalizado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)  // ✅ Padding horizontal mínimo
        ) {
            Text(
                "Código de verificación",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            VerificationCodeInput(
                code = verificationCode,
                onCodeChange = onCodeChange,
                length = 7,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "${verificationCode.length}/7 dígitos",
                fontSize = 12.sp,
                color = if (verificationCode.length == 7) DarkOrange else TextGray
            )
        }

        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = {
                Text("Nueva contraseña")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Lock icon",
                    tint = if (newPassword.isNotEmpty()) DarkOrange else TextGray
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

        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            Text(
                "Las contraseñas no coinciden",
                color = CoralRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (errorMessage.isNotEmpty()) {
            Text(
                errorMessage,
                color = if (errorMessage.contains("correcto")) CoralRed else DarkOrange,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { onVerifyCode(verificationCode) },
            enabled = verificationCode.length == 7 &&
                    newPassword.isNotEmpty() &&
                    confirmPassword.isNotEmpty() &&
                    newPassword == confirmPassword &&
                    !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (verificationCode.length == 7 &&
                    newPassword.isNotEmpty() &&
                    confirmPassword.isNotEmpty() &&
                    newPassword == confirmPassword &&
                    !isLoading) PrimaryOrange else PrimaryOrange.copy(alpha = 0.5f),
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
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
                        "Verificando...",
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
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Verify icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Verificar código",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onResendCode,
            enabled = timeLeft == 0 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (timeLeft == 0) DarkOrange else TextGray
            ),
            border = BorderStroke(2.dp, if (timeLeft == 0) DarkOrange else TextGray.copy(alpha = 0.5f))
        ) {
            if (timeLeft > 0) {
                Text(
                    "Reenviar código en $timeLeft segundos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Reenviar código",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessScreen(onBackToLogin: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(LightOrange.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Success icon",
                modifier = Modifier.size(60.dp),
                tint = DarkOrange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "¡Contraseña actualizada!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkOrange,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Tu contraseña ha sido cambiada exitosamente.",
            fontSize = 16.sp,
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryOrange,
                contentColor = Color.White
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Login icon",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Iniciar sesión",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}