// ui/components/BottomNavBar.kt
package com.example.tesis.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.tesis.R
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray

@Composable
fun BottomNavBar(
    currentRoute: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // ✅ Barra de navegación inferior con gradiente
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(70.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Botón izquierdo - Logros (Vector Asset)
            BottomNavItemWithPainter(
                painter = painterResource(id = R.drawable.militarytech), // ✅ Vector Asset
                selected = currentRoute == "achievements",
                onClick = { navController.navigate("achievements") },
                color = ConchodeVino
            )

            // ✅ Botón central - Home (Icono de Material)
            BottomNavItem(
                icon = Icons.Outlined.Home,  // ✅ Icono de Material
                selected = currentRoute == "home",
                onClick = { navController.navigate("home") },
                color = ConchodeVino,
                isHome = true  // ✅ Indicar que es el botón Home
            )

            // ✅ Botón derecho - Comidas (Vector Asset)
            BottomNavItemWithPainter(
                painter = painterResource(id = R.drawable.restaurant), // ✅ Vector Asset
                selected = currentRoute == "food_history",
                onClick = { navController.navigate("food_history") },
                color = ConchodeVino
            )
        }
    }
}

// ✅ Componente para ítems con Vector Assets - CORREGIDO
@Composable
private fun BottomNavItemWithPainter(
    painter: Painter,  // ✅ Vector Asset
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    isHome: Boolean = false
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ✅ Ícono circular con Vector Asset
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    brush = if (selected) {
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF5F5F5)
                            )
                        )
                    }
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) color else Color(0xFFE0E0E0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,  // ✅ Vector Asset
                contentDescription = null,
                tint = if (selected) color else TextGray,
                modifier = Modifier.size(if (isHome) 28.dp else 24.dp)
            )
        }

        // ✅ Indicador de página activa
        if (selected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ✅ Componente para ítems con Iconos de Material - CORREGIDO
@Composable
private fun BottomNavItem(
    icon: ImageVector,  // ✅ Icono de Material
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    isHome: Boolean = false
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ✅ Ícono circular con Icono de Material
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    brush = if (selected) {
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFF5F5F5)
                            )
                        )
                    }
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) color else Color(0xFFE0E0E0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,  // ✅ Icono de Material
                contentDescription = null,
                tint = if (selected) color else TextGray,
                modifier = Modifier.size(if (isHome) 28.dp else 24.dp)
            )
        }

        // ✅ Indicador de página activa
        if (selected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}