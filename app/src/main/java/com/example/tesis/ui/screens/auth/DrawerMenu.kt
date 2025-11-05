// ui/screens/auth/DrawerMenu.kt
package com.example.tesis.ui.screens.auth

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tesis.R
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.CoralRed
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.model.User

@Composable
fun DrawerMenu(
    currentUser: User?,
    onOptionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(315.dp)
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
    ) {
        DrawerContent(
            currentUser = currentUser,
            onOptionSelected = onOptionSelected,
            onClose = onClose
        )
    }
}

@Composable
private fun DrawerContent(
    currentUser: User?,
    onOptionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        DrawerHeader(
            currentUser = currentUser,
            onClose = onClose
        )

        Spacer(modifier = Modifier.height(32.dp))

        DrawerMenuItem(
            icon = Icons.Outlined.Person,
            text = "Editar perfil",
            onClick = {
                onOptionSelected("profile")
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.Outlined.Settings,
            text = "Configuración",
            onClick = {
                onOptionSelected("settings")
                onClose()
            }
        )

        DrawerMenuItemWithPainter(
            painter = painterResource(id = R.drawable.help),
            text = "Ayuda y tutoriales",
            onClick = {
                onOptionSelected("help")
                onClose()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkOrange.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        Spacer(modifier = Modifier.height(16.dp))

        DrawerMenuItemWithPainter(
            painter = painterResource(id = R.drawable.logout),
            text = "Cerrar sesión",
            onClick = {
                onOptionSelected("logout")
                onClose()
            },
            color = CoralRed
        )
    }
}

// En DrawerMenu.kt - Actualizar DrawerHeader
@Composable
private fun DrawerHeader(
    currentUser: User?,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ⭐ Cambiar el Box por esto:
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE0B2)), // Fondo claro
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUser?.avatar ?: "🦁", // ⭐ Usar avatar en lugar de inicial
                fontSize = 36.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = currentUser?.name ?: "Usuario",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ConchodeVino
            )
            Text(
                text = if (currentUser?.age ?: 0 > 0) {
                    "${currentUser?.age} años"
                } else {
                    "Usuario"
                },
                fontSize = 14.sp,
                color = TextGray
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Cerrar",
                tint = ConchodeVino,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = DarkOrange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White) // ✅ Fondo blanco sólido
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 16.sp,
            color = ConchodeVino,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DrawerMenuItemWithPainter(
    painter: androidx.compose.ui.graphics.painter.Painter,
    text: String,
    onClick: () -> Unit,
    color: Color = DarkOrange
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White) // ✅ ¡CORREGIDO! Ahora es sólido, sin alpha
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = text,
            fontSize = 16.sp,
            color = ConchodeVino,
            fontWeight = FontWeight.Medium
        )
    }
}