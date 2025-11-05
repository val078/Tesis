package com.example.tesis.admin.home

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AdminHomeScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8F0),
                        Color(0xFFFFF0E6),
                        Color(0xFFFFE8D6)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 🔹 Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Panel de Administrador",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C)
            )

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFFD35400)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Herramientas disponibles",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6B4C2A)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Grid responsivo
        AdminActionsGrid(navController)
    }
}

@Composable
private fun AdminActionsGrid(navController: NavController) {
    val actions = listOf(
        AdminAction(
            "Gestión de Usuarios",
            Icons.Default.AccountCircle,
            "Ver, editar o eliminar cuentas de niños"
        ) { navController.navigate("admin_users") },
        AdminAction(
            "Editar Juegos",
            Icons.Default.VideogameAsset,
            "Gestionar alimentos y trivias"
        ) { navController.navigate("admin_games") },
        AdminAction(
            "Estadísticas",
            Icons.Default.BarChart,
            "Ver métricas totales de la aplicación"
        ) { navController.navigate("admin_stats") },
        AdminAction(
            "Configuración IA",
            Icons.Default.Face,
            "Editar prompt y ver historial"
        ) { navController.navigate("admin_ai_config") },
        AdminAction(
            "Configuración General",
            Icons.Default.Settings,
            "Ajustes principales de la app"
        ) { navController.navigate("admin_app_config") },
        AdminAction(
            "Reportes Email",
            Icons.Default.Email,
            "Enviar reportes a padres"
        ) { navController.navigate("admin_reports") }
    )

    // 🔸 Grid adaptativo 2 columnas
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { action ->
                    AdminCard(
                        action = action,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Si hay un número impar de tarjetas, agregamos un espacio vacío
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

data class AdminAction(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

@Composable
private fun AdminCard(
    action: AdminAction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // 🔹 Mantiene forma cuadrada, adaptable a ancho disponible
            .clickable { action.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = Color(0xFFE67E22),
                modifier = Modifier.size(38.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = action.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF5D4037),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.description,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
