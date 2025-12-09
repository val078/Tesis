package com.example.tesis.admin.gamesAdmin

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesManagementScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestión de Juegos",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF8F0),
                            Color(0xFFFFE4CC)
                        )
                    )
                )
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona el juego que deseas editar",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Grid de juegos
            GameCard(
                title = "Arrastra y Suelta",
                emoji = "🎯",
                description = "Editar alimentos saludables y chatarra",
                status = "Activo",
                statusColor = Color(0xFF4CAF50),
                onClick = { navController.navigate("admin_game_dragdrop") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameCard(
                title = "Memory Game",
                emoji = "🧠",
                description = "Editar pares de alimentos y beneficios",
                status = "Activo", // ⭐ Cambio
                statusColor = Color(0xFF4CAF50), // ⭐ Cambio
                onClick = { navController.navigate("admin_game_memory") } // ⭐ Cambio
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameCard(
                title = "Preguntón",
                emoji = "🧠",
                description = "Editar preguntas de trivia nutricional",
                status = "Activo",
                statusColor = Color(0xFF4CAF50),
                onClick = { navController.navigate("admin_game_pregunton") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GameCard(
                title = "NutriChef",
                emoji = "🍽️",
                description = "Editar rondas de alimentos",
                status = "Activo",
                statusColor = Color(0xFF4CAF50),
                onClick = { navController.navigate("admin_game_nutriplate") }
            )
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    emoji: String,
    description: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji grande
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Información del juego
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de estado
                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            // Flecha
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFE67E22),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}