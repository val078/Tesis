// Crea: screens/EditProfileScreen.kt
package com.example.tesis.ui.screens.drawer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.data.model.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userId: String, // ⭐ Recibir userId
    viewModel: EditProfileViewModel = viewModel(),
    onProfileUpdated: () -> Unit = {} // ⭐ Callback
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("🦁") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Actualizar cuando se carguen los datos
    LaunchedEffect(currentUser) {
        currentUser?.let {
            name = it.name
            selectedAvatar = it.avatar
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showSaveDialog = true
            viewModel.resetSaveSuccess()
            onProfileUpdated() // ⭐ Notificar que se actualizó
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showErrorDialog = true
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveDialog = false
                navController.popBackStack()
            },
            title = { Text("✅ Guardado") },
            text = { Text("Tu perfil se actualizó correctamente.") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearError()
            },
            title = { Text("❌ Error") },
            text = { Text(errorMessage ?: "Ocurrió un error") },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    viewModel.clearError()
                }) {
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
                        "Editar Perfil",
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
        if (isLoading && currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE67E22))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFFFF8F0))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Avatar grande
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE0B2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedAvatar,
                        fontSize = 64.sp
                    )
                }

                Text(
                    text = "Selecciona tu avatar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                // Selector de avatar
                AvatarSelector(
                    selectedAvatar = selectedAvatar,
                    onAvatarSelected = { selectedAvatar = it }
                )

                // Botón guardar
                Button(
                    onClick = {
                        viewModel.updateName(name)
                        viewModel.updateAvatar(selectedAvatar)
                        viewModel.saveProfile(
                            onSuccess = {
                                // ⭐ Se ejecuta después de guardar exitosamente
                                Log.d("EditProfileScreen", "✅ Perfil guardado, actualizando UI")
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Guardar Cambios",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AvatarSelector(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit
) {
    val animalEmojis = listOf(
        "🦁", "🐯", "🐻", "🐼", "🐨",
        "🐰", "🦊", "🐶", "🐱", "🐭",
        "🐹", "🐷", "🐮", "🐸", "🐵",
        "🦄", "🐔", "🐧", "🦆", "🦉"
    )

    val fruitEmojis = listOf(
        "🍎", "🍊", "🍋", "🍌", "🍉",
        "🍇", "🍓", "🫐", "🍈", "🍒",
        "🍑", "🥭", "🍍", "🥥", "🥝",
        "🍅", "🥑", "🌽", "🥕", "🥦"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Animales
            Text(
                text = "🦁 Animales",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ⭐ SOLUCIÓN: Usar EmojiGrid simple en lugar de LazyVerticalGrid
            EmojiGrid(
                emojis = animalEmojis,
                selectedEmoji = selectedAvatar,
                onEmojiSelected = onAvatarSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Frutas
            Text(
                text = "🍎 Frutas y Verduras",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5D4037)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ⭐ SOLUCIÓN: Usar EmojiGrid simple en lugar de LazyVerticalGrid
            EmojiGrid(
                emojis = fruitEmojis,
                selectedEmoji = selectedAvatar,
                onEmojiSelected = onAvatarSelected
            )
        }
    }
}

// ⭐ NUEVO: Componente que reemplaza LazyVerticalGrid
@Composable
private fun EmojiGrid(
    emojis: List<String>,
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit
) {
    // Dividir en filas de 5 columnas
    val rows = emojis.chunked(5)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowEmojis ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowEmojis.forEach { emoji ->
                    EmojiItem(
                        emoji = emoji,
                        isSelected = emoji == selectedEmoji,
                        onClick = { onEmojiSelected(emoji) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Rellenar espacios vacíos en la última fila
                repeat(5 - rowEmojis.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmojiItem(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f) // ⭐ Mantener cuadrado
            .clip(CircleShape)
            .background(
                if (isSelected) Color(0xFFFFE0B2) else Color(0xFFF5F5F5)
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF9800) else Color(0xFFE0E0E0),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
    }
}