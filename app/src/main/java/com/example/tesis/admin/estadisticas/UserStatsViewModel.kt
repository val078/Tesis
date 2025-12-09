package com.example.tesis.admin.estadisticas

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.admin.ia.AIRecommendationLog
import com.example.tesis.data.model.User
import com.example.tesis.data.model.GameResult
import com.example.tesis.data.viewmodel.AchievementData
import com.example.tesis.ui.screens.achievements.calculateAllAchievements // ⭐ Importar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class UserStatsData(
    val user: User? = null,
    val diaryEntries: Int = 0,
    val lastDiaryDate: String = "N/A",
    val lastDiaryContent: String? = null,
    val lastDiaryEmotion: String? = null,
    val entriesThisWeek: Int = 0,
    val entriesThisMonth: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Int = 0,
    val gameStats: List<UserGameStat> = emptyList(),
    val achievements: List<AchievementData> = emptyList(), // ⭐ Usar AchievementData de achievements
    val level: Int = 1,
    val lastGameDate: String = "N/A",
    val aiLogs: List<AIRecommendationLog> = emptyList(),
    val totalAIInteractions: Int = 0,
    val lastAIInteraction: String = "N/A"
)

data class UserGameStat(
    val gameId: String,
    val gameName: String,
    val emoji: String,
    val gamesPlayed: Int,
    val bestScore: Int,
    val averageScore: Int,
    val accuracy: Int
)

class UserStatsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _userStats = MutableStateFlow(UserStatsData())
    val userStats: StateFlow<UserStatsData> = _userStats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val gameMetadata = mapOf(
        "drip_and_drop" to Pair("Arrastra y Suelta", "🎯"),
        "memory_game" to Pair("Memoria", "🧠"),
        "pregunton" to Pair("Preguntón", "❓"),
        "nutri_plate" to Pair("NutriChef", "📝")
    )

    fun loadUserStats(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("UserStatsViewModel", "════════════════════════════════")
                Log.d("UserStatsViewModel", "📊 Cargando stats para usuario: $userId")

                // Cargar usuario
                val userDoc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                Log.d("UserStatsViewModel", "👤 Usuario: ${user?.name}")

                // Cargar entradas del diario
                Log.d("UserStatsViewModel", "📖 Cargando diario...")
                val diarySnapshot = firestore.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val diaryEntries = diarySnapshot.size()
                Log.d("UserStatsViewModel", "✅ Entradas del diario: $diaryEntries")

                // Última entrada completa
                val lastDiaryDoc = diarySnapshot.documents.firstOrNull()
                val lastDiaryDate = lastDiaryDoc?.getTimestamp("timestamp")
                    ?.let {
                        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        formatter.format(it.toDate())
                    } ?: "N/A"

                val lastDiaryContent = lastDiaryDoc?.getString("description")
                val lastDiaryEmotion = lastDiaryDoc?.getString("sticker")

                Log.d("UserStatsViewModel", "📝 Última entrada: $lastDiaryDate")

                // ⭐ NUEVO: Cargar logs de IA
                Log.d("UserStatsViewModel", "🤖 Cargando logs de IA...")
                val aiLogsSnapshot = firestore.collection("aiLogs")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                val aiLogs = aiLogsSnapshot.documents.mapNotNull {
                    it.toObject(AIRecommendationLog::class.java)?.copy(id = it.id)
                }

                val totalAIInteractions = aiLogs.size
                val lastAIInteraction = aiLogs.firstOrNull()?.timestamp
                    ?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it.toDate()) }
                    ?: "N/A"

                Log.d("UserStatsViewModel", "🤖 Interacciones con IA: $totalAIInteractions")
                Log.d("UserStatsViewModel", "🤖 Última interacción: $lastAIInteraction")

                // Cargar resultados de juegos
                Log.d("UserStatsViewModel", "🎮 Cargando resultados de juegos...")
                val gameResultsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("gameResults")
                    .get()
                    .await()

                val gameResults = gameResultsSnapshot.documents.mapNotNull {
                    it.toObject(GameResult::class.java)
                }
                Log.d("UserStatsViewModel", "✅ Partidas encontradas: ${gameResults.size}")

                // Cargar estado de Mr. Pollo
                Log.d("UserStatsViewModel", "🐔 Cargando estado de Mr. Pollo...")
                val polloDoc = firestore.collection("users")
                    .document(userId)
                    .collection("mrPollo")
                    .document("state")
                    .get()
                    .await()

                val currentStreak = polloDoc.getLong("currentStreak")?.toInt() ?: 0
                val longestStreak = polloDoc.getLong("longestStreak")?.toInt() ?: 0
                val happinessLevel = polloDoc.getLong("happinessLevel")?.toInt() ?: 0

                Log.d("UserStatsViewModel", "🐔 Racha actual: $currentStreak, Mejor racha: $longestStreak, Felicidad: $happinessLevel")

                // Calcular estadísticas de juegos
                val totalGamesPlayed = gameResults.size
                val totalScore = gameResults.sumOf { it.score }
                val averageScore = if (totalGamesPlayed > 0) totalScore / totalGamesPlayed else 0
                val level = (totalScore / 500).coerceAtLeast(1)

                val perfectGames = gameResults.count {
                    it.totalQuestions > 0 && it.correctAnswers == it.totalQuestions
                }

                // Juegos jugados hoy
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDayMillis = calendar.timeInMillis
                val nowMillis = System.currentTimeMillis()

                val gamesPlayedToday = gameResults.count { result ->
                    val timestampMillis = result.date.toDate().time
                    timestampMillis >= startOfDayMillis && timestampMillis <= nowMillis
                }

                // Última partida jugada
                val lastGame = gameResults
                    .maxByOrNull { it.date.toDate().time }
                    ?.date
                    ?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it.toDate()) }
                    ?: "N/A"

                // Estadísticas por juego
                val gameStats = calculateGameStats(gameResults)

                // Calcular logros
                val achievements = calculateAllAchievements(
                    totalScore = totalScore,
                    totalGames = totalGamesPlayed,
                    gamesPlayedToday = gamesPlayedToday,
                    perfectGames = perfectGames,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    happinessLevel = happinessLevel,
                    level = level
                )

                Log.d("UserStatsViewModel", "🏅 Logros desbloqueados: ${achievements.count { it.unlocked }}/${achievements.size}")

                // Entradas esta semana
                calendar.time = Date()
                val startOfWeek = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val entriesThisWeek = diarySnapshot.documents.count { doc ->
                    val date = doc.getTimestamp("timestamp")?.toDate()
                    date != null && date.after(startOfWeek)
                }

                // Entradas este mes
                calendar.time = Date()
                val startOfMonth = calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val entriesThisMonth = diarySnapshot.documents.count { doc ->
                    val date = doc.getTimestamp("timestamp")?.toDate()
                    date != null && date.after(startOfMonth)
                }

                _userStats.value = UserStatsData(
                    user = user,
                    diaryEntries = diaryEntries,
                    lastDiaryDate = lastDiaryDate,
                    lastDiaryContent = lastDiaryContent,
                    lastDiaryEmotion = lastDiaryEmotion,
                    entriesThisWeek = entriesThisWeek,
                    entriesThisMonth = entriesThisMonth,
                    totalGamesPlayed = totalGamesPlayed,
                    totalScore = totalScore,
                    averageScore = averageScore,
                    gameStats = gameStats,
                    achievements = achievements,
                    level = level,
                    lastGameDate = lastGame,
                    // ⭐ NUEVO
                    aiLogs = aiLogs,
                    totalAIInteractions = totalAIInteractions,
                    lastAIInteraction = lastAIInteraction
                )

                Log.d("UserStatsViewModel", "✅ STATS CARGADAS CORRECTAMENTE")
                Log.d("UserStatsViewModel", "════════════════════════════════")

            } catch (e: Exception) {
                Log.e("UserStatsViewModel", "❌ ERROR:", e)
                e.printStackTrace()
                _userStats.value = UserStatsData()
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun calculateGameStats(gameResults: List<GameResult>): List<UserGameStat> {
        val resultsByGame = gameResults.groupBy { it.gameId }

        return gameMetadata.map { (gameId, metadata) ->
            val (gameName, emoji) = metadata
            val results = resultsByGame[gameId] ?: emptyList()

            if (results.isEmpty()) {
                UserGameStat(
                    gameId = gameId,
                    gameName = gameName,
                    emoji = emoji,
                    gamesPlayed = 0,
                    bestScore = 0,
                    averageScore = 0,
                    accuracy = 0
                )
            } else {
                val gamesPlayed = results.size
                val bestScore = results.maxOf { it.score }
                val averageScore = results.sumOf { it.score } / gamesPlayed
                val totalCorrect = results.sumOf { it.correctAnswers }
                val totalQuestions = results.sumOf { it.totalQuestions }
                val accuracy = if (totalQuestions > 0) {
                    ((totalCorrect.toDouble() / totalQuestions) * 100).toInt()
                } else 0

                UserGameStat(
                    gameId = gameId,
                    gameName = gameName,
                    emoji = emoji,
                    gamesPlayed = gamesPlayed,
                    bestScore = bestScore,
                    averageScore = averageScore,
                    accuracy = accuracy
                )
            }
        }
    }
}