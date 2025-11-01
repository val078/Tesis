// ui/screens/foodhistory/FoodHistoryScreen.kt
package com.example.tesis.ui.screens.foodhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.components.DiaryEntry
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.model.DiaryViewModel
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.theme.*
import com.example.tesis.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodHistoryScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val diaryViewModel: DiaryViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return // ⭐ IMPORTANTE: Salir temprano
    }

    val diaryEntries by diaryViewModel.allEntries.collectAsState()
    val isLoading by diaryViewModel.isLoading.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Agrupar entradas por fecha
    val entriesByDate = remember(diaryEntries) {
        diaryEntries
            .sortedByDescending { it.timestamp.toDate().time }
            .groupBy { entry ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(entry.timestamp.toDate())
            }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            diaryViewModel.loadAllEntries()
        }
    }

    LaunchedEffect(showDrawer) {
        if (showDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // ✅ SOLUCIÓN: ModalNavigationDrawer FUERA del Scaffold
    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        drawerContent = {
            DrawerMenu(
                currentUser = currentUser,
                onOptionSelected = { option ->
                    when (option) {
                        "profile" -> {
                            val userId = currentUser?.userId ?: ""
                            navController.navigate("edit_profile/$userId")
                        }
                        "settings" -> navController.navigate("settings")
                        "achievements" -> navController.navigate("achievements")
                        "food_history" -> navController.navigate("food_history")
                        "statistics" -> navController.navigate("statistics")
                        "help" -> navController.navigate("help")
                        "logout" -> {
                            authViewModel.logout()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                    coroutineScope.launch {
                        drawerState.close()
                    }
                },
                onClose = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "food_history",
                    navController = navController
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FoodHistoryHeader(
                            onMenuClick = {
                                showDrawer = true
                                coroutineScope.launch { drawerState.open() }
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Resumen
                    item {
                        HistorySummaryCard(
                            totalEntries = diaryEntries.size,
                            daysWithEntries = entriesByDate.size
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    if (isLoading) {
                        item {
                            LoadingState()
                        }
                    } else if (diaryEntries.isEmpty()) {
                        item {
                            EmptyState(navController = navController)
                        }
                    } else {
                        // Mostrar entradas agrupadas por fecha
                        entriesByDate.forEach { (date, entries) ->
                            item {
                                DateHeader(date = date, entriesCount = entries.size)
                            }

                            items(entries) { entry ->
                                DiaryEntryCard(entry = entry)
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FoodHistoryHeader(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Menú",
                tint = DarkOrange,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "Mi Diario",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun HistorySummaryCard(
    totalEntries: Int,
    daysWithEntries: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                icon = "📝",
                value = totalEntries.toString(),
                label = "Registros",
                color = Color(0xFF4CAF50)
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            SummaryItem(
                icon = "📅",
                value = daysWithEntries.toString(),
                label = "Días",
                color = PrimaryOrange
            )
        }
    }
}

@Composable
private fun SummaryItem(
    icon: String,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = TextGray
        )
    }
}

@Composable
private fun DateHeader(date: String, entriesCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryOrange.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$entriesCount ${if (entriesCount == 1) "registro" else "registros"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryOrange
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun DiaryEntryCard(entry: DiaryEntry) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time = timeFormat.format(entry.timestamp.toDate())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con hora y sticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sticker del usuario
                    Text(
                        text = entry.sticker,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = entry.moment,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConchodeVino
                        )
                        Text(
                            text = time,
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                // Rating chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(getRatingColor(entry.rating).copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = entry.rating,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = getRatingColor(entry.rating)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción de la comida
            Text(
                text = "¿Qué comiste?",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.description,
                fontSize = 14.sp,
                color = ConchodeVino,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F5E9))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = category,
            fontSize = 10.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PrimaryOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando tu diario...",
                fontSize = 14.sp,
                color = TextGray
            )
        }
    }
}

@Composable
private fun EmptyState(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📝",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No hay entradas aún",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Comienza a registrar tus comidas y emociones en el diario",
            fontSize = 14.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate("diary") },
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryOrange
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear primera entrada")
        }
    }
}

// Helper functions
private fun getRatingColor(rating: String): Color = when (rating.lowercase()) {
    "excelente" -> Color(0xFF4CAF50)
    "bueno" -> Color(0xFF8BC34A)
    "regular" -> Color(0xFFFFEB3B)
    "malo" -> Color(0xFFFF9800)
    else -> Color(0xFF757575)
}