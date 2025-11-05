// ui/screens/diary/DiaryScreen.kt
package com.example.tesis.ui.screens.home

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import com.example.tesis.ui.components.BottomNavBar
import com.example.tesis.data.model.FoodEntry
import com.example.tesis.data.viewmodel.DiaryViewModel
import com.example.tesis.ui.screens.auth.DrawerMenu
import com.example.tesis.ui.theme.PrimaryOrange
import com.example.tesis.ui.theme.DarkOrange
import com.example.tesis.ui.theme.ConchodeVino
import com.example.tesis.ui.theme.TextGray
import com.example.tesis.data.viewmodel.AuthViewModel
import java.util.*

@Composable
fun DiaryScreen(
    navController: NavController,
    diaryViewModel: DiaryViewModel = viewModel()
) {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return // ⭐ IMPORTANTE: Salir temprano
    }

    val foodEntries by diaryViewModel.foodEntries.collectAsState()

    var showDrawer by remember { mutableStateOf(false) }

    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH) + 1

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    var showDatePicker by remember { mutableStateOf(false) }

    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var showDateModal by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    /*LaunchedEffect(Unit) {
        authViewModel.getCurrentUser()
    }*/

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
                        "settings" -> navController.navigate("user_settings")
                        "achievements" -> navController.navigate("achievements")
                        "food_history" -> navController.navigate("food_history")
                        "statistics" -> navController.navigate("statistics")
                        "help" -> navController.navigate("help")
                        "logout" -> {
                            Log.d("HomeScreen", "🚪 Iniciando logout...")
                            diaryViewModel.stopListening()
                            authViewModel.logout()
                            // ✅ El Flow automático se encarga de navegar
                        }
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
        // ✅ Scaffold DENTRO del drawer
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                BottomNavBar(
                    currentRoute = "diary",
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
                                Color(0xFFFF9AA2).copy(alpha = 0.3f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    DiaryHeader(
                        onMenuClick = {
                            showDrawer = true
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    DateSelector(
                        year = selectedYear,
                        month = selectedMonth,
                        onDateSelectorClick = { showDatePicker = true }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CalendarView(
                        year = selectedYear,
                        month = selectedMonth,
                        diaryViewModel = diaryViewModel,
                        onDateSelected = { day ->
                            val calendar = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth - 1)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            selectedDate = calendar
                            showDateModal = true
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Dialog para seleccionar año y mes
    if (showDatePicker) {
        DatePickerDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onDateSelected = { year, month ->
                selectedYear = year
                selectedMonth = month
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Modal para la fecha seleccionada
    if (showDateModal && selectedDate != null) {
        val currentSelectedDate = selectedDate!!

        DateDetailModal(
            selectedDate = currentSelectedDate,
            diaryViewModel = diaryViewModel,
            onDismiss = { showDateModal = false },
            onAddFood = {
                val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
                    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
                val dayOfWeekNames = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

                val day = currentSelectedDate.get(Calendar.DAY_OF_MONTH)
                val month = currentSelectedDate.get(Calendar.MONTH)
                val dayOfWeek = currentSelectedDate.get(Calendar.DAY_OF_WEEK) - 1

                val dateText = "${dayOfWeekNames[dayOfWeek].replaceFirstChar { it.uppercase() }} $day de ${monthNames[month]}"

                navController.navigate("add_food/$dateText")
                showDateModal = false
            }
        )
    }
}

@Composable
private fun DiaryHeader(
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón hamburger menu
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

        // Título centrado
        Text(
            text = "Diario",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        // Spacer para equilibrar
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun DateSelector(
    year: Int,
    month: Int,
    onDateSelectorClick: () -> Unit
) {
    val monthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        // Año
        Text(
            text = year.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ConchodeVino,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mes con flecha
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onDateSelectorClick() }
                .padding(start = 8.dp)
        ) {
            Text(
                text = "${monthNames[month - 1]} ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = ConchodeVino
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = "Cambiar fecha",
                tint = ConchodeVino,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CalendarView(
    year: Int,
    month: Int,
    diaryViewModel: DiaryViewModel, // ✅ Añadir parámetro
    onDateSelected: (Int) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9AA2).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            WeekDaysHeader()

            Spacer(modifier = Modifier.height(12.dp))

            CalendarGrid(
                firstDayOfWeek = firstDayOfWeek,
                daysInMonth = daysInMonth,
                selectedYear = year, // ✅ Añadir parámetro
                selectedMonth = month, // ✅ Añadir parámetro
                diaryViewModel = diaryViewModel, // ✅ Añadir parámetro
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun WeekDaysHeader() {
    val weekDays = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { day ->
            Text(
                text = day,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    firstDayOfWeek: Int,
    daysInMonth: Int,
    selectedYear: Int,
    selectedMonth: Int,
    diaryViewModel: DiaryViewModel,
    onDateSelected: (Int) -> Unit
) {
    // ⭐ CAMBIO: Observar el StateFlow
    val foodEntriesMap by diaryViewModel.foodEntries.collectAsState()

    val ecuadorTimeZone = java.util.TimeZone.getTimeZone("America/Guayaquil")
    val currentDate = Calendar.getInstance(ecuadorTimeZone)
    val currentYear = currentDate.get(Calendar.YEAR)
    val currentMonth = currentDate.get(Calendar.MONTH) + 1
    val currentDay = currentDate.get(Calendar.DAY_OF_MONTH)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (week in 0 until 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val dayNumber = cellIndex - firstDayOfWeek + 1

                    Box(modifier = Modifier.weight(1f)) {
                        if (dayNumber in 1..daysInMonth) {
                            val dateString = formatDate(selectedYear, selectedMonth, dayNumber)

                            // ⭐ CAMBIO: Obtener de la map observada
                            val dayEntries = foodEntriesMap[dateString] ?: emptyList()

                            val isToday = selectedYear == currentYear &&
                                    selectedMonth == currentMonth &&
                                    dayNumber == currentDay

                            DayCell(
                                dayNumber = dayNumber,
                                isToday = isToday,
                                onClick = { onDateSelected(dayNumber) },
                                foodEntries = dayEntries
                            )
                        } else {
                            Spacer(modifier = Modifier.height(56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isToday: Boolean, // ✅ Nuevo parámetro
    onClick: () -> Unit,
    foodEntries: List<FoodEntry>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = dayNumber.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color.White.copy(alpha = 0.8f),
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isToday) 2.dp else 1.dp, // ✅ Borde más grueso para hoy
                    color = if (isToday) {
                        PrimaryOrange // ✅ Color especial para hoy
                    } else {
                        Color.White.copy(alpha = 0.6f)
                    },
                    RoundedCornerShape(8.dp)
                )
        ) {
            // MOSTRAR STICKERS si hay entradas
            if (foodEntries.isNotEmpty()) {
                Text(
                    text = foodEntries.first().sticker,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ✅ INDICADOR ADICIONAL para el día de hoy
            if (isToday) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(8.dp)
                        .background(
                            PrimaryOrange,
                            CircleShape
                        )
                )
            }
        }
    }
}

private fun formatDate(year: Int, month: Int, day: Int): String {
    val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val dayOfWeekNames = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

    val ecuadorTimeZone = java.util.TimeZone.getTimeZone("America/Guayaquil")
    val calendar = Calendar.getInstance(ecuadorTimeZone).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }

    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val formattedDate = "${dayOfWeekNames[dayOfWeek].replaceFirstChar { it.uppercase() }} $day de ${monthNames[month - 1]}"

    // ✅ LOG CRÍTICO
    Log.d("DiaryScreen", "📅 Fecha generada: '$formattedDate'")

    return formattedDate
}

// ✅ Modal para detalles de la fecha - CORREGIDO
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailModal(
    selectedDate: Calendar,
    diaryViewModel: DiaryViewModel,
    onDismiss: () -> Unit,
    onAddFood: () -> Unit
) {
    val monthNames = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val dayOfWeekNames = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

    val ecuadorTimeZone = java.util.TimeZone.getTimeZone("America/Guayaquil")
    val ecuadorCalendar = Calendar.getInstance(ecuadorTimeZone).apply {
        time = selectedDate.time
    }

    val day = ecuadorCalendar.get(Calendar.DAY_OF_MONTH)
    val month = ecuadorCalendar.get(Calendar.MONTH)
    val dayOfWeek = ecuadorCalendar.get(Calendar.DAY_OF_WEEK) - 1

    val dateString = "${dayOfWeekNames[dayOfWeek].replaceFirstChar { it.uppercase() }} $day de ${monthNames[month]}"

    // ⭐ CAMBIO: Observar el StateFlow
    val foodEntriesMap by diaryViewModel.foodEntries.collectAsState()
    val foodEntries = foodEntriesMap[dateString] ?: emptyList()

    var entryToEdit by remember { mutableStateOf<FoodEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<FoodEntry?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFFFF8F0),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = dateString,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ⭐ La lista se actualiza automáticamente
            if (foodEntries.isNotEmpty()) {
                Column {
                    foodEntries.forEach { entry ->
                        FoodEntryItem(
                            entry = entry,
                            onEdit = { entryToEdit = it },
                            onDelete = { entryToDelete = it }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onAddFood,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.5.dp,
                        color = Color(0x5E443C).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD9D9D9).copy(alpha = 0.3f)
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Color(0xFF603B2D).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF603B2D)
                        )
                    }
                    Text(
                        text = "¡Agrega tu alimento!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF603B2D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.Gray.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modales de edición y eliminación
    if (entryToEdit != null) {
        EditEntryModal(
            entry = entryToEdit!!,
            onDismiss = { entryToEdit = null },
            onSave = { updatedEntry ->
                diaryViewModel.updateFoodEntry(
                    date = dateString,
                    originalMoment = entryToEdit!!.moment,
                    updatedEntry = updatedEntry
                )
                entryToEdit = null
            }
        )
    }

    if (entryToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                diaryViewModel.deleteFoodEntry(
                    date = dateString,
                    moment = entryToDelete!!.moment
                )
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }
}

@Composable
private fun FoodEntryItem(entry: FoodEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sticker
            Text(
                text = entry.sticker,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.moment,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                if (entry.description.isNotBlank()) {
                    Text(
                        text = entry.description.take(50) + if (entry.description.length > 50) "..." else "",
                        fontSize = 14.sp,
                        color = TextGray,
                        maxLines = 2
                    )
                }
                Text(
                    text = "Calificación: ${entry.rating}",
                    fontSize = 12.sp,
                    color = TextGray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DatePickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDateSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val years = (2020..2030).toList()
    val months = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar Fecha",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConchodeVino
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Selector de año
                Text(
                    text = "Año",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConchodeVino
                )

                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    state = rememberLazyListState()
                ) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedYear = year }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = year.toString(),
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de mes
                Text(
                    text = "Mes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConchodeVino
                )

                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    state = rememberLazyListState()
                ) {
                    items(months) { monthName ->
                        val monthIndex = months.indexOf(monthName) + 1
                        val isSelected = monthIndex == selectedMonth
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMonth = monthIndex }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = monthName,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextGray)
                    }

                    Button(
                        onClick = { onDateSelected(selectedYear, selectedMonth) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text("Confirmar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodEntryItem(
    entry: FoodEntry,
    onEdit: (FoodEntry) -> Unit,
    onDelete: (FoodEntry) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sticker grande
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        getRatingColor(entry.rating).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.sticker,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.moment,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConchodeVino
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rating chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(getRatingColor(entry.rating).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = entry.rating,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getRatingColor(entry.rating)
                        )
                    }
                }

                if (entry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.description,
                        fontSize = 14.sp,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            // Botón de menú
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Opciones",
                        tint = ConchodeVino
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Editar")
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEdit(entry)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Eliminar", color = Color(0xFFF44336))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDelete(entry)
                        }
                    )
                }
            }
        }
    }
}

private fun getRatingColor(rating: String): Color = when (rating.lowercase()) {
    "bueno" -> Color(0xFF4CAF50)      // Verde Material Design
    "regular" -> Color(0xFFFFC107)    // Ámbar Material Design
    "malo" -> Color(0xFFFF5722)       // Naranja profundo
    else -> Color(0xFF757575)         // Gris medio (el que ya tenías)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryModal(
    entry: FoodEntry,
    onDismiss: () -> Unit,
    onSave: (FoodEntry) -> Unit
) {
    var selectedMoment by remember { mutableStateOf(entry.moment) }
    var selectedSticker by remember { mutableStateOf(entry.sticker) }
    var description by remember { mutableStateOf(entry.description) }
    var selectedRating by remember { mutableStateOf(entry.rating) }

    val moments = listOf("Desayuno", "Almuerzo", "Cena", "Snack")
    val stickers = listOf("🍎", "🥗", "🍕", "🍔", "🥦", "🍗", "🍱", "🥪")
    val ratings = listOf("Bueno", "Regular", "Malo")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFFFF8F0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Editar entrada",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de momento
            Text(
                text = "Momento del día",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moments.forEach { moment ->
                    val isSelected = selectedMoment == moment
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryOrange else Color.Gray.copy(alpha = 0.1f)
                            )
                            .clickable { selectedMoment = moment }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = moment,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else ConchodeVino
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selector de sticker
            Text(
                text = "Elige tu sticker",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stickers.chunked(4).first().forEach { sticker ->
                    val isSelected = selectedSticker == sticker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryOrange.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.1f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = PrimaryOrange,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedSticker = sticker }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sticker, fontSize = 32.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stickers.chunked(4).last().forEach { sticker ->
                    val isSelected = selectedSticker == sticker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) PrimaryOrange.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.1f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = PrimaryOrange,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedSticker = sticker }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = sticker, fontSize = 32.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Descripción
            Text(
                text = "Descripción",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("¿Qué comiste?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rating
            Text(
                text = "¿Cómo te sentiste?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ConchodeVino
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ratings.forEach { rating ->
                    val isSelected = selectedRating == rating
                    val ratingColor = getRatingColor(rating)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) ratingColor.copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.1f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = ratingColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedRating = rating }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rating,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ratingColor else ConchodeVino
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Text("Cancelar", color = ConchodeVino)
                }

                Button(
                    onClick = {
                        val updatedEntry = entry.copy(
                            moment = selectedMoment,
                            sticker = selectedSticker,
                            description = description,
                            rating = selectedRating
                        )
                        onSave(updatedEntry)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange
                    )
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "¿Eliminar entrada?",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Esta acción no se puede deshacer.",
                textAlign = TextAlign.Center,
                color = TextGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ConchodeVino)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}