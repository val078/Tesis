// utils/TutorialManager.kt
package com.example.tesis.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object TutorialManager {
    private const val TAG = "TutorialManager"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Cache en memoria para evitar múltiples lecturas
    private val tutorialCache = mutableMapOf<String, Boolean>()

    /**
     * Verifica si el usuario ya vio el tutorial de un juego específico
     */
    suspend fun hasSeen(gameId: String): Boolean {
        try {
            val userId = auth.currentUser?.uid ?: return false

            // Verificar cache primero
            if (tutorialCache.containsKey(gameId)) {
                Log.d(TAG, "📚 Tutorial $gameId: ${tutorialCache[gameId]} (desde cache)")
                return tutorialCache[gameId] ?: false
            }

            // Si no está en cache, consultar Firestore
            val doc = firestore.collection("users")
                .document(userId)
                .collection("tutorials")
                .document(gameId)
                .get()
                .await()

            val seen = doc.exists() && doc.getBoolean("seen") == true

            // Guardar en cache
            tutorialCache[gameId] = seen

            Log.d(TAG, "📚 Tutorial $gameId visto: $seen")
            return seen

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al verificar tutorial: ${e.message}", e)
            return false
        }
    }

    /**
     * Marca el tutorial de un juego como visto
     */
    suspend fun markAsSeen(gameId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return

            Log.d(TAG, "✅ Marcando tutorial $gameId como visto")

            firestore.collection("users")
                .document(userId)
                .collection("tutorials")
                .document(gameId)
                .set(
                    mapOf(
                        "seen" to true,
                        "firstSeenAt" to com.google.firebase.Timestamp.now(),
                        "gameId" to gameId
                    )
                )
                .await()

            // Actualizar cache
            tutorialCache[gameId] = true

            Log.d(TAG, "✅ Tutorial $gameId marcado como visto")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al marcar tutorial: ${e.message}", e)
        }
    }

    /**
     * Resetear el tutorial de un juego (útil para testing)
     */
    suspend fun reset(gameId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return

            Log.d(TAG, "🔄 Reseteando tutorial $gameId")

            firestore.collection("users")
                .document(userId)
                .collection("tutorials")
                .document(gameId)
                .delete()
                .await()

            // Limpiar cache
            tutorialCache.remove(gameId)

            Log.d(TAG, "✅ Tutorial $gameId reseteado")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al resetear tutorial: ${e.message}", e)
        }
    }

    /**
     * Resetear TODOS los tutoriales (útil para testing)
     */
    suspend fun resetAll() {
        try {
            val userId = auth.currentUser?.uid ?: return

            Log.d(TAG, "🔄 Reseteando TODOS los tutoriales")

            val docs = firestore.collection("users")
                .document(userId)
                .collection("tutorials")
                .get()
                .await()

            docs.documents.forEach { it.reference.delete().await() }

            // Limpiar cache
            tutorialCache.clear()

            Log.d(TAG, "✅ Todos los tutoriales reseteados")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al resetear tutoriales: ${e.message}", e)
        }
    }

    /**
     * Limpiar cache (útil al cerrar sesión)
     */
    fun clearCache() {
        tutorialCache.clear()
        Log.d(TAG, "🧹 Cache de tutoriales limpiado")
    }
}