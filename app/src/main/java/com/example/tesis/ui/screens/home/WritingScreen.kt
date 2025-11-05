package com.example.tesis.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.model.FoodEntry
import com.example.tesis.data.viewmodel.DiaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    navController: NavController,
    selectedDate: String,
    selectedMoment: String,
    selectedSticker: String,
    diaryViewModel: DiaryViewModel = viewModel()
) {

    var textValue by remember { mutableStateOf(TextFieldValue()) }
    var selectedRating by remember { mutableStateOf<String?>(null) }

    val ratings = listOf("Malo", "Regular", "Bueno")

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF8F0),
            Color(0xFFFFE4CC),
            Color(0xFFFF9AA2).copy(alpha = 0.3f)
        )
    )

    val isFormValid = textValue.text.isNotBlank() && selectedRating != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 100.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título principal
            Text(
                text = "¿Qué comiste?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F2E1B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Cuéntanos sobre tu comida",
                fontSize = 15.sp,
                color = Color(0xFF3F2E1B).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Campo de texto mejorado
            EnhancedTextField(
                textValue = textValue,
                onValueChange = { newValue ->
                    if (newValue.text.length <= 200) {
                        textValue = newValue
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Contador de caracteres
            Text(
                text = "${textValue.text.length}/300",
                fontSize = 13.sp,
                color = TextGray.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                textAlign = TextAlign.End,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Emoji decorativo
            Text(
                text = "⭐",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sección de calificación
            Text(
                text = "¿Cómo estuvo?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F2E1B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Selecciona una opción",
                fontSize = 15.sp,
                color = Color(0xFF3F2E1B).copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Selector de calificación mejorado
            EnhancedRatingSelector(
                ratings = ratings,
                selectedRating = selectedRating,
                onRatingSelected = { rating ->
                    selectedRating = rating
                }
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Botones inferiores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón Anterior
                EnhancedOutlinedButton(
                    onClick = { navController.popBackStack() },
                    text = "Anterior",
                    modifier = Modifier.weight(1f)
                )

                // Botón Listo
                EnhancedFilledButton(
                    onClick = {
                        val foodEntry = FoodEntry(
                            date = selectedDate,
                            moment = selectedMoment,
                            sticker = selectedSticker,
                            description = textValue.text,
                            rating = selectedRating ?: ""
                        )

                        diaryViewModel.saveFoodEntry(foodEntry)

                        navController.navigate("diary") {
                            popUpTo("diary") { inclusive = true }
                        }
                    },
                    text = "Listo",
                    enabled = isFormValid,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EnhancedTextField(
    textValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize(),
                textStyle = TextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFF2C2C2C),
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (textValue.text.isEmpty()) {
                            Text(
                                text = "Ejemplo: Comí pasta con salsa de tomate y albahaca...",
                                fontSize = 17.sp,
                                color = TextGray.copy(alpha = 0.4f),
                                lineHeight = 26.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
private fun EnhancedRatingSelector(
    ratings: List<String>,
    selectedRating: String?,
    onRatingSelected: (String) -> Unit
) {
    val emojis = mapOf(
        "Malo" to "😞",
        "Regular" to "😐",
        "Bueno" to "😊"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ratings.forEach { rating ->
            EnhancedRatingButton(
                text = rating,
                emoji = emojis[rating] ?: "",
                isSelected = selectedRating == rating,
                onClick = { onRatingSelected(rating) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EnhancedRatingButton(
    text: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .wrapContentHeight()
            .defaultMinSize(minHeight = 72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                PrimaryOrange.copy(alpha = 0.12f)
            } else {
                Color.White
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, PrimaryOrange)
        } else {
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) PrimaryOrange else Color(0xFF666666),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun EnhancedOutlinedButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp)),
        shape = RoundedCornerShape(27.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.25f)),
        onClick = onClick,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EnhancedFilledButton(
    onClick: () -> Unit,
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp)),
        shape = RoundedCornerShape(27.dp),
        color = if (enabled) {
            PrimaryOrange
        } else {
            PrimaryOrange.copy(alpha = 0.35f)
        },
        onClick = { if (enabled) onClick() },
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}