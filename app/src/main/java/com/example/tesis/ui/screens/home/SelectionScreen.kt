package com.example.tesis.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.viewmodel.DiaryViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionScreen(
    navController: NavController,
    selectedDate: String,
    diaryViewModel: DiaryViewModel = viewModel() // ✅ Parámetro añadido para consistencia
) {
    var selectedMoment by remember { mutableStateOf<String?>(null) }
    var selectedSticker by remember { mutableStateOf<String?>(null) }

    val moments = listOf("Desayuno", "Almuerzo", "Snack", "Cena")
    val stickers = listOf("🍎", "🥦", "🍗", "🥛", "🍕", "🥗", "🥪", "🍌")

    // Mismo degradado de fondo que en la pantalla anterior
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F0),
            Color(0xFFFFE4CC),
            Color(0xFFFF9AA2).copy(alpha = 0.3f)
        )
    )

    // Aplicar el degradado a toda la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Agregar alimento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Atrás",
                                tint = ConchodeVino
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Fecha seleccionada
                Text(
                    text = selectedDate,
                    fontSize = 16.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Sección: Selecciona el momento
                Text(
                    text = "Selecciona el momento",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Selector de momento
                MomentSelector(
                    moments = moments,
                    selectedMoment = selectedMoment,
                    onMomentSelected = { moment ->
                        selectedMoment = if (selectedMoment == moment) null else moment
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sección: Escoge una etiqueta
                Text(
                    text = "Escoge una etiqueta",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Grid de stickers
                StickerGrid(
                    stickers = stickers,
                    selectedSticker = selectedSticker,
                    onStickerSelected = { sticker ->
                        selectedSticker = if (selectedSticker == sticker) null else sticker
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Línea separadora
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.Gray.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botón Siguiente - CORREGIDO
                Button(
                    onClick = {
                        // ✅ Encoding seguro de los parámetros para evitar problemas con caracteres especiales
                        val encodedDate = URLEncoder.encode(selectedDate, StandardCharsets.UTF_8.toString())
                        val encodedMoment = URLEncoder.encode(selectedMoment ?: "", StandardCharsets.UTF_8.toString())
                        val encodedSticker = URLEncoder.encode(selectedSticker ?: "", StandardCharsets.UTF_8.toString())

                        navController.navigate("food_description/${encodedDate}/${encodedMoment}/${encodedSticker}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMoment != null && selectedSticker != null) {
                            PrimaryOrange
                        } else {
                            PrimaryOrange.copy(alpha = 0.3f)
                        }
                    ),
                    enabled = selectedMoment != null && selectedSticker != null
                ) {
                    Text(
                        text = "Siguiente",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentSelector(
    moments: List<String>,
    selectedMoment: String?,
    onMomentSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        moments.forEach { moment ->
            MomentOption(
                text = moment,
                isSelected = selectedMoment == moment,
                onClick = { onMomentSelected(moment) }
            )
        }
    }
}

@Composable
private fun MomentOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            PrimaryOrange.copy(alpha = 0.1f)
        } else {
            Color.White.copy(alpha = 0.8f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) PrimaryOrange else Color.Black
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(PrimaryOrange, CircleShape)
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerGrid(
    stickers: List<String>,
    selectedSticker: String?,
    onStickerSelected: (String) -> Unit
) {
    val columns = 4

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (i in stickers.indices step columns) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until columns) {
                    val index = i + j
                    if (index < stickers.size) {
                        StickerItem(
                            sticker = stickers[index],
                            isSelected = selectedSticker == stickers[index],
                            onClick = { onStickerSelected(stickers[index]) }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerItem(
    sticker: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Fondo del sticker
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    color = if (isSelected) {
                        PrimaryOrange.copy(alpha = 0.1f)
                    } else {
                        Color.White.copy(alpha = 0.8f)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sticker,
                fontSize = 32.sp
            )
        }

        // Indicador de selección
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .background(PrimaryOrange, CircleShape)
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}