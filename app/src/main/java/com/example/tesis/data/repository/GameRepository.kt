// data/repository/GameRepository.kt
package com.example.tesis.data.repository

import android.util.Log
import com.example.tesis.ui.components.GameResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class GameRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "GameRepository"
    }

    // ✅ NUEVO: Guardar cada partida como un documento único
    suspend fun saveGameResult(gameId: String, result: GameResult) {
        Log.d(TAG, "=== saveGameResult INICIADO ===")

        try {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Log.e(TAG, "❌ Usuario no autenticado en Firebase Auth")
                throw Exception("Usuario no autenticado")
            }

            Log.d(TAG, "✅ Usuario autenticado: $userId")
            Log.d(TAG, "GameId: $gameId")

            // ✅ CAMBIO: Usar .add() en lugar de .document(gameId).set()
            // Esto crea un documento único cada vez con ID automático
            val gameResultRef = db.collection("users")
                .document(userId)
                .collection("gameResults")
                .add(result) // ← ¡Crea un documento nuevo cada vez!
                .await()

            Log.d(TAG, "✅✅✅ Partida guardada con ID: ${gameResultRef.id}")
            Log.d(TAG, "✅✅✅ GameId: $gameId, Score: ${result.score}")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ ERROR AL GUARDAR", e)
            Log.e(TAG, "Mensaje: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            Log.d(TAG, "=== saveGameResult FINALIZADO ===")
        }
    }

    // ✅ Obtener el MEJOR resultado de un juego específico
    suspend fun getBestGameResult(gameId: String): GameResult? {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            Log.d(TAG, "🏆 Obteniendo MEJOR resultado para gameId: $gameId")

            val snapshot = db.collection("users")
                .document(userId)
                .collection("gameResults")
                .whereEqualTo("gameId", gameId)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val result = snapshot.documents.firstOrNull()?.toObject(GameResult::class.java)
            Log.d(TAG, "🏆 Mejor resultado: ${result?.score ?: "ninguno"}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener mejor resultado: ${e.message}", e)
            null
        }
    }

    // ✅ Obtener TODAS las partidas de TODOS los juegos
    suspend fun getAllGameResults(): List<GameResult> {
        return try {
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Log.e(TAG, "❌ Usuario no autenticado al obtener resultados")
                return emptyList()
            }

            Log.d(TAG, "📚 Obteniendo TODOS los resultados para userId: $userId")

            val snapshot = db.collection("users")
                .document(userId)
                .collection("gameResults")
                .orderBy("date", Query.Direction.DESCENDING) // ← Ordenar por fecha
                .get()
                .await()

            val results = snapshot.documents.mapNotNull { doc ->
                Log.d(TAG, "📄 Documento encontrado: ${doc.id}")
                try {
                    doc.toObject(GameResult::class.java)?.also {
                        Log.d(TAG, "   ✅ GameId: ${it.gameId}, Score: ${it.score}, Date: ${it.date}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "   ❌ Error al parsear documento ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "📚 Total de partidas obtenidas: ${results.size}")

            results
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener todos los resultados: ${e.message}", e)
            emptyList()
        }
    }

    // ✅ Obtener el historial completo de un juego específico
    suspend fun getGameHistory(gameId: String): List<GameResult> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            Log.d(TAG, "📜 Obteniendo historial para gameId: $gameId")

            val snapshot = db.collection("users")
                .document(userId)
                .collection("gameResults")
                .whereEqualTo("gameId", gameId)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()

            val history = snapshot.documents.mapNotNull {
                it.toObject(GameResult::class.java)
            }

            Log.d(TAG, "📜 Partidas encontradas: ${history.size}")

            history
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener historial: ${e.message}", e)
            emptyList()
        }
    }

    // ✅ NUEVO: Obtener partidas de hoy para un juego específico
    suspend fun getTodayGameResults(gameId: String): List<GameResult> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")

            // Calcular inicio del día de hoy
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = Timestamp(calendar.time)

            Log.d(TAG, "📅 Buscando partidas de hoy para $gameId desde: ${startOfDay.toDate()}")

            val snapshot = db.collection("users")
                .document(userId)
                .collection("gameResults")
                .whereEqualTo("gameId", gameId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .get()
                .await()

            val todayResults = snapshot.documents.mapNotNull {
                it.toObject(GameResult::class.java)
            }

            Log.d(TAG, "📅 Partidas de hoy encontradas: ${todayResults.size}")

            todayResults
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener partidas de hoy: ${e.message}", e)
            emptyList()
        }
    }
}