package com.example.tesis.admin.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFoodHistoryScreen(
    navController: NavController,
    userId: String,
    viewModel: AdminFoodHistoryViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // ✅ Agrupar entradas por fecha fuera del LazyColumn
    val entriesByDate = remember(entries) {
        entries
            .sortedByDescending { it.timestamp.toDate().time }
            .groupBy { entry ->
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(entry.timestamp.toDate())
            }
    }

    // ✅ Estado para confirmación de eliminación
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadEntries(userId)
    }

    // ✅ Diálogo de confirmación
    if (showDeleteDialog && entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar entrada") },
            text = {
                Text("¿Estás seguro de que deseas eliminar esta entrada de comida? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        entryToDelete?.let { entry ->
                            viewModel.deleteEntry(entry.id)
                        }
                        showDeleteDialog = false
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE74C3C))
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Historial de Comidas",
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
                .padding(bottom = innerPadding.calculateBottomPadding())
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
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // ✅ Resumen
                item {
                    HistorySummaryCard(
                        totalEntries = entries.size,
                        daysWithEntries = entriesByDate.size
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (isLoading) {
                    item {
                        LoadingState()
                    }
                } else if (entries.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    // ✅ Mostrar entradas agrupadas por fecha
                    entriesByDate.forEach { (date, entries) ->
                        item {
                            DateHeader(date = date, entriesCount = entries.size)
                        }

                        items(entries) { entry ->
                            DiaryEntryCard(
                                entry = entry,
                                onDelete = {
                                    entryToDelete = entry
                                    showDeleteDialog = true
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
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
                color = Color(0xFFE67E22)
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
            color = Color(0xFF8B5E3C)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
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
            color = Color(0xFF8B5E3C)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE67E22).copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$entriesCount ${if (entriesCount == 1) "registro" else "registros"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE67E22)
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
private fun DiaryEntryCard(entry: DiaryEntry, onDelete: () -> Unit) {
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
                            color = Color(0xFF8B5E3C)
                        )
                        Text(
                            text = time,
                            fontSize = 12.sp,
                            color = Color.Gray
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
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.description,
                fontSize = 14.sp,
                color = Color(0xFF8B5E3C),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón de eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color(0xFFE74C3C)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar entrada"
                    )
                }
            }
        }
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
                color = Color(0xFFE67E22),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando historial...",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun EmptyState() {
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
            color = Color(0xFF8B5E3C),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Este usuario aún no ha registrado comidas en su diario",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
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