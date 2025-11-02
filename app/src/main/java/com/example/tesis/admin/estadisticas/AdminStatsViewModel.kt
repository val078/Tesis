package com.example.tesis.admin.estadisticas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class StatsData(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val inactiveUsers: Int = 0,
    val childUsers: Int = 0,
    val adultUsers: Int = 0,
    val diaryEntries: Int = 0,
    val totalMealsLogged: Int = 0, // ⭐ Mantener para retrocompatibilidad pero no usar
    val averageMealsPerUser: Double = 0.0,
    val mostActiveUser: String = "N/A",
    val lastAccessDate: String = "N/A",
    val newUsersInPeriod: Int = 0,
    val diaryEntriesInPeriod: Int = 0,
    val mealsInPeriod: Int = 0, // ⭐ Esto ahora será el conteo de foodHistory dentro del diario
    val gamesPlayedInPeriod: Int = 0,
    val topUsers: List<TopUserData> = emptyList(),
    val activityByDay: Map<String, Int> = emptyMap()
)

data class TopUserData(
    val name: String,
    val mealsCount: Int,      // ⭐ Ahora es foodHistory count
    val diaryCount: Int,      // ⭐ diaryEntries count
    val gamesCount: Int
)
enum class StatsPeriod(val label: String, val emoji: String) {
    TODAY("Hoy", "📅"),
    WEEK("Esta Semana", "📆"),
    MONTH("Este Mes", "📊"),
    ALL_TIME("Todo", "🌟")
}

class AdminStatsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _stats = MutableStateFlow(StatsData())
    val stats: StateFlow<StatsData> = _stats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.ALL_TIME)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod

    fun setPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("AdminStatsViewModel", "════════════════════════════════")
                Log.d("AdminStatsViewModel", "📊 Cargando estadísticas - Período: ${_selectedPeriod.value.label}")

                // Calcular rango de fechas según el período
                val (startDate, endDate) = getDateRange(_selectedPeriod.value)
                Log.d("AdminStatsViewModel", "📅 Rango: ${startDate?.toDate()} a ${endDate.toDate()}")

                // Cargar usuarios
                val usersSnapshot = firestore.collection("users").get().await()
                val users = usersSnapshot.documents.mapNotNull {
                    it.toObject(User::class.java)
                }

                val nonAdminUsers = users.filter { it.role != "admin" }
                val nonAdminUserIds = nonAdminUsers.map { it.userId }.toSet()

                Log.d("AdminStatsViewModel", "👥 Total usuarios: ${users.size}")
                Log.d("AdminStatsViewModel", "👥 Usuarios no-admin: ${nonAdminUsers.size}")
                Log.d("AdminStatsViewModel", "👥 IDs no-admin: $nonAdminUserIds")

                // Filtrar usuarios nuevos en el período
                val newUsersInPeriod = if (startDate != null) {
                    nonAdminUsers.count { user ->
                        val createdTime = user.createdAt.time
                        createdTime >= startDate.toDate().time && createdTime <= endDate.toDate().time
                    }
                } else {
                    nonAdminUsers.size
                }

                // Cargar entradas del diario
                val diaryQuery = if (startDate != null) {
                    firestore.collection("diaryEntries")
                        .whereGreaterThanOrEqualTo("timestamp", startDate)
                        .whereLessThanOrEqualTo("timestamp", endDate)
                } else {
                    firestore.collection("diaryEntries")
                }
                val diarySnapshot = diaryQuery.get().await()

                // ⭐ LOGS DE DEBUGGING
                Log.d("AdminStatsViewModel", "📝 Total docs en query: ${diarySnapshot.documents.size}")
                diarySnapshot.documents.forEachIndexed { index, doc ->
                    val userId = doc.getString("userId")
                    val timestamp = doc.getTimestamp("timestamp")
                    val isNonAdmin = userId in nonAdminUserIds
                    Log.d("AdminStatsViewModel", "   [$index] userId: $userId, timestamp: ${timestamp?.toDate()}, isNonAdmin: $isNonAdmin")
                }

                // ⭐ FILTRAR solo diarios de usuarios no-admin
                val diaryEntriesInPeriod = diarySnapshot.documents.count { doc ->
                    val userId = doc.getString("userId")
                    userId in nonAdminUserIds
                }

                Log.d("AdminStatsViewModel", "📝 Diarios después del filtro: $diaryEntriesInPeriod")

                // Cargar comidas
                val foodQuery = if (startDate != null) {
                    firestore.collectionGroup("foodHistory")
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                } else {
                    firestore.collectionGroup("foodHistory")
                }
                val foodHistorySnapshot = foodQuery.get().await()

                // ⭐ FILTRAR solo comidas de usuarios no-admin
                val mealsInPeriod = foodHistorySnapshot.documents.count { doc ->
                    val userId = doc.reference.parent.parent?.id
                    userId in nonAdminUserIds
                }

                // ⭐ Cargar juegos jugados
                val gamesQuery = if (startDate != null) {
                    firestore.collectionGroup("gameResults")
                        .whereGreaterThanOrEqualTo("date", startDate)
                        .whereLessThanOrEqualTo("date", endDate)
                } else {
                    firestore.collectionGroup("gameResults")
                }
                val gamesSnapshot = gamesQuery.get().await()

                // ⭐ LOGS DE DEBUGGING PARA JUEGOS
                Log.d("AdminStatsViewModel", "🎮 Total docs de juegos en query: ${gamesSnapshot.documents.size}")
                gamesSnapshot.documents.forEachIndexed { index, doc ->
                    val userId = doc.reference.parent.parent?.id
                    val timestamp = doc.getTimestamp("date")
                    val isNonAdmin = userId in nonAdminUserIds
                    Log.d("AdminStatsViewModel", "   [$index] userId: $userId, date: ${timestamp?.toDate()}, isNonAdmin: $isNonAdmin")
                }

                // ⭐ FILTRAR solo juegos de usuarios no-admin
                val gamesPlayedInPeriod = gamesSnapshot.documents.count { doc ->
                    val userId = doc.reference.parent.parent?.id
                    userId in nonAdminUserIds
                }

                Log.d("AdminStatsViewModel", "🎮 Juegos después del filtro: $gamesPlayedInPeriod")
                Log.d("AdminStatsViewModel", "📝 Diarios filtrados: $diaryEntriesInPeriod")
                Log.d("AdminStatsViewModel", "🍽️ Comidas filtradas: $mealsInPeriod")

                // Calcular usuarios más activos
                val topUsers = calculateTopUsers(
                    users = nonAdminUsers,
                    diaryDocs = diarySnapshot.documents,
                    foodDocs = foodHistorySnapshot.documents,
                    gameDocs = gamesSnapshot.documents
                )

                // Actividad por día (últimos 7 días) - también filtrar
                val activityByDay = calculateActivityByDay(
                    diarySnapshot.documents.filter { doc ->
                        doc.getString("userId") in nonAdminUserIds
                    }
                )

                // Estadísticas generales
                val totalUsers = nonAdminUsers.size
                val activeUsers = nonAdminUsers.count { it.active }
                val inactiveUsers = nonAdminUsers.count { !it.active }
                val childUsers = nonAdminUsers.count { it.isChild() }
                val adultUsers = nonAdminUsers.count { !it.isChild() }

                // ⭐ Para totales generales también filtrar
                val totalDiarySnapshot = firestore.collection("diaryEntries").get().await()
                val totalDiaryEntries = totalDiarySnapshot.documents.count { doc ->
                    doc.getString("userId") in nonAdminUserIds
                }

                val totalFoodSnapshot = firestore.collectionGroup("foodHistory").get().await()
                val totalMeals = totalFoodSnapshot.documents.count { doc ->
                    doc.reference.parent.parent?.id in nonAdminUserIds
                }

                val averageMeals = if (totalUsers > 0) {
                    totalMeals.toDouble() / totalUsers
                } else {
                    0.0
                }

                val mostActiveUser = topUsers.firstOrNull()?.name ?: "N/A"

                val lastAccess = nonAdminUsers
                    .maxByOrNull { it.createdAt.time }
                    ?.createdAt
                    ?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
                    ?: "N/A"

                _stats.value = StatsData(
                    totalUsers = totalUsers,
                    activeUsers = activeUsers,
                    inactiveUsers = inactiveUsers,
                    childUsers = childUsers,
                    adultUsers = adultUsers,
                    diaryEntries = totalDiaryEntries,
                    totalMealsLogged = totalMeals,
                    averageMealsPerUser = averageMeals,
                    mostActiveUser = mostActiveUser,
                    lastAccessDate = lastAccess,
                    newUsersInPeriod = newUsersInPeriod,
                    diaryEntriesInPeriod = diaryEntriesInPeriod,
                    mealsInPeriod = mealsInPeriod,
                    gamesPlayedInPeriod = gamesPlayedInPeriod,
                    topUsers = topUsers,
                    activityByDay = activityByDay
                )

                Log.d("AdminStatsViewModel", "✅ Estadísticas cargadas")
                Log.d("AdminStatsViewModel", "   Período: ${_selectedPeriod.value.label}")
                Log.d("AdminStatsViewModel", "   Nuevos usuarios: $newUsersInPeriod")
                Log.d("AdminStatsViewModel", "   Entradas diario: $diaryEntriesInPeriod")
                Log.d("AdminStatsViewModel", "   Comidas: $mealsInPeriod")
                Log.d("AdminStatsViewModel", "   Juegos: $gamesPlayedInPeriod")
                Log.d("AdminStatsViewModel", "════════════════════════════════")

            } catch (e: Exception) {
                Log.e("AdminStatsViewModel", "❌ ERROR:", e)
                e.printStackTrace()
                _stats.value = StatsData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getDateRange(period: StatsPeriod): Pair<Timestamp?, Timestamp> {
        val calendar = Calendar.getInstance()

        // Fin del período es AHORA
        val endDate = Timestamp.now()

        val startDate = when (period) {
            StatsPeriod.TODAY -> {
                // Inicio del día de hoy (00:00:00)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time)
            }
            StatsPeriod.WEEK -> {
                // Hace 7 días desde ahora
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time)
            }
            StatsPeriod.MONTH -> {
                // Hace 1 mes desde ahora
                calendar.time = Date()
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time)
            }
            StatsPeriod.ALL_TIME -> null
        }

        Log.d("AdminStatsViewModel", "📅 StartDate: ${startDate?.toDate()}")
        Log.d("AdminStatsViewModel", "📅 EndDate: ${endDate.toDate()}")

        return Pair(startDate, endDate)
    }

    private fun calculateTopUsers(
        users: List<User>,
        diaryDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        foodDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        gameDocs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): List<TopUserData> {
        val userActivity = mutableMapOf<String, Triple<Int, Int, Int>>()

        // Contar actividad por usuario
        users.forEach { user ->
            val userId = user.userId
            val diaryCount = diaryDocs.count { it.getString("userId") == userId }
            val mealsCount = foodDocs.count {
                it.reference.parent.parent?.id == userId
            }
            val gamesCount = gameDocs.count {
                it.reference.parent.parent?.id == userId
            }

            if (diaryCount + mealsCount + gamesCount > 0) {
                userActivity[user.name] = Triple(mealsCount, diaryCount, gamesCount)
            }
        }

        return userActivity.entries
            .sortedByDescending { it.value.first + it.value.second + it.value.third }
            .take(5)
            .map { (name, counts) ->
                TopUserData(
                    name = name,
                    mealsCount = counts.first,
                    diaryCount = counts.second,
                    gamesCount = counts.third
                )
            }
    }

    private fun calculateActivityByDay(
        diaryDocs: List<com.google.firebase.firestore.DocumentSnapshot>
    ): Map<String, Int> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val activityMap = mutableMapOf<String, Int>()

        // Últimos 7 días
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dayName = dateFormat.format(calendar.time)

            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val dayStart = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val dayEnd = calendar.timeInMillis

            val count = diaryDocs.count { doc ->
                val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0
                timestamp in dayStart..dayEnd
            }

            activityMap[dayName] = count
        }

        return activityMap
    }
}