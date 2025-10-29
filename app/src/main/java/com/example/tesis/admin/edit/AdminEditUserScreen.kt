package com.example.tesis.admin.edit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.admin.user.AdminUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditUserScreen(
    navController: NavController,
    userId: String,
    viewModel: AdminUsersViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()

    // ✅ Obtener usuario de la lista
    val user = remember(users, userId) {
        users.find { it.userId == userId }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }

    // ✅ Inicializar campos cuando se carga el usuario
    LaunchedEffect(user) {
        user?.let {
            name = it.name
            email = it.email
            parentEmail = it.parentEmail ?: ""
        }
    }

    // ✅ Si no hay usuario, cargar
    LaunchedEffect(userId) {
        if (users.isEmpty()) {
            viewModel.loadUsers()
        }
    }

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE67E22))
        }
        return
    }

    // ✅ Verificar si hay cambios
    val hasChanges by remember {
        derivedStateOf {
            name != user.name ||
                    parentEmail != (user.parentEmail ?: "")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editar Usuario",
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
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8F0),
                            Color(0xFFFFE4CC),
                            Color(0xFFFFD4A8).copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // ✅ Info del usuario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Información del Usuario",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tipo:",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (user.isChild()) "Niño/a (${user.age} años)" else "Adulto (${user.age} años)",
                                color = if (user.isChild()) Color(0xFFFF8C7A) else Color(0xFF4CAF50),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Estado:",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (user.active) "✅ Activo" else "❌ Inactivo",
                                color = if (user.active) Color(0xFF27AE60) else Color(0xFFE74C3C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ✅ Formulario de edición
                Text(
                    text = "Editar Datos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5E3C)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE67E22),
                        focusedLabelColor = Color(0xFFE67E22),
                        cursorColor = Color(0xFFE67E22)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                        disabledLabelColor = Color.Gray,
                        disabledTextColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (user.isChild()) {
                    OutlinedTextField(
                        value = parentEmail,
                        onValueChange = { parentEmail = it },
                        label = { Text("Email del Padre/Madre") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE67E22),
                            focusedLabelColor = Color(0xFFE67E22),
                            cursorColor = Color(0xFFE67E22)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "* Requerido para usuarios menores de 18 años",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ✅ Botón de guardar
                if (hasChanges) {
                    Button(
                        onClick = {
                            if (user.isChild() && parentEmail.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "El email del padre es obligatorio para menores de edad",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val updatedUser = user.copy(
                                name = name,
                                parentEmail = if (user.isChild()) parentEmail else null
                            )

                            viewModel.updateUser(updatedUser)
                            Toast.makeText(context, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE67E22)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Guardar Cambios",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Gray.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No hay cambios pendientes",
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}