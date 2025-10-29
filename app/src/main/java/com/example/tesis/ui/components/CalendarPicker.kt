// ui/components/CalendarPicker.kt
package com.example.tesis.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.PinkOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarPicker(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var showYearSelector by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Selecciona tu fecha de nacimiento",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showYearSelector) {
                    // Selector de año
                    YearSelector(
                        currentYear = currentDate.get(Calendar.YEAR),
                        onYearSelected = { year ->
                            currentDate = Calendar.getInstance().apply {
                                time = currentDate.time
                                set(Calendar.YEAR, year)
                            }
                            showYearSelector = false
                        },
                        onDismiss = { showYearSelector = false }
                    )
                } else {
                    // Controles de navegación
                    CalendarHeader(
                        currentDate = currentDate,
                        onPreviousMonth = {
                            currentDate = Calendar.getInstance().apply {
                                time = currentDate.time
                                add(Calendar.MONTH, -1)
                            }
                        },
                        onNextMonth = {
                            currentDate = Calendar.getInstance().apply {
                                time = currentDate.time
                                add(Calendar.MONTH, 1)
                            }
                        },
                        onShowYearSelector = { showYearSelector = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Días de la semana
                    WeekDaysHeader()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendario
                    CalendarGrid(
                        currentDate = currentDate,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = date
                        }
                    )
                }

                // Fecha seleccionada
                if (selectedDate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Seleccionado: ${formatDate(selectedDate!!)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            if (selectedDate != null && !showYearSelector) {
                Button(
                    onClick = {
                        selectedDate?.let { onDateSelected(it) }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            if (!showYearSelector) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            } else {
                TextButton(onClick = { showYearSelector = false }) {
                    Text("Volver")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CalendarHeader(
    currentDate: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowYearSelector: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Mes anterior",
                tint = PrimaryOrange
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onShowYearSelector() }
        ) {
            Text(
                text = SimpleDateFormat("MMMM", Locale.getDefault()).format(currentDate.time)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = currentDate.get(Calendar.YEAR).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryOrange,
                fontWeight = FontWeight.Medium
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Mes siguiente",
                tint = PrimaryOrange
            )
        }
    }
}

@Composable
private fun YearSelector(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val startYear = currentYear - 50 // 50 años atrás
    val endYear = currentYear + 5   // 5 años adelante (por si acaso)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Text(
            text = "Selecciona un año",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✅ Corrección: usar items correctamente
            items((startYear..endYear).toList().reversed()) { year ->
                YearItem(
                    year = year,
                    isSelected = year == currentYear,
                    onSelect = { onYearSelected(year) }
                )
            }
        }
    }
}

@Composable
private fun YearItem(
    year: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onSelect() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange.copy(alpha = 0.2f))
            )
        }
    }
}

@Composable
private fun WeekDaysHeader() {
    val daysOfWeek = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEach { day ->
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentDate: Calendar,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        time = currentDate.time
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val selectedCalendar = selectedDate?.let { Calendar.getInstance().apply { time = it } }

    Column {
        repeat(6) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayOfWeek ->
                    val dayIndex = week * 7 + dayOfWeek - (firstDayOfWeek - 1)

                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayIndex > 0 && dayIndex <= daysInMonth) {
                            val dayCalendar = Calendar.getInstance().apply {
                                time = currentDate.time
                                set(Calendar.DAY_OF_MONTH, dayIndex)
                            }

                            val isToday = dayCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    dayCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                                    dayCalendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

                            val isSelected = selectedCalendar?.let {
                                dayCalendar.get(Calendar.YEAR) == it.get(Calendar.YEAR) &&
                                        dayCalendar.get(Calendar.MONTH) == it.get(Calendar.MONTH) &&
                                        dayCalendar.get(Calendar.DAY_OF_MONTH) == it.get(Calendar.DAY_OF_MONTH)
                            } ?: false

                            val isClickable = dayCalendar.timeInMillis <= System.currentTimeMillis()

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> PrimaryOrange
                                            isToday -> PinkOrange.copy(alpha = 0.3f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable(
                                        enabled = isClickable,
                                        onClick = {
                                            if (isClickable) {
                                                onDateSelected(dayCalendar.time)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayIndex.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> PrimaryOrange
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(date)
}