package com.example.tesis.admin.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tesis.data.model.User
import com.example.tesis.viewmodel.AuthViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.SolidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val viewModel: AdminUsersViewModel = viewModel()
    val users by viewModel.users.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState() // ⭐ Obtener usuario actual

    var selectedFilter by remember { mutableStateOf<UserFilter>(UserFilter.All) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    // ✅ Filtrar usuarios (excluir al admin actual)
    val filteredUsers = remember(users, selectedFilter, searchQuery, currentUser) {
        users
            .filter { user ->
                // ⭐ EXCLUIR al usuario admin actual
                user.userId != currentUser?.userId
            }
            .filter { user ->
                val matchesFilter = when (selectedFilter) {
                    UserFilter.All -> true
                    UserFilter.Children -> user.isChild()
                    UserFilter.Adults -> !user.isChild()
                    UserFilter.Active -> user.active
                    UserFilter.Inactive -> !user.active
                }

                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    user.name.contains(searchQuery, ignoreCase = true) ||
                            user.email.contains(searchQuery, ignoreCase = true)
                }

                matchesFilter && matchesSearch
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Gestión de Usuarios",
                            color = Color(0xFF8B5E3C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "${filteredUsers.size} usuarios",
                            color = Color(0xFF8B5E3C).copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("admin_home") }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF8B5E3C)
                        )
                    }
                },
                actions = {
                    // ⚠️ BOTÓN TEMPORAL - Eliminar después de usar
                    TextButton(
                        onClick = { viewModel.cleanAndStandardizeUsers() }
                    ) {
                        Text(
                            "🧹 Limpiar",
                            color = Color(0xFFE67E22),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ✅ Barra de búsqueda
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // ✅ Chips de filtros
                FilterChipsRow(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    userCounts = UserCounts(
                        total = users.size,
                        children = users.count { it.isChild() },
                        adults = users.count { !it.isChild() },
                        active = users.count { it.active },
                        inactive = users.count { !it.active }
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ✅ Lista de usuarios
                if (filteredUsers.isEmpty()) {
                    EmptyState(
                        filter = selectedFilter,
                        hasSearch = searchQuery.isNotBlank()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredUsers, key = { it.userId }) { user ->
                            UserCard(
                                user = user,
                                onEdit = { navController.navigate("admin_edit_user/${user.userId}") },
                                onViewHistory = { navController.navigate("admin_food_history/${user.userId}") },
                                onViewStats = { navController.navigate("user_stats/${user.userId}") }, // ⭐ NUEVO
                                onToggleStatus = { newStatus ->
                                    viewModel.toggleUserStatus(user.userId, newStatus)
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

// ✅ Enum para filtros
enum class UserFilter {
    All, Children, Adults, Active, Inactive
}

// ✅ Data class para conteo
data class UserCounts(
    val total: Int,
    val children: Int,
    val adults: Int,
    val active: Int,
    val inactive: Int
)

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por nombre o email...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = Color(0xFFE67E22)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar",
                        tint = Color.Gray
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFE67E22),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        singleLine = true
    )
}

@Composable
private fun FilterChipsRow(
    selectedFilter: UserFilter,
    onFilterSelected: (UserFilter) -> Unit,
    userCounts: UserCounts,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == UserFilter.All,
                onClick = { onFilterSelected(UserFilter.All) },
                label = { Text("Todos (${userCounts.total})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE67E22),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        item {
            FilterChip(
                selected = selectedFilter == UserFilter.Children,
                onClick = { onFilterSelected(UserFilter.Children) },
                label = { Text("Niños (${userCounts.children})") },
                leadingIcon = {
                    Text("👶", fontSize = 16.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF8C7A),
                    selectedLabelColor = Color.White
                )
            )
        }

        item {
            FilterChip(
                selected = selectedFilter == UserFilter.Adults,
                onClick = { onFilterSelected(UserFilter.Adults) },
                label = { Text("Adultos (${userCounts.adults})") },
                leadingIcon = {
                    Text("👨", fontSize = 16.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50),
                    selectedLabelColor = Color.White
                )
            )
        }

        item {
            FilterChip(
                selected = selectedFilter == UserFilter.Active,
                onClick = { onFilterSelected(UserFilter.Active) },
                label = { Text("Activos (${userCounts.active})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF27AE60),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        item {
            FilterChip(
                selected = selectedFilter == UserFilter.Inactive,
                onClick = { onFilterSelected(UserFilter.Inactive) },
                label = { Text("Inactivos (${userCounts.inactive})") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFE74C3C),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onEdit: () -> Unit,
    onViewHistory: () -> Unit,
    onViewStats: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header con nombre y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B5E3C)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Badge de tipo
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (user.isChild())
                                        Color(0xFFFF8C7A).copy(alpha = 0.2f)
                                    else
                                        Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (user.isChild()) "Niño/a" else "Adulto",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isChild())
                                    Color(0xFFFF8C7A)
                                else
                                    Color(0xFF4CAF50)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // Switch de activar/desactivar
                Switch(
                    checked = user.active,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF27AE60),
                        checkedTrackColor = Color(0xFF27AE60).copy(alpha = 0.5f),
                        uncheckedThumbColor = Color(0xFFE74C3C),
                        uncheckedTrackColor = Color(0xFFE74C3C).copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = if (user.isChild()) "👶" else "👨",
                    text = "${user.age} años",
                    color = if (user.isChild()) Color(0xFFFF8C7A) else Color(0xFF4CAF50)
                )

                InfoChip(
                    icon = if (user.active) "✅" else "❌",
                    text = if (user.active) "Activo" else "Inactivo",
                    color = if (user.active) Color(0xFF27AE60) else Color(0xFFE74C3C)
                )
            }

            // Email de padre si aplica
            if (user.needsParentEmail() && user.hasParentEmail()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = Color(0xFF7F8C8D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Padre: ${user.parentEmail}",
                        fontSize = 12.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Botones de acción (ahora con 3 botones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Editar
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE67E22)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.5.dp,
                        brush = SolidColor(Color(0xFFE67E22))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Botón Historial
                Button(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3498DB)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Historial", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // ⭐ NUEVO: Botón Estadísticas
                Button(
                    onClick = onViewStats,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9B59B6)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stats", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: String,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun EmptyState(
    filter: UserFilter,
    hasSearch: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when {
                    hasSearch -> "🔍"
                    filter == UserFilter.Children -> "👶"
                    filter == UserFilter.Adults -> "👨"
                    filter == UserFilter.Active -> "✅"
                    filter == UserFilter.Inactive -> "❌"
                    else -> "👥"
                },
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    hasSearch -> "No se encontraron usuarios"
                    filter == UserFilter.Children -> "No hay niños registrados"
                    filter == UserFilter.Adults -> "No hay adultos registrados"
                    filter == UserFilter.Active -> "No hay usuarios activos"
                    filter == UserFilter.Inactive -> "No hay usuarios inactivos"
                    else -> "No hay usuarios registrados"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5E3C)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearch)
                    "Intenta con otros términos de búsqueda"
                else
                    "Los usuarios registrados aparecerán aquí",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}